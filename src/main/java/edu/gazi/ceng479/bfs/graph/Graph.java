package edu.gazi.ceng479.bfs.graph;

/**
 * Immutable graph in Compressed Sparse Row (CSR) form.
 *
 * <p>Design reference: design.md §3.1. The graph is stored as two primitive
 * {@code int[]} arrays — {@code rowPtr} (length {@code V+1}) holding the start
 * offset of each vertex's neighbour list, and {@code colIdx} holding the
 * concatenated neighbour ids. This layout avoids autoboxing (a
 * {@code List<List<Integer>>} would exceed 3.7&nbsp;GB on com-Orkut), gives
 * cache-friendly contiguous iteration, and is safely shareable read-only across
 * threads (all fields {@code final}; full construction happens-before publication).
 *
 * <p>For an undirected graph each edge is stored twice (both directions) by the
 * loader/generator, so {@code colIdx.length} equals the number of stored directed
 * entries.
 */
public final class Graph {

    private final int numVertices;
    private final long numEdges;     // number of stored directed entries (== colIdx.length)
    private final int[] rowPtr;      // length numVertices + 1
    private final int[] colIdx;      // length numEdges
    private final boolean directed;

    /**
     * Constructs a CSR graph. The arrays are referenced, not copied — callers must
     * not mutate them after construction (the loader/generator build them locally).
     *
     * @param numVertices number of vertices V (ids are dense in [0, V))
     * @param rowPtr      CSR offsets, length V+1, monotonic non-decreasing,
     *                    {@code rowPtr[0] == 0} and {@code rowPtr[V] == colIdx.length}
     * @param colIdx      concatenated neighbour ids
     * @param directed    whether the graph is directed
     * @throws IllegalArgumentException if the CSR invariants are violated
     */
    public Graph(int numVertices, int[] rowPtr, int[] colIdx, boolean directed) {
        if (numVertices < 0) {
            throw new IllegalArgumentException("numVertices must be >= 0, got " + numVertices);
        }
        if (rowPtr == null || colIdx == null) {
            throw new IllegalArgumentException("rowPtr/colIdx must not be null");
        }
        if (rowPtr.length != numVertices + 1) {
            throw new IllegalArgumentException(
                    "rowPtr length must be V+1=" + (numVertices + 1) + ", got " + rowPtr.length);
        }
        if (rowPtr[0] != 0) {
            throw new IllegalArgumentException("rowPtr[0] must be 0, got " + rowPtr[0]);
        }
        if (rowPtr[numVertices] != colIdx.length) {
            throw new IllegalArgumentException(
                    "rowPtr[V]=" + rowPtr[numVertices] + " must equal colIdx.length=" + colIdx.length);
        }
        for (int i = 1; i <= numVertices; i++) {
            if (rowPtr[i] < rowPtr[i - 1]) {
                throw new IllegalArgumentException(
                        "rowPtr must be monotonic non-decreasing; violated at index " + i);
            }
        }
        this.numVertices = numVertices;
        this.numEdges = colIdx.length;
        this.rowPtr = rowPtr;
        this.colIdx = colIdx;
        this.directed = directed;
    }

    /** @return number of vertices V. */
    public int vertexCount() {
        return numVertices;
    }

    /** @return number of stored directed entries (== colIdx.length). */
    public long edgeCount() {
        return numEdges;
    }

    /** @return whether the graph is directed. */
    public boolean isDirected() {
        return directed;
    }

    /**
     * @param u vertex id in [0, V)
     * @return out-degree of {@code u} (number of stored neighbours)
     */
    public int degree(int u) {
        return rowPtr[u + 1] - rowPtr[u];
    }

    /**
     * @param u vertex id in [0, V)
     * @return inclusive start index into the neighbour array for {@code u}
     * @see #neighborEnd(int)
     * @see #neighborAt(int)
     */
    public int neighborBegin(int u) {
        return rowPtr[u];
    }

    /**
     * @param u vertex id in [0, V)
     * @return exclusive end index into the neighbour array for {@code u}.
     *         Iterate neighbours as {@code for (int i = neighborBegin(u); i < neighborEnd(u); i++)}.
     */
    public int neighborEnd(int u) {
        return rowPtr[u + 1];
    }

    /**
     * @param i index in [neighborBegin(u), neighborEnd(u))
     * @return the neighbour vertex id stored at position {@code i}
     */
    public int neighborAt(int i) {
        return colIdx[i];
    }
}
