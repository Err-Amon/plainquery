package com.plainquery.service.ai;

import com.plainquery.exception.AiConnectorException;
import com.plainquery.exception.SqlExtractionException;
import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.TableSchema;
import com.plainquery.util.SqlExtractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class AbstractHttpAiConnector implements AiConnector {

    private static final Logger LOG =
        Logger.getLogger(AbstractHttpAiConnector.class.getName());

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(30);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected final HttpClient httpClient;
    protected final String apiKey;
    protected final String endpointUrl;

    protected AbstractHttpAiConnector(String apiKey, String endpointUrl) {
        Objects.requireNonNull(apiKey,      "API key must not be null");
        Objects.requireNonNull(endpointUrl, "Endpoint URL must not be null");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be blank");
        }
        this.apiKey      = apiKey;
        this.endpointUrl = endpointUrl;
        this.httpClient  = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    }

    @Override
    public final String translateToSql(String question, List<TableSchema> schemas)
            throws AiConnectorException, SqlExtractionException {

        Objects.requireNonNull(question, "Question must not be null");
        Objects.requireNonNull(schemas,  "Schemas must not be null");

        if (question.isBlank()) {
            throw new AiConnectorException("Question must not be blank");
        }
        if (schemas.isEmpty()) {
            throw new AiConnectorException("Schema list must not be empty");
        }

        String prompt      = buildPrompt(question, schemas);
        String requestBody = buildRequestBody(prompt);

        LOG.fine("Sending AI request to: " + endpointUrl);

        HttpRequest request = buildHttpRequest(requestBody);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AiConnectorException(
                "HTTP request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiConnectorException(
                "HTTP request was interrupted", e);
        }

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new AiConnectorException(
                "AI API returned HTTP " + statusCode
                + ". Check your API key and provider settings.");
        }

        String rawText;
        try {
            JsonNode root = MAPPER.readTree(response.body());
            rawText = extractRawText(root);
        } catch (IOException e) {
            throw new AiConnectorException(
                "Failed to parse AI API response: " + e.getMessage(), e);
        }

        if (rawText == null || rawText.isBlank()) {
            throw new AiConnectorException(
                "AI API returned an empty response body");
        }

        LOG.fine("AI raw response length: " + rawText.length() + " chars");
        return SqlExtractor.extract(rawText);
    }

    protected HttpRequest buildHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    }

    protected String buildPrompt(String question, List<TableSchema> schemas) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a SQL expert. Given the following SQLite database schema, ");
        sb.append("generate a single valid SQLite SELECT statement to answer the question.\n");
        sb.append("Rules:\n");
        sb.append("- Output ONLY the SQL statement, no explanation.\n");
        sb.append("- Use only SELECT. No INSERT, UPDATE, DELETE, DROP, PRAGMA, or semicolons.\n");
        sb.append("- Use table and column names exactly as shown below.\n");
        sb.append("- Wrap column and table names in double quotes if they contain spaces.\n\n");
        sb.append("Schema:\n");

        for (TableSchema schema : schemas) {
            sb.append("Table: ").append(schema.getTableName()).append("\n");
            sb.append("Columns:\n");
            for (ColumnDefinition col : schema.getColumns()) {
                sb.append("  - ").append(col.getName())
                  .append(" (").append(col.getColumnType().sqlTypeName()).append(")");
                if (!col.getSampleValues().isEmpty()) {
                    sb.append(" samples: ").append(col.getSampleValues());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("Question: ").append(question).append("\n");
        sb.append("SQL:");
        return sb.toString();
    }

    protected abstract String buildRequestBody(String prompt) throws AiConnectorException;

    protected abstract String extractRawText(JsonNode responseRoot) throws AiConnectorException;
}
