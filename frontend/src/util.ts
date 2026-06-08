export const ms = (nanos: number) => (nanos / 1e6).toFixed(1) + " ms";
export const pct = (v: number) => (v * 100).toFixed(0) + "%";
export const mb = (bytes: number) => (bytes / 1048576).toFixed(0) + " MB";
export const fmt = (n: number) => n.toLocaleString();
