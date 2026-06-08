package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.bench.Records.RunRecord;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes benchmark results to CSV with the fixed schemas of design.md §7.2.
 * The CSV pipeline feeds the report figures/tables (design.md §11, §20.1).
 */
public final class CsvWriter {

    private static final String RAW_HEADER =
            "graph_name,vertices,edges,graph_type,mode,threads,rep,bfs_ns,reached,max_level,source";
    private static final String AGG_HEADER =
            "graph_name,vertices,edges,graph_type,mode,threads,"
                    + "mean_ns,sd_ns,ci95_ns,mean_ms,speedup,efficiency_pct,amdahl_pred,"
                    + "reached,max_level,source";

    private CsvWriter() {
    }

    public static void writeRaw(Path path, List<RunRecord> rows) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(RAW_HEADER);
            w.write('\n');
            for (RunRecord r : rows) {
                w.write(String.join(",",
                        r.graphName(), String.valueOf(r.vertices()), String.valueOf(r.edges()),
                        r.graphType(), r.mode(), String.valueOf(r.threads()), String.valueOf(r.rep()),
                        String.valueOf(r.bfsNanos()), String.valueOf(r.reached()),
                        String.valueOf(r.maxLevel()), String.valueOf(r.source())));
                w.write('\n');
            }
        }
    }

    public static void writeAgg(Path path, List<AggRecord> rows) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(AGG_HEADER);
            w.write('\n');
            for (AggRecord r : rows) {
                w.write(String.join(",",
                        r.graphName(), String.valueOf(r.vertices()), String.valueOf(r.edges()),
                        r.graphType(), r.mode(), String.valueOf(r.threads()),
                        f(r.meanNs()), f(r.sdNs()), f(r.ci95Ns()), f(r.meanMs()),
                        f(r.speedup()), f(r.efficiencyPct()), f(r.amdahlPred()),
                        String.valueOf(r.reached()), String.valueOf(r.maxLevel()),
                        String.valueOf(r.source())));
                w.write('\n');
            }
        }
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
