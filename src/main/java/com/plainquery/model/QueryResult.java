package com.plainquery.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QueryResult {

    private final List<String> columnNames;
    private final List<List<Object>> rows;
    private final int rowCount;

    public QueryResult(List<String> columnNames, List<List<Object>> rows) {
        Objects.requireNonNull(columnNames, "Column names must not be null");
        Objects.requireNonNull(rows, "Rows must not be null");
        this.columnNames = Collections.unmodifiableList(List.copyOf(columnNames));
        this.rows = Collections.unmodifiableList(List.copyOf(rows));
        this.rowCount = rows.size();
    }

    public List<String> getColumnNames() { return columnNames; }
    public List<List<Object>> getRows() { return rows; }
    public int getRowCount() { return rowCount; }
    public boolean isEmpty() { return rows.isEmpty(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryResult that)) return false;
        return columnNames.equals(that.columnNames) && rows.equals(that.rows);
    }

    @Override
    public int hashCode() { return Objects.hash(columnNames, rows); }

    @Override
    public String toString() {
        return "QueryResult{columns=" + columnNames.size() + ", rows=" + rowCount + "}";
    }
}
