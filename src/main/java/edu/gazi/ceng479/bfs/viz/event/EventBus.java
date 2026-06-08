package edu.gazi.ceng479.bfs.viz.event;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Decouples fast event producers (BFS worker threads) from slow consumers (network/UI)
 * via a bounded ring buffer drained by a single dispatcher thread (design.md §18.2).
 *
 * <p>{@link #offer(Event)} is non-blocking: if the buffer is full the event is dropped
 * and counted, so the BFS path is NEVER stalled by a slow consumer (reinforcing
 * Principle A even on the observed path). The dispatcher forwards each event to all
 * registered listeners (e.g. the WebSocket broadcaster, persistence collector).
 */
public final class EventBus {

    private final BlockingQueue<Event> queue;
    private final CopyOnWriteArrayList<Consumer<Event>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean running = false;
    private Thread dispatcher;

    public EventBus() {
        this(8192);
    }

    public EventBus(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /** Register a consumer invoked (on the dispatcher thread) for every event. */
    public void subscribe(Consumer<Event> listener) {
        listeners.add(listener);
    }

    /** Non-blocking publish. Returns false (and increments the drop counter) if full. */
    public boolean offer(Event e) {
        boolean ok = queue.offer(e);
        if (!ok) {
            dropped.incrementAndGet();
        }
        return ok;
    }

    /** @return number of events dropped due to a full buffer (backpressure telemetry). */
    public long droppedCount() {
        return dropped.get();
    }

    /** A {@link EventSink} view that publishes to this bus. */
    public EventSink asSink() {
        return this::offer;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        dispatcher = new Thread(this::dispatchLoop, "event-dispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    public synchronized void stop() {
        running = false;
        if (dispatcher != null) {
            dispatcher.interrupt();
            dispatcher = null;
        }
    }

    private void dispatchLoop() {
        while (running || !queue.isEmpty()) {
            try {
                Event e = queue.poll(50, TimeUnit.MILLISECONDS);
                if (e != null) {
                    for (Consumer<Event> l : listeners) {
                        try {
                            l.accept(e);
                        } catch (RuntimeException ignored) {
                            // a faulty listener must not kill the dispatcher
                        }
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                if (!running) {
                    break;
                }
            }
        }
    }
}
