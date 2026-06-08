package edu.gazi.ceng479.bfs.cli;

/**
 * Parsed CLI configuration (design.md §4.1, §7.1).
 */
public final class Config {

    public enum Mode {SEQ, PAR, VERIFY, BENCH, UI}

    public Mode mode = Mode.BENCH;

    public int port = 7070; // for UI mode

    // Graph source: either a file path or a generator spec.
    public String graphPath = null;
    public Integer genN = null;     // generator: vertices
    public Integer genDeg = null;   // generator: average degree
    public long genSeed = 42L;      // generator: seed

    public boolean directed = false;
    public int source = -1;         // -1 => auto (highest-degree)

    public int threads = 4;             // for PAR/VERIFY
    public int[] threadList = {1, 2, 4, 8}; // for BENCH
    public int reps = 5;
    public int warmups = 2;
    public long maxEdges = -1;

    public String outDir = "out";

    public boolean hasGraphSource() {
        return graphPath != null || (genN != null && genDeg != null);
    }
}
