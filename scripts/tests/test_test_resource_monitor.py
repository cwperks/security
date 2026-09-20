# SPDX-License-Identifier: Apache-2.0
#
# Copyright OpenSearch Contributors

from pathlib import Path
import sys
import tempfile
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import test_resource_monitor as monitor


def stat_line(pid, command, ppid, process_group, utime, stime, threads, start_ticks, rss_pages):
    fields = [
        "S",
        str(ppid),
        str(process_group),
        "0",
        "0",
        "0",
        "0",
        "0",
        "0",
        "0",
        "0",
        str(utime),
        str(stime),
        "0",
        "0",
        "0",
        "0",
        str(threads),
        "0",
        str(start_ticks),
        "0",
        str(rss_pages),
    ]
    return f"{pid} ({command}) " + " ".join(fields)


class ProcStatTests(unittest.TestCase):
    def test_parses_command_with_spaces_and_metrics(self):
        info = monitor.parse_stat_line(stat_line(41, "java worker", 12, 41, 150, 50, 7, 900, 25), 4096, 100)

        self.assertEqual(41, info.identity.pid)
        self.assertEqual(900, info.identity.start_ticks)
        self.assertEqual("java worker", info.command)
        self.assertEqual(12, info.ppid)
        self.assertEqual(41, info.process_group)
        self.assertEqual(2.0, info.cpu_seconds)
        self.assertEqual(7, info.threads)
        self.assertEqual(102400, info.rss_bytes)


class ProcessSelectionTests(unittest.TestCase):
    def process(self, pid, ppid, process_group, command, start_ticks=None):
        identity = monitor.ProcessIdentity(pid, start_ticks or pid * 10)
        return identity, monitor.ProcessInfo(identity, ppid, process_group, 1000, 1.0, 1, command)

    def test_selects_descendants_process_group_and_new_java_processes(self):
        root_id, root = self.process(10, 1, 10, "python monitor")
        child_id, child = self.process(11, 10, 10, "sh gradlew")
        grandchild_id, grandchild = self.process(12, 11, 12, "/jdk/bin/java GradleWorkerMain")
        detached_id, detached = self.process(13, 1, 13, "/jdk/bin/java org.opensearch.bootstrap.OpenSearch")
        unrelated_id, unrelated = self.process(14, 1, 14, "bash")
        old_java_id, old_java = self.process(15, 1, 15, "/jdk/bin/java existing")
        table = {
            root_id: root,
            child_id: child,
            grandchild_id: grandchild,
            detached_id: detached,
            unrelated_id: unrelated,
            old_java_id: old_java,
        }

        selected = monitor.select_processes(table, 10, 10, {old_java_id}, set())

        self.assertEqual({root_id, child_id, grandchild_id, detached_id}, set(selected))

    def test_keeps_tracking_process_after_it_is_reparented(self):
        identity, process = self.process(20, 1, 20, "sleep")

        selected = monitor.select_processes({identity: process}, 10, 10, set(), {identity})

        self.assertEqual({identity}, set(selected))


class LogAndSummaryTests(unittest.TestCase):
    def test_counts_memory_pressure_events(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "command.log"
            log.write_text(
                "JvmGcMonitorService: [gc][1] overhead, spent 900ms\n"
                "circuit_breaking_exception: data too large\n"
                "java.lang.OutOfMemoryError: Java heap space\n"
                "Dumping heap to build/heapdump/test.hprof\n"
            )

            self.assertEqual(
                {"gc_overhead": 1, "circuit_breaker": 1, "out_of_memory": 1, "heap_dump": 1},
                monitor.count_log_events(log),
            )

    def test_writes_human_readable_summary(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "summary.md"
            summary = {
                "exit_code": 0,
                "elapsed_seconds": 12.5,
                "peak_aggregate_rss_bytes": 1024 * 1024,
                "total_cpu_seconds": 3.5,
                "peak_process_count": 2,
                "peak_java_process_count": 1,
                "peak_thread_count": 12,
                "lingering_processes": [],
                "log_events": {"gc_overhead": 0, "circuit_breaker": 0, "out_of_memory": 0, "heap_dump": 0},
            }

            monitor.write_markdown(summary, output)

            text = output.read_text()
            self.assertIn("Peak aggregate RSS | 1.0 MiB", text)
            self.assertIn("Lingering processes | 0", text)


if __name__ == "__main__":
    unittest.main()
