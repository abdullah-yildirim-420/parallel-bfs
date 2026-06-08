import { LineChart, Line, ResponsiveContainer, YAxis } from "recharts";
import { useStore } from "../store";
import { mb } from "../util";
import MetricCard from "./MetricCard";

/** Live system-resource panel via JMX samples (design.md §19.6). */
export default function SystemPanel() {
  const system = useStore((s) => s.system);
  const latest = system[system.length - 1];
  const cpu = latest && latest.cpuProcessPct >= 0 ? latest.cpuProcessPct.toFixed(0) + "%" : "n/a";
  const heap = latest ? `${mb(latest.heapUsedBytes)} / ${mb(latest.heapMaxBytes)}` : "—";

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-3 gap-3">
        <MetricCard label="Process CPU" value={cpu} accent="#3fb950" />
        <MetricCard label="Live threads" value={latest?.liveThreads ?? "—"} accent="#bc8cff" />
        <MetricCard label="GC count" value={latest?.gcCount ?? "—"} accent="#d29922" />
      </div>
      <div className="bg-panel border border-border rounded-lg p-3">
        <div className="text-muted text-xs mb-1">CPU % (last ~30s)</div>
        <ResponsiveContainer width="100%" height={90}>
          <LineChart data={system.map((s) => ({ cpu: Math.max(0, s.cpuProcessPct) }))}>
            <YAxis domain={[0, 100]} hide />
            <Line type="monotone" dataKey="cpu" stroke="#3fb950" dot={false} isAnimationActive={false} />
          </LineChart>
        </ResponsiveContainer>
        <div className="text-muted text-xs mt-1">Heap: {heap}</div>
      </div>
    </div>
  );
}
