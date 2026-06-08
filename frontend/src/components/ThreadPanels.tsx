import { RunState } from "../store";
import { threadColor } from "../theme";
import { fmt } from "../util";

/** Live per-thread utilization panels (design.md §19.2). Aggregates across all levels. */
export default function ThreadPanels({ run }: { run?: RunState }) {
  if (!run) return <Empty />;

  // Aggregate processed/edges/busy/wait per thread across levels seen so far.
  const agg = new Map<number, { processed: number; edges: number; busy: number; wait: number }>();
  for (const lvl of run.levels) {
    for (const t of lvl.threads) {
      const a = agg.get(t.id) ?? { processed: 0, edges: 0, busy: 0, wait: 0 };
      a.processed += t.processed;
      a.edges += t.edges;
      a.busy += t.busyNanos;
      a.wait += t.waitNanos;
      agg.set(t.id, a);
    }
  }
  const rows = [...agg.entries()].sort((a, b) => a[0] - b[0]);
  if (rows.length === 0) return <Empty />;

  return (
    <div className="flex flex-col gap-2">
      {rows.map(([id, a]) => {
        const total = a.busy + a.wait;
        const util = total === 0 ? 0 : (a.busy / total) * 100;
        return (
          <div key={id} className="bg-panel border border-border rounded-lg px-3 py-2">
            <div className="flex justify-between items-center mb-1">
              <span className="font-mono text-sm" style={{ color: threadColor(id) }}>
                Thread {id}
              </span>
              <span className="font-mono text-sm">{util.toFixed(0)}%</span>
            </div>
            <div className="h-2 rounded bg-bg overflow-hidden">
              <div
                className="h-full transition-all duration-200"
                style={{ width: `${util}%`, background: threadColor(id) }}
              />
            </div>
            <div className="text-muted text-xs mt-1 font-mono">
              {fmt(a.processed)} nodes · {fmt(a.edges)} edges
            </div>
          </div>
        );
      })}
    </div>
  );
}

function Empty() {
  return <div className="text-muted text-sm p-4">No thread activity yet — start a run.</div>;
}
