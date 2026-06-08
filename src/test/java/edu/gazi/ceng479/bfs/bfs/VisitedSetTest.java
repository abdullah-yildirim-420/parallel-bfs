package edu.gazi.ceng479.bfs.bfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link VisitedSet} implementations (design.md §4.1–4.2). */
class VisitedSetTest {

    @Test
    void boolClaimsOnceThenReportsVisited() {
        VisitedSet v = new VisitedSet.Bool(4);
        assertFalse(v.isVisited(2));
        assertTrue(v.tryClaim(2));
        assertTrue(v.isVisited(2));
        assertFalse(v.tryClaim(2)); // second claim fails
    }

    @Test
    void atomicClaimsOnceThenReportsVisited() {
        VisitedSet v = new VisitedSet.Atomic(4);
        assertFalse(v.isVisited(3));
        assertTrue(v.tryClaim(3));
        assertTrue(v.isVisited(3));
        assertFalse(v.tryClaim(3));
    }

    @Test
    void resetClearsState() {
        VisitedSet v = new VisitedSet.Atomic(2);
        v.tryClaim(0);
        v.tryClaim(1);
        v.reset();
        assertFalse(v.isVisited(0));
        assertTrue(v.tryClaim(0));
    }

    @Test
    void atomicClaimedExactlyOnceUnderConcurrency() throws InterruptedException {
        // Stress: many threads race to claim the same nodes; each node claimed exactly once.
        int size = 10_000;
        VisitedSet v = new VisitedSet.Atomic(size);
        int threads = 8;
        java.util.concurrent.atomic.AtomicInteger totalClaims = new java.util.concurrent.atomic.AtomicInteger();
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            ts[t] = new Thread(() -> {
                int local = 0;
                for (int i = 0; i < size; i++) {
                    if (v.tryClaim(i)) local++;
                }
                totalClaims.addAndGet(local);
            });
        }
        for (Thread th : ts) th.start();
        for (Thread th : ts) th.join();
        // Each node must be claimed by exactly one thread → total successful claims == size.
        assertEquals(size, totalClaims.get());
    }
}
