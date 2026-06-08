package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.EdgeListLoader;
import edu.gazi.ceng479.bfs.graph.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link SequentialBFS} on hand-built graphs (design.md §9.1). */
class SequentialBFSTest {

    private final SequentialBFS bfs = new SequentialBFS();

    @Test
    void lineGraphLevelsAreDepths() {
        // 0-1-2-3 undirected
        Graph g = EdgeListLoader.loadFromString("0 1\n1 2\n2 3\n", false, -1);
        BfsResult r = bfs.run(g, 0);
        assertArrayEquals(new int[]{0, 1, 2, 3}, r.level());
        assertEquals(4, r.reachedCount());
        assertEquals(3, r.maxLevel());
    }

    @Test
    void starGraphAllNeighboursLevelOne() {
        // center 0 connected to 1,2,3
        Graph g = EdgeListLoader.loadFromString("0 1\n0 2\n0 3\n", false, -1);
        BfsResult r = bfs.run(g, 0);
        assertArrayEquals(new int[]{0, 1, 1, 1}, r.level());
        assertEquals(4, r.reachedCount());
        assertEquals(1, r.maxLevel());
    }

    @Test
    void cycleGraphLevels() {
        // 0-1-2-3-0 (square). From 0: 1 and 3 at level 1, 2 at level 2.
        Graph g = EdgeListLoader.loadFromString("0 1\n1 2\n2 3\n3 0\n", false, -1);
        BfsResult r = bfs.run(g, 0);
        assertEquals(0, r.level()[0]);
        assertEquals(1, r.level()[1]);
        assertEquals(2, r.level()[2]);
        assertEquals(1, r.level()[3]);
        assertEquals(4, r.reachedCount());
        assertEquals(2, r.maxLevel());
    }

    @Test
    void disconnectedComponentsLeaveUnreachedAtMinusOne() {
        // component A: 0-1 ; component B: 2-3
        Graph g = EdgeListLoader.loadFromString("0 1\n2 3\n", false, -1);
        BfsResult r = bfs.run(g, 0);
        assertEquals(0, r.level()[0]);
        assertEquals(1, r.level()[1]);
        assertEquals(-1, r.level()[2]);
        assertEquals(-1, r.level()[3]);
        assertEquals(2, r.reachedCount());
    }

    @Test
    void singleNodeGraph() {
        Graph g = new Graph(1, new int[]{0, 0}, new int[]{}, false);
        BfsResult r = bfs.run(g, 0);
        assertArrayEquals(new int[]{0}, r.level());
        assertEquals(1, r.reachedCount());
        assertEquals(0, r.maxLevel());
    }

    @Test
    void directedGraphRespectsDirection() {
        // 0->1->2 directed
        Graph g = EdgeListLoader.loadFromString("0 1\n1 2\n", true, -1);
        BfsResult fromZero = bfs.run(g, 0);
        assertArrayEquals(new int[]{0, 1, 2}, fromZero.level());
        assertEquals(3, fromZero.reachedCount());

        BfsResult fromTwo = bfs.run(g, 2);
        assertEquals(0, fromTwo.level()[2]);
        assertEquals(-1, fromTwo.level()[0]);
        assertEquals(-1, fromTwo.level()[1]);
        assertEquals(1, fromTwo.reachedCount());
    }

    @Test
    void parentPointersFormValidTree() {
        Graph g = EdgeListLoader.loadFromString("0 1\n1 2\n2 3\n", false, -1);
        BfsResult r = bfs.run(g, 0);
        assertEquals(-1, r.parent()[0]);          // root has no parent
        assertEquals(r.level()[r.parent()[3]] + 1, r.level()[3]); // parent level invariant
    }

    @Test
    void rejectsBadSourceAndEmptyGraph() {
        Graph g = EdgeListLoader.loadFromString("0 1\n", false, -1);
        assertThrows(IllegalArgumentException.class, () -> bfs.run(g, 5));
        Graph empty = new Graph(0, new int[]{0}, new int[]{}, false);
        assertThrows(IllegalArgumentException.class, () -> bfs.run(empty, 0));
    }
}
