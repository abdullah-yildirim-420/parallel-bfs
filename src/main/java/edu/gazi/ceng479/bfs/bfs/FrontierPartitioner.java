package edu.gazi.ceng479.bfs.bfs;

/**
 * Splits a frontier of size {@code F} into up to {@code N} contiguous, near-equal
 * index ranges, one per worker thread (design.md §3.6).
 *
 * <p>Contiguous (not round-robin) ranges are used deliberately so each thread reads a
 * contiguous slice of the frontier array, improving cache behaviour (design.md §3.5
 * D-1). Equal <em>node-count</em> partitioning is used in v1; load imbalance from
 * skewed degree distributions is a measured/discussed limitation (design.md §3.6, R-5).
 */
public final class FrontierPartitioner {

    private FrontierPartitioner() {
    }

    /**
     * @param frontierSize number of nodes in the current frontier (&ge; 0)
     * @param numThreads   number of partitions desired (&ge; 1)
     * @return array of {@code [begin, end)} index pairs covering [0, frontierSize);
     *         empty trailing ranges are dropped, so the result length is
     *         {@code min(numThreads, ceil(frontierSize/chunk))} and may be &lt; numThreads
     */
    public static int[][] split(int frontierSize, int numThreads) {
        if (frontierSize <= 0) {
            return new int[0][];
        }
        if (numThreads < 1) {
            throw new IllegalArgumentException("numThreads must be >= 1, got " + numThreads);
        }
        int chunk = (frontierSize + numThreads - 1) / numThreads; // ceil(F/N)
        int parts = (frontierSize + chunk - 1) / chunk;            // actual non-empty parts
        int[][] ranges = new int[parts][];
        for (int p = 0; p < parts; p++) {
            int begin = p * chunk;
            int end = Math.min(frontierSize, begin + chunk);
            ranges[p] = new int[]{begin, end};
        }
        return ranges;
    }
}
