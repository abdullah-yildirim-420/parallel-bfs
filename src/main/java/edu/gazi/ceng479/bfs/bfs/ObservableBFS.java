package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.viz.instr.Instrumentation;

/**
 * The instrumented (observed) BFS path used by the visualization platform (design.md
 * §14.1, §23). It is a SEPARATE method from {@link BFS#run} so the clean benchmark path
 * carries no instrumentation bytecode (Principle A). Results are identical to the clean
 * path; only timing differs (instrumentation has cost, which is why it is excluded from
 * benchmarks).
 */
public interface ObservableBFS {

    /**
     * @param graph        graph to traverse
     * @param source       start vertex
     * @param instr        instrumentation hooks (use {@link Instrumentation#NO_OP} for none)
     * @param captureNodes whether to collect per-thread discovered ids for node-level
     *                     animation (demo track only — adds memory cost)
     * @return the traversal result (identical to {@link BFS#run})
     */
    BfsResult runObserved(Graph graph, int source, Instrumentation instr, boolean captureNodes);
}
