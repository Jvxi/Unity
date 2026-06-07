package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.*;

@Service
public class DeepSeekService {

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.models[0].id:deepseek-chat}")
    private String defaultModel;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 获取可用模型列表
     */
    public List<Map<String, String>> getModels() {
        List<Map<String, String>> models = new ArrayList<>();
        Map<String, String> m1 = new HashMap<>();
        m1.put("id", "deepseek-chat");
        m1.put("name", "DeepSeek-V3");
        m1.put("description", "通用对话，速度快");
        models.add(m1);

        Map<String, String> m2 = new HashMap<>();
        m2.put("id", "deepseek-reasoner");
        m2.put("name", "DeepSeek-R1");
        m2.put("description", "深度推理，更准确");
        models.add(m2);
        return models;
    }

    /**
     * 调用 DeepSeek API 分析虚表
     */
    public String analyzeWithAI(PeInfo peInfo, List<VtableInfo> vtables, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String prompt = buildPrompt(peInfo, vtables);
        try {
            String response = callApi(prompt, apiKey, model);
            return response;
        } catch (Exception e) {
            return "AI 分析失败: " + e.getMessage();
        }
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

        if (peInfo.getSections() != null && !peInfo.getSections().isEmpty()) {
            sb.append("## 段表\n");
            for (SectionInfo sec : peInfo.getSections()) {
                sb.append("- ").append(sec.getName())
                  .append(" VA=").append(sec.getRva())
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
            if (peInfo.getExports().size() > 50) sb.append("... 等共 ").append(peInfo.getExports().size()).append(" 个\n");
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
            sb.append("- RVA: ").append(vt.getRva())
              .append(" VA: ").append(vt.getVa())
              .append(" 函数数量: ").append(vt.getFunctionCount())
              .append(" 检测方法: ").append(vt.getDetectionMethod()).append("\n");
            if (vt.getRttiTypeName() != null) {
                sb.append("- RTTI 类型名: ").append(vt.getRttiTypeName()).append("\n");
            }
            if (vt.getRelatedSymbol() != null) {
                sb.append("- 关联符号: ").append(vt.getRelatedSymbol()).append("\n");
            }
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

    private String callApi(String prompt, String apiKey, String model) {
        if (model == null || model.isBlank()) model = defaultModel;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        body.put("max_tokens", 4096);
        body.put("temperature", 0.3);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = webClient.post()
            .uri(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (result != null && result.containsKey("choices")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (!choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return "AI 未返回有效结果";
    }
}
