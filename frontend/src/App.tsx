import { useState } from "react";
import { api, GenParams } from "./api";
import { useStore } from "./store";
import { ms, fmt } from "./util";
import MetricCard from "./components/MetricCard";
import ThreadPanels from "./components/ThreadPanels";
import WorkloadHeatmap from "./components/WorkloadHeatmap";
import TraversalCanvas from "./components/TraversalCanvas";
import SystemPanel from "./components/SystemPanel";
import RaceView from "./components/RaceView";
import SpeedupDashboard from "./components/SpeedupDashboard";
import HistoryTable from "./components/HistoryTable";
import Presentation from "./components/Presentation";

type Tab = "live" | "race" | "speedup" | "history";

export default function App() {
  const connected = useStore((s) => s.connected);
  const runs = useStore((s) => s.runs);
  const order = useStore((s) => s.order);
  const [tab, setTab] = useState<Tab>("live");
  const [present, setPresent] = useState(false);
  const [n, setN] = useState(2000);
  const [deg, setDeg] = useState(8);
  const [threads, setThreads] = useState(4);
  const params: GenParams = { n, deg, threads, seed: 42 };

  const liveRun = [...order].reverse()
    .filter((id) => id !== "race-seq" && id !== "race-par")
    .map((id) => runs[id])[0];

  return (
    <div className="min-h-full flex flex-col">
      {present && <Presentation params={params} />}

      <header className="border-b border-border bg-panel px-6 py-3 flex items-center gap-4 flex-wrap">
        <h1 className="text-lg font-semibold text-accent">Parallel BFS · Live Platform</h1>
        <span className={`text-xs px-2 py-0.5 rounded ${connected ? "bg-good/20 text-good" : "bg-warn/20 text-warn"}`}>
          {connected ? "● live" : "○ disconnected"}
        </span>
        <div className="flex-1" />
        <Field label="nodes" value={n} setValue={setN} step={1000} />
        <Field label="avg deg" value={deg} setValue={setDeg} step={2} />
        <Field label="threads" value={threads} setValue={setThreads} step={1} />
        <button onClick={() => setPresent((p) => !p)} className="bg-accent text-bg font-semibold px-3 py-1.5 rounded text-sm">
          {present ? "Exit presentation" : "Presentation"}
        </button>
      </header>

      <nav className="flex gap-1 px-6 pt-3 border-b border-border">
        {(["live", "race", "speedup", "history"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-t text-sm capitalize ${
              tab === t ? "bg-panel border border-border border-b-0 text-accent" : "text-muted"
            }`}
          >
            {t}
          </button>
        ))}
      </nav>

      <main className="flex-1 p-6">
        {tab === "live" && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="flex flex-col gap-4">
              <div className="flex gap-2">
                <button onClick={() => api.startRun(params, "parallel")} className="bg-accent text-bg font-semibold px-4 py-2 rounded">
                  ▶ Run Parallel ({threads}t)
                </button>
                <button onClick={() => api.startRun(params, "sequential")} className="bg-panel border border-border px-4 py-2 rounded">
                  ▶ Run Sequential
                </button>
              </div>
              <TraversalCanvas run={liveRun} />
              <div className="grid grid-cols-2 gap-3">
                <MetricCard label="BFS level" value={liveRun?.maxLevel ?? 0} />
                <MetricCard label="Visited" value={liveRun ? fmt(liveRun.visited) : 0} accent="#3fb950" />
                <MetricCard label="Frontier" value={liveRun?.levels.at(-1)?.frontierSize ?? 0} accent="#bc8cff" />
                <MetricCard label="Time" value={liveRun?.done ? ms(liveRun.totalNanos) : "—"} accent="#d29922" />
              </div>
            </div>
            <div className="flex flex-col gap-4">
              <Section title="Thread Activity"><ThreadPanels run={liveRun} /></Section>
              <Section title="Workload Distribution (heatmap)"><WorkloadHeatmap run={liveRun} /></Section>
              <Section title="System Resources"><SystemPanel /></Section>
            </div>
          </div>
        )}

        {tab === "race" && (
          <div className="flex flex-col gap-4">
            <button onClick={() => api.startRace(params)} className="self-start bg-accent text-bg font-semibold px-4 py-2 rounded">
              ▶ Start Race (sequential vs parallel)
            </button>
            <RaceView />
          </div>
        )}

        {tab === "speedup" && <SpeedupDashboard params={params} />}
        {tab === "history" && <HistoryTable />}
      </main>
    </div>
  );
}

function Field({ label, value, setValue, step }: { label: string; value: number; setValue: (n: number) => void; step: number }) {
  return (
    <label className="flex items-center gap-1 text-xs text-muted">
      {label}
      <input
        type="number"
        value={value}
        step={step}
        onChange={(e) => setValue(Math.max(1, parseInt(e.target.value) || 1))}
        className="w-20 bg-bg border border-border rounded px-2 py-1 text-ink font-mono"
      />
    </label>
  );
}

function Section({ title, children }: { title: string; children: any }) {
  return (
    <div>
      <div className="text-muted text-xs uppercase tracking-wide mb-2">{title}</div>
      {children}
    </div>
  );
}
