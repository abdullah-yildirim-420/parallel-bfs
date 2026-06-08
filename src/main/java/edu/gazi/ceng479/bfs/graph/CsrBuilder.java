package edu.gazi.ceng479.bfs.graph;

/**
 * Builds an immutable CSR {@link Graph} from a flat edge list using a counting-sort /
 * prefix-sum pass (design.md §3.2 internals, §3.3). Shared by {@link EdgeListLoader}
 * and {@link GraphGenerator} to keep CSR construction in one tested place (DRY).
 *
 * <p>Complexity: O(V + E) time, O(V + E) memory.
 */
final class CsrBuilder {

    private CsrBuilder() {
    }

    /**
     * Build a CSR graph from {@code m} edges given as parallel arrays.
     *
     * @param numVertices number of vertices V; all ids in edge arrays must be in [0, V)
     * @param src         edge source ids, valid indices [0, m)
     * @param dst         edge destination ids, valid indices [0, m)
     * @param m           number of edges to consume from {@code src}/{@code dst}
     * @param directed    if true, each edge stores only src→dst; otherwise both directions
     * @return the constructed immutable graph
     */
    static Graph build(int numVertices, int[] src, int[] dst, int m, boolean directed) {
        int[] rowPtr = new int[numVertices + 1];

        // Pass 1 — degree counting (into rowPtr[v+1]).
        for (int i = 0; i < m; i++) {
            rowPtr[src[i] + 1]++;
            if (!directed) {
                rowPtr[dst[i] + 1]++;
            }
        }

        // Prefix sum → CSR offsets.
        for (int v = 0; v < numVertices; v++) {
            rowPtr[v + 1] += rowPtr[v];
        }

        int totalEntries = rowPtr[numVertices];
        int[] colIdx = new int[totalEntries];
        int[] cursor = new int[numVertices];
        System.arraycopy(rowPtr, 0, cursor, 0, numVertices);

        // Pass 2 — scatter neighbours.
        for (int i = 0; i < m; i++) {
            int u = src[i];
            int v = dst[i];
            colIdx[cursor[u]++] = v;
            if (!directed) {
                colIdx[cursor[v]++] = u;
            }
        }

        return new Graph(numVertices, rowPtr, colIdx, directed);
    }
}
