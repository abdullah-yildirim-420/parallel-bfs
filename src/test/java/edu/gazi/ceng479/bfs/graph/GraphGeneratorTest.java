package edu.gazi.ceng479.bfs.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link GraphGenerator} (design.md §3.3, §9.1, SC-5 reproducibility). */
class GraphGeneratorTest {

    @Test
    void edgeCountMatchesAverageDegree() {
        int n = 1000, deg = 10;
        Graph g = GraphGenerator.erdosRenyi(n, deg, 42L);
        assertEquals(n, g.vertexCount());
        // undirected stores 2 entries per edge; m = n*deg/2 => entries = n*deg
        assertEquals((long) n * deg, g.edgeCount());
        assertFalse(g.isDirected());
    }

    @Test
    void deterministicForSameSeed() {
        Graph a = GraphGenerator.erdosRenyi(500, 8, 123L);
        Graph b = GraphGenerator.erdosRenyi(500, 8, 123L);
        assertEquals(a.vertexCount(), b.vertexCount());
        assertEquals(a.edgeCount(), b.edgeCount());
        for (int v = 0; v < a.vertexCount(); v++) {
            assertEquals(a.degree(v), b.degree(v), "degree mismatch at " + v);
            for (int i = a.neighborBegin(v); i < a.neighborEnd(v); i++) {
                assertEquals(a.neighborAt(i), b.neighborAt(i), "neighbour mismatch at index " + i);
            }
        }
    }

    @Test
    void noSelfLoops() {
        Graph g = GraphGenerator.erdosRenyi(300, 12, 7L);
        for (int v = 0; v < g.vertexCount(); v++) {
            for (int i = g.neighborBegin(v); i < g.neighborEnd(v); i++) {
                assertNotEquals(v, g.neighborAt(i), "self-loop found at vertex " + v);
            }
        }
    }

    @Test
    void degreeSumEqualsStoredEntries() {
        Graph g = GraphGenerator.erdosRenyi(400, 6, 99L);
        long sum = 0;
        for (int v = 0; v < g.vertexCount(); v++) {
            sum += g.degree(v);
        }
        assertEquals(g.edgeCount(), sum);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.erdosRenyi(0, 5, 1L));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.erdosRenyi(10, 0, 1L));
    }
}
