package edu.gazi.ceng479.bfs.viz.event;

/** Emitted once when an observed BFS run finishes (design.md §18.3). */
public record RunCompletedEvent(
        String runId, long ts, long totalNanos, int reached, int maxLevel) implements Event {

    @Override
    public String type() {
        return "RunCompletedEvent";
    }
}
