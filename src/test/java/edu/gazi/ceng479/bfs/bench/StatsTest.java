package edu.gazi.ceng479.bfs.bench;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link Stats} (design.md §5.3–5.4). */
class StatsTest {

    @Test
    void meanAndStdDev() {
        double[] xs = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, Stats.mean(xs), 1e-9);
        // sample sd of this classic dataset = sqrt(32/7) ~= 2.138
        assertEquals(Math.sqrt(32.0 / 7.0), Stats.stdDev(xs), 1e-9);
    }

    @Test
    void ci95UsesTValueForFiveReps() {
        double[] xs = {10, 10, 10, 10, 10};
        assertEquals(0.0, Stats.ci95(xs), 1e-9); // zero variance
        assertEquals(2.776, Stats.tValue95(4), 1e-9); // df = n-1 = 4
    }

    @Test
    void speedupAndEfficiency() {
        assertEquals(4.0, Stats.speedup(100, 25), 1e-9);
        assertEquals(100.0, Stats.efficiencyPct(4.0, 4), 1e-9);
        assertEquals(75.0, Stats.efficiencyPct(3.0, 4), 1e-9);
    }

    @Test
    void amdahlMatchesProposalTable() {
        // proposal Table 1 with p=0.90
        assertEquals(1.82, Stats.amdahl(2), 0.01);
        assertEquals(3.08, Stats.amdahl(4), 0.01);
        assertEquals(4.71, Stats.amdahl(8), 0.01);
        assertEquals(1.0, Stats.amdahl(1), 1e-9);
    }

    @Test
    void largeDfFallsBackToNormal() {
        assertEquals(1.96, Stats.tValue95(100), 1e-9);
    }
}
