package edu.gazi.ceng479.bfs.viz.instr;

import java.util.List;

/**
 * Hooks the observed BFS path invokes to report progress (design.md §23). Injected into
 * {@code runObserved(...)}; the clean {@code run(...)} path never references it.
 *
 * <p>{@link #NO_OP} is a shared do-nothing implementation; when it is the only impl on a
 * code path the JVM inlines the calls away, but the project keeps the clean and observed
 * paths as <em>separate methods</em> anyway so the benchmark bytecode is provably free of
 * instrumentation (Principle A, design.md §14.1).
 */
public interface Instrumentation {

    void onRunStart(String mode, int threads, int vertices, long edges, int source);

    /**
     * Called after each level's barrier.
     *
     * @param level            level just completed
     * @param frontierSize     nodes expanded at this level
     * @param nextFrontierSize nodes discovered for the next level
     * @param totalVisited     cumulative visited count
     * @param syncNanos        merge/barrier time (sequential fraction)
     * @param threads          per-thread workload (one entry for sequential)
     * @param nodesByThread    discovered ids grouped by claiming thread, or {@code null}
     *                         when node-level streaming is disabled (large graphs)
     */
    void onLevelComplete(int level, int frontierSize, int nextFrontierSize, int totalVisited,
                         long syncNanos, List<ThreadStat> threads, int[][] nodesByThread);

    void onRunComplete(long totalNanos, int reached, int maxLevel);

    /** Shared no-op instrumentation (used by the observed path when no UI is attached). */
    Instrumentation NO_OP = new Instrumentation() {
        @Override
        public void onRunStart(String m, int t, int v, long e, int s) {
        }

        @Override
        public void onLevelComplete(int l, int f, int nf, int tv, long sn,
                                    List<ThreadStat> th, int[][] nbt) {
        }

        @Override
        public void onRunComplete(long tn, int r, int ml) {
        }
    };
}
