# Scripts

This folder contains several scripts we wrote to test or debug things.

## Profile.sh

**Usage:**
Run executeProfiling.sh with a pod name and optional event type. It will download the async profiler package, run in your current namespace, copy necessary binaries to the pod, run the async profiler, and copy the resulting flamegraph back to your local disk.

**Syntax:**
```
./executeProfiling.sh <POD-NAME> [EVENT-TYPE]
```

**Event Types:**
- `cpu` - CPU profiling (default)
- `wall` - Wall clock time profiling (includes waiting/blocking time)
- `alloc` - Memory allocation profiling

Example with CPU profiling (default):

```
 $ ./executeProfiling.sh release-8-8-0-alpha6-zeebe-2
...
Profiling for 100 seconds
Done
+ kubectl cp release-8-8-0-alpha6-zeebe-2:/usr/local/camunda/data/flamegraph-cpu-2025-07-11_19-02-52.html release-8-8-0-alpha6-zeebe-2-flamegraph-cpu-2025-07-11_19-02-52.html
tar: Removing leading `/' from member names

```

Example with wall clock profiling:

```
 $ ./executeProfiling.sh release-8-8-0-alpha6-zeebe-2 wall
...
Profiling for 100 seconds
Done
+ kubectl cp release-8-8-0-alpha6-zeebe-2:/usr/local/camunda/data/flamegraph-wall-2025-07-11_19-05-23.html release-8-8-0-alpha6-zeebe-2-flamegraph-wall-2025-07-11_19-05-23.html
```

**GitHub Actions Workflow:**
You can also use the [Profile Load Test workflow](https://github.com/camunda/camunda/actions/workflows/profile-load-test.yml) to profile pods running in load tests. This workflow allows you to select the load test name, pod name, and event type (cpu/wall/alloc) through the GitHub UI.

## PartitionDistribution.sh

**Usage:**

```bash
./partitionDistribution.sh {nodes} {partitionCount} {replicationFactor}
```

This script will calculate the distribution of partitions in a cluster.

_Example Output:_

```bash
$ ./partitionDistribution.sh 3 3 3
Distribution:
P\N|	N 0|	N 1|	N 2
P 0|	L  |	F  |	F
P 1|	F  |	L  |	F
P 2|	F  |	F  |	L

Partitions per Node:
N 0: 3
N 1: 3
N 2: 3
```

