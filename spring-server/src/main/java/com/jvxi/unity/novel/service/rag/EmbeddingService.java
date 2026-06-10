package com.jvxi.unity.novel.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.EmbeddingSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public List<Float> embed(String text) {
        return embed(text, null);
    }

    public List<Float> embed(String text, EmbeddingSettings settings) {
        if (!isAvailable(settings) || text == null || text.isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", normalizedModel(settings),
                    "input", text
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingEndpoint(settings.baseUrl())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Embedding API returned status {}", response.statusCode());
                return List.of();
            }

            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (data.isArray() && !data.isEmpty()) {
                return readEmbedding(data.get(0).path("embedding"));
            }
            return List.of();
        } catch (Exception exception) {
            log.warn("Embedding API call failed: {}", sanitize(exception.getMessage()));
            return List.of();
        }
    }

    public List<List<Float>> embedBatch(List<String> texts, EmbeddingSettings settings) {
        if (!isAvailable(settings) || texts == null || texts.isEmpty()) {
            return texts == null ? List.of() : texts.stream().map(ignored -> List.<Float>of()).toList();
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", normalizedModel(settings),
                    "input", texts
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingEndpoint(settings.baseUrl())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Embedding batch API returned status {}", response.statusCode());
                return texts.stream().map(ignored -> List.<Float>of()).toList();
            }

            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (!data.isArray()) {
                return texts.stream().map(ignored -> List.<Float>of()).toList();
            }

            List<List<Float>> result = new ArrayList<>();
            for (JsonNode item : data) {
                result.add(readEmbedding(item.path("embedding")));
            }
            while (result.size() < texts.size()) {
                result.add(List.of());
            }
            return result;
        } catch (Exception exception) {
            log.warn("Embedding batch API call failed: {}", sanitize(exception.getMessage()));
            return texts.stream().map(ignored -> List.<Float>of()).toList();
        }
    }

    public boolean isAvailable(EmbeddingSettings settings) {
        return settings != null
                && settings.enabled()
                && settings.baseUrl() != null
                && !settings.baseUrl().isBlank()
                && settings.apiKey() != null
                && !settings.apiKey().isBlank()
                && normalizedModel(settings) != null
                && !normalizedModel(settings).isBlank();
    }

    private List<Float> readEmbedding(JsonNode embeddingNode) {
        if (embeddingNode == null || !embeddingNode.isArray()) {
            return List.of();
        }
        List<Float> result = new ArrayList<>();
        for (JsonNode value : embeddingNode) {
            result.add(value.floatValue());
        }
        return result;
    }

    private String normalizedModel(EmbeddingSettings settings) {
        String model = settings == null ? "" : text(settings.model());
        return model.isBlank() ? "text-embedding-3-small" : model;
    }

    private String embeddingEndpoint(String baseUrl) {
        String normalized = text(baseUrl);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/embeddings")) {
            return normalized;
        }
        if (lower.endsWith("/v1")) {
            return normalized + "/embeddings";
        }
        return normalized + "/v1/embeddings";
    }

    private String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [hidden]")
                .replaceAll("https?://\\S+", "[hidden-url]");
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
