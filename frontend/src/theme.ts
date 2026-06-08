// Colorblind-safe categorical palette for thread identity (design.md §22).
// Thread colour is a primary encoding in the traversal/race views, so it must be
// distinguishable for colour-vision-deficient viewers.
export const THREAD_COLORS = [
  "#58a6ff", // blue
  "#3fb950", // green
  "#d29922", // amber
  "#bc8cff", // purple
  "#ff7b72", // red
  "#39c5cf", // cyan
  "#f778ba", // pink
  "#a5d6ff", // light blue
];

export function threadColor(id: number): string {
  return THREAD_COLORS[id % THREAD_COLORS.length];
}

// Backend host: in dev (Vite on 5173) target the Java backend on 7070;
// in production the SPA is served by the backend itself.
export const BACKEND_HOST = import.meta.env.DEV
  ? "localhost:7070"
  : window.location.host;
