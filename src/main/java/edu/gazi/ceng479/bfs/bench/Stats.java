package edu.gazi.ceng479.bfs.bench;

/**
 * Statistics helpers for the performance study (design.md §5.4, §3.8).
 * Mean, sample standard deviation, 95% confidence interval (t-distribution),
 * speedup, efficiency, and the Amdahl prediction with p = 0.90.
 */
public final class Stats {

    /** Parallel fraction from the proposal (design.md §5.3, proposal §3). */
    public static final double AMDAHL_P = 0.90;

    private Stats() {
    }

    public static double mean(double[] xs) {
        if (xs.length == 0) return 0;
        double s = 0;
        for (double x : xs) s += x;
        return s / xs.length;
    }

    /** Sample standard deviation (divisor n-1). Returns 0 for n &lt; 2. */
    public static double stdDev(double[] xs) {
        int n = xs.length;
        if (n < 2) return 0;
        double m = mean(xs);
        double ss = 0;
        for (double x : xs) {
            double d = x - m;
            ss += d * d;
        }
        return Math.sqrt(ss / (n - 1));
    }

    /** 95% confidence interval half-width: t(0.025, n-1) * sd / sqrt(n). */
    public static double ci95(double[] xs) {
        int n = xs.length;
        if (n < 2) return 0;
        return tValue95(n - 1) * stdDev(xs) / Math.sqrt(n);
    }

    /** Two-sided t critical value at alpha=0.05 for small degrees of freedom. */
    public static double tValue95(int df) {
        double[] table = {
                Double.NaN, // df=0 unused
                12.706, 4.303, 3.182, 2.776, 2.571,
                2.447, 2.365, 2.306, 2.262, 2.228
        };
        if (df <= 0) return Double.NaN;
        if (df < table.length) return table[df];
        return 1.96; // large-sample normal approximation
    }

    /** Speedup = sequential time / parallel time. */
    public static double speedup(double seqMean, double parMean) {
        return parMean == 0 ? 0 : seqMean / parMean;
    }

    /** Efficiency (%) = speedup / threads * 100. */
    public static double efficiencyPct(double speedup, int threads) {
        return threads == 0 ? 0 : speedup / threads * 100.0;
    }

    /** Amdahl predicted speedup S(n) = 1 / ((1-p) + p/n) with p = 0.90. */
    public static double amdahl(int n) {
        return 1.0 / ((1 - AMDAHL_P) + AMDAHL_P / n);
    }
}
