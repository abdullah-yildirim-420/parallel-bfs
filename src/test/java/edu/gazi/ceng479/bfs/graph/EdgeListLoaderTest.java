package edu.gazi.ceng479.bfs.graph;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link EdgeListLoader} (design.md §3.2, §9.1). */
class EdgeListLoaderTest {

    private Set<Integer> neighbours(Graph g, int v) {
        Set<Integer> s = new HashSet<>();
        for (int i = g.neighborBegin(v); i < g.neighborEnd(v); i++) {
            s.add(g.neighborAt(i));
        }
        return s;
    }

    @Test
    void parsesUndirectedTriangleWithCommentsAndBlanks() {
        String content = "# directed? no\n0 1\n\n1 2\n2 0\n";
        Graph g = EdgeListLoader.loadFromString(content, false, -1);
        assertEquals(3, g.vertexCount());
        assertEquals(6, g.edgeCount()); // 3 undirected edges * 2
        assertEquals(Set.of(1, 2), neighbours(g, 0));
        assertEquals(Set.of(0, 2), neighbours(g, 1));
        assertEquals(Set.of(0, 1), neighbours(g, 2));
    }

    @Test
    void remapsNonContiguousIds() {
        String content = "10 20\n20 30\n";
        Graph g = EdgeListLoader.loadFromString(content, false, -1);
        assertEquals(3, g.vertexCount()); // 10->0, 20->1, 30->2
        assertEquals(4, g.edgeCount());
        assertEquals(1, g.degree(0)); // 10 connects to 20
        assertEquals(2, g.degree(1)); // 20 connects to 10 and 30
        assertEquals(1, g.degree(2)); // 30 connects to 20
    }

    @Test
    void directedStoresOneDirection() {
        String content = "0 1\n0 2\n";
        Graph g = EdgeListLoader.loadFromString(content, true, -1);
        assertTrue(g.isDirected());
        assertEquals(3, g.vertexCount());
        assertEquals(2, g.edgeCount());
        assertEquals(2, g.degree(0));
        assertEquals(0, g.degree(1));
        assertEquals(0, g.degree(2));
    }

    @Test
    void dropsSelfLoops() {
        String content = "5 5\n5 7\n";
        Graph g = EdgeListLoader.loadFromString(content, false, -1);
        assertEquals(2, g.vertexCount()); // 5->0, 7->1 (self-loop line dropped)
        assertEquals(2, g.edgeCount());   // one undirected edge
    }

    @Test
    void honoursMaxEdgesCap() {
        String content = "0 1\n1 2\n2 3\n";
        Graph g = EdgeListLoader.loadFromString(content, false, 1);
        assertEquals(2, g.vertexCount()); // only first edge ingested
        assertEquals(2, g.edgeCount());
    }

    @Test
    void tabSeparatedTokens() {
        String content = "100\t200\n200\t300\n";
        Graph g = EdgeListLoader.loadFromString(content, true, -1);
        assertEquals(3, g.vertexCount());
        assertEquals(2, g.edgeCount());
    }

    @Test
    void emptyInputYieldsEmptyGraph() {
        Graph g = EdgeListLoader.loadFromString("# only a comment\n\n", false, -1);
        assertEquals(0, g.vertexCount());
        assertEquals(0, g.edgeCount());
    }
}
