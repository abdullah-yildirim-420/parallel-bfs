import { RunState } from "../store";

/**
 * Workload-distribution heatmap (design.md §19.3, hero view): rows = threads,
 * columns = levels, cell intensity = nodes processed. Reveals skew/load-imbalance
 * over time (a thread's row staying hot).
 */
export default function WorkloadHeatmap({ run }: { run?: RunState }) {
  if (!run || run.levels.length === 0) {
    return <div className="text-muted text-sm p-4">No workload data yet.</div>;
  }
  const numThreads = Math.max(...run.levels.map((l) => l.threads.length), 1);
  let max = 1;
  for (const l of run.levels) for (const t of l.threads) max = Math.max(max, t.processed);

  return (
    <div className="overflow-x-auto">
      <div className="inline-flex flex-col gap-1">
        {Array.from({ length: numThreads }).map((_, tid) => (
          <div key={tid} className="flex gap-1 items-center">
            <span className="text-muted text-xs font-mono w-8">T{tid}</span>
            {run.levels.map((l) => {
              const stat = l.threads.find((t) => t.id === tid);
              const v = stat ? stat.processed / max : 0;
              return (
                <div
                  key={l.level}
                  title={`level ${l.level}: ${stat?.processed ?? 0} nodes`}
                  className="w-5 h-5 rounded-sm"
                  style={{ background: heat(v) }}
                />
              );
            })}
          </div>
        ))}
        <div className="flex gap-1 items-center mt-1">
          <span className="w-8" />
          {run.levels.map((l) => (
            <span key={l.level} className="w-5 text-center text-[10px] text-muted">
              {l.level}
            </span>
          ))}
        </div>
      </div>
      <div className="text-muted text-xs mt-2">columns = BFS level · rows = thread · brighter = more work</div>
    </div>
  );
}

function heat(v: number): string {
  // dark -> accent blue gradient
  const a = Math.max(0.06, v);
  return `rgba(88,166,255,${a})`;
}
