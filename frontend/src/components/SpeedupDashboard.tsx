import { useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { api, GenParams } from "../api";
import MetricCard from "./MetricCard";

/**
 * Speedup dashboard (design.md §19.5): runs a clean benchmark grid and plots measured
 * speedup vs thread count against the Amdahl prediction (p=0.90).
 */
export default function SpeedupDashboard({ params }: { params: GenParams }) {
  const [agg, setAgg] = useState<any[]>([]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  const run = async () => {
    setBusy(true);
    setErr("");
    try {
      const rows = await api.runBench(params, "1,2,4,8", 3);
      setAgg(rows);
    } catch (e: any) {
      setErr(String(e));
    } finally {
      setBusy(false);
    }
  };

  const par = agg.filter((r) => r.mode === "parallel");
  const seq = agg.find((r) => r.mode === "sequential");
  const chart = par.map((r) => ({ threads: r.threads, measured: r.speedup, amdahl: r.amdahlPred }));
  const best = par.reduce((m, r) => (r.speedup > m ? r.speedup : m), 0);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <button
          onClick={run}
          disabled={busy}
          className="bg-accent text-bg font-semibold px-4 py-2 rounded disabled:opacity-50"
        >
          {busy ? "Benchmarking…" : "Run clean benchmark (1,2,4,8)"}
        </button>
        <span className="text-muted text-sm">
          n={params.n}, deg={params.deg} — clean (uninstrumented) runs, 3 reps
        </span>
      </div>
      {err && <div className="text-warn text-sm">{err}</div>}

      {agg.length > 0 && (
        <>
          <div className="grid grid-cols-3 gap-3">
            <MetricCard label="Best speedup" value={best.toFixed(2) + "×"} big />
            <MetricCard label="Sequential" value={seq ? seq.meanMs.toFixed(1) + " ms" : "—"} accent="#d29922" />
            <MetricCard
              label="Parallel @max"
              value={par.length ? par[par.length - 1].meanMs.toFixed(1) + " ms" : "—"}
              accent="#3fb950"
            />
          </div>

          <div className="bg-panel border border-border rounded-lg p-4">
            <div className="text-muted text-sm mb-2">Speedup vs thread count — measured vs Amdahl (p=0.90)</div>
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chart}>
                <CartesianGrid stroke="#30363d" />
                <XAxis dataKey="threads" stroke="#8b949e" label={{ value: "threads", position: "insideBottom", offset: -3, fill: "#8b949e" }} />
                <YAxis stroke="#8b949e" />
                <Tooltip contentStyle={{ background: "#161b22", border: "1px solid #30363d" }} />
                <Legend />
                <Line type="monotone" dataKey="measured" stroke="#58a6ff" strokeWidth={2} name="Measured" />
                <Line type="monotone" dataKey="amdahl" stroke="#8b949e" strokeDasharray="5 5" name="Amdahl (p=0.9)" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <table className="w-full text-sm font-mono border border-border rounded-lg overflow-hidden">
            <thead className="bg-panel text-muted">
              <tr>
                <th className="text-left p-2">mode</th><th className="p-2">threads</th>
                <th className="p-2">mean ms</th><th className="p-2">speedup</th>
                <th className="p-2">eff %</th><th className="p-2">amdahl</th>
              </tr>
            </thead>
            <tbody>
              {agg.map((r, i) => (
                <tr key={i} className="border-t border-border">
                  <td className="p-2">{r.mode}</td>
                  <td className="p-2 text-center">{r.threads}</td>
                  <td className="p-2 text-center">{r.meanMs.toFixed(1)}</td>
                  <td className="p-2 text-center text-accent">{r.speedup.toFixed(2)}×</td>
                  <td className="p-2 text-center">{r.efficiencyPct.toFixed(0)}</td>
                  <td className="p-2 text-center text-muted">{r.amdahlPred.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
