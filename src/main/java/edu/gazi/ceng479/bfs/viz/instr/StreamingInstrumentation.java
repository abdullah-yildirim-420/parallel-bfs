package edu.gazi.ceng479.bfs.viz.instr;

import edu.gazi.ceng479.bfs.viz.event.EventSink;
import edu.gazi.ceng479.bfs.viz.event.FrontierBatchEvent;
import edu.gazi.ceng479.bfs.viz.event.LevelCompletedEvent;
import edu.gazi.ceng479.bfs.viz.event.RunCompletedEvent;
import edu.gazi.ceng479.bfs.viz.event.RunStartedEvent;

import java.util.List;

/**
 * {@link Instrumentation} that converts BFS hooks into {@link edu.gazi.ceng479.bfs.viz.event.Event}s
 * published to an {@link EventSink} (design.md §23). Used by the observed path when a UI
 * (or test collector) is attached.
 */
public final class StreamingInstrumentation implements Instrumentation {

    private final EventSink sink;
    private final String runId;
    private final boolean streamNodes;

    /**
     * @param sink        destination for events
     * @param runId       id correlating all events of this run
     * @param streamNodes whether to emit per-node {@link FrontierBatchEvent}s (demo track only)
     */
    public StreamingInstrumentation(EventSink sink, String runId, boolean streamNodes) {
        this.sink = sink;
        this.runId = runId;
        this.streamNodes = streamNodes;
    }

    @Override
    public void onRunStart(String mode, int threads, int vertices, long edges, int source) {
        sink.emit(new RunStartedEvent(runId, now(), mode, threads, vertices, edges, source));
    }

    @Override
    public void onLevelComplete(int level, int frontierSize, int nextFrontierSize, int totalVisited,
                                long syncNanos, List<ThreadStat> threads, int[][] nodesByThread) {
        sink.emit(new LevelCompletedEvent(runId, now(), level, frontierSize, nextFrontierSize,
                totalVisited, syncNanos, threads));
        if (streamNodes && nodesByThread != null) {
            sink.emit(new FrontierBatchEvent(runId, now(), level, nodesByThread));
        }
    }

    @Override
    public void onRunComplete(long totalNanos, int reached, int maxLevel) {
        sink.emit(new RunCompletedEvent(runId, now(), totalNanos, reached, maxLevel));
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
