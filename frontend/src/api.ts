import { BACKEND_HOST } from "./theme";

const base = `http://${BACKEND_HOST}`;

async function post(path: string): Promise<any> {
  const r = await fetch(base + path, { method: "POST" });
  if (!r.ok) throw new Error(`${path} -> ${r.status}`);
  return r.json();
}

async function get(path: string): Promise<any> {
  const r = await fetch(base + path);
  if (!r.ok) throw new Error(`${path} -> ${r.status}`);
  return r.json();
}

export interface GenParams {
  n: number;
  deg: number;
  seed?: number;
  threads?: number;
}

export const api = {
  startRun: (p: GenParams, mode: "parallel" | "sequential") =>
    post(`/api/run?mode=${mode}&threads=${p.threads ?? 4}&n=${p.n}&deg=${p.deg}&seed=${p.seed ?? 42}&stream=true`),
  startRace: (p: GenParams) =>
    post(`/api/race?threads=${p.threads ?? 4}&n=${p.n}&deg=${p.deg}&seed=${p.seed ?? 42}`),
  runBench: (p: GenParams, threads = "1,2,4,8", reps = 3) =>
    post(`/api/bench?threads=${threads}&reps=${reps}&warmup=1&n=${p.n}&deg=${p.deg}&seed=${p.seed ?? 42}`),
  history: () => get(`/api/runs`),
  system: () => get(`/api/system`),
};
