package edu.gazi.ceng479.bfs.bfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for SC-1 equivalence semantics of {@link BfsResult} (design.md §3.9, §9.1). */
class BfsResultTest {

    @Test
    void equivalentWhenLevelsMatchEvenIfParentsDiffer() {
        BfsResult a = new BfsResult(0, new int[]{0, 1, 1, 2}, new int[]{-1, 0, 0, 1}, 4, 2);
        BfsResult b = new BfsResult(0, new int[]{0, 1, 1, 2}, new int[]{-1, 0, 0, 2}, 4, 2); // diff parent
        assertTrue(a.isEquivalentTo(b));
        assertEquals(-1, a.firstDifferingVertex(b));
    }

    @Test
    void notEquivalentWhenLevelsDiffer() {
        BfsResult a = new BfsResult(0, new int[]{0, 1, 1, 2}, new int[]{-1, 0, 0, 1}, 4, 2);
        BfsResult b = new BfsResult(0, new int[]{0, 1, 2, 2}, new int[]{-1, 0, 1, 1}, 4, 2);
        assertFalse(a.isEquivalentTo(b));
        assertEquals(2, a.firstDifferingVertex(b));
    }

    @Test
    void notEquivalentWhenReachedCountDiffers() {
        BfsResult a = new BfsResult(0, new int[]{0, 1, -1}, new int[]{-1, 0, -1}, 2, 1);
        BfsResult b = new BfsResult(0, new int[]{0, 1, -1}, new int[]{-1, 0, -1}, 3, 1);
        assertFalse(a.isEquivalentTo(b));
    }

    @Test
    void notEquivalentToNull() {
        BfsResult a = new BfsResult(0, new int[]{0}, new int[]{-1}, 1, 0);
        assertFalse(a.isEquivalentTo(null));
    }
}
