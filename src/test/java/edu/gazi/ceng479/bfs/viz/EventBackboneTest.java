package edu.gazi.ceng479.bfs.viz;

import edu.gazi.ceng479.bfs.bfs.BfsResult;
import edu.gazi.ceng479.bfs.bfs.ParallelBFS;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import edu.gazi.ceng479.bfs.viz.event.Event;
import edu.gazi.ceng479.bfs.viz.event.EventBus;
import edu.gazi.ceng479.bfs.viz.event.LevelCompletedEvent;
import edu.gazi.ceng479.bfs.viz.event.RunCompletedEvent;
import edu.gazi.ceng479.bfs.viz.event.RunStartedEvent;
import edu.gazi.ceng479.bfs.viz.instr.Instrumentation;
import edu.gazi.ceng479.bfs.viz.instr.StreamingInstrumentation;
import edu.gazi.ceng479.bfs.viz.instr.ThreadStat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the event/instrumentation backbone (design.md §18, §23, §26). */
class EventBackboneTest {

    /** Collects events on the calling thread (NO_OP-style sink, no bus). */
    private static final class Collector implements edu.gazi.ceng479.bfs.viz.event.EventSink {
        final List<Event> events = new ArrayList<>();
        @Override public void emit(Event e) { events.add(e); }
    }

    @Test
    void observedParallelResultEqualsCleanResult() {
        // SC-1 also holds for the observed path: same level array as sequential.
        Graph g = GraphGenerator.erdosRenyi(5000, 20, 3L);
        BfsResult ref = new SequentialBFS().run(g, 0);
        BfsResult cleanPar = new ParallelBFS(4, 1).run(g, 0);
        BfsResult observedPar = new ParallelBFS(4, 1)
                .runObserved(g, 0, Instrumentation.NO_OP, false);
        assertTrue(ref.isEquivalentTo(cleanPar));
        assertTrue(ref.isEquivalentTo(observedPar));
        assertTrue(cleanPar.isEquivalentTo(observedPar));
    }

    @Test
    void observedSequentialMatchesCleanSequential() {
        Graph g = GraphGenerator.erdosRenyi(3000, 10, 9L);
        BfsResult clean = new SequentialBFS().run(g, 0);
        BfsResult observed = new SequentialBFS().runObserved(g, 0, Instrumentation.NO_OP, false);
        assertTrue(clean.isEquivalentTo(observed));
    }

    @Test
    void streamingEmitsWellOrderedEvents() {
        Graph g = GraphGenerator.erdosRenyi(2000, 12, 1L);
        Collector c = new Collector();
        StreamingInstrumentation instr = new StreamingInstrumentation(c, "run-1", false);
        new ParallelBFS(4, 1).runObserved(g, 0, instr, false);

        // First event RunStarted, last RunCompleted, levels in between in order.
        assertInstanceOf(RunStartedEvent.class, c.events.get(0));
        assertInstanceOf(RunCompletedEvent.class, c.events.get(c.events.size() - 1));

        int prevLevel = -1;
        int sumDiscovered = 0;
        for (Event e : c.events) {
            if (e instanceof LevelCompletedEvent lc) {
                assertEquals(prevLevel + 1, lc.level(), "levels must be contiguous");
                prevLevel = lc.level();
                sumDiscovered += lc.nextFrontierSize();
                assertFalse(lc.threads().isEmpty());
            }
        }
        RunCompletedEvent done = (RunCompletedEvent) c.events.get(c.events.size() - 1);
        // source + all discovered == reached
        assertEquals(done.reached(), 1 + sumDiscovered);
    }

    @Test
    void perThreadProcessedCountsReconcileWithFrontier() {
        Graph g = GraphGenerator.erdosRenyi(4000, 30, 5L);
        Collector c = new Collector();
        new ParallelBFS(8, 1).runObserved(g, 0, new StreamingInstrumentation(c, "r", false), false);
        for (Event e : c.events) {
            if (e instanceof LevelCompletedEvent lc) {
                long processed = lc.threads().stream().mapToLong(ThreadStat::processed).sum();
                assertEquals(lc.frontierSize(), processed,
                        "sum of per-thread processed must equal frontier size at level " + lc.level());
            }
        }
    }

    @Test
    void captureNodesGroupsDiscoveredByThread() {
        Graph g = GraphGenerator.erdosRenyi(1500, 16, 2L);
        Collector c = new Collector();
        new ParallelBFS(4, 1).runObserved(g, 0, new StreamingInstrumentation(c, "r", true), true);
        int batchNodes = 0;
        for (Event e : c.events) {
            if (e instanceof edu.gazi.ceng479.bfs.viz.event.FrontierBatchEvent fb) {
                for (int[] perThread : fb.nodesByThread()) {
                    batchNodes += perThread.length;
                }
            }
        }
        // every non-source reached node appears exactly once across batches
        BfsResult ref = new SequentialBFS().run(g, 0);
        assertEquals(ref.reachedCount() - 1, batchNodes);
    }

    @Test
    void eventBusDeliversToSubscribersAsync() throws InterruptedException {
        EventBus bus = new EventBus(1024);
        CopyOnWriteArrayList<Event> received = new CopyOnWriteArrayList<>();
        bus.subscribe(received::add);
        bus.start();

        Graph g = GraphGenerator.erdosRenyi(1000, 10, 7L);
        new ParallelBFS(4, 1).runObserved(g, 0,
                new StreamingInstrumentation(bus.asSink(), "bus-run", false), false);

        // allow the dispatcher to drain
        long deadline = System.currentTimeMillis() + 2000;
        while (received.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        bus.stop();
        assertFalse(received.isEmpty(), "bus must deliver events to subscribers");
        assertEquals(0, bus.droppedCount(), "no drops expected at this volume");
        assertInstanceOf(RunStartedEvent.class, received.get(0));
    }
}
