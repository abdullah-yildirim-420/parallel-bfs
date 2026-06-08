package edu.gazi.ceng479.bfs.util;

import java.util.Arrays;

/**
 * Minimal growable primitive-{@code int} list — avoids the autoboxing of
 * {@code ArrayList<Integer>} on the BFS hot path (frontier buffers, thread-local
 * next-frontier lists) and in the graph builders. Design reference: design.md §3.4,
 * §3.5 (thread-local {@code localNext}).
 *
 * <p>Not thread-safe; each parallel worker uses its own instance (design.md §3.5).
 */
public final class IntArrayList {

    private int[] data;
    private int size;

    public IntArrayList() {
        this(16);
    }

    public IntArrayList(int initialCapacity) {
        this.data = new int[Math.max(1, initialCapacity)];
        this.size = 0;
    }

    public void add(int value) {
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length + (data.length >> 1) + 1);
        }
        data[size++] = value;
    }

    public void addAll(IntArrayList other) {
        ensureCapacity(size + other.size);
        System.arraycopy(other.data, 0, this.data, this.size, other.size);
        this.size += other.size;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        return data[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    /** @return a copy of the live elements (length == size). */
    public int[] toArray() {
        return Arrays.copyOf(data, size);
    }

    /**
     * @return the backing array directly (length may exceed {@link #size()}).
     *         Read-only use within [0,size); do not retain across mutations.
     */
    public int[] backingArray() {
        return data;
    }

    private void ensureCapacity(int min) {
        if (min > data.length) {
            int newCap = Math.max(min, data.length + (data.length >> 1) + 1);
            data = Arrays.copyOf(data, newCap);
        }
    }
}
