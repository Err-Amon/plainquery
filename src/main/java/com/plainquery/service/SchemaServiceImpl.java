package com.plainquery.service;

import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.TableSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class SchemaServiceImpl implements SchemaService {

    private static final Logger LOG = Logger.getLogger(SchemaServiceImpl.class.getName());

    private final ConcurrentHashMap<String, TableSchema> schemas = new ConcurrentHashMap<>();

    public SchemaServiceImpl() {}

    @Override
    public void register(TableSchema schema) {
        Objects.requireNonNull(schema, "TableSchema must not be null");
        schemas.put(schema.getTableName(), schema);
        LOG.fine("Registered schema for table: " + schema.getTableName());
    }

    @Override
    public List<TableSchema> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(schemas.values()));
    }

    @Override
    public Optional<TableSchema> getByName(String tableName) {
        Objects.requireNonNull(tableName, "Table name must not be null");
        return Optional.ofNullable(schemas.get(tableName));
    }

    @Override
    public void remove(String tableName) {
        Objects.requireNonNull(tableName, "Table name must not be null");
        schemas.remove(tableName);
        LOG.fine("Removed schema for table: " + tableName);
    }

    @Override
    public void clear() {
        schemas.clear();
        LOG.fine("All schemas cleared");
    }

    @Override
    public List<String> getAllColumnNames() {
        List<String> names = new ArrayList<>();
        for (TableSchema schema : schemas.values()) {
            for (ColumnDefinition col : schema.getColumns()) {
                String qualified = schema.getTableName() + "." + col.getName();
                if (!names.contains(qualified)) {
                    names.add(qualified);
                }
            }
        }
        return Collections.unmodifiableList(names);
    }
}
