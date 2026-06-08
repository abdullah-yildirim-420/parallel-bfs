package edu.gazi.ceng479.bfs.viz.instr;

/**
 * Per-thread workload statistics for one BFS level (design.md §17.2, §23).
 * Powers the Thread Activity panels and the Workload Distribution chart.
 *
 * @param id          worker thread index [0, numThreads)
 * @param processed   frontier nodes expanded by this thread at this level
 * @param edges       neighbour entries scanned by this thread at this level
 * @param busyNanos   time this thread spent inside its task body
 * @param waitNanos   time finished-but-waiting at the barrier (slowest-peer slack)
 */
public record ThreadStat(int id, long processed, long edges, long busyNanos, long waitNanos) {

    /** Utilization for the level: busy / (busy + wait), as a percentage. */
    public double utilizationPct() {
        long total = busyNanos + waitNanos;
        return total == 0 ? 0.0 : (busyNanos * 100.0) / total;
    }
}
