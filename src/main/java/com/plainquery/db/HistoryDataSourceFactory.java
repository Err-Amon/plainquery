package com.plainquery.db;

import com.plainquery.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class HistoryDataSourceFactory implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(HistoryDataSourceFactory.class.getName());

    private static final String FILE_URL_PREFIX = "jdbc:sqlite:";
    private static final String DEFAULT_HISTORY_FILENAME = "plainquery-history.db";

    private static final String DDL_CREATE_HISTORY_TABLE =
        "CREATE TABLE IF NOT EXISTS query_history ("
        + "  id               INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "  natural_language TEXT    NOT NULL,"
        + "  generated_sql    TEXT    NOT NULL,"
        + "  executed_at      TEXT    NOT NULL,"
        + "  starred          INTEGER NOT NULL DEFAULT 0"
        + ")";

    private static final String DDL_INDEX_EXECUTED_AT =
        "CREATE INDEX IF NOT EXISTS idx_history_executed_at "
        + "ON query_history(executed_at DESC)";

    private static final String DDL_INDEX_STARRED =
        "CREATE INDEX IF NOT EXISTS idx_history_starred "
        + "ON query_history(starred) WHERE starred = 1";

    private static final String DDL_INDEX_NATURAL_LANGUAGE =
        "CREATE INDEX IF NOT EXISTS idx_history_natural_language "
        + "ON query_history(natural_language)";

    private final AppConfig config;
    private Connection connection;

    public HistoryDataSourceFactory(AppConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AppConfig must not be null");
        }
        this.config = config;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
            configureConnection(connection);
            applySchema(connection);
        }
        return connection;
    }

    private Connection openConnection() throws SQLException {
        Path dbPath = resolveHistoryPath();
        LOG.fine("Opening history SQLite database at: " + dbPath);
        return DriverManager.getConnection(FILE_URL_PREFIX + dbPath.toAbsolutePath());
    }

    private Path resolveHistoryPath() throws SQLException {
        String configured = config.getHistoryDbPath();

        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured);
            try {
                Files.createDirectories(p.getParent() != null ? p.getParent() : p);
                return p;
            } catch (Exception e) {
                LOG.warning("Configured history path unavailable, using default. "
                    + e.getMessage());
            }
        }

        try {
            Path dir = Path.of(System.getProperty("user.home"), ".plainquery");
            Files.createDirectories(dir);
            Path resolved = dir.resolve(DEFAULT_HISTORY_FILENAME);
            config.setHistoryDbPath(resolved.toAbsolutePath().toString());
            return resolved;
        } catch (Exception e) {
            throw new SQLException("Could not resolve history database path: "
                + e.getMessage(), e);
        }
    }

    private void configureConnection(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA cache_size=-2000");
        }
    }

    private void applySchema(Connection conn) throws SQLException {
        if (tableExists(conn, "query_history")) {
            LOG.fine("History schema already present, skipping DDL");
            return;
        }

        LOG.fine("Applying history database schema");
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(DDL_CREATE_HISTORY_TABLE);
            stmt.execute(DDL_INDEX_EXECUTED_AT);
            stmt.execute(DDL_INDEX_STARRED);
            stmt.execute(DDL_INDEX_NATURAL_LANGUAGE);
            conn.commit();
            LOG.fine("History schema applied successfully");
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Failed to apply history schema: " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOG.fine("History SQLite connection closed");
                }
            } catch (SQLException e) {
                LOG.warning("Error closing history SQLite connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
