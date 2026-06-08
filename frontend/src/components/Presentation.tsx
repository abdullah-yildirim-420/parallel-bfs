import { useEffect, useState } from "react";
import { GenParams } from "../api";
import RaceView from "./RaceView";
import SpeedupDashboard from "./SpeedupDashboard";
import WorkloadHeatmap from "./WorkloadHeatmap";
import ThreadPanels from "./ThreadPanels";
import TraversalCanvas from "./TraversalCanvas";
import { useStore } from "../store";

/**
 * Presentation mode (design.md §21): full-screen scripted scenes for the CENG-479 final
 * talk. Arrow keys advance scenes; oversized typography; one plain-language caption each.
 */
const SCENES = [
  { title: "1 · What BFS does — the search spreads outward in rings", key: "traversal" },
  { title: "2 · Sequential vs Parallel — they start together; parallel finishes first", key: "race" },
  { title: "3 · How the work is shared across threads", key: "workload" },
  { title: "4 · The payoff — measured speedup vs Amdahl's law", key: "speedup" },
];

export default function Presentation({ params }: { params: GenParams }) {
  const [scene, setScene] = useState(0);
  const runs = useStore((s) => s.runs);
  const order = useStore((s) => s.order);
  const liveRun = [...order].reverse().map((id) => runs[id]).find((r) => r && !id0(r));

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "ArrowRight") setScene((s) => Math.min(SCENES.length - 1, s + 1));
      if (e.key === "ArrowLeft") setScene((s) => Math.max(0, s - 1));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const s = SCENES[scene];
  return (
    <div className="fixed inset-0 bg-bg z-50 flex flex-col p-10">
      <div className="text-3xl font-semibold text-accent mb-6">{s.title}</div>
      <div className="flex-1 overflow-auto">
        {s.key === "traversal" && (
          <div className="grid grid-cols-2 gap-6">
            <TraversalCanvas run={liveRun} />
            <ThreadPanels run={liveRun} />
          </div>
        )}
        {s.key === "race" && <RaceView />}
        {s.key === "workload" && <WorkloadHeatmap run={liveRun} />}
        {s.key === "speedup" && <SpeedupDashboard params={params} />}
      </div>
      <div className="text-muted text-center mt-4">
        ← / → to navigate · scene {scene + 1}/{SCENES.length} · press Esc-equivalent (exit button in toolbar)
      </div>
    </div>
  );
}

function id0(r: any) {
  return r.runId === "race-seq" || r.runId === "race-par";
}
