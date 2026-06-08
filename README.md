# Parallel BFS on Graphs Using Java Threads — Live Demonstration Platform

CENG-479 Parallel Programming · Gazi University · Submission 2
**Team:** Ahmet Muhittin Gürkan (21118080059), Abdullah Yıldırım (21118080025)

A sequential **and** parallel (frontier-level, Java-threads) Breadth-First Search, benchmarked
against each other, wrapped in a **live parallelism demonstration platform** (real-time WebSocket
dashboard: traversal animation, per-thread workload, sequential-vs-parallel race, speedup vs
Amdahl, system metrics, benchmark history).

See `../design.md` for the full engineering design (Part I = algorithm core, Part II = visualization).
Development is tracked step-by-step in `context.md`.

---

## Requirements

- **JDK 17+** (built/tested on Temurin 21; compiles to release 17)
- **Maven 3.9+**
- **Node 18+ / npm** (only to rebuild the dashboard)
- *(optional)* Python 3 + matplotlib (to regenerate report figures)

## Build

```bash
# 1) (optional) build the dashboard and bundle it into the jar
bash scripts/build_frontend.sh        # npm install + vite build -> src/main/resources/public

# 2) build the fat jar (runs all tests)
mvn clean package
# -> target/parallel-bfs.jar
```

## Run

```bash
# Sequential BFS on a generated graph
java -jar target/parallel-bfs.jar --mode seq --gen n=1000000,deg=10

# Parallel BFS with 4 threads
java -jar target/parallel-bfs.jar --mode par --threads 4 --gen n=1000000,deg=10

# Correctness gate: assert parallel == sequential (exit 4 on mismatch)
java -jar target/parallel-bfs.jar --mode verify --threads 8 --gen n=500000,deg=40

# Full benchmark grid (1,2,4,8 threads, 5 reps, 2 warmups) -> out/*.csv
java -Xmx6g -jar target/parallel-bfs.jar --mode bench --threads-list 1,2,4,8 --reps 5 --warmup 2

# Live visualization platform (open http://localhost:7070)
java -jar target/parallel-bfs.jar --mode ui
```

Use real SNAP graphs by downloading edge lists into `data/` and passing `--graph`:

```bash
java -Xmx8g -jar target/parallel-bfs.jar --mode bench --graph data/com-orkut.ungraph.txt --directed false
java -Xmx6g -jar target/parallel-bfs.jar --mode bench --graph data/web-Google.txt --directed true
```

## Reproduce the report figures

```bash
java -Xmx6g -jar target/parallel-bfs.jar --mode bench       # writes out/results_agg.csv
python scripts/plot.py out/results_agg.csv                  # writes docs/figures/*.png
```

## Measured speedup (Intel-class, 4 physical / 8 logical cores, JDK 21)

| graph | 2t | 4t | 8t |
|-------|----|----|----|
| synthetic-dense (400k / 32M) | 1.32× | **2.26×** | **3.18×** |
| synthetic-sparse (1M / 10M)  | 1.32× | 1.75× | 2.67× |

Dense graphs amortize coordination over larger frontiers → closer to Amdahl (p=0.90); sparse
graphs have many small frontiers → more relative overhead. See `docs/ImplementationReport.md`.

## Project layout

```
src/main/java/edu/gazi/ceng479/bfs/
  graph/   CSR Graph, EdgeListLoader (SNAP), GraphGenerator (Erdos-Renyi)
  bfs/     SequentialBFS, ParallelBFS, VisitedSet, FrontierPartitioner, ObservableBFS
  bench/   BenchmarkRunner, MetricsCollector, Stats, ResultVerifier, CsvWriter
  viz/     event/ instr/ probe/ store/ server/  (live platform — Part II)
  cli/     ArgParser, Config        Main.java
frontend/  React + TS + Vite dashboard
scripts/   build_frontend.sh, plot.py
docs/      ImplementationReport.md, figures/
```

## Tests

```bash
mvn test     # 77 tests: unit, SC-1 correctness/equivalence, concurrency stress,
             # benchmark integration, WebSocket/REST integration, persistence round-trip
```
