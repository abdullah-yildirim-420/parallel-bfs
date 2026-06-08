package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.util.IntArrayList;
import edu.gazi.ceng479.bfs.viz.instr.Instrumentation;
import edu.gazi.ceng479.bfs.viz.instr.ThreadStat;

import java.util.Arrays;
import java.util.List;

/**
 * Sequential breadth-first search — the correctness oracle and performance baseline
 * (design.md §3.4, Listing 1 §5.1).
 *
 * <p>Uses a primitive {@code int[]} ring-buffer queue and the {@code level} array as
 * the visited marker ({@code level[v] == -1} ⇔ unvisited). This deliberately avoids
 * {@code Queue<Integer>} autoboxing so the baseline is fast and the measured speedup
 * is honest (design.md §3.4, R-4).
 *
 * <p>Complexity: O(V + E) time, O(V) space. Each edge is examined exactly once.
 */
public final class SequentialBFS implements BFS, ObservableBFS {

    @Override
    public String name() {
        return "sequential";
    }

    @Override
    public BfsResult run(Graph graph, int source) {
        final int n = graph.vertexCount();
        if (n == 0) {
            throw new IllegalArgumentException("cannot run BFS on an empty graph");
        }
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("source " + source + " out of range [0," + n + ")");
        }

        int[] level = new int[n];
        int[] parent = new int[n];
        Arrays.fill(level, -1);
        Arrays.fill(parent, -1);

        // Ring buffer: each node is enqueued at most once, so capacity n suffices.
        int[] queue = new int[n];
        int head = 0, tail = 0;

        level[source] = 0;
        queue[tail++] = source;

        int reached = 1;
        int maxLevel = 0;

        while (head < tail) {
            int node = queue[head++];
            int nodeLevel = level[node];
            int begin = graph.neighborBegin(node);
            int end = graph.neighborEnd(node);
            for (int i = begin; i < end; i++) {
                int nbr = graph.neighborAt(i);
                if (level[nbr] == -1) {
                    level[nbr] = nodeLevel + 1;
                    parent[nbr] = node;
                    if (level[nbr] > maxLevel) {
                        maxLevel = level[nbr];
                    }
                    queue[tail++] = nbr;
                    reached++;
                }
            }
        }

        return new BfsResult(source, level, parent, reached, maxLevel);
    }

    /**
     * Instrumented, level-structured sequential traversal (design.md §23). Produces the
     * same result as {@link #run} but walks the graph frontier-by-frontier so it can emit
     * per-level events for the visualization. This is a separate method; the clean
     * {@link #run} above is untouched (Principle A).
     */
    @Override
    public BfsResult runObserved(Graph graph, int source, Instrumentation instr, boolean captureNodes) {
        final int n = graph.vertexCount();
        if (n == 0) {
            throw new IllegalArgumentException("cannot run BFS on an empty graph");
        }
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("source " + source + " out of range [0," + n + ")");
        }

        int[] level = new int[n];
        int[] parent = new int[n];
        Arrays.fill(level, -1);
        Arrays.fill(parent, -1);

        long runStart = System.nanoTime();
        instr.onRunStart(name(), 1, n, graph.edgeCount(), source);

        level[source] = 0;
        int[] frontier = {source};
        int curLevel = 0;
        int reached = 1;
        int maxLevel = 0;

        while (frontier.length > 0) {
            long levelStart = System.nanoTime();
            int childLevel = curLevel + 1;
            long edges = 0;
            IntArrayList next = new IntArrayList();
            for (int node : frontier) {
                int begin = graph.neighborBegin(node);
                int end = graph.neighborEnd(node);
                edges += (end - begin);
                for (int i = begin; i < end; i++) {
                    int nbr = graph.neighborAt(i);
                    if (level[nbr] == -1) {
                        level[nbr] = childLevel;
                        parent[nbr] = node;
                        next.add(nbr);
                    }
                }
            }
            long busy = System.nanoTime() - levelStart;
            if (!next.isEmpty()) {
                maxLevel = childLevel;
                reached += next.size();
            }
            List<ThreadStat> stats = List.of(new ThreadStat(0, frontier.length, edges, busy, 0));
            int[][] nodesByThread = captureNodes ? new int[][]{next.toArray()} : null;
            instr.onLevelComplete(curLevel, frontier.length, next.size(), reached, 0, stats, nodesByThread);

            frontier = next.toArray();
            curLevel = childLevel;
        }

        long total = System.nanoTime() - runStart;
        instr.onRunComplete(total, reached, maxLevel);
        return new BfsResult(source, level, parent, reached, maxLevel);
    }
}
