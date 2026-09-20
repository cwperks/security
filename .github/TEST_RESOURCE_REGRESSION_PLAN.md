# Test resource regression monitoring

Status: Prototype

The first observation-only prototype is included with this document. It
provides a label/manual-triggered workflow around `dlicRestApiTest`, samples
the Linux process tree, publishes summary and raw artifacts, and does not
enforce resource thresholds. Paired merge-base/head execution remains a
follow-up after the observer has been validated.

## Context

Issue [#5915](https://github.com/opensearch-project/security/issues/5915)
showed that tests can pass while progressively exhausting the CI runner. Two
different problems contributed to that incident:

- Password4j retained large Argon2 function instances in a static cache. The
  integration tests created enough parameter combinations to retain roughly
  600 MB. This was addressed by
  [#5923](https://github.com/opensearch-project/security/pull/5923).
- Config REST API tests shared mutable static cluster references across test
  classes. Parallel execution could overwrite an active reference before its
  cluster was stopped, leaving the old cluster alive. This was addressed by
  [#5934](https://github.com/opensearch-project/security/pull/5934).

Both defects appeared first as long-running tests, garbage-collection overhead,
circuit-breaker responses, and eventually timeouts. The existing CI reports
test results and coverage, but it does not preserve enough resource data to
show when a pull request introduces this pattern.

## Goals

- Show the incremental memory, CPU, and elapsed-time cost of test changes on a
  pull request.
- Detect strong leak indicators such as orphaned OpenSearch processes,
  sustained memory growth, GC overhead, circuit-breaker responses, and OOMs.
- Compare the pull request with its merge base on the same runner to reduce
  host-to-host noise.
- Preserve raw measurements for investigation while presenting a short summary
  to reviewers.
- Collect data before deciding which measurements are stable enough to block a
  pull request.

## Non-goals

- Treat every increase caused by adding a test as a product performance
  regression. A new test necessarily consumes some CPU and time.
- Replace focused microbenchmarks or end-to-end OpenSearch benchmarks for
  production performance-sensitive code.
- Automatically upload heap dumps, which may contain sensitive test data and
  are too large for routine artifact storage.
- Make measurements from different GitHub-hosted runners directly comparable
  without accounting for runner variance.

## Signals to collect

The first implementation should collect the following signals for the complete
process tree, not only the Gradle client process. Integration tests also start
Gradle test workers and OpenSearch node JVMs.

| Signal | Purpose |
| --- | --- |
| Aggregate and per-process peak RSS | Detect heap and native-memory growth across all JVMs |
| Process CPU time | Quantify the computational cost of the test suite |
| Wall-clock duration | Identify slower tests and operational CI impact |
| Live Java/OpenSearch process count | Detect clusters or workers that were not stopped |
| GC-overhead warnings | Detect sustained allocation or retention pressure |
| Circuit-breaker, OOM, and heap-dump events | Detect severe memory exhaustion |
| Per-class test duration and retry count | Attribute time regressions and distinguish retry noise |

Where possible, samples should include a timestamp, PID, parent PID, command,
RSS, CPU ticks, and thread count. The raw format should be machine-readable,
such as CSV or newline-delimited JSON.

## Proposed CI design

### Pull-request comparison

Use a dedicated `test-resource-regression` workflow on Ubuntu with JDK 21. The
job should:

1. Resolve the pull request merge base.
2. Build the merge base once so dependency downloads and compilation do not
   dominate the measurement.
3. Run the selected test task for the merge base under the resource monitor.
4. Check out the pull request head without changing runners.
5. Run the same task, JVM, random seed, parallelism, and Gradle options under
   the same monitor.
6. Compare the two result files and write a table to the GitHub job summary.
7. Upload the raw samples, comparison JSON, Gradle test results, and relevant
   logs as workflow artifacts.

Running base and head sequentially on one runner is more useful than comparing
the pull request with a historical run from another host. It does not remove
all noise, but it controls the largest environmental differences.

The initial task should be `integrationTest` with retries disabled. Retrying a
test changes CPU, memory, and elapsed-time totals and can hide the first failure
that caused resource pressure. Once the harness is stable, the same monitor can
wrap the split integration-test tasks.

### Scope and triggering

Running the full comparison for every documentation or configuration-only pull
request would waste CI capacity. Initially, run it when a pull request changes
one of these areas:

- `src/integrationTest/**`
- the test framework or cluster lifecycle helpers
- `build.gradle`, `gradle/**`, or test dependencies
- production Java code used by the integration tests

Also support `workflow_dispatch` and a pull-request label such as
`run-resource-regression` so maintainers can request it for any change. A
nightly scheduled run on `main` can establish longer-term trends independently
of pull requests.

### Monitoring implementation

The first monitor can be a small repository-owned Linux script. It should use
`/proc` and cgroup data rather than adding another third-party action. It would:

- launch the Gradle command;
- discover descendants of the Gradle process and OpenSearch JVMs created by
  the test framework;
- sample those processes at a fixed interval, such as five seconds;
- retain the maximum values for exited processes;
- wait for the Gradle command and preserve its exit status;
- record any relevant warnings found in the test and cluster logs; and
- verify that no test-created OpenSearch process remains after the task.

The proposed repository layout is:

```text
.github/workflows/test-resource-regression.yml
scripts/test-resource-monitor.sh
scripts/compare-test-resources.py
build/test-resource-usage/
  base-samples.csv
  head-samples.csv
  comparison.json
```

The generated `build/test-resource-usage` directory must remain untracked and
should be uploaded only as a CI artifact.

### Class-boundary correlation

Sampling the entire process tree identifies large regressions but does not
always identify the responsible class. A later increment can add a Gradle test
listener that writes suite/class completion timestamps to a marker file. Those
timestamps can be correlated with the process samples without trying to read
the test worker's heap from the Gradle daemon.

This is particularly useful for the #5915 failure mode: reviewers could see
that aggregate memory did not return after `SnapshotAuthorizationIntTests` or
`RolesRestApiIntegrationTest` completed.

## Reporting

The pull-request job summary should remain short. For example:

```text
Metric                  Base       PR         Change
Peak aggregate RSS      6.2 GB     7.1 GB     +14.5%
Process CPU time        22m 10s    24m 02s    +8.4%
Integration wall time   18m 42s    20m 01s    +7.0%
Peak Java processes     11         14         +3
GC-overhead events      0          18         regression
Orphan processes        0          1          regression
```

The summary should list the slowest changed test classes and link to the raw
artifact. A test added by the pull request should be labelled as new rather than
shown as an infinite percentage increase.

Artifacts are useful for initial evaluation, but they are not a permanent
metrics database. If long-term trends prove valuable, comparison JSON from
scheduled `main` runs should be published to a durable OpenSearch index, object
store, or other project-owned metrics system.

## Diagnostics on anomalies

Java Flight Recorder can provide allocation, garbage-collection, thread, and
CPU evidence with low overhead when using its continuous/default settings. It
should not be the primary comparison metric because recordings are harder to
aggregate across multiple JVMs.

A later phase can start a bounded JFR recording for each discovered JVM by
using `jcmd` and a unique PID-based filename. To control artifact size:

- use continuous/default settings and a maximum recording size;
- retain recordings only when a threshold is crossed or the test fails; and
- never collect or upload an automatic heap dump by default.

On a memory anomaly, the workflow may additionally collect a class histogram
from a still-running JVM. This should be diagnostic-only because obtaining a
histogram can pause the process and affect the measurement.

## Thresholds and rollout

### Phase 1: Observe

- Run manually or by label.
- Never fail the pull request because of a resource delta.
- Publish summaries and artifacts.
- Gather enough paired samples to understand normal variance.

### Phase 2: Warn

After calibration, add non-blocking warnings. Candidate starting thresholds
are:

- peak RSS increases by both more than 15 percent and more than 256 MB;
- CPU time increases by both more than 20 percent and more than 60 seconds;
- wall time increases by more than 25 percent;
- memory usage has a sustained positive slope in the latter half of the run;
  or
- the number of concurrently live Java processes unexpectedly increases.

Requiring both a percentage and an absolute increase avoids noisy warnings for
small tasks.

### Phase 3: Enforce strong invariants

Only high-confidence signals should block a pull request:

- a test-created OpenSearch or worker process remains alive after the task;
- an OOM or heap-dump event occurs;
- a new GC-overhead or memory circuit-breaker event occurs; or
- a repeatedly reproduced resource increase exceeds a calibrated hard limit.

CPU and wall-clock changes should remain advisory until measurements are made
on stable, isolated runners. Shared hosted-runner scheduling can produce false
positives.

## Product performance versus test cost

This workflow measures the cost and lifecycle behavior of the test suite. It
does not prove that production authorization code became faster or slower.

For production code on hot paths, use separate benchmarks:

- JMH microbenchmarks for deterministic units such as privilege evaluation,
  wildcard matching, or role compilation; and
- an OpenSearch Benchmark workload for end-to-end request throughput, latency,
  CPU, and heap behavior with the Security plugin enabled.

Those benchmarks can use the same base-versus-head reporting format but should
run separately from the test-resource workflow.

## Security considerations

- Pull-request code is untrusted. Do not expose cloud credentials or other
  repository secrets to the measurement job.
- Do not run untrusted pull-request code on a persistent self-hosted runner.
  Any future dedicated runner must be ephemeral and isolated.
- If a trusted follow-up workflow comments on a pull request, treat downloaded
  artifacts as untrusted input and validate the JSON schema and pull-request
  number before using them.
- Do not publish heap dumps. JFR and logs must still be reviewed for sensitive
  test data before their retention is expanded.

## Estimated CI cost

A paired comparison executes the chosen test task twice. Based on current
integration-test durations, a full comparison could add roughly 40 to 60
runner-minutes to an affected pull request. Label-gated rollout is therefore
preferred before enabling path-based automatic execution.

The monitor itself should have negligible cost compared with the tests. JFR is
optional and should be collected only for anomalous or failed runs.

## Implementation sequence

1. Add the Linux process monitor and unit-test its parsing and aggregation with
   recorded `/proc` fixtures.
2. Add a manually triggered workflow that measures one `integrationTest` run
   and uploads the raw result.
3. Add paired merge-base/head execution and a job-summary comparison.
4. Run the workflow by label on representative pull requests, including a
   controlled test that intentionally retains memory or leaves a process alive.
5. Calibrate warning thresholds from the paired results.
6. Add class-boundary markers and anomaly-only JFR capture if the aggregate
   signal does not provide enough attribution.
7. Consider scheduled `main` trends and narrowly scoped blocking invariants.

## Open questions

- Should the first measured task be the complete `integrationTest` task or one
  of the split CI tasks with a history of memory pressure?
- What additional runner cost is acceptable per pull request?
- Should resource testing start label-only, path-filtered, or nightly-only?
- Where should metrics live after GitHub artifact retention expires?
- Which process and log patterns are reliable enough to become blocking
  invariants?

## References

- [Issue #5915: Integration test issues](https://github.com/opensearch-project/security/issues/5915)
- [PR #5923: Remove redundant Argon2 tests](https://github.com/opensearch-project/security/pull/5923)
- [PR #5934: Avoid unsafe shared integration-test state](https://github.com/opensearch-project/security/pull/5934)
- [Java Flight Recorder configurations](https://docs.oracle.com/en/java/javase/26/jfapi/flight-recorder-configurations.html)
- [GitHub Actions workflow artifacts](https://docs.github.com/en/actions/concepts/workflows-and-actions/workflow-artifacts)
- [GitHub Actions `workflow_run` event](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_run)
