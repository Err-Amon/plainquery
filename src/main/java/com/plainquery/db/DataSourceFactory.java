package com.plainquery.db;

import com.plainquery.config.AppConfig;
import com.plainquery.config.AppConfig.DbMode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class DataSourceFactory implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(DataSourceFactory.class.getName());

    private static final String MEMORY_URL = "jdbc:sqlite::memory:";
    private static final String FILE_URL_PREFIX = "jdbc:sqlite:";
    private static final String TEMP_DB_FILENAME = "plainquery-data.db";

    private final AppConfig config;
    private Connection connection;
    private Path tempFilePath;

    public DataSourceFactory(AppConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AppConfig must not be null");
        }
        this.config = config;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
            configureConnection(connection);
        }
        return connection;
    }

    private Connection openConnection() throws SQLException {
        DbMode mode = config.getDbMode();

        if (mode == DbMode.MEMORY) {
            LOG.fine("Opening in-memory SQLite database");
            return DriverManager.getConnection(MEMORY_URL);
        }

        Path dbPath = resolveFilePath();
        LOG.fine("Opening file-based SQLite database at: " + dbPath);
        return DriverManager.getConnection(FILE_URL_PREFIX + dbPath.toAbsolutePath());
    }

    private Path resolveFilePath() throws SQLException {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".plainquery");
            Files.createDirectories(dir);
            tempFilePath = dir.resolve(TEMP_DB_FILENAME);
            return tempFilePath;
        } catch (Exception e) {
            throw new SQLException("Could not resolve file-based database path: "
                + e.getMessage(), e);
        }
    }

    private void configureConnection(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA temp_store=MEMORY");
            stmt.execute("PRAGMA cache_size=-8000");
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOG.fine("SQLite data connection closed");
                }
            } catch (SQLException e) {
                LOG.warning("Error closing SQLite data connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }

        if (tempFilePath != null) {
            File f = tempFilePath.toFile();
            if (f.exists() && config.getDbMode() == DbMode.MEMORY) {
                boolean deleted = f.delete();
                if (!deleted) {
                    LOG.warning("Could not delete temp database file: " + tempFilePath);
                }
            }
            tempFilePath = null;
        }
    }
}
