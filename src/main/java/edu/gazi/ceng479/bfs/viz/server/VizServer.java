package edu.gazi.ceng479.bfs.viz.server;

import edu.gazi.ceng479.bfs.bench.BenchmarkRunner;
import edu.gazi.ceng479.bfs.bench.GraphSpec;
import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.bfs.ParallelBFS;
import edu.gazi.ceng479.bfs.bfs.SequentialBFS;
import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.graph.GraphGenerator;
import edu.gazi.ceng479.bfs.viz.event.EventBus;
import edu.gazi.ceng479.bfs.viz.instr.StreamingInstrumentation;
import edu.gazi.ceng479.bfs.viz.probe.SystemProbe;
import edu.gazi.ceng479.bfs.viz.store.JsonExporter;
import edu.gazi.ceng479.bfs.viz.store.RunStore;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Embedded Javalin server exposing the live event stream over WebSocket and control/
 * history over REST (design.md §16, §18.4). The BFS engines remain UI-agnostic: they
 * emit to the {@link EventBus}, whose dispatcher broadcasts JSON to all WebSocket
 * sessions. The built SPA (when present on the classpath at {@code /public}) is served
 * from the same port for one-command demos (design.md §16.3).
 */
public final class VizServer {

    private final Javalin app;
    private final EventBus bus = new EventBus(16384);
    private final SystemProbe systemProbe = new SystemProbe(bus.asSink());
    private final RaceCoordinator race = new RaceCoordinator();
    private final RunStore store;
    private final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();
    private final ExecutorService jobs = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "viz-job");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong runSeq = new AtomicLong();
    private final int port;

    public VizServer(int port, RunStore store) {
        this.port = port;
        this.store = store;
        boolean hasSpa = VizServer.class.getResource("/public/index.html") != null;
        this.app = Javalin.create(cfg -> {
            if (hasSpa) {
                cfg.staticFiles.add(s -> {
                    s.directory = "/public";
                    s.location = Location.CLASSPATH;
                });
                cfg.spaRoot.addFile("/", "/public/index.html", Location.CLASSPATH);
            }
        });
        configureRoutes(hasSpa);
        bus.subscribe(e -> broadcast(JsonExporter.eventJson(e)));
        bus.start();
    }

    private void configureRoutes(boolean hasSpa) {
        if (!hasSpa) {
            app.get("/", ctx -> ctx.html(statusPage()));
        }
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok", "port", port)));
        app.get("/api/system", ctx -> ctx.result(JsonExporter.eventJson(systemProbe.sample()))
                .contentType("application/json"));

        app.get("/api/runs", ctx -> ctx.result(JsonExporter.toJson(
                store != null ? store.loadAll() : List.of())).contentType("application/json"));

        // Start a single observed BFS run; events stream over the WebSocket.
        app.post("/api/run", ctx -> {
            String mode = ctx.queryParam("mode") != null ? ctx.queryParam("mode") : "parallel";
            int threads = intParam(ctx.queryParam("threads"), 4);
            Graph g = buildGraph(ctx.queryParam("n"), ctx.queryParam("deg"), ctx.queryParam("seed"));
            int source = GraphSpec.autoSource(g);
            boolean stream = Boolean.parseBoolean(
                    ctx.queryParam("stream") != null ? ctx.queryParam("stream") : "true");
            String runId = "run-" + runSeq.incrementAndGet();
            jobs.submit(() -> {
                StreamingInstrumentation instr = new StreamingInstrumentation(bus.asSink(), runId, stream);
                if ("sequential".equals(mode)) {
                    new SequentialBFS().runObserved(g, source, instr, stream);
                } else {
                    new ParallelBFS(threads, 1).runObserved(g, source, instr, stream);
                }
            });
            ctx.json(Map.of("runId", runId, "vertices", g.vertexCount(),
                    "edges", g.edgeCount(), "source", source));
        });

        // Start a sequential-vs-parallel race.
        app.post("/api/race", ctx -> {
            int threads = intParam(ctx.queryParam("threads"), 4);
            Graph g = buildGraph(ctx.queryParam("n"), ctx.queryParam("deg"), ctx.queryParam("seed"));
            int source = GraphSpec.autoSource(g);
            race.start(g, source, threads, bus.asSink());
            ctx.json(Map.of("seqRunId", RaceCoordinator.SEQ_RUN_ID,
                    "parRunId", RaceCoordinator.PAR_RUN_ID,
                    "vertices", g.vertexCount(), "edges", g.edgeCount(), "source", source));
        });

        // Run a (small) clean benchmark grid synchronously and persist it.
        app.post("/api/bench", ctx -> {
            int[] threadList = intList(ctx.queryParam("threads"), new int[]{1, 2, 4, 8});
            int reps = intParam(ctx.queryParam("reps"), 3);
            int warmup = intParam(ctx.queryParam("warmup"), 1);
            Graph g = buildGraph(ctx.queryParam("n"), ctx.queryParam("deg"), ctx.queryParam("seed"));
            GraphSpec spec = GraphSpec.auto("ui-" + System.currentTimeMillis(), "ui", g);
            BenchmarkRunner runner = new BenchmarkRunner(threadList, reps, warmup);
            runner.run(spec);
            List<AggRecord> agg = runner.aggRecords();
            if (store != null) {
                store.saveGroup("group-" + System.currentTimeMillis(), System.currentTimeMillis(),
                        agg, System.getProperty("java.version"),
                        String.valueOf(Runtime.getRuntime().availableProcessors()) + " cores");
            }
            ctx.result(JsonExporter.toJson(agg)).contentType("application/json");
        });

        app.ws("/live", ws -> {
            ws.onConnect(sessions::add);
            ws.onClose(ctx -> sessions.remove(ctx));
            ws.onError(ctx -> sessions.remove(ctx));
        });
    }

    private Graph buildGraph(String n, String deg, String seed) {
        return GraphGenerator.erdosRenyi(intParam(n, 2000), intParam(deg, 8), longParam(seed, 42L));
    }

    private void broadcast(String json) {
        for (WsContext ctx : sessions) {
            try {
                if (ctx.session.isOpen()) {
                    ctx.send(json);
                }
            } catch (RuntimeException ex) {
                sessions.remove(ctx);
            }
        }
    }

    public VizServer start() {
        systemProbe.start(250);
        app.start(port);
        return this;
    }

    public void stop() {
        systemProbe.stop();
        race.shutdown();
        bus.stop();
        jobs.shutdownNow();
        app.stop();
    }

    public int port() {
        return port;
    }

    private static int intParam(String s, int def) {
        try {
            return s == null ? def : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long longParam(String s, long def) {
        try {
            return s == null ? def : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int[] intList(String s, int[] def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }

    private String statusPage() {
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>Parallel BFS Platform</title>
                <style>body{font-family:system-ui;background:#0d1117;color:#c9d1d9;margin:40px}
                code{background:#161b22;padding:2px 6px;border-radius:4px;color:#58a6ff}
                h1{color:#58a6ff}</style></head><body>
                <h1>Parallel BFS — Visualization Backend</h1>
                <p>Backend is running on port %d. The React dashboard is not bundled yet
                (build it into <code>src/main/resources/public</code>). REST/WS endpoints are live:</p>
                <ul>
                  <li><code>GET  /api/health</code></li>
                  <li><code>GET  /api/system</code> — live JMX sample</li>
                  <li><code>GET  /api/runs</code> — benchmark history</li>
                  <li><code>POST /api/run?mode=parallel&threads=4&n=2000&deg=8</code></li>
                  <li><code>POST /api/race?threads=4&n=1500&deg=10</code></li>
                  <li><code>POST /api/bench?threads=1,2,4,8&reps=3&n=200000&deg=40</code></li>
                  <li><code>WS   /live</code> — live event stream</li>
                </ul></body></html>""".formatted(port);
    }
}
