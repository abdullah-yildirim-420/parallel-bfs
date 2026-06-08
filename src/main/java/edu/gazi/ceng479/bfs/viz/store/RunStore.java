package edu.gazi.ceng479.bfs.viz.store;

import edu.gazi.ceng479.bfs.bench.Records.AggRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed persistence of benchmark aggregate results (design.md §9 Persistence,
 * §20.1). Powers the History and Comparison dashboards. Each benchmark invocation is a
 * "run group"; its aggregate rows (one per graph×mode×threads) are stored together.
 */
public final class RunStore implements AutoCloseable {

    private final Connection conn;

    public RunStore(String dbPath) {
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            init();
        } catch (SQLException e) {
            throw new RuntimeException("failed to open RunStore at " + dbPath, e);
        }
    }

    private void init() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS agg_runs (
                        group_id     TEXT,
                        ts           INTEGER,
                        graph_name   TEXT,
                        vertices     INTEGER,
                        edges        INTEGER,
                        graph_type   TEXT,
                        mode         TEXT,
                        threads      INTEGER,
                        mean_ns      REAL,
                        sd_ns        REAL,
                        ci95_ns      REAL,
                        mean_ms      REAL,
                        speedup      REAL,
                        efficiency_pct REAL,
                        amdahl_pred  REAL,
                        reached      INTEGER,
                        max_level    INTEGER,
                        source       INTEGER,
                        jdk          TEXT,
                        cpu          TEXT
                    )""");
        }
    }

    /** Persist one benchmark run group. */
    public void saveGroup(String groupId, long ts, List<AggRecord> rows, String jdk, String cpu) {
        String sql = """
                INSERT INTO agg_runs (group_id, ts, graph_name, vertices, edges, graph_type,
                    mode, threads, mean_ns, sd_ns, ci95_ns, mean_ms, speedup, efficiency_pct,
                    amdahl_pred, reached, max_level, source, jdk, cpu)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (AggRecord r : rows) {
                ps.setString(1, groupId);
                ps.setLong(2, ts);
                ps.setString(3, r.graphName());
                ps.setInt(4, r.vertices());
                ps.setLong(5, r.edges());
                ps.setString(6, r.graphType());
                ps.setString(7, r.mode());
                ps.setInt(8, r.threads());
                ps.setDouble(9, r.meanNs());
                ps.setDouble(10, r.sdNs());
                ps.setDouble(11, r.ci95Ns());
                ps.setDouble(12, r.meanMs());
                ps.setDouble(13, r.speedup());
                ps.setDouble(14, r.efficiencyPct());
                ps.setDouble(15, r.amdahlPred());
                ps.setInt(16, r.reached());
                ps.setInt(17, r.maxLevel());
                ps.setInt(18, r.source());
                ps.setString(19, jdk);
                ps.setString(20, cpu);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("failed to save run group " + groupId, e);
        }
    }

    /** @return all aggregate rows, newest groups first. */
    public List<AggRecord> loadAll() {
        List<AggRecord> out = new ArrayList<>();
        String sql = "SELECT * FROM agg_runs ORDER BY ts DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("failed to load runs", e);
        }
        return out;
    }

    /** @return distinct run-group ids with their timestamp, newest first. */
    public List<String> listGroups() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT group_id, MAX(ts) AS t FROM agg_runs GROUP BY group_id ORDER BY t DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString("group_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("failed to list groups", e);
        }
        return out;
    }

    private static AggRecord map(ResultSet rs) throws SQLException {
        return new AggRecord(
                rs.getString("graph_name"), rs.getInt("vertices"), rs.getLong("edges"),
                rs.getString("graph_type"), rs.getString("mode"), rs.getInt("threads"),
                rs.getDouble("mean_ns"), rs.getDouble("sd_ns"), rs.getDouble("ci95_ns"),
                rs.getDouble("mean_ms"), rs.getDouble("speedup"), rs.getDouble("efficiency_pct"),
                rs.getDouble("amdahl_pred"), rs.getInt("reached"), rs.getInt("max_level"),
                rs.getInt("source"));
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }
}
