package com.sims.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection manager.
 *
 * <h2>Design Pattern – Creational: Singleton</h2>
 * <p><b>Academic Justification</b>: Opening a new JDBC connection for every
 * data access operation imposes significant resource overhead (TCP handshake,
 * Oracle session setup, memory allocation).  The Singleton pattern ensures
 * that exactly one {@link Connection} is shared across the entire JVM
 * lifecycle, providing a globally accessible, lazy-initialised access point
 * to the Oracle Database without the complexity of a full connection pool.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>The instance field is declared {@code volatile} and the factory method
 * uses double-checked locking (DCL), which is safe under JDK 5+ memory model
 * (JSR-133). For a single-desktop Swing application this is sufficient.</p>
 *
 * <h2>Resource Management</h2>
 * <p>Callers receive the shared {@link Connection} and must use it inside
 * {@code try-with-resources} blocks for their own {@code PreparedStatement}
 * and {@code ResultSet} objects.  They must <em>not</em> close the connection
 * itself — call {@link #close()} only on application shutdown.</p>
 */
public final class DBConnection {

    // ── Singleton instance (volatile for DCL visibility) ───────────────────
    private static volatile DBConnection instance;

    // ── Underlying JDBC connection ──────────────────────────────────────────
    private Connection connection;

    // ── Private constructor — reads credentials from .env ──────────────────
    private DBConnection() throws SQLException {
        Dotenv dotenv = Dotenv.configure()
                              .ignoreIfMissing()   // fall back to System env in CI
                              .load();

        String url      = dotenv.get("DB_URL",      "jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        String user     = dotenv.get("DB_USER",     "system");
        String password = dotenv.get("DB_PASSWORD", "admin");

        this.connection = DriverManager.getConnection(url, user, password);
        this.connection.setAutoCommit(false); // explicit transaction demarcation
    }

    /**
     * Returns the singleton {@code DBConnection} instance, creating it on
     * first call (double-checked locking).
     *
     * @return the singleton instance
     * @throws RuntimeException wrapping any {@link SQLException} on first connect
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    try {
                        instance = new DBConnection();
                    } catch (SQLException e) {
                        throw new RuntimeException(
                            "Failed to establish Oracle DB connection: " + e.getMessage(), e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Returns the raw JDBC {@link Connection}.
     * <p>The connection may be stale if the DB restarted; callers should
     * handle {@link SQLException} and call {@link #reconnect()} if needed.</p>
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Attempts to re-create the underlying JDBC connection.
     * Used by DAO error-handling to recover from a dropped connection.
     */
    public void reconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
        // Re-read .env to pick up any rotated credentials
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String url      = dotenv.get("DB_URL",      "jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        String user     = dotenv.get("DB_USER",     "system");
        String password = dotenv.get("DB_PASSWORD", "admin");
        connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(false);
    }

    /**
     * Closes the underlying connection.  Call once on application shutdown
     * (e.g., a JVM shutdown hook or the main-frame's {@code windowClosing}
     * listener).
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[WARN] Error closing DB connection: " + e.getMessage());
            }
        }
    }
}
