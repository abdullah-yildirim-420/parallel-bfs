package edu.gazi.ceng479.bfs.bfs;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Abstraction over the BFS "visited" state (design.md §4.1).
 *
 * <p>{@link #tryClaim(int)} atomically marks a node as visited and returns whether
 * <em>this</em> call was the first to do so — the single coordination primitive of
 * the parallel algorithm. Sequential and parallel BFS use different implementations
 * but the same contract.
 */
public interface VisitedSet {

    /**
     * Attempt to mark {@code node} as visited.
     *
     * @param node vertex id
     * @return true iff this call transitioned the node from unvisited to visited
     */
    boolean tryClaim(int node);

    /** @return whether {@code node} has been visited. */
    boolean isVisited(int node);

    /** Reset all nodes to unvisited (for reuse across repetitions). */
    void reset();

    /** Single-threaded visited set backed by a {@code boolean[]} (design.md §3.4). */
    final class Bool implements VisitedSet {
        private final boolean[] flags;

        public Bool(int size) {
            this.flags = new boolean[size];
        }

        @Override
        public boolean tryClaim(int node) {
            if (flags[node]) {
                return false;
            }
            flags[node] = true;
            return true;
        }

        @Override
        public boolean isVisited(int node) {
            return flags[node];
        }

        @Override
        public void reset() {
            java.util.Arrays.fill(flags, false);
        }
    }

    /**
     * Lock-free concurrent visited set backed by {@link AtomicIntegerArray}
     * (design.md §3.5, §4.2). {@code tryClaim} uses {@code compareAndSet(node,0,1)}
     * so exactly one thread can claim any node — no coarse locking.
     */
    final class Atomic implements VisitedSet {
        private final AtomicIntegerArray flags;

        public Atomic(int size) {
            this.flags = new AtomicIntegerArray(size);
        }

        @Override
        public boolean tryClaim(int node) {
            return flags.compareAndSet(node, 0, 1);
        }

        @Override
        public boolean isVisited(int node) {
            return flags.get(node) == 1;
        }

        @Override
        public void reset() {
            for (int i = 0; i < flags.length(); i++) {
                flags.set(i, 0);
            }
        }
    }
}
