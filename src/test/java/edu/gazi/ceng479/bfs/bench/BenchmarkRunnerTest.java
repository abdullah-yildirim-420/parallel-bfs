package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.bench.Records.RunRecord;
import edu.gazi.ceng479.bfs.bfs.BfsResult;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for the benchmark harness + CSV (design.md §3.7, §9.4). */
class BenchmarkRunnerTest {

    @Test
    void producesWellFormedRecordsAndSc1Holds() {
        GraphSpec spec = GraphSpec.auto("test-dense", "synthetic-dense",
                GraphGenerator.erdosRenyi(20_000, 40, 7L));
        BenchmarkRunner runner = new BenchmarkRunner(new int[]{1, 2, 4}, 3, 1);
        runner.run(spec); // SC-1 verified internally; throws on mismatch

        List<AggRecord> agg = runner.aggRecords();
        // 1 sequential + 3 parallel rows
        assertEquals(4, agg.size());
        AggRecord seq = agg.get(0);
        assertEquals("sequential", seq.mode());
        assertEquals(1.0, seq.speedup(), 1e-9);
        assertEquals(100.0, seq.efficiencyPct(), 1e-9);

        // raw rows = (1 seq + 3 par) configs * 3 reps = 12
        List<RunRecord> raw = runner.rawRecords();
        assertEquals(12, raw.size());
        for (RunRecord r : raw) {
            assertTrue(r.bfsNanos() > 0, "timing must be positive");
            assertTrue(r.reached() > 0);
        }

        // all parallel results reached the same set as sequential reference
        BfsResult ref = new SequentialBFS().run(spec.graph(), spec.source());
        for (AggRecord a : agg) {
            assertEquals(ref.reachedCount(), a.reached());
            assertTrue(a.speedup() > 0);
            assertTrue(a.amdahlPred() >= 1.0);
        }
    }

    @Test
    void csvFilesAreWrittenWithHeaders(@TempDir Path tmp) throws IOException {
        GraphSpec spec = GraphSpec.auto("tiny", "synthetic-sparse",
                GraphGenerator.erdosRenyi(2_000, 8, 1L));
        BenchmarkRunner runner = new BenchmarkRunner(new int[]{1, 2}, 2, 0);
        runner.run(spec);

        Path raw = tmp.resolve("results_raw.csv");
        Path agg = tmp.resolve("results_agg.csv");
        CsvWriter.writeRaw(raw, runner.rawRecords());
        CsvWriter.writeAgg(agg, runner.aggRecords());

        List<String> rawLines = Files.readAllLines(raw);
        List<String> aggLines = Files.readAllLines(agg);
        assertTrue(rawLines.get(0).startsWith("graph_name,vertices,edges"));
        assertTrue(aggLines.get(0).contains("speedup,efficiency_pct,amdahl_pred"));
        assertTrue(rawLines.size() > 1);
        assertTrue(aggLines.size() > 1);
    }
}
