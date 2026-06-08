package edu.gazi.ceng479.bfs.viz.event;

/**
 * Per-level node discovery batch for the demo-track graph animation (design.md §18.3,
 * §19.1). Carries the ids discovered at this level grouped by the thread that claimed
 * each, so the UI can colour nodes by owning thread. Emitted only for small demo graphs
 * (Principle B, design.md §14.2) to avoid flooding the stream on huge graphs.
 *
 * @param level         the level whose nodes were just discovered
 * @param nodesByThread {@code nodesByThread[t]} = ids claimed by thread t at this level
 */
public record FrontierBatchEvent(
        String runId, long ts, int level, int[][] nodesByThread) implements Event {

    @Override
    public String type() {
        return "FrontierBatchEvent";
    }
}
