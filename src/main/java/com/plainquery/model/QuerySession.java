package com.plainquery.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuerySession {
    
    private final String id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QueryHistoryEntry> historyEntries;
    
    @JsonCreator
    public QuerySession(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("updatedAt") LocalDateTime updatedAt,
            @JsonProperty("historyEntries") List<QueryHistoryEntry> historyEntries) {
        this.id = Objects.requireNonNull(id, "Session ID cannot be null");
        this.name = Objects.requireNonNull(name, "Session name cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update timestamp cannot be null");
        this.historyEntries = historyEntries != null ? new ArrayList<>(historyEntries) : new ArrayList<>();
    }
    
    public QuerySession(String id, String name) {
        this(id, name, LocalDateTime.now(), LocalDateTime.now(), null);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Session name cannot be null");
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update timestamp cannot be null");
    }
    
    public List<QueryHistoryEntry> getHistoryEntries() {
        return historyEntries;
    }
    
    public void setHistoryEntries(List<QueryHistoryEntry> historyEntries) {
        this.historyEntries = new ArrayList<>(Objects.requireNonNullElse(historyEntries, new ArrayList<>()));
    }
    
    public void addHistoryEntry(QueryHistoryEntry entry) {
        Objects.requireNonNull(entry, "History entry cannot be null");
        if (historyEntries == null) {
            historyEntries = new ArrayList<>();
        }
        historyEntries.add(entry);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeHistoryEntry(long entryId) {
        if (historyEntries != null) {
            historyEntries.removeIf(entry -> entry.getId() == entryId);
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public boolean isEmpty() {
        return historyEntries == null || historyEntries.isEmpty();
    }
    
    public int getEntryCount() {
        return historyEntries != null ? historyEntries.size() : 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuerySession that = (QuerySession) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "QuerySession{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", entryCount=" + getEntryCount() +
                '}';
    }
}
