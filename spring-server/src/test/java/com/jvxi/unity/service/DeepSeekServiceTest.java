package com.jvxi.unity.service;

import com.jvxi.unity.model.AiModel;
import com.jvxi.unity.model.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekServiceTest {

    @Test
    void returnsDefaultProvidersWhenConfigIsMissing() {
        DeepSeekService service = new DeepSeekService();

        List<Map<String, Object>> providers = service.getProvidersSummary();

        assertFalse(providers.isEmpty());
        assertEquals("deepseek", providers.get(0).get("id"));
        assertEquals("DeepSeek", providers.get(0).get("name"));
        assertFalse(((List<?>) providers.get(0).get("models")).isEmpty());
        assertProviderHasModel(providers, "openai", "gpt-5.5");
        assertProviderHasModel(providers, "anthropic", "claude-sonnet-4-6");
        assertProviderHasModel(providers, "gemini", "gemini-2.5-pro");
        assertProviderHasModel(providers, "qwen", "qwen3.7-max");
        assertProviderHasModel(providers, "xiaomi", "MiMo-V2.5");
        assertProviderHasModel(providers, "xiaomi", "mimo-v2.5-pro");
        assertProviderHasModel(providers, "zhipu", "glm-5.1");
        assertProviderHasModel(providers, "moonshot", "kimi-k2.6");
        assertProviderHasModel(providers, "xai", "grok-4.3");
        assertProviderHasModel(providers, "custom", "custom-model");
        assertEquals("https://api.deepseek.com/chat/completions", providers.get(0).get("apiUrl"));
        assertEquals("openai-chat", providers.get(0).get("apiFormat"));
    }

    @Test
    void mergesConfiguredProvidersWithDefaults() {
        DeepSeekService service = new DeepSeekService();
        AiProvider custom = new AiProvider();
        custom.setId("custom");
        custom.setName("Custom Provider");
        custom.setApiUrl("https://example.com/chat/completions");
        AiModel customModel = new AiModel();
        customModel.setId("custom-model");
        customModel.setName("Custom Model");
        customModel.setDescription("local config model");
        custom.setModels(List.of(customModel));
        service.setProviders(List.of(custom));

        List<Map<String, Object>> providers = service.getProvidersSummary();

        assertProviderHasModel(providers, "deepseek", "deepseek-v4-flash");
        assertProviderHasModel(providers, "custom", "custom-model");
    }

    @SuppressWarnings("unchecked")
    private void assertProviderHasModel(List<Map<String, Object>> providers, String providerId, String modelId) {
        Map<String, Object> provider = providers.stream()
            .filter(item -> providerId.equals(item.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing provider: " + providerId));
        List<Map<String, String>> models = (List<Map<String, String>>) provider.get("models");
        assertTrue(models.stream().anyMatch(item -> modelId.equals(item.get("id"))),
            "Missing model " + modelId + " in provider " + providerId);
    }
}
