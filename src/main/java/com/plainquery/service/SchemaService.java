package com.plainquery.service;

import com.plainquery.model.TableSchema;

import java.util.List;
import java.util.Optional;

public interface SchemaService {

    void register(TableSchema schema);

    List<TableSchema> getAll();

    Optional<TableSchema> getByName(String tableName);

    void remove(String tableName);

    void clear();

    List<String> getAllColumnNames();
}
