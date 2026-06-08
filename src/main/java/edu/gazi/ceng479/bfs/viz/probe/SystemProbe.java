package edu.gazi.ceng479.bfs.viz.probe;

import edu.gazi.ceng479.bfs.viz.event.EventSink;
import edu.gazi.ceng479.bfs.viz.event.SystemSampleEvent;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Samples JVM/OS resource metrics via JMX on a fixed timer and emits
 * {@link SystemSampleEvent}s (design.md §17.3). Decoupled from BFS levels so the System
 * panel updates smoothly regardless of traversal progress.
 */
public final class SystemProbe {

    private final EventSink sink;
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final java.lang.management.OperatingSystemMXBean os =
            ManagementFactory.getOperatingSystemMXBean();

    private ScheduledExecutorService scheduler;
    private volatile String runId = "system";

    public SystemProbe(EventSink sink) {
        this.sink = sink;
    }

    /** Start periodic sampling every {@code periodMs} milliseconds (default 250). */
    public void start(long periodMs) {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "system-probe");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> sink.emit(sample()), 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    /** Take one immediate sample (also used directly by REST and tests). */
    public SystemSampleEvent sample() {
        MemoryUsage heap = memory.getHeapMemoryUsage();
        long gcCount = 0, gcMillis = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long c = gc.getCollectionCount();
            long m = gc.getCollectionTime();
            if (c > 0) gcCount += c;
            if (m > 0) gcMillis += m;
        }
        double procCpu = -1, sysCpu = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            procCpu = clampPct(sun.getProcessCpuLoad());
            sysCpu = clampPct(sun.getCpuLoad());
        }
        return new SystemSampleEvent(
                runId, System.currentTimeMillis(),
                procCpu, sysCpu,
                heap.getUsed(), heap.getMax(),
                gcCount, gcMillis,
                threads.getThreadCount(), runtime.getUptime());
    }

    private static double clampPct(double load) {
        if (load < 0 || Double.isNaN(load)) {
            return -1; // JVM reports negative when not yet available
        }
        return load * 100.0;
    }
}
