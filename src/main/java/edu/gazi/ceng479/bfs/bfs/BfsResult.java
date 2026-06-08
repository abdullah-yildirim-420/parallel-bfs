package edu.gazi.ceng479.bfs.bfs;

/**
 * Immutable value object holding the output of a BFS traversal.
 *
 * <p>Design reference: design.md §4.1. Correctness equality (SC-1) is defined over
 * the {@code level} array and the reached set only — <b>not</b> {@code parent},
 * because a parallel BFS may produce a different (but equally valid) BFS tree
 * (design.md §3.5 D-4, §9.1).
 */
public final class BfsResult {

    private final int source;
    private final int[] level;   // -1 == unreached, else BFS depth from source
    private final int[] parent;  // -1 == root/unreached (NOT part of equality)
    private final int reachedCount;
    private final int maxLevel;
    private long bfsNanos;       // filled by the timing layer; not part of equality

    public BfsResult(int source, int[] level, int[] parent, int reachedCount, int maxLevel) {
        this.source = source;
        this.level = level;
        this.parent = parent;
        this.reachedCount = reachedCount;
        this.maxLevel = maxLevel;
    }

    public int source() {
        return source;
    }

    /** @return the level array; index = vertex id, value = BFS depth or -1 if unreached. */
    public int[] level() {
        return level;
    }

    /** @return the parent array; index = vertex id, value = BFS-tree parent or -1. */
    public int[] parent() {
        return parent;
    }

    public int reachedCount() {
        return reachedCount;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public long bfsNanos() {
        return bfsNanos;
    }

    public void setBfsNanos(long nanos) {
        this.bfsNanos = nanos;
    }

    /**
     * Correctness equality per SC-1: same source, same reached count, and identical
     * {@code level} for every vertex. Parent pointers are intentionally ignored.
     *
     * @param other another result (typically the sequential reference)
     * @return true if the two traversals are BFS-equivalent
     */
    public boolean isEquivalentTo(BfsResult other) {
        if (other == null) return false;
        if (this.source != other.source) return false;
        if (this.reachedCount != other.reachedCount) return false;
        if (this.level.length != other.level.length) return false;
        for (int v = 0; v < level.length; v++) {
            if (this.level[v] != other.level[v]) return false;
        }
        return true;
    }

    /**
     * @param other another result
     * @return the first vertex id whose level differs, or -1 if equivalent
     *         (diagnostic aid for the correctness verifier, design.md §3.9)
     */
    public int firstDifferingVertex(BfsResult other) {
        if (other == null || this.level.length != other.level.length) return 0;
        for (int v = 0; v < level.length; v++) {
            if (this.level[v] != other.level[v]) return v;
        }
        return -1;
    }
}
