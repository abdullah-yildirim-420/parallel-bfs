package edu.gazi.ceng479.bfs.viz;

import edu.gazi.ceng479.bfs.bench.BenchmarkRunner;
import edu.gazi.ceng479.bfs.bench.GraphSpec;
import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import edu.gazi.ceng479.bfs.viz.store.JsonExporter;
import edu.gazi.ceng479.bfs.viz.store.RunStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for SQLite persistence and JSON export (design.md §20.1, §26 round-trip). */
class PersistenceTest {

    private List<AggRecord> sampleAgg() {
        GraphSpec spec = GraphSpec.auto("p-test", "synthetic-sparse",
                GraphGenerator.erdosRenyi(2000, 8, 1L));
        BenchmarkRunner runner = new BenchmarkRunner(new int[]{1, 2, 4}, 2, 0);
        runner.run(spec);
        return runner.aggRecords();
    }

    @Test
    void runStoreRoundTrip(@TempDir Path tmp) {
        List<AggRecord> agg = sampleAgg();
        Path db = tmp.resolve("history.db");
        try (RunStore store = new RunStore(db.toString())) {
            store.saveGroup("g1", System.currentTimeMillis(), agg, "21", "8 cores");
            List<AggRecord> loaded = store.loadAll();
            assertEquals(agg.size(), loaded.size());
            assertEquals(1, store.listGroups().size());
            // sequential row preserved
            assertTrue(loaded.stream().anyMatch(r -> r.mode().equals("sequential") && r.speedup() == 1.0));
            // speedup values survive round-trip
            for (AggRecord src : agg) {
                assertTrue(loaded.stream().anyMatch(
                        r -> r.mode().equals(src.mode()) && r.threads() == src.threads()
                                && Math.abs(r.speedup() - src.speedup()) < 1e-9));
            }
        }
    }

    @Test
    void multipleGroupsAccumulate(@TempDir Path tmp) {
        Path db = tmp.resolve("h.db");
        try (RunStore store = new RunStore(db.toString())) {
            store.saveGroup("a", 1000, sampleAgg(), "21", "x");
            store.saveGroup("b", 2000, sampleAgg(), "21", "x");
            assertEquals(2, store.listGroups().size());
        }
    }

    @Test
    void jsonExportProducesArray(@TempDir Path tmp) throws IOException {
        List<AggRecord> agg = sampleAgg();
        Path json = tmp.resolve("agg.json");
        JsonExporter.writeAgg(json, agg);
        String content = Files.readString(json);
        assertTrue(content.trim().startsWith("["));
        assertTrue(content.contains("\"speedup\""));
        assertTrue(content.contains("\"graphName\""));
    }
}
