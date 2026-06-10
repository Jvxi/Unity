package com.jvxi.unity.novel.model;

public record EmbeddingSettings(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model
) {
}
