package com.plainquery.service.ai;

import com.plainquery.config.AppConfig;
import com.plainquery.config.AiProvider;

import java.util.Objects;

public final class AiConnectorFactory {

    private AiConnectorFactory() {}

    public static AiConnector create(AppConfig config) {
        Objects.requireNonNull(config, "AppConfig must not be null");

        AiProvider provider = config.getAiProvider();
        String apiKey       = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "No API key configured. Please open Settings and enter an API key "
                + "for the selected AI provider (" + provider.name() + ").");
        }

        switch (provider) {
            case GROQ:
                return new GroqConnector(apiKey, provider.getEndpointUrl());
            case GEMINI:
                return new GeminiConnector(apiKey, provider.getEndpointUrl());
            case OPENAI:
                return new OpenAiConnector(apiKey);
            default:
                throw new IllegalStateException(
                    "Unsupported AI provider: " + provider.name());
        }
    }
}
