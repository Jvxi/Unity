package com.jvxi.unity.controller;

import com.jvxi.unity.model.*;
import com.jvxi.unity.service.DeepSeekService;
import com.jvxi.unity.service.VtableDetectorService;
import com.jvxi.unity.service.pe.PeParserAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final PeParserAggregator peParser;
    private final VtableDetectorService vtableDetector;
    private final DeepSeekService deepSeekService;

    public AnalysisController(PeParserAggregator peParser,
                              VtableDetectorService vtableDetector,
                              DeepSeekService deepSeekService) {
        this.peParser = peParser;
        this.vtableDetector = vtableDetector;
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "unity-vtable-analyzer");
    }

    @GetMapping("/models")
    public Map<String, Object> getModels() {
        return Map.of("models", deepSeekService.getModels());
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "model", required = false) String model) {

        try {
            byte[] data = file.getBytes();
            String fileName = file.getOriginalFilename();

            // 1. PE 解析
            PeInfo peInfo = peParser.parse(data, fileName);

            // 2. 虚表检测
            List<VtableInfo> vtables = vtableDetector.detect(data, peInfo);

            // 3. AI 分析（可选）
            String aiSummary = null;
            if (apiKey != null && !apiKey.isBlank()) {
                aiSummary = deepSeekService.analyzeWithAI(peInfo, vtables, apiKey, model);
                // 将 AI 分析结果应用到各虚表
                applyAiNotes(vtables, aiSummary);
            }

            // 4. 组装结果
            AnalysisResult result = new AnalysisResult();
            result.setPeInfo(peInfo);
            result.setVtables(vtables);
            result.setAiSummary(aiSummary);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private void applyAiNotes(List<VtableInfo> vtables, String aiSummary) {
        // 简单设置：将 AI 摘要的前 200 字符作为每个虚表的备注
        if (aiSummary != null && vtables != null) {
            String note = aiSummary.length() > 200 ? aiSummary.substring(0, 200) + "..." : aiSummary;
            for (VtableInfo vt : vtables) {
                if (vt.getAiNote() == null) {
                    vt.setAiNote("AI: " + note);
                }
            }
        }
    }
}
