package com.jvxi.unity.novel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class AiGatewayServiceTest {

    @Test
    void openAiResponsesEndpointIsNotRewrittenToChatCompletions() throws Exception {
        try (RecordingServer server = new RecordingServer("""
            {"output_text":"OK","usage":{"input_tokens":2,"output_tokens":3,"total_tokens":5}}
            """)) {
            AiGatewayService gateway = gateway();
            AiSettings settings = settings("openai", server.baseUrl() + "/v1/responses");

            AiGatewayService.CompletionResult result = gateway.complete(
                settings,
                "system",
                "user",
                new AiCompletionOptions(0.1, 128, 10, false)
            );

            assertEquals("/v1/responses", server.path());
            assertEquals("OK", result.content());
            assertEquals(2, result.promptTokens());
            assertEquals(3, result.completionTokens());
            assertEquals(5, result.totalTokens());
            assertTrue(server.body().contains("\"input\":\"user\""));
            assertTrue(server.body().contains("\"instructions\":\"system\""));
        }
    }

    @Test
    void anthropicMessagesUsesApiKeyHeaderAndVersionHeader() throws Exception {
        try (RecordingServer server = new RecordingServer("""
            {"content":[{"type":"text","text":"pong"}],"usage":{"input_tokens":4,"output_tokens":5},"stop_reason":"end_turn"}
            """)) {
            AiGatewayService gateway = gateway();
            AiSettings settings = settings("anthropic", server.baseUrl() + "/v1/messages");

            AiGatewayService.CompletionResult result = gateway.complete(
                settings,
                "system",
                "user",
                new AiCompletionOptions(0.1, 128, 10, false)
            );

            assertEquals("/v1/messages", server.path());
            assertEquals("sk-test-secret", server.header("x-api-key"));
            assertEquals("2023-06-01", server.header("anthropic-version"));
            assertNull(server.header("Authorization"));
            assertEquals("pong", result.content());
            assertEquals(4, result.promptTokens());
            assertEquals(5, result.completionTokens());
            assertEquals(9, result.totalTokens());
        }
    }

    @Test
    void chatCompletionsCompatibleEndpointStillUsesChatPathAndParsesMessage() throws Exception {
        try (RecordingServer server = new RecordingServer("""
            {"choices":[{"message":{"content":"hello"}}],"usage":{"prompt_tokens":7,"completion_tokens":8,"total_tokens":15}}
            """)) {
            AiGatewayService gateway = gateway();
            AiSettings settings = settings("custom", server.baseUrl() + "/v1");

            AiGatewayService.CompletionResult result = gateway.complete(
                settings,
                "system",
                "user",
                new AiCompletionOptions(0.1, 128, 10, false)
            );

            assertEquals("/v1/chat/completions", server.path());
            assertEquals("Bearer sk-test-secret", server.header("Authorization"));
            assertEquals("hello", result.content());
            assertEquals(7, result.promptTokens());
            assertEquals(8, result.completionTokens());
            assertEquals(15, result.totalTokens());
        }
    }

    @Test
    void chatCompletionsStreamEmitsDeltasAndUsage() throws Exception {
        try (RecordingServer server = new RecordingServer("""
            data: {"choices":[{"delta":{"content":"你"}}]}

            data: {"choices":[{"delta":{"content":"好"}}],"usage":{"prompt_tokens":1,"completion_tokens":2,"total_tokens":3}}

            data: [DONE]

            """)) {
            AiGatewayService gateway = gateway();
            AiSettings settings = settings("custom", server.baseUrl() + "/v1");
            StringBuilder emitted = new StringBuilder();

            AiGatewayService.CompletionResult result = gateway.stream(
                settings,
                "system",
                "user",
                new AiCompletionOptions(0.1, 128, 10, false),
                emitted::append,
                () -> {}
            );

            assertEquals("/v1/chat/completions", server.path());
            assertTrue(server.body().contains("\"stream\":true"));
            assertEquals("你好", emitted.toString());
            assertEquals("你好", result.content());
            assertEquals(1, result.promptTokens());
            assertEquals(2, result.completionTokens());
            assertEquals(3, result.totalTokens());
        }
    }

    @Test
    void responsesStreamEmitsOutputTextDeltasAndUsage() throws Exception {
        try (RecordingServer server = new RecordingServer("""
            data: {"type":"response.output_text.delta","delta":"甲"}

            data: {"type":"response.output_text.delta","delta":"乙","response":{"usage":{"input_tokens":4,"output_tokens":5,"total_tokens":9}}}

            data: [DONE]

            """)) {
            AiGatewayService gateway = gateway();
            AiSettings settings = settings("openai-responses", server.baseUrl() + "/v1/responses");
            StringBuilder emitted = new StringBuilder();

            AiGatewayService.CompletionResult result = gateway.stream(
                settings,
                "system",
                "user",
                new AiCompletionOptions(0.1, 128, 10, false),
                emitted::append,
                () -> {}
            );

            assertEquals("/v1/responses", server.path());
            assertTrue(server.body().contains("\"stream\":true"));
            assertEquals("甲乙", emitted.toString());
            assertEquals("甲乙", result.content());
            assertEquals(4, result.promptTokens());
            assertEquals(5, result.completionTokens());
            assertEquals(9, result.totalTokens());
        }
    }

    @Test
    void safeErrorMessageMasksSecretsTokensAndUrls() {
        AiGatewayService gateway = gateway();

        String message = gateway.safeErrorMessage(
            new IOException("Bearer sk-test-secret api_key=sk-test-secret authorization: token123 https://relay.example.com/v1"),
            "fallback"
        );

        assertFalse(message.contains("sk-test-secret"));
        assertFalse(message.contains("token123"));
        assertFalse(message.contains("relay.example.com"));
        assertTrue(message.contains("Bearer [hidden]"));
        assertTrue(message.contains("[hidden-url]"));
    }

    private AiGatewayService gateway() {
        return new AiGatewayService(new ObjectMapper(), HttpClient.newHttpClient());
    }

    private AiSettings settings(String provider, String baseUrl) {
        return new AiSettings(
            true,
            provider,
            baseUrl,
            "sk-test-secret",
            "test-model",
            0.1,
            128,
            8192,
            ""
        );
    }

    private static final class RecordingServer implements AutoCloseable {
        private final HttpServer server;
        private final String responseBody;
        private final AtomicReference<HttpExchange> exchangeRef = new AtomicReference<>();
        private final AtomicReference<String> bodyRef = new AtomicReference<>("");

        RecordingServer(String responseBody) throws IOException {
            this.responseBody = responseBody;
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        String path() {
            return exchangeRef.get().getRequestURI().getPath();
        }

        String body() {
            return bodyRef.get();
        }

        String header(String name) {
            return exchangeRef.get().getRequestHeaders().getFirst(name);
        }

        private void handle(HttpExchange exchange) throws IOException {
            exchangeRef.set(exchange);
            bodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
