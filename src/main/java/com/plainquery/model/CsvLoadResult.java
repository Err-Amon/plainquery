package com.plainquery.model;

import java.util.Objects;

public final class CsvLoadResult {

    private final String tableName;
    private final TableSchema schema;
    private final int rowsInserted;

    public CsvLoadResult(String tableName, TableSchema schema, int rowsInserted) {
        Objects.requireNonNull(tableName, "Table name must not be null");
        Objects.requireNonNull(schema, "Schema must not be null");
        if (rowsInserted < 0) {
            throw new IllegalArgumentException("Rows inserted must not be negative");
        }
        this.tableName = tableName;
        this.schema = schema;
        this.rowsInserted = rowsInserted;
    }

    public String getTableName() { return tableName; }
    public TableSchema getSchema() { return schema; }
    public int getRowsInserted() { return rowsInserted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CsvLoadResult that)) return false;
        return rowsInserted == that.rowsInserted
            && tableName.equals(that.tableName)
            && schema.equals(that.schema);
    }

    @Override
    public int hashCode() { return Objects.hash(tableName, schema, rowsInserted); }

    @Override
    public String toString() {
        return "CsvLoadResult{table=" + tableName + ", rows=" + rowsInserted + "}";
    }
}
