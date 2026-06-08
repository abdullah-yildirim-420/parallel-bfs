package edu.gazi.ceng479.bfs.bench;

import edu.gazi.ceng479.bfs.bfs.BfsResult;

/**
 * Runtime SC-1 correctness gate (design.md §3.9). Confirms a parallel result is
 * level-equivalent to the sequential reference; on mismatch raises a diagnostic
 * naming the first differing vertex and its two level values.
 */
public final class ResultVerifier {

    private ResultVerifier() {
    }

    /** Thrown when a parallel result diverges from the sequential reference. */
    public static final class VerificationException extends RuntimeException {
        public VerificationException(String message) {
            super(message);
        }
    }

    /**
     * @param reference sequential reference result
     * @param candidate parallel result to check
     * @throws VerificationException if they are not level-equivalent (SC-1 violated)
     */
    public static void assertEquivalent(BfsResult reference, BfsResult candidate) {
        if (reference.isEquivalentTo(candidate)) {
            return;
        }
        if (reference.reachedCount() != candidate.reachedCount()) {
            throw new VerificationException(
                    "reached-count mismatch: seq=" + reference.reachedCount()
                            + " par=" + candidate.reachedCount());
        }
        int v = reference.firstDifferingVertex(candidate);
        throw new VerificationException(
                "level mismatch at vertex " + v
                        + ": seq=" + reference.level()[v]
                        + " par=" + candidate.level()[v]);
    }

    /** Non-throwing variant. */
    public static boolean isEquivalent(BfsResult reference, BfsResult candidate) {
        return reference.isEquivalentTo(candidate);
    }
}
