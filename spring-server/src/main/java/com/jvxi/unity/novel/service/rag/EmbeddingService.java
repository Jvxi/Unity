package com.jvxi.unity.novel.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${inkfield.rag.embed-base-url:}")
    private String embedBaseUrl;

    @Value("${inkfield.rag.embed-model:text-embedding-3-small}")
    private String embedModel;

    @Value("${inkfield.rag.embed-api-key:}")
    private String embedApiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 生成文本嵌入向量
     */
    public List<Float> embed(String text) {
        if (embedBaseUrl == null || embedBaseUrl.isEmpty()) {
            log.warn("Embedding API未配置，返回空向量");
            return List.of();
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", embedModel,
                    "input", text
            );

            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embedBaseUrl + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + embedApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding API返回错误: status={}, body={}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode data = jsonNode.get("data");
            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode embedding = data.get(0).get("embedding");
                if (embedding != null && embedding.isArray()) {
                    List<Float> result = new ArrayList<>();
                    for (JsonNode value : embedding) {
                        result.add(value.floatValue());
                    }
                    return result;
                }
            }

            log.error("无法解析Embedding API响应");
            return List.of();

        } catch (Exception e) {
            log.error("调用Embedding API失败", e);
            return List.of();
        }
    }

    /**
     * 批量生成嵌入向量
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        if (embedBaseUrl == null || embedBaseUrl.isEmpty()) {
            log.warn("Embedding API未配置，返回空向量列表");
            return texts.stream().map(t -> List.<Float>of()).toList();
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", embedModel,
                    "input", texts
            );

            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embedBaseUrl + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + embedApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding Batch API返回错误: status={}, body={}", response.statusCode(), response.body());
                return texts.stream().map(t -> List.<Float>of()).toList();
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode data = jsonNode.get("data");
            if (data != null && data.isArray()) {
                List<List<Float>> results = new ArrayList<>();
                for (JsonNode item : data) {
                    JsonNode embedding = item.get("embedding");
                    if (embedding != null && embedding.isArray()) {
                        List<Float> vector = new ArrayList<>();
                        for (JsonNode value : embedding) {
                            vector.add(value.floatValue());
                        }
                        results.add(vector);
                    } else {
                        results.add(List.of());
                    }
                }
                return results;
            }

            log.error("无法解析Embedding Batch API响应");
            return texts.stream().map(t -> List.<Float>of()).toList();

        } catch (Exception e) {
            log.error("调用Embedding Batch API失败", e);
            return texts.stream().map(t -> List.<Float>of()).toList();
        }
    }

    /**
     * 检查Embedding API是否可用
     */
    public boolean isAvailable() {
        return embedBaseUrl != null && !embedBaseUrl.isEmpty() && embedApiKey != null && !embedApiKey.isEmpty();
    }
}

