package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.graph.Graph;

/**
 * A named benchmark target: a graph plus its category label and BFS source.
 * Source defaults to the highest-degree vertex so the traversal is non-trivial
 * (design.md §3.2.2, R-9).
 */
public final class GraphSpec {

    private final String name;
    private final String type; // e.g. "synthetic-sparse", "synthetic-dense", "real-sparse"
    private final Graph graph;
    private final int source;

    public GraphSpec(String name, String type, Graph graph, int source) {
        this.name = name;
        this.type = type;
        this.graph = graph;
        this.source = source;
    }

    /** Build a spec with auto-selected source (highest-degree vertex). */
    public static GraphSpec auto(String name, String type, Graph graph) {
        return new GraphSpec(name, type, graph, autoSource(graph));
    }

    /** @return the id of the highest-degree vertex (deterministic; ties → lowest id). */
    public static int autoSource(Graph g) {
        int best = 0, bestDeg = -1;
        for (int v = 0; v < g.vertexCount(); v++) {
            int d = g.degree(v);
            if (d > bestDeg) {
                bestDeg = d;
                best = v;
            }
        }
        return best;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public Graph graph() {
        return graph;
    }

    public int source() {
        return source;
    }
}
