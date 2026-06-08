package edu.gazi.ceng479.bfs;

import edu.gazi.ceng479.bfs.bench.BenchmarkRunner;
import edu.gazi.ceng479.bfs.bench.CsvWriter;
import edu.gazi.ceng479.bfs.bench.GraphSpec;
import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.bench.ResultVerifier;
import edu.gazi.ceng479.bfs.bfs.BfsResult;
import edu.gazi.ceng479.bfs.bfs.ParallelBFS;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.cli.ArgParser;
import edu.gazi.ceng479.bfs.cli.Config;
import edu.gazi.ceng479.bfs.graph.EdgeListLoader;
import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import edu.gazi.ceng479.bfs.viz.server.VizServer;
import edu.gazi.ceng479.bfs.viz.store.RunStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CLI entry point (design.md §7.1, §7.3). Dispatches modes seq/par/bench/verify.
 * Exit codes follow design.md §7.3 (1 bad args, 2 IO, 4 correctness mismatch).
 */
public final class Main {

    public static void main(String[] args) {
        Config cfg;
        try {
            cfg = ArgParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        try {
            switch (cfg.mode) {
                case SEQ -> runSeq(cfg);
                case PAR -> runPar(cfg);
                case VERIFY -> runVerify(cfg);
                case BENCH -> runBench(cfg);
                case UI -> runUi(cfg);
            }
        } catch (ResultVerifier.VerificationException e) {
            System.err.println("CORRECTNESS MISMATCH: " + e.getMessage());
            System.exit(4);
        } catch (java.io.IOException e) {
            System.err.println("IO error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static Graph resolveGraph(Config cfg) throws java.io.IOException {
        if (cfg.graphPath != null) {
            System.out.printf("Loading graph from %s (directed=%b)...%n", cfg.graphPath, cfg.directed);
            return EdgeListLoader.load(Path.of(cfg.graphPath), cfg.directed, cfg.maxEdges);
        }
        System.out.printf("Generating Erdos-Renyi graph n=%d deg=%d seed=%d...%n",
                cfg.genN, cfg.genDeg, cfg.genSeed);
        return GraphGenerator.erdosRenyi(cfg.genN, cfg.genDeg, cfg.genSeed);
    }

    private static int resolveSource(Config cfg, Graph g) {
        return cfg.source >= 0 ? cfg.source : GraphSpec.autoSource(g);
    }

    private static void runSeq(Config cfg) throws java.io.IOException {
        Graph g = resolveGraph(cfg);
        int src = resolveSource(cfg, g);
        long t0 = System.nanoTime();
        BfsResult r = new SequentialBFS().run(g, src);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        printResult("sequential", g, src, r, ms);
    }

    private static void runPar(Config cfg) throws java.io.IOException {
        Graph g = resolveGraph(cfg);
        int src = resolveSource(cfg, g);
        long t0 = System.nanoTime();
        BfsResult r = new ParallelBFS(cfg.threads).run(g, src);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        printResult("parallel-" + cfg.threads, g, src, r, ms);
    }

    private static void runVerify(Config cfg) throws java.io.IOException {
        Graph g = resolveGraph(cfg);
        int src = resolveSource(cfg, g);
        BfsResult ref = new SequentialBFS().run(g, src);
        BfsResult par = new ParallelBFS(cfg.threads).run(g, src);
        ResultVerifier.assertEquivalent(ref, par); // throws -> exit 4
        System.out.printf("VERIFY OK: parallel-%d == sequential on %s (reached=%d, maxLevel=%d)%n",
                cfg.threads, describe(g), ref.reachedCount(), ref.maxLevel());
    }

    private static void runBench(Config cfg) throws java.io.IOException {
        List<GraphSpec> specs = buildSpecs(cfg);
        BenchmarkRunner runner = new BenchmarkRunner(cfg.threadList, cfg.reps, cfg.warmups);
        for (GraphSpec spec : specs) {
            System.out.printf("%n=== Benchmarking %s (%s): V=%d E=%d source=%d ===%n",
                    spec.name(), spec.type(), spec.graph().vertexCount(),
                    spec.graph().edgeCount(), spec.source());
            runner.run(spec);
        }
        Path raw = Path.of(cfg.outDir, "results_raw.csv");
        Path agg = Path.of(cfg.outDir, "results_agg.csv");
        CsvWriter.writeRaw(raw, runner.rawRecords());
        CsvWriter.writeAgg(agg, runner.aggRecords());
        printAggTable(runner.aggRecords());
        System.out.printf("%nWrote %s and %s%n", raw.toAbsolutePath(), agg.toAbsolutePath());
    }

    /** Build benchmark targets: a custom one if --gen/--graph given, else a default grid. */
    private static List<GraphSpec> buildSpecs(Config cfg) throws java.io.IOException {
        List<GraphSpec> specs = new ArrayList<>();
        if (cfg.hasGraphSource()) {
            Graph g = resolveGraph(cfg);
            String name = cfg.graphPath != null ? Path.of(cfg.graphPath).getFileName().toString()
                    : ("gen-n" + cfg.genN + "-d" + cfg.genDeg);
            int src = resolveSource(cfg, g);
            specs.add(new GraphSpec(name, "custom", g, src));
            return specs;
        }
        // Default grid (design.md §11 E1/E2 scale, kept modest for a quick run).
        System.out.println("No --graph/--gen given; running default synthetic grid...");
        specs.add(GraphSpec.auto("synthetic-sparse", "synthetic-sparse",
                GraphGenerator.erdosRenyi(1_000_000, 10, 42L)));
        specs.add(GraphSpec.auto("synthetic-dense", "synthetic-dense",
                GraphGenerator.erdosRenyi(400_000, 80, 42L)));
        return specs;
    }

    private static void runUi(Config cfg) throws java.io.IOException {
        java.nio.file.Files.createDirectories(Path.of(cfg.outDir));
        RunStore store = new RunStore(Path.of(cfg.outDir, "history.db").toString());
        VizServer server = new VizServer(cfg.port, store).start();
        System.out.printf("Visualization backend running at http://localhost:%d%n", server.port());
        System.out.println("Endpoints: /api/health /api/system /api/runs /api/run /api/race /api/bench ; WS /live");
        System.out.println("Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            store.close();
        }));
        // Block the main thread while the daemon server runs.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printResult(String mode, Graph g, int src, BfsResult r, long ms) {
        System.out.printf("[%s] %s source=%d reached=%d maxLevel=%d time=%d ms%n",
                mode, describe(g), src, r.reachedCount(), r.maxLevel(), ms);
    }

    private static String describe(Graph g) {
        return "V=" + g.vertexCount() + " E=" + g.edgeCount() + (g.isDirected() ? " directed" : " undirected");
    }

    private static void printAggTable(List<AggRecord> rows) {
        System.out.printf("%n%-20s %-10s %7s %10s %8s %8s %8s%n",
                "graph", "mode", "threads", "mean_ms", "speedup", "eff%", "amdahl");
        for (AggRecord r : rows) {
            System.out.printf(Locale.ROOT, "%-20s %-10s %7d %10.2f %8.2f %8.1f %8.2f%n",
                    r.graphName(), r.mode(), r.threads(), r.meanMs(),
                    r.speedup(), r.efficiencyPct(), r.amdahlPred());
        }
    }
}
