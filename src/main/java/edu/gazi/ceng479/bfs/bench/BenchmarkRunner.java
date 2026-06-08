package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.bench.Records.RunRecord;
import edu.gazi.ceng479.bfs.bfs.BfsResult;
import edu.gazi.ceng479.bfs.bfs.ParallelBFS;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.graph.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the clean experiment grid (design.md §3.7): for each graph, run the
 * sequential baseline and parallel BFS across thread counts, discarding warm-up
 * repetitions, verifying SC-1 each time, and aggregating speedup/efficiency.
 *
 * <p>All timed runs use the clean (uninstrumented) BFS path so timings are honest
 * (Principle A). Both baselines required by the proposal are produced: pure sequential
 * and parallel-with-1-thread (framework overhead).
 */
public final class BenchmarkRunner {

    private final int[] threadCounts;
    private final int reps;
    private final int warmups;

    private final List<RunRecord> rawRecords = new ArrayList<>();
    private final List<AggRecord> aggRecords = new ArrayList<>();

    public BenchmarkRunner(int[] threadCounts, int reps, int warmups) {
        this.threadCounts = threadCounts.clone();
        this.reps = reps;
        this.warmups = warmups;
    }

    public List<RunRecord> rawRecords() {
        return rawRecords;
    }

    public List<AggRecord> aggRecords() {
        return aggRecords;
    }

    /** Run the full grid for one graph spec, appending to the record lists. */
    public void run(GraphSpec spec) {
        Graph g = spec.graph();
        int source = spec.source();

        // --- Sequential baseline ---
        SequentialBFS seq = new SequentialBFS();
        BfsResult reference = null;
        double[] seqTimes = new double[reps];
        for (int i = 0; i < warmups; i++) {
            seq.run(g, source); // warm-up, untimed
        }
        for (int rep = 0; rep < reps; rep++) {
            BfsResult r = MetricsCollector.timedRun(seq, g, source);
            reference = r;
            seqTimes[rep] = r.bfsNanos();
            rawRecords.add(new RunRecord(spec.name(), g.vertexCount(), g.edgeCount(), spec.type(),
                    "sequential", 1, rep, r.bfsNanos(), r.reachedCount(), r.maxLevel(), source));
        }
        double seqMean = Stats.mean(seqTimes);
        aggRecords.add(new AggRecord(spec.name(), g.vertexCount(), g.edgeCount(), spec.type(),
                "sequential", 1, seqMean, Stats.stdDev(seqTimes), Stats.ci95(seqTimes),
                seqMean / 1e6, 1.0, 100.0, 1.0,
                reference.reachedCount(), reference.maxLevel(), source));

        // --- Parallel across thread counts ---
        for (int t : threadCounts) {
            ParallelBFS par = new ParallelBFS(t);
            double[] parTimes = new double[reps];
            for (int i = 0; i < warmups; i++) {
                par.run(g, source);
            }
            BfsResult last = null;
            for (int rep = 0; rep < reps; rep++) {
                BfsResult r = MetricsCollector.timedRun(par, g, source);
                last = r;
                ResultVerifier.assertEquivalent(reference, r); // SC-1 gate at runtime
                parTimes[rep] = r.bfsNanos();
                rawRecords.add(new RunRecord(spec.name(), g.vertexCount(), g.edgeCount(), spec.type(),
                        "parallel", t, rep, r.bfsNanos(), r.reachedCount(), r.maxLevel(), source));
            }
            double parMean = Stats.mean(parTimes);
            double speedup = Stats.speedup(seqMean, parMean);
            aggRecords.add(new AggRecord(spec.name(), g.vertexCount(), g.edgeCount(), spec.type(),
                    "parallel", t, parMean, Stats.stdDev(parTimes), Stats.ci95(parTimes),
                    parMean / 1e6, speedup, Stats.efficiencyPct(speedup, t), Stats.amdahl(t),
                    last.reachedCount(), last.maxLevel(), source));
        }
    }
}
