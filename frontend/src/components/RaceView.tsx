import { useStore, RunState } from "../store";
import { ms, fmt } from "../util";

/**
 * Flagship Sequential-vs-Parallel race (design.md §19.4). Both sides run on the same
 * demo graph; the bars and counters update live; the faster side shows its time and the
 * live (illustrative) speedup.
 */
export default function RaceView() {
  const runs = useStore((s) => s.runs);
  const seq = runs["race-seq"];
  const par = runs["race-par"];

  return (
    <div className="grid grid-cols-2 gap-4">
      <Lane title="Sequential BFS" run={seq} color="#d29922" peer={par} />
      <Lane title="Parallel BFS" run={par} color="#58a6ff" peer={seq} />
      <div className="col-span-2 text-center">
        <Speedup seq={seq} par={par} />
      </div>
    </div>
  );
}

function Lane({ title, run, color, peer }: { title: string; run?: RunState; color: string; peer?: RunState }) {
  const progress = run ? Math.min(100, (run.visited / Math.max(1, run.vertices)) * 100) : 0;
  const leading = run && peer && !run.done && !peer.done && run.visited > peer.visited;
  return (
    <div className="bg-panel border border-border rounded-lg p-4">
      <div className="flex justify-between items-center mb-2">
        <span className="font-semibold" style={{ color }}>
          {title} {leading && <span className="text-good">🏁 leading</span>}
        </span>
        <span className="font-mono text-sm">{run?.done ? ms(run.totalNanos) : "running…"}</span>
      </div>
      <div className="h-4 rounded bg-bg overflow-hidden mb-2">
        <div className="h-full transition-all duration-100" style={{ width: `${progress}%`, background: color }} />
      </div>
      <div className="grid grid-cols-3 gap-2 text-center font-mono text-sm">
        <Stat label="visited" value={run ? fmt(run.visited) : "0"} />
        <Stat label="level" value={run ? run.maxLevel : 0} />
        <Stat label="progress" value={progress.toFixed(0) + "%"} />
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: any }) {
  return (
    <div>
      <div className="text-muted text-xs">{label}</div>
      <div>{value}</div>
    </div>
  );
}

function Speedup({ seq, par }: { seq?: RunState; par?: RunState }) {
  if (!seq?.done || !par?.done || par.totalNanos === 0) {
    return <div className="text-muted text-sm">Run a race to see the live speedup…</div>;
  }
  const s = seq.totalNanos / par.totalNanos;
  return (
    <div className="inline-block bg-panel border border-border rounded-lg px-8 py-4">
      <div className="text-muted text-xs uppercase">Live (illustrative) speedup</div>
      <div className="font-mono font-bold text-accent" style={{ fontSize: "3rem" }}>
        {s.toFixed(2)}×
      </div>
      <div className="text-muted text-xs">authoritative speedup → Speedup dashboard (clean benchmark)</div>
    </div>
  );
}
