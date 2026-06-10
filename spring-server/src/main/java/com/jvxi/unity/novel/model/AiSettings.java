package com.jvxi.unity.novel.model;

public record AiSettings(
    boolean enabled,
    String provider,
    String baseUrl,
    String apiKey,
    String model,
    double temperature,
    int maxTokens,
    int contextWindowSize,
    String systemPrompt
) {
}

