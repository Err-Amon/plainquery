package com.plainquery.service.ai;

import com.plainquery.config.AiProvider;
import com.plainquery.exception.AiConnectorException;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpRequest;

public class OpenAiConnector extends AbstractHttpAiConnector {

    public OpenAiConnector(String apiKey) {
        super(apiKey, AiProvider.OPENAI.getEndpointUrl());
    }

    @Override
    protected String buildRequestBody(String prompt) throws AiConnectorException {
        String model = AiProvider.OPENAI.getModelIdentifier();
        
        return "{"
            + "\"model\": \"" + model + "\","
            + "\"messages\": ["
            + "  {"
            + "    \"role\": \"system\","
            + "    \"content\": \"You are a SQL expert specialized in SQLite. Generate only valid SQLite SELECT statements without explanations.\""
            + "  },"
            + "  {"
            + "    \"role\": \"user\","
            + "    \"content\": \"" + escapeJsonString(prompt) + "\""
            + "  }"
            + "],"
            + "\"temperature\": 0.1,"
            + "\"max_tokens\": 1000"
            + "}";
    }

    @Override
    protected String extractRawText(JsonNode responseRoot) throws AiConnectorException {
        try {
            return responseRoot.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText()
                .trim();
        } catch (Exception e) {
            throw new AiConnectorException(
                "Failed to extract text from OpenAI response: " + e.getMessage(), e);
        }
    }

    @Override
    protected HttpRequest buildHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    }

    private String escapeJsonString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
