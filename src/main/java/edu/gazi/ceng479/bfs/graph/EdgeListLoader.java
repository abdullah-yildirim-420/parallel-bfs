package edu.gazi.ceng479.bfs.graph;

import edu.gazi.ceng479.bfs.util.IntArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a SNAP-format edge-list file into a CSR {@link Graph} (design.md §3.2).
 *
 * <p>Format: lines beginning with {@code #} are comments and ignored; blank lines
 * ignored; data lines are {@code srcId<whitespace>dstId} where ids are arbitrary,
 * possibly non-contiguous integers. Ids are remapped to a dense range [0, V) in
 * order of first appearance. Self-loops are dropped. Duplicate edges are kept
 * (BFS-safe). For undirected graphs each edge is stored in both directions by
 * {@link CsrBuilder}.
 */
public final class EdgeListLoader {

    private EdgeListLoader() {
    }

    /**
     * Load a graph from a file.
     *
     * @param path     edge-list file path
     * @param directed whether to treat edges as directed (web-Google) or undirected (com-Orkut)
     * @param maxEdges cap on edges to ingest, or a non-positive value for unlimited
     * @return the constructed graph
     * @throws IOException on read/parse failure
     */
    public static Graph load(Path path, boolean directed, long maxEdges) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader, directed, maxEdges);
        }
    }

    /** Convenience for tests: parse from a raw string. */
    public static Graph loadFromString(String content, boolean directed, long maxEdges) {
        try (Reader r = new StringReader(content)) {
            return load(new BufferedReader(r), directed, maxEdges);
        } catch (IOException e) {
            throw new RuntimeException("unexpected IO error parsing string", e);
        }
    }

    /**
     * Core parser working over any {@link BufferedReader}.
     */
    public static Graph load(BufferedReader reader, boolean directed, long maxEdges) throws IOException {
        Map<Integer, Integer> idMap = new HashMap<>();
        IntArrayList src = new IntArrayList();
        IntArrayList dst = new IntArrayList();
        long limit = maxEdges > 0 ? maxEdges : Long.MAX_VALUE;
        long edges = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            if (edges >= limit) {
                break;
            }
            int len = line.length();
            // Skip leading whitespace.
            int i = 0;
            while (i < len && isSpace(line.charAt(i))) {
                i++;
            }
            if (i >= len || line.charAt(i) == '#') {
                continue; // blank or comment
            }
            // Parse first token (source).
            int t1Start = i;
            while (i < len && !isSpace(line.charAt(i))) {
                i++;
            }
            int rawSrc = parseInt(line, t1Start, i);
            // Skip separators.
            while (i < len && isSpace(line.charAt(i))) {
                i++;
            }
            if (i >= len) {
                continue; // malformed: only one token
            }
            int t2Start = i;
            while (i < len && !isSpace(line.charAt(i))) {
                i++;
            }
            int rawDst = parseInt(line, t2Start, i);

            if (rawSrc == rawDst) {
                continue; // drop self-loop
            }
            int u = idMap.computeIfAbsent(rawSrc, k -> idMap.size());
            int v = idMap.computeIfAbsent(rawDst, k -> idMap.size());
            src.add(u);
            dst.add(v);
            edges++;
        }

        int numVertices = idMap.size();
        return CsrBuilder.build(numVertices, src.backingArray(), dst.backingArray(), (int) edges, directed);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\f';
    }

    private static int parseInt(String s, int start, int end) {
        // Lightweight non-negative integer parse (SNAP ids are non-negative).
        int val = 0;
        boolean any = false;
        for (int k = start; k < end; k++) {
            char c = s.charAt(k);
            if (c < '0' || c > '9') {
                throw new NumberFormatException("invalid id token '" + s.substring(start, end) + "'");
            }
            val = val * 10 + (c - '0');
            any = true;
        }
        if (!any) {
            throw new NumberFormatException("empty id token at [" + start + "," + end + ")");
        }
        return val;
    }
}
