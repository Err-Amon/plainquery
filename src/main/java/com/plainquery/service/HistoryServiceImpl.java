package com.plainquery.service;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryHistoryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class HistoryServiceImpl implements HistoryService {

    private static final Logger LOG = Logger.getLogger(HistoryServiceImpl.class.getName());

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String SQL_INSERT =
        "INSERT INTO query_history (natural_language, generated_sql, executed_at, starred) "
        + "VALUES (?, ?, ?, 0)";

    private static final String SQL_SEARCH =
        "SELECT id, natural_language, generated_sql, executed_at, starred "
        + "FROM query_history "
        + "WHERE natural_language LIKE ? "
        + "ORDER BY executed_at DESC "
        + "LIMIT 200";

    private static final String SQL_RECENT =
        "SELECT id, natural_language, generated_sql, executed_at, starred "
        + "FROM query_history "
        + "ORDER BY executed_at DESC "
        + "LIMIT ?";

    private static final String SQL_STARRED =
        "SELECT id, natural_language, generated_sql, executed_at, starred "
        + "FROM query_history "
        + "WHERE starred = 1 "
        + "ORDER BY executed_at DESC";

    private static final String SQL_TOGGLE_STAR =
        "UPDATE query_history SET starred = CASE WHEN starred = 1 THEN 0 ELSE 1 END "
        + "WHERE id = ?";

    private static final String SQL_DELETE =
        "DELETE FROM query_history WHERE id = ?";

    private final Connection connection;

    public HistoryServiceImpl(Connection connection) {
        Objects.requireNonNull(connection, "History connection must not be null");
        this.connection = connection;
    }

    @Override
    public void record(String naturalLanguage, String generatedSql) throws QueryException {
        Objects.requireNonNull(naturalLanguage, "Natural language must not be null");
        Objects.requireNonNull(generatedSql, "Generated SQL must not be null");

        try (PreparedStatement stmt = connection.prepareStatement(SQL_INSERT)) {
            stmt.setString(1, naturalLanguage.trim());
            stmt.setString(2, generatedSql.trim());
            stmt.setString(3, LocalDateTime.now().format(FORMATTER));
            stmt.executeUpdate();
            LOG.fine("History entry recorded");
        } catch (SQLException e) {
            throw new QueryException("Failed to record query history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QueryHistoryEntry> search(String term) throws QueryException {
        Objects.requireNonNull(term, "Search term must not be null");

        try (PreparedStatement stmt = connection.prepareStatement(SQL_SEARCH)) {
            stmt.setString(1, "%" + term.trim() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs);
            }
        } catch (SQLException e) {
            throw new QueryException("History search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QueryHistoryEntry> getRecent(int limit) throws QueryException {
        if (limit < 1) {
            throw new QueryException("Limit must be at least 1");
        }

        try (PreparedStatement stmt = connection.prepareStatement(SQL_RECENT)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs);
            }
        } catch (SQLException e) {
            throw new QueryException("Failed to fetch recent history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QueryHistoryEntry> getStarred() throws QueryException {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_STARRED)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs);
            }
        } catch (SQLException e) {
            throw new QueryException("Failed to fetch starred history: " + e.getMessage(), e);
        }
    }

    @Override
    public void toggleStar(long id) throws QueryException {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_TOGGLE_STAR)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new QueryException("Failed to toggle star for id=" + id
                + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(long id) throws QueryException {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            LOG.fine("Deleted history entry id=" + id);
        } catch (SQLException e) {
            throw new QueryException("Failed to delete history entry id=" + id
                + ": " + e.getMessage(), e);
        }
    }

    private List<QueryHistoryEntry> mapEntries(ResultSet rs) throws SQLException {
        List<QueryHistoryEntry> entries = new ArrayList<>();
        while (rs.next()) {
            long id              = rs.getLong("id");
            String nlq           = rs.getString("natural_language");
            String sql           = rs.getString("generated_sql");
            String executedAtStr = rs.getString("executed_at");
            boolean starred      = rs.getInt("starred") == 1;

            LocalDateTime executedAt;
            try {
                executedAt = LocalDateTime.parse(executedAtStr, FORMATTER);
            } catch (Exception e) {
                executedAt = LocalDateTime.now();
                LOG.warning("Could not parse executed_at value: " + executedAtStr);
            }

            entries.add(new QueryHistoryEntry(id, nlq, sql, executedAt, starred));
        }
        return entries;
    }
}
