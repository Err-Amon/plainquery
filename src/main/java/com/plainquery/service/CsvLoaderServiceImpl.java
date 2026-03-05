package com.plainquery.service;

import com.plainquery.exception.CsvLoadException;
import com.plainquery.exception.InsufficientMemoryException;
import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.CsvLoadResult;
import com.plainquery.model.TableSchema;
import com.plainquery.util.DelimiterDetector;
import com.plainquery.util.EncodingDetector;
import com.plainquery.util.MemoryGuard;
import com.plainquery.util.SchemaInferer;
import com.plainquery.util.TableNameSanitizer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class CsvLoaderServiceImpl implements CsvLoaderService {

    private static final Logger LOG = Logger.getLogger(CsvLoaderServiceImpl.class.getName());

    private static final int BATCH_SIZE       = 500;
    private static final int SCHEMA_SAMPLE    = 200;
    private static final long BYTES_PER_ROW_ESTIMATE = 512L;

    public CsvLoaderServiceImpl() {}

    @Override
    public CsvLoadResult load(File csvFile, Connection connection)
            throws CsvLoadException, InsufficientMemoryException {

        Objects.requireNonNull(csvFile, "CSV file must not be null");
        Objects.requireNonNull(connection, "Connection must not be null");

        if (!csvFile.exists() || !csvFile.isFile()) {
            throw new CsvLoadException("File does not exist or is not a regular file: "
                + csvFile.getPath());
        }
        if (!csvFile.canRead()) {
            throw new CsvLoadException("File is not readable: " + csvFile.getPath());
        }

        long estimatedBytes = csvFile.length() * 3;
        MemoryGuard.assertSufficientMemory(estimatedBytes);

        String tableName = TableNameSanitizer.sanitize(csvFile.getName());
        Charset charset  = EncodingDetector.detect(csvFile);
        char delimiter   = DelimiterDetector.detect(csvFile, charset);

        LOG.fine("Loading CSV: " + csvFile.getName()
            + " | charset=" + charset + " | delimiter='" + delimiter + "'");

        List<String> headers    = readHeaders(csvFile, charset, delimiter);
        List<String[]> samples  = readSampleRows(csvFile, charset, delimiter, SCHEMA_SAMPLE);
        List<ColumnDefinition> columns = SchemaInferer.infer(headers, samples);
        TableSchema schema = new TableSchema(tableName, columns);

        try {
            createTable(connection, schema);
            int rowsInserted = insertData(connection, csvFile, charset, delimiter, schema);
            LOG.fine("Loaded " + rowsInserted + " rows into table: " + tableName);
            return new CsvLoadResult(tableName, schema, rowsInserted);
        } catch (SQLException e) {
            throw new CsvLoadException("Database error while loading CSV: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CsvLoadException("IO error while loading CSV: " + e.getMessage(), e);
        }
    }

    private List<String> readHeaders(File file, Charset charset, char delimiter)
            throws CsvLoadException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
             CSVParser parser = CSVParser.parse(reader, format)) {

            CSVRecord headerRecord = null;
            for (CSVRecord record : parser) {
                headerRecord = record;
                break;
            }
            if (headerRecord == null) {
                throw new CsvLoadException("CSV file is empty: " + file.getName());
            }
            List<String> headers = new ArrayList<>();
            for (String value : headerRecord) {
                headers.add(value.isBlank() ? "column_" + (headers.size() + 1) : value.trim());
            }
            if (headers.isEmpty()) {
                throw new CsvLoadException("CSV file has no columns: " + file.getName());
            }
            return headers;
        } catch (IOException e) {
            throw new CsvLoadException("Failed to read CSV headers: " + e.getMessage(), e);
        }
    }

    private List<String[]> readSampleRows(File file, Charset charset,
            char delimiter, int maxRows) throws CsvLoadException {

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .setSkipHeaderRecord(true)
            .build();

        List<String[]> samples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
             CSVParser parser = CSVParser.parse(reader, format)) {

            for (CSVRecord record : parser) {
                if (samples.size() >= maxRows) break;
                String[] row = new String[record.size()];
                for (int i = 0; i < record.size(); i++) {
                    row[i] = record.get(i);
                }
                samples.add(row);
            }
        } catch (IOException e) {
            throw new CsvLoadException("Failed to read sample rows: " + e.getMessage(), e);
        }
        return samples;
    }

    private void createTable(Connection connection, TableSchema schema)
            throws SQLException {

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS \"");
        ddl.append(schema.getTableName()).append("\" (");

        List<ColumnDefinition> cols = schema.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            ColumnDefinition col = cols.get(i);
            ddl.append("\"").append(sanitizeColumnName(col.getName())).append("\"");
            ddl.append(" ").append(col.getColumnType().sqlTypeName());
            if (i < cols.size() - 1) {
                ddl.append(", ");
            }
        }
        ddl.append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ddl.toString());
            LOG.fine("Created table: " + schema.getTableName());
        }
    }

    private int insertData(Connection connection, File file, Charset charset,
            char delimiter, TableSchema schema) throws SQLException, IOException {

        List<ColumnDefinition> cols = schema.getColumns();
        String insertSql = buildInsertSql(schema.getTableName(), cols);

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .setSkipHeaderRecord(true)
            .build();

        int totalInserted = 0;
        int batchCount    = 0;

        connection.setAutoCommit(false);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
             CSVParser parser = CSVParser.parse(reader, format);
             PreparedStatement stmt = connection.prepareStatement(insertSql)) {

            for (CSVRecord record : parser) {
                for (int i = 0; i < cols.size(); i++) {
                    String value = (i < record.size()) ? record.get(i) : null;
                    stmt.setObject(i + 1, normalizeValue(value, cols.get(i)));
                }
                stmt.addBatch();
                batchCount++;

                if (batchCount >= BATCH_SIZE) {
                    stmt.executeBatch();
                    connection.commit();
                    totalInserted += batchCount;
                    batchCount = 0;
                }
            }

            if (batchCount > 0) {
                stmt.executeBatch();
                connection.commit();
                totalInserted += batchCount;
            }

        } catch (SQLException | IOException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOG.warning("Rollback failed: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.warning("Failed to restore autocommit: " + e.getMessage());
            }
        }

        return totalInserted;
    }

    private String buildInsertSql(String tableName, List<ColumnDefinition> cols) {
        StringBuilder sb = new StringBuilder("INSERT INTO \"");
        sb.append(tableName).append("\" (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append("\"").append(sanitizeColumnName(cols.get(i).getName())).append("\"");
            if (i < cols.size() - 1) sb.append(", ");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append("?");
            if (i < cols.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private Object normalizeValue(String value, ColumnDefinition col) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        switch (col.getColumnType()) {
            case INTEGER:
                try {
                    return Long.parseLong(trimmed.replace(",", ""));
                } catch (NumberFormatException e) {
                    return trimmed;
                }
            case REAL:
                try {
                    return Double.parseDouble(trimmed.replace(",", ""));
                } catch (NumberFormatException e) {
                    return trimmed;
                }
            default:
                return trimmed;
        }
    }

    private String sanitizeColumnName(String name) {
        return name.replace("\"", "\"\"");
    }
}
