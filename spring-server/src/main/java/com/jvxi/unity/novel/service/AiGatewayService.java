package com.jvxi.unity.novel.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;

@Service
public class AiGatewayService {
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiGatewayService(ObjectMapper objectMapper, HttpClient aiHttpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = aiHttpClient;
    }

    public boolean isRemoteModelReady(AiSettings settings) {
        return settings != null
            && settings.enabled()
            && !"rule-based".equalsIgnoreCase(settings.provider())
            && !text(settings.baseUrl()).isBlank()
            && !text(settings.apiKey()).isBlank()
            && !text(settings.model()).isBlank();
    }

    public String providerLabel(AiSettings settings, boolean stream) {
        Protocol protocol = resolveProtocol(settings);
        String suffix = stream ? "-stream" : "";
        return switch (protocol) {
            case RESPONSES -> "openai-responses" + suffix;
            case ANTHROPIC_MESSAGES -> "anthropic-messages" + suffix;
            case CHAT_COMPLETIONS -> "openai-compatible" + suffix;
        };
    }

    public CompletionResult complete(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options
    ) throws IOException, InterruptedException {
        Protocol protocol = resolveProtocol(settings);
        AiCompletionOptions resolved = resolveOptions(settings, options);
        URI endpoint = endpointFor(settings, protocol);

        HttpResponse<String> response = postJson(
            endpoint,
            settings.apiKey(),
            headersFor(settings, protocol),
            buildPayload(settings, systemPrompt, userPrompt, resolved, protocol, false, false),
            resolved.timeoutSeconds()
        );

        if (!resolved.connectivityProbe()
            && response.statusCode() >= 400
            && resolved.jsonObjectMode()
            && protocol != Protocol.ANTHROPIC_MESSAGES) {
            response = postJson(
                endpoint,
                settings.apiKey(),
                headersFor(settings, protocol),
                buildPayload(settings, systemPrompt, userPrompt, withoutJsonMode(resolved), protocol, false, false),
                resolved.timeoutSeconds()
            );
        }

        if (!resolved.connectivityProbe() && response.statusCode() >= 400 && protocol == Protocol.CHAT_COMPLETIONS) {
            response = postJson(
                endpoint,
                settings.apiKey(),
                headersFor(settings, protocol),
                buildPayload(settings, systemPrompt, userPrompt, resolved, protocol, false, true),
                resolved.timeoutSeconds()
            );
        }

        if (response.statusCode() >= 400) {
            throw new IOException("Remote API error " + response.statusCode() + ": " + readErrorMessage(response.body()));
        }

        CompletionResult result = readCompletion(response.body(), protocol);
        if (resolved.connectivityProbe()) {
            return result.content().isBlank()
                ? new CompletionResult(readConnectivityProbeFallback(response.body(), protocol), 0, 0, 0)
                : new CompletionResult(result.content(), 0, 0, 0);
        }
        return result;
    }

    public CompletionResult stream(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options,
        Consumer<String> onDelta,
        Runnable cancellationCheck
    ) throws IOException, InterruptedException {
        if (onDelta == null) {
            return new CompletionResult("", 0, 0, 0);
        }

        Protocol protocol = resolveProtocol(settings);
        AiCompletionOptions resolved = resolveOptions(settings, options);
        if (protocol == Protocol.ANTHROPIC_MESSAGES) {
            CompletionResult fallback = complete(settings, systemPrompt, userPrompt, resolved);
            emitIfPresent(onDelta, fallback.content());
            return fallback;
        }

        URI endpoint = endpointFor(settings, protocol);
        Map<String, Object> payload = buildPayload(settings, systemPrompt, userPrompt, resolved, protocol, true, false);
        HttpResponse<InputStream> response = postStreamJson(endpoint, settings.apiKey(), headersFor(settings, protocol), payload);

        if (response.statusCode() >= 400) {
            String errorBody = readStreamError(response.body());
            try {
                CompletionResult fallback = complete(settings, systemPrompt, userPrompt, resolved);
                emitIfPresent(onDelta, fallback.content());
                return fallback;
            } catch (Exception fallbackException) {
                throw new IOException("Remote API error " + response.statusCode() + ": " + readErrorMessage(errorBody), fallbackException);
            }
        }

        return switch (protocol) {
            case RESPONSES -> readResponsesStream(response.body(), onDelta, cancellationCheck);
            case CHAT_COMPLETIONS -> readChatCompletionsStream(response.body(), onDelta, cancellationCheck);
            case ANTHROPIC_MESSAGES -> new CompletionResult("", 0, 0, 0);
        };
    }

    String sanitizeRemoteMessage(String message) {
        String sanitized = message == null ? "" : message;
        sanitized = sanitized.replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [hidden]");
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|key|token|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[hidden]");
        sanitized = sanitized.replaceAll("https?://\\S+", "[hidden-url]");
        if (sanitized.length() > 500) {
            return sanitized.substring(0, 500) + "...";
        }
        return sanitized;
    }

    public String safeErrorMessage(Exception exception, String fallback) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return fallback;
        }
        String sanitized = sanitizeRemoteMessage(exception.getMessage());
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private CompletionResult readCompletion(String responseBody, Protocol protocol) throws JsonProcessingException {
        return switch (protocol) {
            case RESPONSES -> readContentFromResponses(responseBody);
            case ANTHROPIC_MESSAGES -> readContentFromAnthropicMessages(responseBody);
            case CHAT_COMPLETIONS -> readContentFromChatCompletion(responseBody);
        };
    }

    private CompletionResult readContentFromChatCompletion(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(0);

        String content = readAssistantText(message, false);
        if (!content.isBlank()) {
            return new CompletionResult(content, promptTokens, completionTokens, totalTokens);
        }

        String legacyText = choice.path("text").asText("");
        if (!legacyText.isBlank()) {
            return new CompletionResult(legacyText, promptTokens, completionTokens, totalTokens);
        }

        String refusal = message.path("refusal").asText("");
        if (!refusal.isBlank()) {
            return new CompletionResult(refusal, promptTokens, completionTokens, totalTokens);
        }

        if (message.isMissingNode() || message.isNull()) {
            throw new JsonProcessingException("Remote response does not contain choices[0].message.") {
                private static final long serialVersionUID = 1L;
            };
        }

        return new CompletionResult("", promptTokens, completionTokens, totalTokens);
    }

    private CompletionResult readContentFromResponses(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("input_tokens").asInt(0);
        int completionTokens = usage.path("output_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);

        String outputText = root.path("output_text").asText("");
        if (!outputText.isBlank()) {
            return new CompletionResult(outputText, promptTokens, completionTokens, totalTokens);
        }

        String content = StreamSupport.stream(root.path("output").spliterator(), false)
            .flatMap(item -> StreamSupport.stream(item.path("content").spliterator(), false))
            .map(this::readResponseTextPart)
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining());
        return new CompletionResult(content, promptTokens, completionTokens, totalTokens);
    }

    private CompletionResult readContentFromAnthropicMessages(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("input_tokens").asInt(0);
        int completionTokens = usage.path("output_tokens").asInt(0);
        int totalTokens = promptTokens + completionTokens;

        String content = StreamSupport.stream(root.path("content").spliterator(), false)
            .map(part -> part.path("text").asText(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining());
        return new CompletionResult(content, promptTokens, completionTokens, totalTokens);
    }

    private String readConnectivityProbeFallback(String responseBody, Protocol protocol) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return switch (protocol) {
                case CHAT_COMPLETIONS -> {
                    JsonNode choice = root.path("choices").path(0);
                    int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
                    int totalTokens = root.path("usage").path("total_tokens").asInt(0);
                    String finishReason = choice.path("finish_reason").asText("");
                    if (completionTokens > 0 || totalTokens > 0 || "stop".equals(finishReason) || "length".equals(finishReason)) {
                        yield "OK";
                    }
                    yield "";
                }
                case RESPONSES -> {
                    int totalTokens = root.path("usage").path("total_tokens").asInt(0);
                    String status = root.path("status").asText("");
                    yield totalTokens > 0 || "completed".equals(status) ? "OK" : "";
                }
                case ANTHROPIC_MESSAGES -> {
                    int outputTokens = root.path("usage").path("output_tokens").asInt(0);
                    String stopReason = root.path("stop_reason").asText("");
                    yield outputTokens > 0 || !stopReason.isBlank() ? "OK" : "";
                }
            };
        } catch (Exception ignored) {
            return "";
        }
    }

    private CompletionResult readChatCompletionsStream(
        InputStream inputStream,
        Consumer<String> onDelta,
        Runnable cancellationCheck
    ) throws IOException {
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                checkCancelled(cancellationCheck);
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(data);
                String delta = readTextualOrParts(root.path("choices").path(0).path("delta").path("content"));
                if (!delta.isBlank()) {
                    content.append(delta);
                    onDelta.accept(delta);
                }
                JsonNode usage = root.path("usage");
                if (!usage.isMissingNode() && !usage.isNull()) {
                    promptTokens = usage.path("prompt_tokens").asInt(0);
                    completionTokens = usage.path("completion_tokens").asInt(0);
                    totalTokens = usage.path("total_tokens").asInt(0);
                }
            }
        }

        return new CompletionResult(content.toString(), promptTokens, completionTokens, totalTokens);
    }

    private CompletionResult readResponsesStream(
        InputStream inputStream,
        Consumer<String> onDelta,
        Runnable cancellationCheck
    ) throws IOException {
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                checkCancelled(cancellationCheck);
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(data);
                String delta = readResponsesDelta(root);
                if (!delta.isBlank()) {
                    content.append(delta);
                    onDelta.accept(delta);
                }
                JsonNode usage = root.path("response").path("usage");
                if (usage.isMissingNode() || usage.isNull()) {
                    usage = root.path("usage");
                }
                if (!usage.isMissingNode() && !usage.isNull()) {
                    promptTokens = usage.path("input_tokens").asInt(promptTokens);
                    completionTokens = usage.path("output_tokens").asInt(completionTokens);
                    totalTokens = usage.path("total_tokens").asInt(totalTokens);
                }
            }
        }

        return new CompletionResult(content.toString(), promptTokens, completionTokens, totalTokens);
    }

    private String readResponsesDelta(JsonNode root) {
        String type = root.path("type").asText("");
        if ("response.output_text.delta".equals(type)) {
            return root.path("delta").asText("");
        }
        String delta = root.path("delta").asText("");
        if (!delta.isBlank()) {
            return delta;
        }
        return readTextualOrParts(root.path("output_text"));
    }

    private String readAssistantText(JsonNode message, boolean allowReasoningFallback) {
        if (message == null || message.isMissingNode() || message.isNull()) {
            return "";
        }

        JsonNode contentNode = message.path("content");
        String content = readTextualOrParts(contentNode);
        if (!content.isBlank()) {
            return content;
        }

        if (allowReasoningFallback) {
            String reasoning = message.path("reasoning_content").asText("");
            if (!reasoning.isBlank()) {
                return reasoning;
            }
            return readTextualOrParts(message.path("output"));
        }

        return "";
    }

    private String readTextualOrParts(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                .map(part -> {
                    String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        return text;
                    }
                    return part.path("content").asText("");
                })
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining());
        }
        if (node.isObject()) {
            String text = node.path("text").asText("");
            if (!text.isBlank()) {
                return text;
            }
            return node.path("content").asText("");
        }
        return "";
    }

    private String readResponseTextPart(JsonNode part) {
        String text = part.path("text").asText("");
        if (!text.isBlank()) {
            return text;
        }
        String content = part.path("content").asText("");
        if (!content.isBlank()) {
            return content;
        }
        return part.path("output_text").asText("");
    }

    private Map<String, Object> buildPayload(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options,
        Protocol protocol,
        boolean stream,
        boolean minimal
    ) {
        return switch (protocol) {
            case RESPONSES -> buildResponsesPayload(settings, systemPrompt, userPrompt, options, stream, minimal);
            case ANTHROPIC_MESSAGES -> buildAnthropicMessagesPayload(settings, systemPrompt, userPrompt, options);
            case CHAT_COMPLETIONS -> buildChatPayload(settings, systemPrompt, userPrompt, options, stream, minimal);
        };
    }

    private Map<String, Object> buildChatPayload(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options,
        boolean stream,
        boolean minimal
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", settings.model());
        payload.put("stream", stream);
        payload.put("messages", buildMessages(systemPrompt, userPrompt));
        if (!minimal) {
            payload.put("temperature", options.temperature());
            payload.put("max_tokens", options.effectiveMaxTokens());
            if (options.jsonObjectMode()) {
                payload.put("response_format", Map.of("type", "json_object"));
            }
        }
        return payload;
    }

    private Map<String, Object> buildResponsesPayload(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options,
        boolean stream,
        boolean minimal
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", settings.model());
        payload.put("input", userPrompt == null ? "" : userPrompt);
        if (!text(systemPrompt).isBlank()) {
            payload.put("instructions", systemPrompt);
        }
        if (stream) {
            payload.put("stream", true);
        }
        if (!minimal) {
            payload.put("temperature", options.temperature());
            payload.put("max_output_tokens", options.effectiveMaxTokens());
            if (options.jsonObjectMode()) {
                payload.put("text", Map.of("format", Map.of("type", "json_object")));
            }
        }
        return payload;
    }

    private Map<String, Object> buildAnthropicMessagesPayload(
        AiSettings settings,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", settings.model());
        payload.put("max_tokens", Math.max(1, options.effectiveMaxTokens()));
        payload.put("temperature", options.temperature());
        if (!text(systemPrompt).isBlank()) {
            payload.put("system", systemPrompt);
        }
        payload.put("messages", List.of(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
        return payload;
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt));
        return messages;
    }

    private HttpResponse<String> postJson(
        URI endpoint,
        String apiKey,
        Map<String, String> headers,
        Map<String, Object> payload,
        int timeoutSeconds
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json");
        headers.forEach(builder::header);
        HttpRequest request = builder
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<InputStream> postStreamJson(
        URI endpoint,
        String apiKey,
        Map<String, String> headers,
        Map<String, Object> payload
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(240))
            .header("Content-Type", "application/json");
        headers.forEach(builder::header);
        HttpRequest request = builder
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private Map<String, String> headersFor(AiSettings settings, Protocol protocol) {
        if (protocol == Protocol.ANTHROPIC_MESSAGES) {
            return Map.of(
                "x-api-key", settings.apiKey(),
                "anthropic-version", ANTHROPIC_VERSION
            );
        }
        return Map.of("Authorization", "Bearer " + settings.apiKey());
    }

    private URI endpointFor(AiSettings settings, Protocol protocol) {
        String normalized = trimTrailingSlash(settings.baseUrl());
        String lower = normalized.toLowerCase(Locale.ROOT);
        return switch (protocol) {
            case RESPONSES -> {
                if (lower.endsWith("/responses")) {
                    yield URI.create(normalized);
                }
                if (lower.endsWith("/v1")) {
                    yield URI.create(normalized + "/responses");
                }
                yield URI.create(normalized + "/v1/responses");
            }
            case ANTHROPIC_MESSAGES -> {
                if (lower.endsWith("/messages")) {
                    yield URI.create(normalized);
                }
                if (lower.endsWith("/v1")) {
                    yield URI.create(normalized + "/messages");
                }
                yield URI.create(normalized + "/v1/messages");
            }
            case CHAT_COMPLETIONS -> {
                if (lower.endsWith("/chat/completions")) {
                    yield URI.create(normalized);
                }
                if (lower.endsWith("/v1")) {
                    yield URI.create(normalized + "/chat/completions");
                }
                yield URI.create(normalized + "/v1/chat/completions");
            }
        };
    }

    private Protocol resolveProtocol(AiSettings settings) {
        String provider = text(settings == null ? "" : settings.provider()).toLowerCase(Locale.ROOT);
        String baseUrl = text(settings == null ? "" : settings.baseUrl()).toLowerCase(Locale.ROOT);
        if (baseUrl.endsWith("/messages") || "anthropic".equals(provider)) {
            return Protocol.ANTHROPIC_MESSAGES;
        }
        if (baseUrl.endsWith("/responses") || "openai-responses".equals(provider)) {
            return Protocol.RESPONSES;
        }
        if ("openai".equals(provider) && !baseUrl.endsWith("/chat/completions")) {
            return Protocol.RESPONSES;
        }
        return Protocol.CHAT_COMPLETIONS;
    }

    private AiCompletionOptions resolveOptions(AiSettings settings, AiCompletionOptions options) {
        if (options != null) {
            return options;
        }
        return new AiCompletionOptions(settings.temperature(), settings.maxTokens(), 120, false);
    }

    private AiCompletionOptions withoutJsonMode(AiCompletionOptions options) {
        return new AiCompletionOptions(
            options.temperature(),
            options.maxTokens(),
            options.timeoutSeconds(),
            false,
            options.connectivityProbe()
        );
    }

    private String readErrorMessage(String responseBody) {
        String message;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String error = root.path("error").path("message").asText("");
            if (!error.isBlank()) {
                message = error;
            } else {
                message = responseBody;
            }
        } catch (Exception ignored) {
            message = responseBody;
        }
        return sanitizeRemoteMessage(message);
    }

    private String readStreamError(InputStream inputStream) throws IOException {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void emitIfPresent(Consumer<String> onDelta, String content) {
        if (content != null && !content.isBlank()) {
            onDelta.accept(content);
        }
    }

    private void checkCancelled(Runnable cancellationCheck) {
        if (cancellationCheck != null) {
            cancellationCheck.run();
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = text(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private enum Protocol {
        CHAT_COMPLETIONS,
        RESPONSES,
        ANTHROPIC_MESSAGES
    }

    public record CompletionResult(String content, int promptTokens, int completionTokens, int totalTokens) {
    }
}
