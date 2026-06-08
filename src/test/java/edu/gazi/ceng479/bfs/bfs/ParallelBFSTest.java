package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.EdgeListLoader;
import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SC-1 correctness gate (design.md §9.2): the parallel BFS result must be
 * <b>level-equivalent</b> to the sequential reference on every graph, repeatedly,
 * across thread counts — surfacing any nondeterministic race.
 *
 * <p>{@code seqCutoff = 1} forces the parallel expansion path even on small graphs.
 */
class ParallelBFSTest {

    private final SequentialBFS seq = new SequentialBFS();

    private void assertEquivalent(Graph g, int source, int threads, int repeats) {
        BfsResult ref = seq.run(g, source);
        ParallelBFS par = new ParallelBFS(threads, 1); // force parallel path
        for (int i = 0; i < repeats; i++) {
            BfsResult got = par.run(g, source);
            int diff = ref.firstDifferingVertex(got);
            assertEquals(-1, diff,
                    "rep " + i + " threads=" + threads + ": level mismatch at vertex " + diff
                            + " (seq=" + (diff >= 0 ? ref.level()[diff] : "-")
                            + ", par=" + (diff >= 0 ? got.level()[diff] : "-") + ")");
            assertEquals(ref.reachedCount(), got.reachedCount(), "reached count mismatch");
            assertEquals(ref.maxLevel(), got.maxLevel(), "max level mismatch");
        }
    }

    @Test
    void equivalentOnHandBuiltGraphs() {
        assertEquivalent(EdgeListLoader.loadFromString("0 1\n1 2\n2 3\n", false, -1), 0, 4, 20);
        assertEquivalent(EdgeListLoader.loadFromString("0 1\n0 2\n0 3\n", false, -1), 0, 4, 20); // star
        assertEquivalent(EdgeListLoader.loadFromString("0 1\n1 2\n2 3\n3 0\n", false, -1), 0, 4, 20); // cycle
    }

    @Test
    void equivalentOnDisconnectedGraph() {
        Graph g = EdgeListLoader.loadFromString("0 1\n2 3\n", false, -1);
        assertEquivalent(g, 0, 4, 20);
    }

    @Test
    void equivalentOnDirectedGraph() {
        Graph g = EdgeListLoader.loadFromString("0 1\n1 2\n0 2\n", true, -1);
        assertEquivalent(g, 0, 4, 20);
    }

    @Test
    void equivalentOnManyRandomGraphsAcrossThreadCounts() {
        long[] seeds = {1L, 2L, 3L, 7L, 42L, 99L};
        int[] sizes = {200, 800, 2000};
        int[] degrees = {6, 20};
        for (long seed : seeds) {
            for (int n : sizes) {
                for (int deg : degrees) {
                    Graph g = GraphGenerator.erdosRenyi(n, deg, seed);
                    for (int threads : new int[]{2, 4, 8}) {
                        assertEquivalent(g, 0, threads, 3);
                    }
                }
            }
        }
    }

    @Test
    void serialFallbackPathAlsoCorrect() {
        // Large cutoff => the small-frontier serial fallback (D-3) handles every level.
        Graph g = GraphGenerator.erdosRenyi(500, 10, 5L);
        BfsResult ref = seq.run(g, 0);
        ParallelBFS par = new ParallelBFS(4, 100_000); // everything below cutoff -> serial path
        BfsResult got = par.run(g, 0);
        assertTrue(ref.isEquivalentTo(got));
    }

    @Test
    void singleThreadFrameworkBaselineCorrect() {
        // parallel with 1 thread = framework-overhead baseline (design.md §3.7)
        Graph g = GraphGenerator.erdosRenyi(1000, 8, 11L);
        BfsResult ref = seq.run(g, 0);
        BfsResult got = new ParallelBFS(1, 1).run(g, 0);
        assertTrue(ref.isEquivalentTo(got));
    }

    @Test
    void rejectsBadSourceAndEmptyGraph() {
        Graph g = GraphGenerator.erdosRenyi(50, 6, 1L);
        ParallelBFS par = new ParallelBFS(4, 1);
        assertThrows(IllegalArgumentException.class, () -> par.run(g, 999));
        Graph empty = new Graph(0, new int[]{0}, new int[]{}, false);
        assertThrows(IllegalArgumentException.class, () -> par.run(empty, 0));
    }
}
