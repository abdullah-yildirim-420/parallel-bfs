package edu.gazi.ceng479.bfs.viz.event;

import edu.gazi.ceng479.bfs.viz.instr.ThreadStat;

import java.util.List;

/**
 * Emitted after each BFS level's synchronization barrier (design.md §18.3
 * BarrierReached + ThreadStats combined). Carries the frontier sizes and the
 * per-thread workload for the level — the primary feed for the BFS-metrics,
 * Thread-Activity, and Workload-Distribution visualizations.
 *
 * @param level            the level just completed (children get level+1)
 * @param frontierSize     number of nodes expanded at this level
 * @param nextFrontierSize number of nodes discovered for the next level
 * @param totalVisited     cumulative visited count after this level
 * @param syncNanos        time spent in the merge/barrier (sequential fraction)
 * @param threads          per-thread stats (single entry for sequential runs)
 */
public record LevelCompletedEvent(
        String runId, long ts, int level,
        int frontierSize, int nextFrontierSize, int totalVisited,
        long syncNanos, List<ThreadStat> threads) implements Event {

    @Override
    public String type() {
        return "LevelCompletedEvent";
    }
}
