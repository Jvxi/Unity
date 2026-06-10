package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConfigurationProperties(prefix = "ai")
public class DeepSeekService {
    private static final String FORMAT_OPENAI_CHAT = "openai-chat";
    private static final String FORMAT_OPENAI_RESPONSES = "openai-responses";
    private static final String FORMAT_ANTHROPIC_MESSAGES = "anthropic-messages";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private List<AiProvider> providers;
    private final WebClient webClient = WebClient.builder().build();

    public List<AiProvider> getProviders() { return providers; }
    public void setProviders(List<AiProvider> providers) { this.providers = providers; }

    public List<Map<String, Object>> getProvidersSummary() {
        return effectiveProviders().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("apiUrl", p.getApiUrl() != null ? p.getApiUrl() : "");
            m.put("apiFormat", p.getApiFormat() != null ? p.getApiFormat() : FORMAT_OPENAI_CHAT);
            m.put("models", p.getModels() != null ? p.getModels().stream().map(model -> {
                Map<String, String> mm = new LinkedHashMap<>();
                mm.put("id", model.getId());
                mm.put("name", model.getName());
                mm.put("description", model.getDescription());
                return mm;
            }).collect(Collectors.toList()) : List.of());
            return m;
        }).collect(Collectors.toList());
    }

    public String analyzeWithAI(PeInfo peInfo, List<VtableInfo> vtables,
                                String apiKey, String providerId, String modelId) {
        return analyzeWithAI(peInfo, vtables, apiKey, providerId, modelId, null);
    }

    public String analyzeWithAI(PeInfo peInfo, List<VtableInfo> vtables,
                                String apiKey, String providerId, String modelId, String customApiUrl) {
        if (apiKey == null || apiKey.isBlank()) return null;

        AiProvider provider = findProvider(providerId);
        if (provider == null) return "未知的 AI 提供商: " + providerId;

        String apiUrl = resolveApiUrl(provider, customApiUrl);
        if (apiUrl == null || apiUrl.isBlank()) {
            return "AI 提供商未配置接口地址: " + provider.getName();
        }

        String selectedModel = (modelId == null || modelId.isBlank()) ? firstModelId(provider) : modelId;
        if (selectedModel == null || selectedModel.isBlank()) {
            return "AI 提供商未配置可用模型: " + provider.getName();
        }

        try {
            return callApi(provider, buildPrompt(peInfo, vtables), apiKey, apiUrl, selectedModel);
        } catch (Exception e) {
            return "AI 分析失败: " + e.getMessage();
        }
    }

    public String testConnection(String apiKey, String providerId, String modelId, String customApiUrl) {
        if (apiKey == null || apiKey.isBlank()) return "API Key 不能为空";

        AiProvider provider = findProvider(providerId);
        if (provider == null) return "未知的 AI 提供商: " + providerId;

        String apiUrl = resolveApiUrl(provider, customApiUrl);
        if (apiUrl == null || apiUrl.isBlank()) {
            return "AI 提供商未配置接口地址: " + provider.getName();
        }

        String selectedModel = (modelId == null || modelId.isBlank()) ? firstModelId(provider) : modelId;
        if (selectedModel == null || selectedModel.isBlank()) {
            return "AI 提供商未配置可用模型: " + provider.getName();
        }

        try {
            return callApi(provider, "请只回复 OK，用于测试接口连接。", apiKey, apiUrl, selectedModel);
        } catch (Exception e) {
            return "AI 连接测试失败: " + e.getMessage();
        }
    }

    private AiProvider findProvider(String providerId) {
        List<AiProvider> allProviders = effectiveProviders();
        if (providerId == null || providerId.isBlank()) {
            return allProviders.isEmpty() ? null : allProviders.get(0);
        }
        return allProviders.stream()
            .filter(p -> p.getId().equals(providerId))
            .findFirst()
            .orElse(null);
    }

    private List<AiProvider> effectiveProviders() {
        Map<String, AiProvider> merged = new LinkedHashMap<>();
        for (AiProvider provider : defaultProviders()) {
            merged.put(provider.getId(), provider);
        }
        if (providers != null) {
            for (AiProvider provider : providers) {
                if (provider.getId() != null && !provider.getId().isBlank()) {
                    merged.put(provider.getId(), provider);
                }
            }
        }
        return List.copyOf(merged.values());
    }

    private List<AiProvider> defaultProviders() {
        return List.of(
            provider("deepseek", "DeepSeek", FORMAT_OPENAI_CHAT, "https://api.deepseek.com/chat/completions",
                model("deepseek-v4-flash", "DeepSeek-V4 Flash", "官方推荐快速模型"),
                model("deepseek-v4-pro", "DeepSeek-V4 Pro", "官方推荐高能力模型")
            ),
            provider("openai", "OpenAI", FORMAT_OPENAI_RESPONSES, "https://api.openai.com/v1/responses",
                model("gpt-5.5", "GPT-5.5", "OpenAI 旗舰通用模型"),
                model("gpt-5.4", "GPT-5.4", "高能力通用模型"),
                model("gpt-5.4-mini", "GPT-5.4 mini", "轻量高性价比模型"),
                model("gpt-5.4-nano", "GPT-5.4 nano", "低延迟小模型")
            ),
            provider("anthropic", "Anthropic Claude", FORMAT_ANTHROPIC_MESSAGES, "https://api.anthropic.com/v1/messages",
                model("claude-fable-5", "Claude Fable 5", "官方最新推理模型"),
                model("claude-opus-4-8", "Claude Opus 4.8", "复杂任务高能力模型"),
                model("claude-sonnet-4-6", "Claude Sonnet 4.6", "均衡通用模型"),
                model("claude-haiku-4-5", "Claude Haiku 4.5", "快速低成本模型")
            ),
            provider("gemini", "Google Gemini", FORMAT_OPENAI_CHAT, "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                model("gemini-3.5-flash", "Gemini 3.5 Flash", "Gemini 快速模型"),
                model("gemini-3.1-pro", "Gemini 3.1 Pro", "Gemini Pro 模型"),
                model("gemini-3-flash", "Gemini 3 Flash", "Gemini 低延迟模型"),
                model("gemini-2.5-pro", "Gemini 2.5 Pro", "Gemini 2.5 高能力模型"),
                model("gemini-2.5-flash", "Gemini 2.5 Flash", "Gemini 2.5 快速模型"),
                model("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", "Gemini 2.5 轻量模型")
            ),
            provider("qwen", "阿里云百炼 / Qwen", FORMAT_OPENAI_CHAT, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                model("qwen3.7-max", "Qwen3.7 Max", "百炼最新旗舰模型"),
                model("qwen3.7-plus", "Qwen3.7 Plus", "高能力通用模型"),
                model("qwen3.6-plus", "Qwen3.6 Plus", "稳定通用模型"),
                model("qwen3.6-flash", "Qwen3.6 Flash", "快速低成本模型"),
                model("qwen3.5-plus", "Qwen3.5 Plus", "通用增强模型"),
                model("qwen3.5-flash", "Qwen3.5 Flash", "快速响应模型"),
                model("qwen-plus", "Qwen Plus", "百炼通用模型"),
                model("qwen-turbo", "Qwen Turbo", "百炼高吞吐模型"),
                model("qwen-long", "Qwen Long", "长上下文模型"),
                model("qwen3-coder-plus", "Qwen3 Coder Plus", "代码分析模型"),
                model("qwen3-coder-flash", "Qwen3 Coder Flash", "快速代码模型"),
                model("qwq-plus", "QwQ Plus", "推理模型"),
                model("mimo-v2.5-pro", "MiMo-V2.5 Pro", "百炼可调用的小米模型"),
                model("kimi-k2.6", "Kimi K2.6", "百炼可调用的 Kimi 模型"),
                model("glm-5.1", "GLM-5.1", "百炼可调用的智谱模型")
            ),
            provider("xiaomi", "小米 MiMo", FORMAT_OPENAI_CHAT, "https://api.xiaomimimo.com/v1/chat/completions",
                model("MiMo-V2.5", "MiMo-V2.5", "小米主模型，通用对话"),
                model("mimo-v2.5-pro", "MiMo-V2.5 Pro", "专业版，能力更强"),
                model("MiMo-V2-Flash", "MiMo-V2 Flash", "轻量快速版"),
                model("MiMo-V2-Pro", "MiMo-V2 Pro (legacy)", "历史兼容模型"),
                model("MiMo-V2-Omni", "MiMo-V2 Omni (legacy)", "历史兼容多模态模型")
            ),
            provider("zhipu", "智谱 GLM", FORMAT_OPENAI_CHAT, "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                model("glm-5.1", "GLM-5.1", "智谱最新深度思考模型"),
                model("glm-5", "GLM-5", "智谱高能力通用模型"),
                model("glm-5-turbo", "GLM-5 Turbo", "快速模型"),
                model("glm-4.7", "GLM-4.7", "通用增强模型"),
                model("glm-4.7-flash", "GLM-4.7 Flash", "免费/快速模型"),
                model("glm-4.7-flashx", "GLM-4.7 FlashX", "高速模型"),
                model("glm-4.6", "GLM-4.6", "长上下文通用模型"),
                model("glm-4.5-air", "GLM-4.5 Air", "轻量通用模型"),
                model("glm-4.5-airx", "GLM-4.5 AirX", "增强轻量模型"),
                model("glm-4-long", "GLM-4 Long", "长上下文模型")
            ),
            provider("moonshot", "Moonshot Kimi", FORMAT_OPENAI_CHAT, "https://api.moonshot.cn/v1/chat/completions",
                model("kimi-k2.6", "Kimi K2.6", "Moonshot 最新专家混合模型"),
                model("kimi-k2.5", "Kimi K2.5", "通用能力增强模型"),
                model("kimi-k2", "Kimi K2", "通用模型"),
                model("kimi-k2-thinking", "Kimi K2 Thinking", "深度思考模型"),
                model("moonshot-v1", "Moonshot V1", "经典稳定模型")
            ),
            provider("xai", "xAI Grok", FORMAT_OPENAI_CHAT, "https://api.x.ai/v1/chat/completions",
                model("grok-4.3", "Grok 4.3", "xAI 最新通用模型"),
                model("grok-4.3-latest", "Grok 4.3 Latest", "最新别名模型"),
                model("grok-4", "Grok 4", "上一代旗舰模型"),
                model("grok-4-fast", "Grok 4 Fast", "快速模型"),
                model("grok-code-fast", "Grok Code Fast", "代码快速模型")
            ),
            provider("custom", "自定义厂商", FORMAT_OPENAI_CHAT, "",
                model("custom-model", "自定义模型", "OpenAI-compatible relay model")
            )
        );
    }

    private AiProvider provider(String id, String name, String apiFormat, String apiUrl, AiModel... models) {
        AiProvider provider = new AiProvider();
        provider.setId(id);
        provider.setName(name);
        provider.setApiFormat(apiFormat);
        provider.setApiUrl(apiUrl);
        provider.setModels(List.of(models));
        return provider;
    }

    private AiModel model(String id, String name, String description) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setName(name);
        model.setDescription(description);
        return model;
    }

    private String buildPrompt(PeInfo peInfo, List<VtableInfo> vtables) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个 Windows PE 逆向分析专家。请分析以下 DLL 文件的虚表（vtable）信息。\n\n");
        sb.append("## PE 基本信息\n");
        sb.append("- 文件名: ").append(peInfo.getFileName()).append("\n");
        sb.append("- 架构: ").append(peInfo.getMachine()).append(" (").append(peInfo.getMagic()).append(")\n");
        sb.append("- ImageBase: 0x").append(Long.toHexString(peInfo.getImageBase())).append("\n");
        sb.append("- 子系统: ").append(peInfo.getSubsystem()).append("\n");
        sb.append("- 段数量: ").append(peInfo.getNumberOfSections()).append("\n\n");

        if (peInfo.getSections() != null) {
            sb.append("## 段表\n");
            for (SectionInfo sec : peInfo.getSections()) {
                sb.append("- ").append(sec.getName()).append(" VA=").append(sec.getRva())
                    .append(" Size=0x").append(Long.toHexString(sec.getVirtualSize()))
                    .append(" [").append(sec.getCharacteristics()).append("]\n");
            }
            sb.append("\n");
        }

        if (peInfo.getExports() != null && !peInfo.getExports().isEmpty()) {
            sb.append("## 导出函数 (").append(peInfo.getExportCount()).append(" 个)\n");
            int limit = Math.min(peInfo.getExports().size(), 50);
            for (int i = 0; i < limit; i++) {
                ExportInfo exp = peInfo.getExports().get(i);
                sb.append("- ").append(exp.getName() != null ? exp.getName() : "ordinal_" + exp.getOrdinal())
                    .append(" RVA=").append(exp.getRva()).append("\n");
            }
            sb.append("\n");
        }

        if (peInfo.getImports() != null && !peInfo.getImports().isEmpty()) {
            sb.append("## 导入 (").append(peInfo.getImportCount()).append(" 个函数)\n");
            for (ImportInfo imp : peInfo.getImports()) {
                sb.append("- ").append(imp.getDllName());
                if (imp.getFunctions() != null) {
                    sb.append(": ");
                    imp.getFunctions().forEach(f -> sb.append(f.getName()).append(", "));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 检测到的虚表候选 (").append(vtables.size()).append(" 个)\n");
        for (int i = 0; i < vtables.size(); i++) {
            VtableInfo vt = vtables.get(i);
            sb.append("### 虚表 ").append(i + 1).append("\n");
            sb.append("- RVA: ").append(vt.getRva()).append(" VA: ").append(vt.getVa())
                .append(" 函数数量: ").append(vt.getFunctionCount())
                .append(" 检测方法: ").append(vt.getDetectionMethod()).append("\n");
            if (vt.getRttiTypeName() != null) sb.append("- RTTI 类型名: ").append(vt.getRttiTypeName()).append("\n");
            if (vt.getRelatedSymbol() != null) sb.append("- 关联符号: ").append(vt.getRelatedSymbol()).append("\n");
            sb.append("- 函数指针:\n");
            for (VFunctionInfo f : vt.getFunctions()) {
                sb.append("  [").append(f.getIndex()).append("] RVA=").append(f.getRva())
                    .append(" VA=").append(f.getVa()).append("\n");
            }
        }

        sb.append("\n## 任务\n");
        sb.append("1. 确认哪些是真实的虚函数表，哪些可能是误报\n");
        sb.append("2. 推测每个虚表可能的类名（如果 RTTI 没有提供）\n");
        sb.append("3. 简要说明该 DLL 的整体结构特征\n");
        sb.append("请用中文回答，简洁明了。\n");
        return sb.toString();
    }

    private String callApi(AiProvider provider, String prompt, String apiKey, String apiUrl, String model) {
        String format = provider.getApiFormat() == null || provider.getApiFormat().isBlank()
            ? FORMAT_OPENAI_CHAT
            : provider.getApiFormat();

        return switch (format) {
            case FORMAT_OPENAI_RESPONSES -> callOpenAiResponses(provider, prompt, apiKey, apiUrl, model);
            case FORMAT_ANTHROPIC_MESSAGES -> callAnthropicMessages(provider, prompt, apiKey, apiUrl, model);
            case FORMAT_OPENAI_CHAT -> callOpenAiChat(provider, prompt, apiKey, apiUrl, model);
            default -> "不支持的 AI 接口格式: " + format;
        };
    }

    private String callOpenAiChat(AiProvider provider, String prompt, String apiKey, String apiUrl, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", maxTokens(provider));
        body.put("temperature", 0.3);

        Map<String, Object> result = postJson(apiUrl, Map.of("Authorization", "Bearer " + apiKey), body);
        String content = extractOpenAiChatContent(result);
        return content != null && !content.isBlank() ? content : "AI 未返回有效结果";
    }

    private String callOpenAiResponses(AiProvider provider, String prompt, String apiKey, String apiUrl, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", prompt);
        body.put("max_output_tokens", maxTokens(provider));

        Map<String, Object> result = postJson(apiUrl, Map.of("Authorization", "Bearer " + apiKey), body);
        String content = extractOpenAiResponseContent(result);
        return content != null && !content.isBlank() ? content : "AI 未返回有效结果";
    }

    private String callAnthropicMessages(AiProvider provider, String prompt, String apiKey, String apiUrl, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens(provider));
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", "2023-06-01");

        Map<String, Object> result = postJson(apiUrl, headers, body);
        String content = contentToText(result != null ? result.get("content") : null);
        return content != null && !content.isBlank() ? content : "AI 未返回有效结果";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String apiUrl, Map<String, String> headers, Map<String, Object> body) {
        WebClient.RequestBodySpec request = webClient.post()
            .uri(apiUrl)
            .header("Content-Type", "application/json");
        headers.forEach(request::header);

        return request
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    @SuppressWarnings("unchecked")
    private String extractOpenAiChatContent(Map<String, Object> result) {
        if (result == null || !result.containsKey("choices")) return null;
        Object choicesValue = result.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) return null;
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> firstChoiceMap)) return null;
        Object message = firstChoiceMap.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) return null;
        return contentToText(messageMap.get("content"));
    }

    private String extractOpenAiResponseContent(Map<String, Object> result) {
        if (result == null) return null;
        Object outputText = result.get("output_text");
        if (outputText instanceof String text && !text.isBlank()) return text;
        return contentToText(result.get("output"));
    }

    private String contentToText(Object content) {
        if (content == null) return null;
        if (content instanceof String text) return text;
        if (content instanceof List<?> items) {
            StringBuilder sb = new StringBuilder();
            for (Object item : items) {
                if (item instanceof String text) {
                    appendText(sb, text);
                } else if (item instanceof Map<?, ?> map) {
                    appendText(sb, contentToText(map.get("text")));
                    appendText(sb, contentToText(map.get("content")));
                }
            }
            return sb.toString();
        }
        return null;
    }

    private void appendText(StringBuilder sb, String text) {
        if (text == null || text.isBlank()) return;
        if (!sb.isEmpty()) sb.append("\n");
        sb.append(text);
    }

    private String firstModelId(AiProvider provider) {
        return provider.getModels() != null && !provider.getModels().isEmpty()
            ? provider.getModels().get(0).getId()
            : "";
    }

    private int maxTokens(AiProvider provider) {
        return provider.getMaxTokens() != null && provider.getMaxTokens() > 0
            ? provider.getMaxTokens()
            : DEFAULT_MAX_TOKENS;
    }

    private String resolveApiUrl(AiProvider provider, String customApiUrl) {
        String candidate = customApiUrl != null && !customApiUrl.isBlank()
            ? customApiUrl
            : provider.getApiUrl();
        return normalizeApiUrl(candidate);
    }

    private String normalizeApiUrl(String value) {
        if (value == null || value.isBlank()) return "";
        String next = value.trim();
        if (!next.startsWith("http://") && !next.startsWith("https://")) {
            next = "https://" + next;
        }
        URI uri = URI.create(next);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("AI 接口链接仅支持 http 或 https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("AI 接口链接格式不正确");
        }
        return next.replaceAll("/+$", "");
    }
}
