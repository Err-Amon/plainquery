package com.plainquery.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TableSchema {

    private final String tableName;
    private final List<ColumnDefinition> columns;

    public TableSchema(String tableName, List<ColumnDefinition> columns) {
        Objects.requireNonNull(tableName, "Table name must not be null");
        Objects.requireNonNull(columns, "Columns must not be null");
        if (tableName.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        this.tableName = tableName;
        this.columns = Collections.unmodifiableList(List.copyOf(columns));
    }

    public String getTableName() { return tableName; }

    public List<ColumnDefinition> getColumns() { return columns; }

    public Optional<ColumnDefinition> getColumnByName(String name) {
        Objects.requireNonNull(name, "Column name must not be null");
        return columns.stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableSchema that)) return false;
        return tableName.equals(that.tableName) && columns.equals(that.columns);
    }

    @Override
    public int hashCode() { return Objects.hash(tableName, columns); }

    @Override
    public String toString() {
        return "TableSchema{tableName=" + tableName + ", columns=" + columns.size() + "}";
    }
}
