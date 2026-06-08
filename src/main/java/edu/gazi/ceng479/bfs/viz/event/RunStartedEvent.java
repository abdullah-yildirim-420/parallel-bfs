package edu.gazi.ceng479.bfs.viz.event;

/** Emitted once when an observed BFS run begins (design.md §18.3). */
public record RunStartedEvent(
        String runId, long ts, String mode, int threads,
        int vertices, long edges, int source) implements Event {

    @Override
    public String type() {
        return "RunStartedEvent";
    }
}
