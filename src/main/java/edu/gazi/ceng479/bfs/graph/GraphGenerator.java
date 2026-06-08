package edu.gazi.ceng479.bfs.graph;

import java.util.SplittableRandom;

/**
 * Generates reproducible Erdős–Rényi G(n, m) undirected graphs for the scaling
 * study (design.md §3.3). A fixed {@code seed} fully determines the graph (and
 * therefore the BFS result), satisfying the reproducibility contract (SC-5).
 *
 * <p>Sparse graphs use average degree 5–10; dense graphs 50–100 (proposal §6).
 * Self-loops are rejected; duplicate edges are allowed (negligible at these
 * densities and harmless to BFS — the visited claim deduplicates the frontier).
 */
public final class GraphGenerator {

    private GraphGenerator() {
    }

    /**
     * @param n         number of vertices (&gt; 0)
     * @param avgDegree target average degree (&gt; 0); m = n·avgDegree/2 undirected edges
     * @param seed      RNG seed (reproducibility)
     * @return an undirected ER graph
     */
    public static Graph erdosRenyi(int n, int avgDegree, long seed) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0, got " + n);
        }
        if (avgDegree <= 0) {
            throw new IllegalArgumentException("avgDegree must be > 0, got " + avgDegree);
        }
        long targetEdges = (long) n * avgDegree / 2L;
        if (targetEdges > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("requested edge count exceeds int capacity: " + targetEdges);
        }
        int m = (int) targetEdges;

        SplittableRandom rng = new SplittableRandom(seed);
        int[] src = new int[m];
        int[] dst = new int[m];

        int e = 0;
        while (e < m) {
            int u = rng.nextInt(n);
            int v = rng.nextInt(n);
            if (u == v) {
                continue; // reject self-loop
            }
            src[e] = u;
            dst[e] = v;
            e++;
        }

        return CsrBuilder.build(n, src, dst, m, false);
    }
}
