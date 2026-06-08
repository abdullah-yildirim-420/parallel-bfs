package edu.gazi.ceng479.bfs.viz.server;

import edu.gazi.ceng479.bfs.bfs.ParallelBFS;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.viz.event.EventSink;
import edu.gazi.ceng479.bfs.viz.instr.StreamingInstrumentation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs Sequential and Parallel BFS concurrently on the same graph, each streaming events
 * under a distinct runId, powering the flagship Sequential-vs-Parallel Race view
 * (design.md §19.4). Both use the observed path (demo track), so absolute times are
 * illustrative — the authoritative speedup comes from the clean Benchmark engine.
 */
public final class RaceCoordinator {

    public static final String SEQ_RUN_ID = "race-seq";
    public static final String PAR_RUN_ID = "race-par";

    private final ExecutorService exec = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "race-worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Start both traversals concurrently. Returns immediately; events flow to {@code sink}.
     *
     * @param graph     graph (should be demo-scale, design.md §14.2)
     * @param source    start vertex
     * @param parThreads thread count for the parallel side
     * @param sink      event destination (typically the EventBus sink)
     */
    public void start(Graph graph, int source, int parThreads, EventSink sink) {
        exec.submit(() -> new SequentialBFS().runObserved(graph, source,
                new StreamingInstrumentation(sink, SEQ_RUN_ID, true), true));
        exec.submit(() -> new ParallelBFS(parThreads, 1).runObserved(graph, source,
                new StreamingInstrumentation(sink, PAR_RUN_ID, true), true));
    }

    public void shutdown() {
        exec.shutdownNow();
    }
}
