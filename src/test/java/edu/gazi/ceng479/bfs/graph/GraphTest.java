package edu.gazi.ceng479.bfs.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CSR {@link Graph} (design.md §9.1: CSR invariants, degree,
 * neighbour ranges).
 */
class GraphTest {

    /**
     * Builds the small undirected triangle 0-1-2 plus a pendant 2-3.
     * Adjacency: 0:{1,2} 1:{0,2} 2:{0,1,3} 3:{2}
     * rowPtr = [0,2,4,7,8], colIdx = [1,2, 0,2, 0,1,3, 2]
     */
    private Graph sampleGraph() {
        int[] rowPtr = {0, 2, 4, 7, 8};
        int[] colIdx = {1, 2, 0, 2, 0, 1, 3, 2};
        return new Graph(4, rowPtr, colIdx, false);
    }

    @Test
    void vertexAndEdgeCounts() {
        Graph g = sampleGraph();
        assertEquals(4, g.vertexCount());
        assertEquals(8, g.edgeCount());
        assertFalse(g.isDirected());
    }

    @Test
    void degreesMatchCsr() {
        Graph g = sampleGraph();
        assertEquals(2, g.degree(0));
        assertEquals(2, g.degree(1));
        assertEquals(3, g.degree(2));
        assertEquals(1, g.degree(3));
    }

    @Test
    void neighborRangesAndContents() {
        Graph g = sampleGraph();
        // vertex 2 neighbours = {0,1,3}
        int[] expected = {0, 1, 3};
        int k = 0;
        for (int i = g.neighborBegin(2); i < g.neighborEnd(2); i++) {
            assertEquals(expected[k++], g.neighborAt(i));
        }
        assertEquals(3, k);
    }

    @Test
    void emptyGraphIsValid() {
        Graph g = new Graph(0, new int[]{0}, new int[]{}, false);
        assertEquals(0, g.vertexCount());
        assertEquals(0, g.edgeCount());
    }

    @Test
    void isolatedVerticesHaveZeroDegree() {
        // 3 vertices, no edges
        Graph g = new Graph(3, new int[]{0, 0, 0, 0}, new int[]{}, false);
        assertEquals(0, g.degree(0));
        assertEquals(0, g.degree(1));
        assertEquals(0, g.degree(2));
        assertEquals(g.neighborBegin(1), g.neighborEnd(1));
    }

    @Test
    void rejectsWrongRowPtrLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(3, new int[]{0, 1}, new int[]{0}, false));
    }

    @Test
    void rejectsNonZeroFirstOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(1, new int[]{1, 1}, new int[]{}, false));
    }

    @Test
    void rejectsRowPtrTailMismatch() {
        // rowPtr[V] says 2 entries but colIdx has 1
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(1, new int[]{0, 2}, new int[]{0}, false));
    }

    @Test
    void rejectsNonMonotonicRowPtr() {
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(2, new int[]{0, 2, 1}, new int[]{0, 1}, false));
    }

    @Test
    void directedFlagPreserved() {
        Graph g = new Graph(2, new int[]{0, 1, 1}, new int[]{1}, true);
        assertTrue(g.isDirected());
        assertEquals(1, g.degree(0));
        assertEquals(0, g.degree(1));
    }
}
