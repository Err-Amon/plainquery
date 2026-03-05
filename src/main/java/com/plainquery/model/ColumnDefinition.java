package com.plainquery.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ColumnDefinition {

    private final String name;
    private final ColumnType columnType;
    private final List<String> sampleValues;

    public ColumnDefinition(String name, ColumnType columnType, List<String> sampleValues) {
        Objects.requireNonNull(name, "Column name must not be null");
        Objects.requireNonNull(columnType, "ColumnType must not be null");
        Objects.requireNonNull(sampleValues, "Sample values must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be blank");
        }
        this.name = name;
        this.columnType = columnType;
        this.sampleValues = Collections.unmodifiableList(List.copyOf(sampleValues));
    }

    public String getName() { return name; }
    public ColumnType getColumnType() { return columnType; }
    public List<String> getSampleValues() { return sampleValues; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnDefinition that)) return false;
        return name.equals(that.name)
            && columnType == that.columnType
            && sampleValues.equals(that.sampleValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnType, sampleValues);
    }

    @Override
    public String toString() {
        return "ColumnDefinition{name=" + name + ", type=" + columnType + "}";
    }
}
