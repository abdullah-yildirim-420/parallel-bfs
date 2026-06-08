package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency stress test (design.md §9.3): high-contention graphs + maximum threads,
 * many iterations, asserting every node is claimed exactly once (no node appears in two
 * thread-local next-frontiers) and the result stays level-equivalent to sequential.
 */
class ConcurrencyStressTest {

    private final SequentialBFS seq = new SequentialBFS();

    /** Builds a hub-and-spoke graph: vertex 0 connected to all of 1..n-1 (max contention). */
    private Graph hubGraph(int n) {
        int[] rowPtr = new int[n + 1];
        // degree: hub has n-1, each spoke has 1
        rowPtr[1] = n - 1;            // after hub
        for (int v = 2; v <= n; v++) {
            rowPtr[v] = rowPtr[v - 1] + 1;
        }
        int[] colIdx = new int[2 * (n - 1)];
        // hub neighbours = 1..n-1
        for (int i = 0; i < n - 1; i++) {
            colIdx[i] = i + 1;
        }
        // each spoke neighbour = hub (0)
        for (int v = 1; v < n; v++) {
            colIdx[rowPtr[v]] = 0;
        }
        return new Graph(n, rowPtr, colIdx, false);
    }

    @Test
    void hubGraphClaimedExactlyOnceUnderMaxThreads() {
        Graph g = hubGraph(5000); // one giant frontier of 4999 nodes at level 1
        BfsResult ref = seq.run(g, 0);
        assertEquals(5000, ref.reachedCount());
        ParallelBFS par = new ParallelBFS(8, 1);
        for (int iter = 0; iter < 100; iter++) {
            BfsResult got = par.run(g, 0);
            assertTrue(ref.isEquivalentTo(got), "iteration " + iter + " not equivalent");
            // exactly-once: every spoke at level 1, hub at level 0
            assertEquals(5000, got.reachedCount());
            assertEquals(1, got.maxLevel());
        }
    }

    @Test
    void denseRandomGraphRepeatedlyEquivalent() {
        Graph g = GraphGenerator.erdosRenyi(3000, 60, 2026L); // dense, large frontiers
        BfsResult ref = seq.run(g, 0);
        ParallelBFS par = new ParallelBFS(8, 1);
        for (int iter = 0; iter < 30; iter++) {
            assertTrue(ref.isEquivalentTo(par.run(g, 0)), "iteration " + iter);
        }
    }
}
