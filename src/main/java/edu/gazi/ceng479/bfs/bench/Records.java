package edu.gazi.ceng479.bfs.bench;

/**
 * Immutable result records for the benchmark harness (design.md §4.1, §7.2).
 */
public final class Records {

    private Records() {
    }

    /** One timed repetition of one (graph, mode, threads) configuration. */
    public record RunRecord(
            String graphName, int vertices, long edges, String graphType,
            String mode, int threads, int rep,
            long bfsNanos, int reached, int maxLevel, int source) {
    }

    /** Aggregated statistics over the repetitions of one (graph, mode, threads). */
    public record AggRecord(
            String graphName, int vertices, long edges, String graphType,
            String mode, int threads,
            double meanNs, double sdNs, double ci95Ns, double meanMs,
            double speedup, double efficiencyPct, double amdahlPred,
            int reached, int maxLevel, int source) {
    }
}
