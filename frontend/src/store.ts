import { create } from "zustand";
import { BACKEND_HOST } from "./theme";

export interface ThreadStat {
  id: number;
  processed: number;
  edges: number;
  busyNanos: number;
  waitNanos: number;
}

export interface LevelData {
  level: number;
  frontierSize: number;
  nextFrontierSize: number;
  totalVisited: number;
  syncNanos: number;
  threads: ThreadStat[];
}

export interface RunState {
  runId: string;
  mode: string;
  threads: number;
  vertices: number;
  edges: number;
  source: number;
  levels: LevelData[];
  visited: number;
  maxLevel: number;
  done: boolean;
  totalNanos: number;
  startedAt: number;
  // node id -> owning thread, for the traversal canvas (demo track)
  nodeOwner: Map<number, number>;
}

export interface SystemSample {
  ts: number;
  cpuProcessPct: number;
  heapUsedBytes: number;
  heapMaxBytes: number;
  gcCount: number;
  liveThreads: number;
}

interface Store {
  connected: boolean;
  runs: Record<string, RunState>;
  order: string[];
  system: SystemSample[];
  apply: (e: any) => void;
  setConnected: (c: boolean) => void;
  reset: () => void;
}

function emptyRun(e: any): RunState {
  return {
    runId: e.runId,
    mode: e.mode,
    threads: e.threads,
    vertices: e.vertices,
    edges: e.edges,
    source: e.source,
    levels: [],
    visited: 1,
    maxLevel: 0,
    done: false,
    totalNanos: 0,
    startedAt: e.ts,
    nodeOwner: new Map(),
  };
}

export const useStore = create<Store>((set) => ({
  connected: false,
  runs: {},
  order: [],
  system: [],
  setConnected: (c) => set({ connected: c }),
  reset: () => set({ runs: {}, order: [], system: [] }),
  apply: (e: any) =>
    set((state) => {
      switch (e.type) {
        case "RunStartedEvent": {
          const runs = { ...state.runs, [e.runId]: emptyRun(e) };
          const order = state.order.includes(e.runId) ? state.order : [...state.order, e.runId];
          return { runs, order };
        }
        case "LevelCompletedEvent": {
          const run = state.runs[e.runId];
          if (!run) return {};
          const level: LevelData = {
            level: e.level,
            frontierSize: e.frontierSize,
            nextFrontierSize: e.nextFrontierSize,
            totalVisited: e.totalVisited,
            syncNanos: e.syncNanos,
            threads: e.threads,
          };
          const updated: RunState = {
            ...run,
            levels: [...run.levels, level],
            visited: e.totalVisited,
            maxLevel: Math.max(run.maxLevel, e.level),
          };
          return { runs: { ...state.runs, [e.runId]: updated } };
        }
        case "FrontierBatchEvent": {
          const run = state.runs[e.runId];
          if (!run) return {};
          const nodeOwner = new Map(run.nodeOwner);
          (e.nodesByThread as number[][]).forEach((ids, t) => {
            ids.forEach((id) => nodeOwner.set(id, t));
          });
          return { runs: { ...state.runs, [e.runId]: { ...run, nodeOwner } } };
        }
        case "RunCompletedEvent": {
          const run = state.runs[e.runId];
          if (!run) return {};
          return {
            runs: {
              ...state.runs,
              [e.runId]: { ...run, done: true, totalNanos: e.totalNanos, visited: e.reached, maxLevel: e.maxLevel },
            },
          };
        }
        case "SystemSampleEvent": {
          const sample: SystemSample = {
            ts: e.ts,
            cpuProcessPct: e.cpuProcessPct,
            heapUsedBytes: e.heapUsedBytes,
            heapMaxBytes: e.heapMaxBytes,
            gcCount: e.gcCount,
            liveThreads: e.liveThreads,
          };
          const system = [...state.system, sample].slice(-120); // keep ~30s @250ms
          return { system };
        }
        default:
          return {};
      }
    }),
}));

/** Open the live WebSocket and pump events into the store. Auto-reconnects. */
export function connectLiveStream() {
  const url = `ws://${BACKEND_HOST}/live`;
  let ws: WebSocket;
  const open = () => {
    ws = new WebSocket(url);
    ws.onopen = () => useStore.getState().setConnected(true);
    ws.onclose = () => {
      useStore.getState().setConnected(false);
      setTimeout(open, 1000); // reconnect (design.md R-17)
    };
    ws.onmessage = (m) => {
      try {
        useStore.getState().apply(JSON.parse(m.data));
      } catch {
        /* ignore malformed frame */
      }
    };
  };
  open();
}
