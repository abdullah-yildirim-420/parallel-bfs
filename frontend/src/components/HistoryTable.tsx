import { useEffect, useState } from "react";
import { api } from "../api";

/** Historical benchmark dashboard (design.md §20.2) — reads persisted runs from SQLite. */
export default function HistoryTable() {
  const [rows, setRows] = useState<any[]>([]);
  const [err, setErr] = useState("");

  const load = () => api.history().then(setRows).catch((e) => setErr(String(e)));
  useEffect(() => { load(); }, []);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <button onClick={load} className="bg-panel border border-border px-3 py-1.5 rounded text-sm">
          Refresh
        </button>
        <span className="text-muted text-sm">{rows.length} persisted aggregate rows</span>
      </div>
      {err && <div className="text-warn text-sm">{err}</div>}
      {rows.length === 0 ? (
        <div className="text-muted text-sm">No history yet — run a benchmark from the Speedup tab.</div>
      ) : (
        <table className="w-full text-sm font-mono border border-border rounded-lg overflow-hidden">
          <thead className="bg-panel text-muted">
            <tr>
              <th className="text-left p-2">graph</th><th className="p-2">V</th><th className="p-2">E</th>
              <th className="p-2">mode</th><th className="p-2">threads</th>
              <th className="p-2">mean ms</th><th className="p-2">speedup</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i} className="border-t border-border">
                <td className="p-2">{r.graphName}</td>
                <td className="p-2 text-center">{r.vertices}</td>
                <td className="p-2 text-center">{r.edges}</td>
                <td className="p-2 text-center">{r.mode}</td>
                <td className="p-2 text-center">{r.threads}</td>
                <td className="p-2 text-center">{r.meanMs?.toFixed?.(1)}</td>
                <td className="p-2 text-center text-accent">{r.speedup?.toFixed?.(2)}×</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
