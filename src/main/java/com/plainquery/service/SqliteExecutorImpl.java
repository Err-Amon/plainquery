package com.plainquery.service;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class SqliteExecutorImpl implements SqliteExecutor {

    private static final Logger LOG = Logger.getLogger(SqliteExecutorImpl.class.getName());

    public SqliteExecutorImpl() {}

    @Override
    public QueryResult execute(String sql, Connection connection, int maxRows)
            throws QueryException {

        Objects.requireNonNull(sql, "SQL must not be null");
        Objects.requireNonNull(connection, "Connection must not be null");

        if (sql.isBlank()) {
            throw new QueryException("SQL statement must not be blank");
        }
        if (maxRows < 1) {
            throw new QueryException("maxRows must be at least 1");
        }

        LOG.fine("Executing SQL (maxRows=" + maxRows + "): "
            + sql.substring(0, Math.min(sql.length(), 120)));

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setMaxRows(maxRows);

            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSet(rs, maxRows);
            }

        } catch (SQLException e) {
            throw new QueryException(
                "SQL execution failed: " + e.getMessage(), e);
        }
    }

    private QueryResult mapResultSet(ResultSet rs, int maxRows) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columnNames = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String label = meta.getColumnLabel(i);
            columnNames.add(label != null && !label.isBlank()
                ? label
                : meta.getColumnName(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;

        while (rs.next() && rowCount < maxRows) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(Collections.unmodifiableList(row));
            rowCount++;
        }

        LOG.fine("Query returned " + rowCount + " rows, " + columnCount + " columns");
        return new QueryResult(columnNames, rows);
    }
}
