package edu.gazi.ceng479.bfs.cli;

/**
 * Minimal CLI argument parser (design.md §7.1). Flags are {@code --key value} pairs;
 * {@code --gen} takes {@code n=..,deg=..,seed=..}; {@code --threads-list} takes
 * {@code 1,2,4,8}. Invalid input throws {@link IllegalArgumentException} (exit code 1).
 */
public final class ArgParser {

    private ArgParser() {
    }

    // Locale-independent case folding — the default (e.g. Turkish) locale would map
    // 'i' -> dotted-I and break valueOf("UI")/("VERIFY"). (Bug fixed via Locale.ROOT.)

    public static final String USAGE = """
            Usage: java -jar parallel-bfs.jar --mode <seq|par|bench|verify> [options]
              --graph <path>             SNAP edge-list file
              --gen n=<N>,deg=<D>,seed=<S>   generate Erdos-Renyi graph instead
              --directed <true|false>    (default false)
              --source <id|auto>         (default auto = highest-degree vertex)
              --threads <N>              for par/verify (default 4)
              --threads-list 1,2,4,8     for bench (default 1,2,4,8)
              --reps <N>                 (default 5)
              --warmup <N>               (default 2)
              --max-edges <N>            cap edges on load
              --out <dir>                output dir for bench CSVs (default out)
              --port <N>                 UI server port (default 7070)
            mode ui starts the live visualization backend (Javalin WS+REST).
            """;

    public static Config parse(String[] args) {
        Config c = new Config();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--mode" -> c.mode = parseMode(next(args, ++i, a));
                case "--graph" -> c.graphPath = next(args, ++i, a);
                case "--gen" -> parseGen(c, next(args, ++i, a));
                case "--directed" -> c.directed = Boolean.parseBoolean(next(args, ++i, a));
                case "--source" -> {
                    String s = next(args, ++i, a);
                    c.source = s.equalsIgnoreCase("auto") ? -1 : parseInt(s, a);
                }
                case "--threads" -> c.threads = parseInt(next(args, ++i, a), a);
                case "--threads-list" -> c.threadList = parseIntList(next(args, ++i, a));
                case "--reps" -> c.reps = parseInt(next(args, ++i, a), a);
                case "--warmup" -> c.warmups = parseInt(next(args, ++i, a), a);
                case "--max-edges" -> c.maxEdges = Long.parseLong(next(args, ++i, a));
                case "--out" -> c.outDir = next(args, ++i, a);
                case "--port" -> c.port = parseInt(next(args, ++i, a), a);
                case "-h", "--help" -> throw new IllegalArgumentException(USAGE);
                default -> throw new IllegalArgumentException("unknown argument: " + a + "\n" + USAGE);
            }
        }
        validate(c);
        return c;
    }

    private static Config.Mode parseMode(String s) {
        try {
            return Config.Mode.valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid --mode '" + s + "' (seq|par|bench|verify)");
        }
    }

    private static void parseGen(Config c, String spec) {
        for (String part : spec.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("invalid --gen part '" + part + "' (expected key=value)");
            }
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "n" -> c.genN = Integer.parseInt(v);
                case "deg" -> c.genDeg = Integer.parseInt(v);
                case "seed" -> c.genSeed = Long.parseLong(v);
                default -> throw new IllegalArgumentException("unknown --gen key '" + k + "'");
            }
        }
    }

    private static int[] parseIntList(String s) {
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }

    private static void validate(Config c) {
        if (c.threads < 1) throw new IllegalArgumentException("--threads must be >= 1");
        for (int t : c.threadList) {
            if (t < 1) throw new IllegalArgumentException("--threads-list entries must be >= 1");
        }
        if (c.reps < 1) throw new IllegalArgumentException("--reps must be >= 1");
        if (c.warmups < 0) throw new IllegalArgumentException("--warmup must be >= 0");
        if ((c.mode == Config.Mode.SEQ || c.mode == Config.Mode.PAR || c.mode == Config.Mode.VERIFY)
                && !c.hasGraphSource()) {
            throw new IllegalArgumentException("mode " + c.mode + " requires --graph or --gen");
        }
    }

    private static String next(String[] args, int i, String flag) {
        if (i >= args.length) {
            throw new IllegalArgumentException("missing value for " + flag);
        }
        return args[i];
    }

    private static int parseInt(String s, String flag) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid integer for " + flag + ": " + s);
        }
    }
}
