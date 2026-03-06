package com.plainquery.service;

import com.plainquery.config.AppConfig;
import com.plainquery.exception.AiConnectorException;
import com.plainquery.exception.QueryException;
import com.plainquery.exception.SqlExtractionException;
import com.plainquery.exception.SqlValidationException;
import com.plainquery.model.QueryResult;
import com.plainquery.model.TableSchema;
import com.plainquery.service.ai.AiConnector;
import com.plainquery.util.SqlValidator;
import com.plainquery.util.SqlIdentifierQuoter;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class QueryServiceImpl implements QueryService {

    private static final Logger LOG = Logger.getLogger(QueryServiceImpl.class.getName());

    private final AiConnector       aiConnector;
    private final SqliteExecutor    executor;
    private final SchemaService     schemaService;
    private final HistoryService    historyService;
    private final AppConfig         config;
    private final Connection        connection;

    public QueryServiceImpl(
            AiConnector aiConnector,
            SqliteExecutor executor,
            SchemaService schemaService,
            HistoryService historyService,
            AppConfig config,
            Connection connection) {

        Objects.requireNonNull(aiConnector,    "AiConnector must not be null");
        Objects.requireNonNull(executor,       "SqliteExecutor must not be null");
        Objects.requireNonNull(schemaService,  "SchemaService must not be null");
        Objects.requireNonNull(historyService, "HistoryService must not be null");
        Objects.requireNonNull(config,         "AppConfig must not be null");
        Objects.requireNonNull(connection,     "Connection must not be null");

        this.aiConnector    = aiConnector;
        this.executor       = executor;
        this.schemaService  = schemaService;
        this.historyService = historyService;
        this.config         = config;
        this.connection     = connection;
    }

    @Override
    public QueryResult executeNaturalLanguage(String question) throws QueryException {
        Objects.requireNonNull(question, "Question must not be null");

        String trimmed = question.trim();
        if (trimmed.isEmpty()) {
            throw new QueryException("Question must not be empty");
        }

        List<TableSchema> schemas = schemaService.getAll();
        if (schemas.isEmpty()) {
            throw new QueryException(
                "No data loaded. Please load at least one CSV file before querying.");
        }

        String generatedSql;
        try {
            generatedSql = aiConnector.translateToSql(trimmed, schemas);
        } catch (AiConnectorException | SqlExtractionException e) {
            throw new QueryException(
                "Could not translate question to SQL: " + e.getMessage(), e);
        }

        // Quote identifiers (table/column names) that contain spaces or
        // special characters so SQLite accepts them. Uses loaded schemas.
        generatedSql = SqlIdentifierQuoter.quoteIdentifiers(generatedSql, schemas);

        LOG.warning("AI generated SQL: " + generatedSql);

        try {
            SqlValidator.validate(generatedSql);
        } catch (SqlValidationException e) {
            throw new QueryException(
                "Generated SQL failed validation: " + e.getMessage(), e);
        }

        QueryResult result = executor.execute(
            generatedSql, connection, config.getPreviewRowLimit());

        try {
            historyService.record(trimmed, generatedSql);
        } catch (QueryException e) {
            LOG.warning("Failed to record query history: " + e.getMessage());
        }

        return result;
    }

    @Override
    public QueryResult executeSql(String sql) throws QueryException {
        Objects.requireNonNull(sql, "SQL must not be null");

        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            throw new QueryException("SQL must not be empty");
        }

        try {
            SqlValidator.validate(trimmed);
        } catch (SqlValidationException e) {
            throw new QueryException("SQL validation failed: " + e.getMessage(), e);
        }

        return executor.execute(trimmed, connection, config.getPreviewRowLimit());
    }
}
