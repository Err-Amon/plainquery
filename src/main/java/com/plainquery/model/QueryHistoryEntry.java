package com.plainquery.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class QueryHistoryEntry {

    private final long id;
    private final String naturalLanguage;
    private final String generatedSql;
    private final LocalDateTime executedAt;
    private final boolean starred;

    public QueryHistoryEntry(
            long id,
            String naturalLanguage,
            String generatedSql,
            LocalDateTime executedAt,
            boolean starred) {
        Objects.requireNonNull(naturalLanguage, "Natural language must not be null");
        Objects.requireNonNull(generatedSql, "Generated SQL must not be null");
        Objects.requireNonNull(executedAt, "ExecutedAt must not be null");
        this.id = id;
        this.naturalLanguage = naturalLanguage;
        this.generatedSql = generatedSql;
        this.executedAt = executedAt;
        this.starred = starred;
    }

    public long getId() { return id; }
    public String getNaturalLanguage() { return naturalLanguage; }
    public String getGeneratedSql() { return generatedSql; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public boolean isStarred() { return starred; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryHistoryEntry that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "QueryHistoryEntry{id=" + id + ", starred=" + starred + "}";
    }
}
