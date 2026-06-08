package edu.gazi.ceng479.bfs.viz.event;

/**
 * Destination for emitted events (design.md §23). BFS engines depend only on this
 * abstraction, never on the transport — which keeps the algorithm decoupled from the UI
 * and lets the clean path use a no-op sink (Principle A).
 */
@FunctionalInterface
public interface EventSink {

    void emit(Event e);

    /** A sink that discards everything — used by the clean path and in tests. */
    EventSink NO_OP = e -> {
    };
}
