package com.plainquery.service.ai;

import com.plainquery.exception.AiConnectorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;

public final class GeminiConnector extends AbstractHttpAiConnector {

    private static final double TEMPERATURE   = 0.0;
    private static final int    MAX_TOKENS    = 512;

    public GeminiConnector(String apiKey, String endpointUrl) {
        super(apiKey, endpointUrl);
        Objects.requireNonNull(endpointUrl, "Endpoint URL must not be null");
    }

    @Override
    protected String buildRequestBody(String prompt) throws AiConnectorException {
        try {
            ObjectNode root = MAPPER.createObjectNode();

            ArrayNode contents = MAPPER.createArrayNode();
            ObjectNode contentItem = MAPPER.createObjectNode();

            ArrayNode parts = MAPPER.createArrayNode();
            ObjectNode part = MAPPER.createObjectNode();
            part.put("text", prompt);
            parts.add(part);

            contentItem.put("role", "user");
            contentItem.set("parts", parts);
            contents.add(contentItem);

            root.set("contents", contents);

            ObjectNode generationConfig = MAPPER.createObjectNode();
            generationConfig.put("temperature", TEMPERATURE);
            generationConfig.put("maxOutputTokens", MAX_TOKENS);
            root.set("generationConfig", generationConfig);

            ObjectNode systemInstruction = MAPPER.createObjectNode();
            ArrayNode systemParts = MAPPER.createArrayNode();
            ObjectNode systemPart = MAPPER.createObjectNode();
            systemPart.put("text",
                "You are a SQL generation assistant. "
                + "Respond with only a valid SQLite SELECT statement. "
                + "No explanation. No markdown. No semicolons.");
            systemParts.add(systemPart);
            systemInstruction.set("parts", systemParts);
            root.set("systemInstruction", systemInstruction);

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new AiConnectorException(
                "Failed to build Gemini request body: " + e.getMessage(), e);
        }
    }

    @Override
    protected String extractRawText(JsonNode responseRoot) throws AiConnectorException {
        JsonNode candidates = responseRoot.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            JsonNode errorNode = responseRoot.get("error");
            if (errorNode != null) {
                JsonNode msg = errorNode.get("message");
                throw new AiConnectorException(
                    "Gemini API error: " + (msg != null ? msg.asText() : "unknown"));
            }
            throw new AiConnectorException(
                "Gemini response missing 'candidates' array");
        }

        JsonNode firstCandidate = candidates.get(0);
        if (firstCandidate == null) {
            throw new AiConnectorException(
                "Gemini response 'candidates' array is empty");
        }

        JsonNode content = firstCandidate.get("content");
        if (content == null) {
            throw new AiConnectorException(
                "Gemini response candidate missing 'content' field");
        }

        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            throw new AiConnectorException(
                "Gemini response content missing 'parts' array");
        }

        JsonNode textNode = parts.get(0).get("text");
        if (textNode == null || textNode.isNull()) {
            throw new AiConnectorException(
                "Gemini response part missing 'text' field");
        }

        return textNode.asText();
    }

    @Override
    protected HttpRequest buildHttpRequest(String requestBody) {
        String urlWithKey = endpointUrl + "?key=" + apiKey;
        return HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    }
}
