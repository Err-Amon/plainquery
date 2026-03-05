package com.plainquery.service.ai;

import com.plainquery.exception.AiConnectorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

public final class GroqConnector extends AbstractHttpAiConnector {

    private static final String MODEL = "llama3-8b-8192";
    private static final double TEMPERATURE = 0.0;
    private static final int MAX_TOKENS = 512;

    public GroqConnector(String apiKey, String endpointUrl) {
        super(apiKey, endpointUrl);
        Objects.requireNonNull(endpointUrl, "Endpoint URL must not be null");
    }

    @Override
    protected String buildRequestBody(String prompt) throws AiConnectorException {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("model", MODEL);
            root.put("temperature", TEMPERATURE);
            root.put("max_tokens", MAX_TOKENS);

            ArrayNode messages = MAPPER.createArrayNode();
            ObjectNode systemMsg = MAPPER.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                "You are a SQL generation assistant. "
                + "Respond with only a valid SQLite SELECT statement. "
                + "No explanation. No markdown. No semicolons.");
            messages.add(systemMsg);

            ObjectNode userMsg = MAPPER.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            root.set("messages", messages);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new AiConnectorException(
                "Failed to build Groq request body: " + e.getMessage(), e);
        }
    }

    @Override
    protected String extractRawText(JsonNode responseRoot) throws AiConnectorException {
        JsonNode choices = responseRoot.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new AiConnectorException(
                "Groq response missing 'choices' array");
        }

        JsonNode firstChoice = choices.get(0);
        if (firstChoice == null) {
            throw new AiConnectorException(
                "Groq response 'choices' array is empty");
        }

        JsonNode message = firstChoice.get("message");
        if (message == null) {
            throw new AiConnectorException(
                "Groq response choice missing 'message' field");
        }

        JsonNode content = message.get("content");
        if (content == null || content.isNull()) {
            throw new AiConnectorException(
                "Groq response message 'content' is null");
        }

        return content.asText();
    }

    @Override
    protected java.net.http.HttpRequest buildHttpRequest(String requestBody) {
        return java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(endpointUrl))
            .timeout(java.time.Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    }
}
