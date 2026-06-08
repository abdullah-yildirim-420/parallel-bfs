import { useEffect, useRef } from "react";
import { RunState } from "../store";
import { threadColor } from "../theme";

/**
 * Demo-track BFS traversal animation (design.md §19.1). Renders up to MAX_NODES cells on
 * a canvas grid; each node fills in coloured by the thread that claimed it as
 * FrontierBatchEvents arrive. For sequential runs all cells share one colour. Node
 * positions are deterministic (grid by id) since the backend streams ids, not coordinates.
 */
const MAX_NODES = 2500;

export default function TraversalCanvas({ run }: { run?: RunState }) {
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    let raf = 0;
    const draw = () => {
      const canvas = ref.current;
      if (canvas && run) {
        const ctx = canvas.getContext("2d")!;
        const n = Math.min(run.vertices, MAX_NODES);
        const cols = Math.ceil(Math.sqrt(n));
        const rows = Math.ceil(n / cols);
        const W = canvas.width, H = canvas.height;
        const cw = W / cols, ch = H / rows;
        ctx.clearRect(0, 0, W, H);
        for (let i = 0; i < n; i++) {
          const r = Math.floor(i / cols), c = i % cols;
          const owner = run.nodeOwner.get(i);
          if (i === run.source) ctx.fillStyle = "#ffffff";
          else if (owner !== undefined) ctx.fillStyle = threadColor(owner);
          else ctx.fillStyle = "#1c2128";
          ctx.fillRect(c * cw + 1, r * ch + 1, cw - 2, ch - 2);
        }
      }
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, [run]);

  return (
    <div className="bg-panel border border-border rounded-lg p-2">
      <canvas ref={ref} width={520} height={420} className="w-full rounded" />
      <div className="text-muted text-xs mt-1">
        each cell = a vertex · colour = claiming thread · white = source
        {run && run.vertices > MAX_NODES && ` · showing first ${MAX_NODES} of ${run.vertices}`}
      </div>
    </div>
  );
}
