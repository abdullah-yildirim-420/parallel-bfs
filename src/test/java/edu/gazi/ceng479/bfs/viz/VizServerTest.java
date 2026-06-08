package edu.gazi.ceng479.bfs.viz;

import edu.gazi.ceng479.bfs.viz.server.VizServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Integration test for the Javalin {@link VizServer} REST + WebSocket contract (design.md §18.4, §26). */
class VizServerTest {

    private VizServer server;
    private int port;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws Exception {
        port = freePort();
        server = new VizServer(port, null).start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop();
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthAndSystemEndpoints() throws Exception {
        HttpResponse<String> health = get("/api/health");
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("ok"));

        HttpResponse<String> sys = get("/api/system");
        assertEquals(200, sys.statusCode());
        assertTrue(sys.body().contains("SystemSampleEvent"));
        assertTrue(sys.body().contains("heapUsedBytes"));
    }

    @Test
    void runEndpointReturnsRunId() throws Exception {
        HttpResponse<String> run = post("/api/run?mode=parallel&threads=4&n=1500&deg=8");
        assertEquals(200, run.statusCode());
        assertTrue(run.body().contains("runId"));
        assertTrue(run.body().contains("vertices"));
    }

    @Test
    void liveWebSocketReceivesRunEvents() throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
        WebSocket ws = http.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/live"), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messages.add(data.toString());
                        webSocket.request(1);
                        return null;
                    }
                }).get(5, TimeUnit.SECONDS);

        // trigger a run; events should stream to the socket
        post("/api/run?mode=parallel&threads=4&n=2000&deg=10");

        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline
                && messages.stream().noneMatch(m -> m.contains("RunCompletedEvent"))) {
            Thread.sleep(20);
        }
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");

        assertTrue(messages.stream().anyMatch(m -> m.contains("RunStartedEvent")),
                "should receive RunStartedEvent");
        assertTrue(messages.stream().anyMatch(m -> m.contains("LevelCompletedEvent")),
                "should receive LevelCompletedEvent");
        assertTrue(messages.stream().anyMatch(m -> m.contains("RunCompletedEvent")),
                "should receive RunCompletedEvent");
    }

    @Test
    void benchEndpointReturnsAggregates() throws Exception {
        HttpResponse<String> bench = post("/api/bench?threads=1,2&reps=2&warmup=0&n=3000&deg=10");
        assertEquals(200, bench.statusCode());
        assertTrue(bench.body().contains("speedup"));
        assertTrue(bench.body().contains("sequential"));
    }
}
