package pl.bell.lands.storage;

import pl.bell.lands.BellLands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SQLite backend for BellLands. All writes go through a single-thread executor
 * so they never block the main server thread and never race each other.
 * Reads at startup happen synchronously on the calling thread (before any
 * async writes are submitted), so the connection is safe to share.
 */
public class Database {

    private Connection conn;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BellLands-DB");
        t.setDaemon(true);
        return t;
    });

    public void init(File dbFile) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("CREATE TABLE IF NOT EXISTS claims (" +
                "chunk_key TEXT PRIMARY KEY, owner TEXT NOT NULL, world TEXT NOT NULL, " +
                "x INTEGER NOT NULL, z INTEGER NOT NULL, flags TEXT, trusted TEXT)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_claims_owner ON claims(owner)");
            st.execute("CREATE TABLE IF NOT EXISTS warps (" +
                "owner TEXT NOT NULL, name TEXT NOT NULL, world TEXT NOT NULL, " +
                "x REAL, y REAL, z REAL, yaw REAL, pitch REAL, PRIMARY KEY(owner, name))");
        }
    }

    public Connection conn() {
        return conn;
    }

    /** Submit a write task to the single DB thread. Exceptions are logged, never thrown to gameplay. */
    public void async(Runnable task) {
        writer.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Database write failed", e);
            }
        });
    }

    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                BellLands.getInstance().getLogger().warning("DB writer did not finish in 30s");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {}
    }
}
