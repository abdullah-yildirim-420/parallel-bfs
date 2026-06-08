package edu.gazi.ceng479.bfs.bfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link FrontierPartitioner} (design.md §3.6, §9.1). */
class FrontierPartitionerTest {

    /** Ranges must tile [0,F) exactly: contiguous, disjoint, full coverage. */
    private void assertTiles(int[][] ranges, int f) {
        int cursor = 0;
        for (int[] r : ranges) {
            assertEquals(2, r.length);
            assertEquals(cursor, r[0], "gap/overlap at range start");
            assertTrue(r[1] > r[0], "empty range not allowed");
            cursor = r[1];
        }
        assertEquals(f, cursor, "ranges must cover exactly [0,F)");
    }

    @Test
    void unevenSplitTilesFrontier() {
        int[][] r = FrontierPartitioner.split(10, 4);
        assertTiles(r, 10);
        assertTrue(r.length <= 4);
    }

    @Test
    void evenSplit() {
        int[][] r = FrontierPartitioner.split(8, 4);
        assertEquals(4, r.length);
        assertTiles(r, 8);
        assertArrayEquals(new int[]{0, 2}, r[0]);
        assertArrayEquals(new int[]{6, 8}, r[3]);
    }

    @Test
    void moreThreadsThanNodesDropsEmptyRanges() {
        int[][] r = FrontierPartitioner.split(3, 8);
        assertEquals(3, r.length); // only 3 non-empty parts
        assertTiles(r, 3);
    }

    @Test
    void singleThreadOneRange() {
        int[][] r = FrontierPartitioner.split(5, 1);
        assertEquals(1, r.length);
        assertArrayEquals(new int[]{0, 5}, r[0]);
    }

    @Test
    void emptyFrontierYieldsNoRanges() {
        assertEquals(0, FrontierPartitioner.split(0, 4).length);
    }

    @Test
    void rejectsZeroThreads() {
        assertThrows(IllegalArgumentException.class, () -> FrontierPartitioner.split(5, 0));
    }
}
