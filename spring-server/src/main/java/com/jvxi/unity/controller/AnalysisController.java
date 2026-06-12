package com.jvxi.unity.controller;

import com.jvxi.unity.model.*;
import com.jvxi.unity.service.DeepSeekService;
import com.jvxi.unity.service.VtableDetectorService;
import com.jvxi.unity.service.WorldArrayAnalysisService;
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
    private final WorldArrayAnalysisService worldArrayAnalysisService;
    private final DeepSeekService deepSeekService;

    public AnalysisController(PeParserAggregator peParser,
                              VtableDetectorService vtableDetector,
                              WorldArrayAnalysisService worldArrayAnalysisService,
                              DeepSeekService deepSeekService) {
        this.peParser = peParser;
        this.vtableDetector = vtableDetector;
        this.worldArrayAnalysisService = worldArrayAnalysisService;
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "unity-vtable-analyzer");
    }

    @GetMapping("/providers")
    public Map<String, Object> getProviders() {
        return Map.of("providers", deepSeekService.getProvidersSummary());
    }

    @PostMapping("/ai/test")
    public ResponseEntity<Map<String, Object>> testAiConnection(@RequestBody Map<String, String> request) {
        String result = deepSeekService.testConnection(
            request.get("apiKey"),
            request.getOrDefault("provider", "deepseek"),
            request.get("model"),
            request.get("apiUrl")
        );
        boolean success = result != null
            && !result.startsWith("AI ")
            && !result.startsWith("未知")
            && !result.startsWith("API Key")
            && !result.startsWith("不支持");
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", result));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", result));
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "provider", required = false, defaultValue = "deepseek") String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "apiUrl", required = false) String apiUrl) {

        try {
            byte[] data = file.getBytes();
            String fileName = file.getOriginalFilename();

            // 1. PE 解析
            PeInfo peInfo = peParser.parse(data, fileName);

            // 2. 虚表检测
            List<VtableInfo> vtables = vtableDetector.detect(data, peInfo);

            // 3. 世界数组和相关全局数据优先分析
            WorldAnalysisResult worldAnalysis = worldArrayAnalysisService.analyze(data, peInfo);

            // 4. AI 分析（可选）
            String aiSummary = null;
            if (apiKey != null && !apiKey.isBlank()) {
                aiSummary = deepSeekService.analyzeWithAI(peInfo, vtables, worldAnalysis, apiKey, provider, model, apiUrl);
            }

            // 5. 组装结果
            AnalysisResult result = new AnalysisResult();
            result.setPeInfo(peInfo);
            result.setVtables(vtables);
            result.setWorldAnalysis(worldAnalysis);
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
}
