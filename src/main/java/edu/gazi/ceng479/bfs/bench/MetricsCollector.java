package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.bfs.BFS;
import edu.gazi.ceng479.bfs.bfs.BfsResult;
import edu.gazi.ceng479.bfs.graph.Graph;

/**
 * Times the <em>clean</em> BFS region with {@link System#nanoTime()} only — graph
 * loading/generation is excluded (design.md §3.8, §14.1 Principle A). The wall-clock
 * BFS duration is the primary metric (proposal §6).
 */
public final class MetricsCollector {

    private MetricsCollector() {
    }

    /**
     * Run a BFS once and record its wall-clock duration on the result.
     *
     * @return the {@link BfsResult} with {@link BfsResult#bfsNanos()} populated
     */
    public static BfsResult timedRun(BFS bfs, Graph graph, int source) {
        long t0 = System.nanoTime();
        BfsResult r = bfs.run(graph, source);
        long dt = System.nanoTime() - t0;
        r.setBfsNanos(dt);
        return r;
    }
}
