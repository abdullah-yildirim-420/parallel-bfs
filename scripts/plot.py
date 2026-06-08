#!/usr/bin/env python3
"""
Generate report figures from the benchmark CSV (design.md S11.3, S19.5).

Reads out/results_agg.csv (written by `--mode bench`) and produces, into
docs/figures/:
  fig1_speedup.png     speedup vs threads (measured) + Amdahl overlay, per graph
  fig2_efficiency.png  efficiency % vs threads, per graph
  fig3_time.png        mean execution time vs threads, per graph
  fig4_sparse_vs_dense.png  grouped bars of speedup at each thread count

Usage:  python scripts/plot.py [out/results_agg.csv]
Requires: matplotlib  (pip install matplotlib)
"""
import csv
import os
import sys
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

DARK_BG = "#0d1117"
PANEL = "#161b22"
INK = "#c9d1d9"
ACCENT = "#58a6ff"
MUTED = "#8b949e"


def style(ax):
    ax.set_facecolor(PANEL)
    ax.tick_params(colors=INK)
    for s in ax.spines.values():
        s.set_color("#30363d")
    ax.xaxis.label.set_color(INK)
    ax.yaxis.label.set_color(INK)
    ax.title.set_color(INK)
    ax.grid(True, color="#30363d", linewidth=0.5)


def load(path):
    rows = []
    with open(path, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            rows.append(r)
    return rows


def by_graph(rows):
    graphs = defaultdict(lambda: {"threads": [], "speedup": [], "eff": [], "time": [], "amdahl": []})
    seq_time = {}
    for r in rows:
        if r["mode"] == "sequential":
            seq_time[r["graph_name"]] = float(r["mean_ms"])
    for r in rows:
        if r["mode"] != "parallel":
            continue
        g = graphs[r["graph_name"]]
        g["threads"].append(int(r["threads"]))
        g["speedup"].append(float(r["speedup"]))
        g["eff"].append(float(r["efficiency_pct"]))
        g["time"].append(float(r["mean_ms"]))
        g["amdahl"].append(float(r["amdahl_pred"]))
    return graphs, seq_time


def fig_speedup(graphs, out):
    fig, ax = plt.subplots(figsize=(8, 5), facecolor=DARK_BG)
    amdahl_drawn = False
    for name, g in graphs.items():
        order = sorted(range(len(g["threads"])), key=lambda i: g["threads"][i])
        t = [g["threads"][i] for i in order]
        sp = [g["speedup"][i] for i in order]
        am = [g["amdahl"][i] for i in order]
        ax.plot(t, sp, marker="o", label=f"{name} (measured)")
        if not amdahl_drawn:
            ax.plot(t, am, "--", color=MUTED, label="Amdahl p=0.90")
            amdahl_drawn = True
    ax.set_xlabel("threads")
    ax.set_ylabel("speedup x")
    ax.set_title("Speedup vs thread count")
    style(ax)
    ax.legend(facecolor=PANEL, labelcolor=INK)
    fig.savefig(out, dpi=130, facecolor=DARK_BG, bbox_inches="tight")
    print("wrote", out)


def fig_efficiency(graphs, out):
    fig, ax = plt.subplots(figsize=(8, 5), facecolor=DARK_BG)
    for name, g in graphs.items():
        order = sorted(range(len(g["threads"])), key=lambda i: g["threads"][i])
        t = [g["threads"][i] for i in order]
        eff = [g["eff"][i] for i in order]
        ax.plot(t, eff, marker="s", label=name)
    ax.set_xlabel("threads")
    ax.set_ylabel("efficiency %")
    ax.set_title("Parallel efficiency vs thread count")
    style(ax)
    ax.legend(facecolor=PANEL, labelcolor=INK)
    fig.savefig(out, dpi=130, facecolor=DARK_BG, bbox_inches="tight")
    print("wrote", out)


def fig_time(graphs, out):
    fig, ax = plt.subplots(figsize=(8, 5), facecolor=DARK_BG)
    for name, g in graphs.items():
        order = sorted(range(len(g["threads"])), key=lambda i: g["threads"][i])
        t = [g["threads"][i] for i in order]
        tm = [g["time"][i] for i in order]
        ax.plot(t, tm, marker="^", label=name)
    ax.set_xlabel("threads")
    ax.set_ylabel("mean BFS time (ms)")
    ax.set_title("Execution time vs thread count")
    style(ax)
    ax.legend(facecolor=PANEL, labelcolor=INK)
    fig.savefig(out, dpi=130, facecolor=DARK_BG, bbox_inches="tight")
    print("wrote", out)


def fig_sparse_dense(graphs, out):
    names = list(graphs.keys())
    all_threads = sorted({t for g in graphs.values() for t in g["threads"]})
    fig, ax = plt.subplots(figsize=(8, 5), facecolor=DARK_BG)
    width = 0.8 / max(1, len(names))
    for gi, name in enumerate(names):
        g = graphs[name]
        sp_by_t = {g["threads"][i]: g["speedup"][i] for i in range(len(g["threads"]))}
        xs = [i + gi * width for i in range(len(all_threads))]
        ys = [sp_by_t.get(t, 0) for t in all_threads]
        ax.bar(xs, ys, width=width, label=name)
    ax.set_xticks([i + 0.4 for i in range(len(all_threads))])
    ax.set_xticklabels([str(t) for t in all_threads])
    ax.set_xlabel("threads")
    ax.set_ylabel("speedup x")
    ax.set_title("Speedup by graph type")
    style(ax)
    ax.legend(facecolor=PANEL, labelcolor=INK)
    fig.savefig(out, dpi=130, facecolor=DARK_BG, bbox_inches="tight")
    print("wrote", out)


def main():
    csv_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join("out", "results_agg.csv")
    if not os.path.exists(csv_path):
        print("CSV not found:", csv_path, "- run `--mode bench` first")
        sys.exit(1)
    out_dir = os.path.join("docs", "figures")
    os.makedirs(out_dir, exist_ok=True)
    graphs, _ = by_graph(load(csv_path))
    fig_speedup(graphs, os.path.join(out_dir, "fig1_speedup.png"))
    fig_efficiency(graphs, os.path.join(out_dir, "fig2_efficiency.png"))
    fig_time(graphs, os.path.join(out_dir, "fig3_time.png"))
    fig_sparse_dense(graphs, os.path.join(out_dir, "fig4_sparse_vs_dense.png"))


if __name__ == "__main__":
    main()
