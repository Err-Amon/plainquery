package com.plainquery.config;

public enum AiProvider {

    GROQ(
        "https://api.groq.com/openai/v1/chat/completions",
        "llama3-8b-8192"
    ),
    GEMINI(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent",
        "gemini-1.5-flash-latest"
    ),
    OPENAI(
        "https://api.openai.com/v1/chat/completions",
        "gpt-4-turbo"
    ),
    LLAMA3(
        "https://api.groq.com/openai/v1/chat/completions",
        "llama3-70b-8192"
    );

    private final String endpointUrl;
    private final String modelIdentifier;

    AiProvider(String endpointUrl, String modelIdentifier) {
        this.endpointUrl = endpointUrl;
        this.modelIdentifier = modelIdentifier;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }
}
