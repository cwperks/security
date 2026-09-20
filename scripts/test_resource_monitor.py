#!/usr/bin/env python3

# SPDX-License-Identifier: Apache-2.0
#
# Copyright OpenSearch Contributors

"""Run a command while sampling its Linux process-tree resource usage."""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import subprocess
import sys
import threading
import time
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, Optional, Sequence, Set, TextIO


LOG_PATTERNS = {
    "gc_overhead": re.compile(r"JvmGcMonitorService.*\[gc\].*overhead", re.IGNORECASE),
    "circuit_breaker": re.compile(r"circuit_breaking_exception|CircuitBreakingException", re.IGNORECASE),
    "out_of_memory": re.compile(r"OutOfMemoryError|Java heap space", re.IGNORECASE),
    "heap_dump": re.compile(r"Dumping heap to|Heap dump file created", re.IGNORECASE),
}


@dataclass(frozen=True)
class ProcessIdentity:
    pid: int
    start_ticks: int


@dataclass
class ProcessInfo:
    identity: ProcessIdentity
    ppid: int
    process_group: int
    rss_bytes: int
    cpu_seconds: float
    threads: int
    command: str


def parse_stat_line(line: str, page_size: int, clock_ticks: int) -> ProcessInfo:
    """Parse one /proc/<pid>/stat line, including commands containing spaces."""
    command_start = line.find("(")
    command_end = line.rfind(")")
    if command_start < 0 or command_end < command_start:
        raise ValueError("invalid proc stat line")

    pid = int(line[:command_start].strip())
    command = line[command_start + 1 : command_end]
    fields = line[command_end + 2 :].split()
    if len(fields) < 22:
        raise ValueError("proc stat line has too few fields")

    return ProcessInfo(
        identity=ProcessIdentity(pid=pid, start_ticks=int(fields[19])),
        ppid=int(fields[1]),
        process_group=int(fields[2]),
        rss_bytes=max(0, int(fields[21])) * page_size,
        cpu_seconds=(int(fields[11]) + int(fields[12])) / clock_ticks,
        threads=int(fields[17]),
        command=command,
    )


def read_process_table(
    proc_root: Path = Path("/proc"),
    uid: Optional[int] = None,
    page_size: Optional[int] = None,
    clock_ticks: Optional[int] = None,
) -> Dict[ProcessIdentity, ProcessInfo]:
    """Read process information owned by uid from a Linux proc filesystem."""
    if uid is None:
        uid = os.getuid()
    if page_size is None:
        page_size = os.sysconf("SC_PAGE_SIZE")
    if clock_ticks is None:
        clock_ticks = os.sysconf("SC_CLK_TCK")

    result: Dict[ProcessIdentity, ProcessInfo] = {}
    for entry in proc_root.iterdir():
        if not entry.name.isdigit():
            continue
        try:
            if entry.stat().st_uid != uid:
                continue
            info = parse_stat_line((entry / "stat").read_text(), page_size, clock_ticks)
            cmdline = (entry / "cmdline").read_bytes().replace(b"\0", b" ").decode("utf-8", errors="replace").strip()
            if cmdline:
                info.command = cmdline
            result[info.identity] = info
        except (FileNotFoundError, PermissionError, ProcessLookupError, ValueError):
            # Processes can disappear between listing /proc and reading a file.
            continue
    return result


def is_java_process(command: str) -> bool:
    executable = Path(command.split(" ", 1)[0]).name.lower()
    lowered = command.lower()
    return executable in {"java", "java.exe"} or "opensearch" in lowered or "gradle" in lowered


def select_processes(
    table: Dict[ProcessIdentity, ProcessInfo],
    root_pid: int,
    root_process_group: int,
    baseline: Set[ProcessIdentity],
    previously_tracked: Set[ProcessIdentity],
) -> Dict[ProcessIdentity, ProcessInfo]:
    """Select the command tree and new Java processes created during the run."""
    tracked_pids = {root_pid}
    changed = True
    while changed:
        changed = False
        for info in table.values():
            if info.ppid in tracked_pids and info.identity.pid not in tracked_pids:
                tracked_pids.add(info.identity.pid)
                changed = True

    selected: Dict[ProcessIdentity, ProcessInfo] = {}
    for identity, info in table.items():
        if (
            identity in previously_tracked
            or info.identity.pid in tracked_pids
            or info.process_group == root_process_group
            or (identity not in baseline and is_java_process(info.command))
        ):
            selected[identity] = info
    return selected


def count_log_events(log_file: Path) -> Dict[str, int]:
    counts = {name: 0 for name in LOG_PATTERNS}
    if not log_file.exists():
        return counts
    with log_file.open(errors="replace") as stream:
        for line in stream:
            for name, pattern in LOG_PATTERNS.items():
                if pattern.search(line):
                    counts[name] += 1
    return counts


def format_bytes(value: int) -> str:
    amount = float(value)
    for suffix in ("B", "KiB", "MiB", "GiB", "TiB"):
        if abs(amount) < 1024.0 or suffix == "TiB":
            return f"{amount:.1f} {suffix}"
        amount /= 1024.0
    raise AssertionError("unreachable")


def write_markdown(summary: dict, path: Path) -> None:
    events = summary["log_events"]
    lines = [
        "## Test resource observation",
        "",
        "| Metric | Value |",
        "| --- | ---: |",
        f"| Command exit code | {summary['exit_code']} |",
        f"| Wall-clock duration | {summary['elapsed_seconds']:.1f} s |",
        f"| Peak aggregate RSS | {format_bytes(summary['peak_aggregate_rss_bytes'])} |",
        f"| Process CPU time | {summary['total_cpu_seconds']:.1f} s |",
        f"| Peak monitored processes | {summary['peak_process_count']} |",
        f"| Peak Java processes | {summary['peak_java_process_count']} |",
        f"| Peak threads | {summary['peak_thread_count']} |",
        f"| Lingering processes | {len(summary['lingering_processes'])} |",
        f"| GC-overhead events | {events['gc_overhead']} |",
        f"| Circuit-breaker events | {events['circuit_breaker']} |",
        f"| Out-of-memory events | {events['out_of_memory']} |",
        f"| Heap-dump events | {events['heap_dump']} |",
        "",
        "This observer reports measurements but does not enforce resource thresholds.",
    ]
    path.write_text("\n".join(lines) + "\n")


def tee_output(stream: TextIO, destinations: Iterable[TextIO]) -> None:
    for line in iter(stream.readline, ""):
        for destination in destinations:
            destination.write(line)
            destination.flush()
    stream.close()


def monitor_command(
    command: Sequence[str],
    output_dir: Path,
    sample_interval: float,
    grace_period: float,
    proc_root: Path = Path("/proc"),
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)
    samples_path = output_dir / "samples.csv"
    log_path = output_dir / "command.log"
    summary_path = output_dir / "summary.json"
    markdown_path = output_dir / "summary.md"

    baseline = set(read_process_table(proc_root).keys())
    started_at = datetime.now(timezone.utc)
    started_monotonic = time.monotonic()

    with log_path.open("w") as log_stream, samples_path.open("w", newline="") as samples_stream:
        writer = csv.writer(samples_stream)
        writer.writerow(
            ["elapsed_seconds", "pid", "ppid", "process_group", "rss_bytes", "cpu_seconds", "threads", "command"]
        )
        process = subprocess.Popen(
            list(command),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            start_new_session=True,
        )
        assert process.stdout is not None
        output_thread = threading.Thread(target=tee_output, args=(process.stdout, (sys.stdout, log_stream)), daemon=True)
        output_thread.start()

        tracked: Set[ProcessIdentity] = set()
        max_cpu_by_process: Dict[ProcessIdentity, float] = {}
        peak_aggregate_rss = 0
        peak_process_count = 0
        peak_java_process_count = 0
        peak_thread_count = 0

        def sample() -> Dict[ProcessIdentity, ProcessInfo]:
            nonlocal peak_aggregate_rss, peak_process_count, peak_java_process_count, peak_thread_count
            table = read_process_table(proc_root)
            selected = select_processes(table, process.pid, process.pid, baseline, tracked)
            tracked.update(selected.keys())
            elapsed = time.monotonic() - started_monotonic
            for identity, info in selected.items():
                max_cpu_by_process[identity] = max(max_cpu_by_process.get(identity, 0.0), info.cpu_seconds)
                writer.writerow(
                    [
                        f"{elapsed:.3f}",
                        identity.pid,
                        info.ppid,
                        info.process_group,
                        info.rss_bytes,
                        f"{info.cpu_seconds:.3f}",
                        info.threads,
                        info.command,
                    ]
                )
            samples_stream.flush()
            peak_aggregate_rss = max(peak_aggregate_rss, sum(info.rss_bytes for info in selected.values()))
            peak_process_count = max(peak_process_count, len(selected))
            peak_java_process_count = max(peak_java_process_count, sum(is_java_process(info.command) for info in selected.values()))
            peak_thread_count = max(peak_thread_count, sum(info.threads for info in selected.values()))
            return selected

        while process.poll() is None:
            sample()
            time.sleep(sample_interval)
        exit_code = process.wait()
        sample()
        output_thread.join(timeout=10)

        grace_deadline = time.monotonic() + grace_period
        lingering: Dict[ProcessIdentity, ProcessInfo] = {}
        while time.monotonic() < grace_deadline:
            current = sample()
            lingering = {identity: info for identity, info in current.items() if identity in tracked and identity.pid != process.pid}
            if not lingering:
                break
            time.sleep(min(sample_interval, max(0.1, grace_deadline - time.monotonic())))

    elapsed_seconds = time.monotonic() - started_monotonic
    log_events = count_log_events(log_path)
    summary = {
        "schema_version": 1,
        "started_at": started_at.isoformat(),
        "command": list(command),
        "exit_code": exit_code,
        "elapsed_seconds": round(elapsed_seconds, 3),
        "sample_interval_seconds": sample_interval,
        "peak_aggregate_rss_bytes": peak_aggregate_rss,
        "total_cpu_seconds": round(sum(max_cpu_by_process.values()), 3),
        "peak_process_count": peak_process_count,
        "peak_java_process_count": peak_java_process_count,
        "peak_thread_count": peak_thread_count,
        "log_events": log_events,
        "lingering_processes": [asdict(info) for info in lingering.values()],
    }
    summary_path.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n")
    write_markdown(summary, markdown_path)
    return exit_code


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--sample-interval", type=float, default=5.0)
    parser.add_argument("--grace-period", type=float, default=10.0)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args(argv)
    if args.command and args.command[0] == "--":
        args.command = args.command[1:]
    if not args.command:
        parser.error("a command is required after --")
    if args.sample_interval <= 0 or args.grace_period < 0:
        parser.error("sample interval must be positive and grace period must not be negative")
    return args


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    if not Path("/proc").is_dir():
        print("test_resource_monitor.py requires Linux /proc", file=sys.stderr)
        return 2
    return monitor_command(args.command, args.output_dir, args.sample_interval, args.grace_period)


if __name__ == "__main__":
    raise SystemExit(main())
