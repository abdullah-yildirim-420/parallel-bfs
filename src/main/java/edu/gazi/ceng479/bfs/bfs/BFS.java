package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.Graph;

/**
 * Common contract for BFS engines (design.md §2.3, §3.4–3.5). Both the sequential
 * baseline and the parallel implementation expose this <em>clean</em> path with no
 * instrumentation, so benchmark timing stays honest (design.md §14.1, Principle A).
 */
public interface BFS {

    /**
     * Run a breadth-first traversal from {@code source}.
     *
     * @param graph  the graph to traverse
     * @param source start vertex id in [0, V)
     * @return the traversal result (level array, parent array, counts)
     */
    BfsResult run(Graph graph, int source);

    /** @return a short human-readable name for reports/plots (e.g. "sequential", "parallel-4"). */
    String name();
}
