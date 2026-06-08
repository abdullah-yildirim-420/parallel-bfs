package edu.gazi.ceng479.bfs.viz.event;

/**
 * Periodic system-resource sample for the System panel (design.md §17.3, §19.6).
 * Sampled on a timer independent of BFS levels via JMX.
 *
 * @param cpuProcessPct process CPU load 0..100 (or -1 if unavailable)
 * @param cpuSystemPct  whole-system CPU load 0..100 (or -1)
 * @param heapUsedBytes JVM heap used
 * @param heapMaxBytes  JVM heap max
 * @param gcCount       cumulative GC collection count
 * @param gcMillis      cumulative GC time (ms)
 * @param liveThreads   live JVM thread count
 * @param uptimeMs      JVM uptime (ms)
 */
public record SystemSampleEvent(
        String runId, long ts,
        double cpuProcessPct, double cpuSystemPct,
        long heapUsedBytes, long heapMaxBytes,
        long gcCount, long gcMillis,
        int liveThreads, long uptimeMs) implements Event {

    @Override
    public String type() {
        return "SystemSampleEvent";
    }
}
