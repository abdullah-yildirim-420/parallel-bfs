package edu.gazi.ceng479.bfs.viz.event;

/**
 * Base type for all live events streamed from the BFS engines to the visualization
 * layer (design.md §18.3). Events are immutable and carry a {@code runId} so multiple
 * concurrent runs (e.g. the seq-vs-par race) can be demultiplexed by the UI.
 *
 * <p>Events are produced ONLY on the observed BFS path; the clean benchmark path emits
 * nothing (Principle A, design.md §14.1).
 */
public sealed interface Event
        permits RunStartedEvent, LevelCompletedEvent, FrontierBatchEvent, RunCompletedEvent,
        SystemSampleEvent {

    /** @return discriminator used as the JSON {@code type} field (design.md §18.4). */
    String type();

    /** @return event creation time ({@link System#currentTimeMillis()}). */
    long ts();

    /** @return id of the run this event belongs to. */
    String runId();
}
