package edu.gazi.ceng479.bfs.bfs;

import edu.gazi.ceng479.bfs.graph.Graph;
import edu.gazi.ceng479.bfs.util.IntArrayList;
import edu.gazi.ceng479.bfs.viz.instr.Instrumentation;
import edu.gazi.ceng479.bfs.viz.instr.ThreadStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Parallel frontier-level breadth-first search using Java threads (design.md §3.5,
 * Listing 2 §5.2).
 *
 * <p>At each BFS level the frontier is partitioned across {@code numThreads} worker
 * threads (an {@link ExecutorService} fixed thread pool created once per run). Each
 * thread expands a disjoint slice, claiming neighbours with a lock-free
 * {@code compareAndSet} on a shared {@link VisitedSet.Atomic}; the only synchronisation
 * points are the atomic claim and the {@link Future#get()} barrier between levels.
 * Writes to {@code level}/{@code parent} are race-free because exactly one thread (the
 * CAS winner) writes any given index (Bernstein's conditions, design.md §3.5, §4.4).
 *
 * <p>Small frontiers (below {@code seqCutoff}) are expanded on the calling thread to
 * avoid coordination overhead dominating (design.md §3.5 D-3), which matters for the
 * many small levels of sparse graphs (SC-3).
 *
 * <p>This is the <em>clean</em> path (no instrumentation) used for honest benchmark
 * timing (design.md §14.1, Principle A).
 */
public final class ParallelBFS implements BFS, ObservableBFS {

    /** Default small-frontier fallback threshold (design.md §3.5 D-3, A-3). */
    public static final int DEFAULT_SEQ_CUTOFF = 1024;

    private final int numThreads;
    private final int seqCutoff;

    public ParallelBFS(int numThreads) {
        this(numThreads, DEFAULT_SEQ_CUTOFF);
    }

    /**
     * @param numThreads worker thread count (&ge; 1)
     * @param seqCutoff  frontiers strictly smaller than this expand serially; use a
     *                   small value in tests to force the parallel path on small graphs
     */
    public ParallelBFS(int numThreads, int seqCutoff) {
        if (numThreads < 1) {
            throw new IllegalArgumentException("numThreads must be >= 1, got " + numThreads);
        }
        if (seqCutoff < 0) {
            throw new IllegalArgumentException("seqCutoff must be >= 0, got " + seqCutoff);
        }
        this.numThreads = numThreads;
        this.seqCutoff = seqCutoff;
    }

    @Override
    public String name() {
        return "parallel-" + numThreads;
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

        final int[] level = new int[n];
        final int[] parent = new int[n];
        Arrays.fill(level, -1);
        Arrays.fill(parent, -1);

        final VisitedSet.Atomic visited = new VisitedSet.Atomic(n);
        visited.tryClaim(source);
        level[source] = 0;

        int[] frontier = {source};
        int curLevel = 0;
        int reached = 1;
        int maxLevel = 0;

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        try {
            while (frontier.length > 0) {
                final int[] frontierRef = frontier;
                final int levelOfChildren = curLevel + 1;
                IntArrayList next;

                if (frontier.length < seqCutoff) {
                    // D-3 small-frontier serial fallback (no task submission).
                    next = expandRange(graph, frontierRef, 0, frontierRef.length,
                            levelOfChildren, visited, level, parent);
                } else {
                    int[][] ranges = FrontierPartitioner.split(frontier.length, numThreads);
                    List<Future<IntArrayList>> futures = new ArrayList<>(ranges.length);
                    for (int[] range : ranges) {
                        final int begin = range[0];
                        final int end = range[1];
                        Callable<IntArrayList> task = () -> expandRange(
                                graph, frontierRef, begin, end, levelOfChildren, visited, level, parent);
                        futures.add(pool.submit(task));
                    }
                    // ---- SYNCHRONIZATION BARRIER ----
                    next = new IntArrayList();
                    for (Future<IntArrayList> f : futures) {
                        next.addAll(getUnchecked(f));
                    }
                }

                if (!next.isEmpty()) {
                    maxLevel = levelOfChildren;
                    reached += next.size();
                }
                frontier = next.toArray();
                curLevel = levelOfChildren;
            }
        } finally {
            pool.shutdown();
        }

        return new BfsResult(source, level, parent, reached, maxLevel);
    }

    /**
     * Expand frontier nodes in index range {@code [begin, end)}: scan neighbours,
     * atomically claim unvisited ones, record level/parent, and collect claimed nodes
     * into a thread-local list. No shared mutable state beyond the atomic visited set
     * and the disjoint-index level/parent writes.
     */
    private static IntArrayList expandRange(Graph graph, int[] frontier, int begin, int end,
                                            int childLevel, VisitedSet.Atomic visited,
                                            int[] level, int[] parent) {
        IntArrayList localNext = new IntArrayList();
        for (int idx = begin; idx < end; idx++) {
            int node = frontier[idx];
            int nb = graph.neighborBegin(node);
            int ne = graph.neighborEnd(node);
            for (int i = nb; i < ne; i++) {
                int nbr = graph.neighborAt(i);
                // Test-and-test-and-set: a cheap volatile read filters the common
                // already-visited case before the expensive CAS, cutting cache-line
                // contention on dense graphs where most claims fail (design.md ADR-005, R-2).
                if (!visited.isVisited(nbr) && visited.tryClaim(nbr)) {
                    level[nbr] = childLevel;
                    parent[nbr] = node;
                    localNext.add(nbr);
                }
            }
        }
        return localNext;
    }

    /**
     * Instrumented parallel traversal (design.md §23). Mirrors {@link #run} but collects
     * per-thread workload and emits per-level events. Separate method → the clean
     * {@link #run} carries no instrumentation (Principle A). Result is identical to the
     * clean path (verified by tests).
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

        final int[] level = new int[n];
        final int[] parent = new int[n];
        Arrays.fill(level, -1);
        Arrays.fill(parent, -1);

        final VisitedSet.Atomic visited = new VisitedSet.Atomic(n);
        visited.tryClaim(source);
        level[source] = 0;

        long runStart = System.nanoTime();
        instr.onRunStart(name(), numThreads, n, graph.edgeCount(), source);

        int[] frontier = {source};
        int curLevel = 0;
        int reached = 1;
        int maxLevel = 0;

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        try {
            while (frontier.length > 0) {
                final int[] frontierRef = frontier;
                final int childLevel = curLevel + 1;

                int[][] ranges = FrontierPartitioner.split(frontier.length, numThreads);
                List<Future<ExpandResult>> futures = new ArrayList<>(ranges.length);
                for (int[] range : ranges) {
                    final int begin = range[0];
                    final int end = range[1];
                    Callable<ExpandResult> task = () -> expandObserved(
                            graph, frontierRef, begin, end, childLevel, visited, level, parent);
                    futures.add(pool.submit(task));
                }

                // ---- BARRIER + per-thread stat assembly ----
                long syncStart = System.nanoTime();
                IntArrayList next = new IntArrayList();
                List<ExpandResult> results = new ArrayList<>(futures.size());
                for (Future<ExpandResult> f : futures) {
                    ExpandResult er = getUnchecked(f);
                    results.add(er);
                    next.addAll(er.next);
                }
                long syncNanos = System.nanoTime() - syncStart;

                long maxBusy = 0;
                for (ExpandResult er : results) {
                    maxBusy = Math.max(maxBusy, er.busyNanos);
                }
                List<ThreadStat> stats = new ArrayList<>(results.size());
                int[][] nodesByThread = captureNodes ? new int[results.size()][] : null;
                for (int t = 0; t < results.size(); t++) {
                    ExpandResult er = results.get(t);
                    stats.add(new ThreadStat(t, er.processed, er.edges, er.busyNanos, maxBusy - er.busyNanos));
                    if (captureNodes) {
                        nodesByThread[t] = er.next.toArray();
                    }
                }

                if (!next.isEmpty()) {
                    maxLevel = childLevel;
                    reached += next.size();
                }
                instr.onLevelComplete(curLevel, frontier.length, next.size(), reached,
                        syncNanos, stats, nodesByThread);

                frontier = next.toArray();
                curLevel = childLevel;
            }
        } finally {
            pool.shutdown();
        }

        long total = System.nanoTime() - runStart;
        instr.onRunComplete(total, reached, maxLevel);
        return new BfsResult(source, level, parent, reached, maxLevel);
    }

    /** Instrumented expansion: same claims as {@link #expandRange} plus workload counters. */
    private static ExpandResult expandObserved(Graph graph, int[] frontier, int begin, int end,
                                               int childLevel, VisitedSet.Atomic visited,
                                               int[] level, int[] parent) {
        long t0 = System.nanoTime();
        IntArrayList localNext = new IntArrayList();
        long edges = 0;
        for (int idx = begin; idx < end; idx++) {
            int node = frontier[idx];
            int nb = graph.neighborBegin(node);
            int ne = graph.neighborEnd(node);
            edges += (ne - nb);
            for (int i = nb; i < ne; i++) {
                int nbr = graph.neighborAt(i);
                if (!visited.isVisited(nbr) && visited.tryClaim(nbr)) {
                    level[nbr] = childLevel;
                    parent[nbr] = node;
                    localNext.add(nbr);
                }
            }
        }
        ExpandResult r = new ExpandResult();
        r.next = localNext;
        r.processed = end - begin;
        r.edges = edges;
        r.busyNanos = System.nanoTime() - t0;
        return r;
    }

    /** Mutable holder for one thread's observed-expansion output (internal). */
    private static final class ExpandResult {
        IntArrayList next;
        long processed;
        long edges;
        long busyNanos;
    }

    private static <T> T getUnchecked(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("parallel BFS interrupted while awaiting barrier", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("parallel BFS task failed", e.getCause());
        }
    }
}
