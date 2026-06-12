package com.jvxi.unity.controller;

import com.jvxi.unity.model.PeInfo;
import com.jvxi.unity.model.VtableInfo;
import com.jvxi.unity.model.WorldAnalysisResult;
import com.jvxi.unity.service.*;
import com.jvxi.unity.service.pe.PeParserAggregator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 工具类 API - 字符串提取、十六进制查看、报告导出
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final StringExtractService stringService;
    private final HexViewService hexService;
    private final ReportExportService reportService;
    private final PeParserAggregator peParser;
    private final VtableDetectorService vtableDetector;
    private final WorldArrayAnalysisService worldArrayAnalysisService;
    private final DeepSeekService deepSeekService;

    public ToolController(StringExtractService stringService,
                          HexViewService hexService,
                          ReportExportService reportService,
                          PeParserAggregator peParser,
                          VtableDetectorService vtableDetector,
                          WorldArrayAnalysisService worldArrayAnalysisService,
                          DeepSeekService deepSeekService) {
        this.stringService = stringService;
        this.hexService = hexService;
        this.reportService = reportService;
        this.peParser = peParser;
        this.vtableDetector = vtableDetector;
        this.worldArrayAnalysisService = worldArrayAnalysisService;
        this.deepSeekService = deepSeekService;
    }

    // ==================== 字符串提取 ====================

    @PostMapping("/strings")
    public ResponseEntity<Map<String, Object>> extractStrings(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "minLength", defaultValue = "4") int minLength,
            @RequestParam(value = "encoding", defaultValue = "all") String encoding,
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            byte[] data = file.getBytes();
            StringExtractService.ExtractResult result = stringService.extract(data, minLength, encoding, keyword);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== 十六进制查看 ====================

    @PostMapping("/hex")
    public ResponseEntity<Map<String, Object>> hexDump(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "length", defaultValue = "0") int length,
            @RequestParam(value = "search", required = false) String search) {
        try {
            byte[] data = file.getBytes();
            HexViewService.HexResult result = hexService.dump(data, offset, length, search);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== 报告导出 ====================

    @PostMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "provider", required = false, defaultValue = "deepseek") String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "apiUrl", required = false) String apiUrl) {
        try {
            byte[] data = file.getBytes();
            PeInfo peInfo = peParser.parse(data, file.getOriginalFilename());
            List<VtableInfo> vtables = vtableDetector.detect(data, peInfo);
            WorldAnalysisResult worldAnalysis = worldArrayAnalysisService.analyze(data, peInfo);
            String aiSummary = null;
            if (apiKey != null && !apiKey.isBlank()) {
                aiSummary = deepSeekService.analyzeWithAI(peInfo, vtables, worldAnalysis, apiKey, provider, model, apiUrl);
            }
            String json = reportService.exportJson(peInfo, vtables, worldAnalysis, aiSummary);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/export/html")
    public ResponseEntity<byte[]> exportHtml(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "provider", required = false, defaultValue = "deepseek") String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "apiUrl", required = false) String apiUrl) {
        try {
            byte[] data = file.getBytes();
            PeInfo peInfo = peParser.parse(data, file.getOriginalFilename());
            List<VtableInfo> vtables = vtableDetector.detect(data, peInfo);
            WorldAnalysisResult worldAnalysis = worldArrayAnalysisService.analyze(data, peInfo);
            String aiSummary = null;
            if (apiKey != null && !apiKey.isBlank()) {
                aiSummary = deepSeekService.analyzeWithAI(peInfo, vtables, worldAnalysis, apiKey, provider, model, apiUrl);
            }
            String html = reportService.exportHtml(peInfo, vtables, worldAnalysis, aiSummary);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.html")
                .contentType(MediaType.TEXT_HTML)
                .body(html.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
