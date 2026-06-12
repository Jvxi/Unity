package com.jvxi.unity.service;

import com.jvxi.unity.model.PeInfo;
import com.jvxi.unity.model.VtableInfo;
import com.jvxi.unity.model.SectionInfo;
import com.jvxi.unity.model.VFunctionInfo;
import com.jvxi.unity.model.WorldAnalysisResult;
import com.jvxi.unity.model.WorldArrayCandidate;
import com.jvxi.unity.model.WorldRelatedData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报告导出服务 - 将分析结果导出为 JSON/HTML 格式
 */
@Service
public class ReportExportService {

    /** 导出为 JSON 格式 */
    public String exportJson(PeInfo peInfo, List<VtableInfo> vtables, String aiSummary) {
        return exportJson(peInfo, vtables, null, aiSummary);
    }

    /** 导出为 JSON 格式 */
    public String exportJson(PeInfo peInfo, List<VtableInfo> vtables, WorldAnalysisResult worldAnalysis, String aiSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"title\": \"猫爪工具 - 分析报告\",\n");
        sb.append("  \"generatedAt\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        if (peInfo != null) {
            sb.append("  \"peInfo\": {\n");
            sb.append("    \"fileName\": \"").append(esc(peInfo.getFileName())).append("\",\n");
            sb.append("    \"fileSize\": ").append(peInfo.getFileSize()).append(",\n");
            sb.append("    \"machine\": \"").append(peInfo.getMachine()).append("\",\n");
            sb.append("    \"magic\": \"").append(peInfo.getMagic()).append("\",\n");
            sb.append("    \"subsystem\": \"").append(peInfo.getSubsystem()).append("\",\n");
            sb.append("    \"numberOfSections\": ").append(peInfo.getNumberOfSections()).append(",\n");
            sb.append("    \"exportCount\": ").append(peInfo.getExportCount()).append(",\n");
            sb.append("    \"importCount\": ").append(peInfo.getImportCount()).append("\n");
            sb.append("  },\n");
        }
        sb.append("  \"vtableCount\": ").append(vtables != null ? vtables.size() : 0).append(",\n");
        appendWorldJson(sb, worldAnalysis);
        sb.append("  \"aiSummary\": ").append(aiSummary != null ? "\"" + esc(aiSummary) + "\"" : "null").append("\n");
        sb.append("}");
        return sb.toString();
    }

    /** 导出为 HTML 格式 */
    public String exportHtml(PeInfo peInfo, List<VtableInfo> vtables, String aiSummary) {
        return exportHtml(peInfo, vtables, null, aiSummary);
    }

    /** 导出为 HTML 格式 */
    public String exportHtml(PeInfo peInfo, List<VtableInfo> vtables, WorldAnalysisResult worldAnalysis, String aiSummary) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html>\n<html lang=\"zh\">\n<head>\n");
        h.append("<meta charset=\"UTF-8\">\n<title>猫爪工具 - 分析报告</title>\n");
        h.append("<style>\n");
        h.append("body{font-family:'Segoe UI',sans-serif;background:#1a1b1e;color:#e2e8f0;padding:32px}\n");
        h.append("h1{color:#4ade80;font-size:24px;margin-bottom:4px}\n");
        h.append("h2{color:#94a3b8;font-size:18px;margin:24px 0 12px;border-bottom:1px solid #2d2e33;padding-bottom:8px}\n");
        h.append("h3{color:#d1d5db;font-size:15px;margin:16px 0 8px}\n");
        h.append(".meta{color:#6b7280;font-size:13px;margin-bottom:24px}\n");
        h.append("table{width:100%;border-collapse:collapse;margin-bottom:16px}\n");
        h.append("th,td{text-align:left;padding:8px 12px;border-bottom:1px solid #2d2e33;font-size:13px}\n");
        h.append("th{color:#94a3b8;font-weight:600;background:#22232a}\n");
        h.append("td{color:#d1d5db}\n");
        h.append(".ai-box{background:#22232a;border-radius:8px;padding:16px;white-space:pre-wrap;font-size:13px;line-height:1.6}\n");
        h.append(".footer{margin-top:32px;color:#4b5563;font-size:12px;text-align:center}\n");
        h.append("</style>\n</head>\n<body>\n");
        h.append("<h1>🐱 猫爪工具 - 分析报告</h1>\n");
        h.append("<div class=\"meta\">生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</div>\n");

        if (peInfo != null) {
            h.append("<h2>PE 基本信息</h2>\n<table>\n<tr><th>属性</th><th>值</th></tr>\n");
            h.append(tr("文件名", peInfo.getFileName()));
            h.append(tr("文件大小", fmtSize(peInfo.getFileSize())));
            h.append(tr("架构", peInfo.getMachine()));
            h.append(tr("PE 格式", peInfo.getMagic()));
            h.append(tr("子系统", peInfo.getSubsystem()));
            h.append(tr("节数量", String.valueOf(peInfo.getNumberOfSections())));
            h.append(tr("导出数量", String.valueOf(peInfo.getExportCount())));
            h.append(tr("导入数量", String.valueOf(peInfo.getImportCount())));
            h.append("</table>\n");

            if (peInfo.getSections() != null) {
                h.append("<h2>节表</h2>\n<table>\n<tr><th>名称</th><th>虚拟地址</th><th>虚拟大小</th><th>原始大小</th><th>特征</th></tr>\n");
                for (SectionInfo s : peInfo.getSections()) {
                    h.append("<tr><td>").append(s.getName()).append("</td>");
                    h.append("<td>0x").append(Long.toHexString(s.getVirtualAddress()).toUpperCase()).append("</td>");
                    h.append("<td>0x").append(Long.toHexString(s.getVirtualSize()).toUpperCase()).append("</td>");
                    h.append("<td>0x").append(Long.toHexString(s.getRawSize()).toUpperCase()).append("</td>");
                    h.append("<td>").append(s.getCharacteristics()).append("</td></tr>\n");
                }
                h.append("</table>\n");
            }
        }

        appendWorldHtml(h, worldAnalysis);

        if (vtables != null && !vtables.isEmpty()) {
            h.append("<h2>虚表检测 (").append(vtables.size()).append(" 个)</h2>\n");
            for (int i = 0; i < vtables.size(); i++) {
                VtableInfo vt = vtables.get(i);
                h.append("<h3>虚表 #").append(i + 1).append(" @ RVA ").append(vt.getRva()).append("</h3>\n");
                h.append("<p>检测方式: ").append(vt.getDetectionMethod() != null ? vt.getDetectionMethod() : "未知");
                if (vt.getRttiTypeName() != null) h.append(" | RTTI: ").append(vt.getRttiTypeName());
                h.append("</p>\n");
                if (vt.getFunctions() != null && !vt.getFunctions().isEmpty()) {
                    h.append("<table>\n<tr><th>#</th><th>RVA</th><th>VA</th><th>备注</th></tr>\n");
                    for (VFunctionInfo fn : vt.getFunctions()) {
                        h.append("<tr><td>").append(fn.getIndex()).append("</td>");
                        h.append("<td>").append(fn.getRva()).append("</td>");
                        h.append("<td>").append(fn.getVa()).append("</td>");
                        h.append("<td>").append(fn.getNote() != null ? fn.getNote() : "-").append("</td></tr>\n");
                    }
                    h.append("</table>\n");
                }
            }
        }

        if (aiSummary != null && !aiSummary.isBlank()) {
            h.append("<h2>AI 分析摘要</h2>\n<div class=\"ai-box\">").append(escH(aiSummary)).append("</div>\n");
        }

        h.append("<div class=\"footer\">由猫爪工具自动生成 | Cat Tool v1.0</div>\n</body>\n</html>");
        return h.toString();
    }

    private void appendWorldJson(StringBuilder sb, WorldAnalysisResult worldAnalysis) {
        if (worldAnalysis == null) {
            sb.append("  \"worldAnalysis\": null,\n");
            return;
        }
        sb.append("  \"worldAnalysis\": {\n");
        sb.append("    \"summary\": \"").append(esc(worldAnalysis.getSummary())).append("\",\n");
        sb.append("    \"priorityHints\": [");
        if (worldAnalysis.getPriorityHints() != null) {
            for (int i = 0; i < worldAnalysis.getPriorityHints().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(esc(worldAnalysis.getPriorityHints().get(i))).append("\"");
            }
        }
        sb.append("],\n");
        sb.append("    \"worldArrayCandidates\": [\n");
        List<WorldArrayCandidate> candidates = worldAnalysis.getWorldArrayCandidates() != null ? worldAnalysis.getWorldArrayCandidates() : List.of();
        for (int i = 0; i < candidates.size(); i++) {
            WorldArrayCandidate c = candidates.get(i);
            sb.append("      {")
                .append("\"name\":\"").append(esc(c.getName())).append("\",")
                .append("\"kind\":\"").append(esc(c.getKind())).append("\",")
                .append("\"rva\":\"").append(esc(c.getRva())).append("\",")
                .append("\"va\":\"").append(esc(c.getVa())).append("\",")
                .append("\"sectionName\":\"").append(esc(c.getSectionName())).append("\",")
                .append("\"detectionMethod\":\"").append(esc(c.getDetectionMethod())).append("\",")
                .append("\"pointerCount\":").append(c.getPointerCount()).append(",")
                .append("\"confidence\":").append(String.format(Locale.ROOT, "%.4f", c.getConfidence())).append(",")
                .append("\"evidence\":");
            appendJsonStringArray(sb, c.getEvidence());
            sb.append(",\"relatedStrings\":");
            appendJsonStringArray(sb, c.getRelatedStrings());
            sb.append("}");
            if (i < candidates.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ],\n");
        List<WorldRelatedData> relatedData = worldAnalysis.getRelatedData() != null ? worldAnalysis.getRelatedData() : List.of();
        sb.append("    \"relatedDataCount\": ").append(relatedData.size()).append(",\n");
        sb.append("    \"relatedData\": [\n");
        for (int i = 0; i < relatedData.size(); i++) {
            WorldRelatedData data = relatedData.get(i);
            sb.append("      {")
                .append("\"kind\":\"").append(esc(data.getKind())).append("\",")
                .append("\"name\":\"").append(esc(data.getName())).append("\",")
                .append("\"rva\":\"").append(esc(data.getRva())).append("\",")
                .append("\"va\":\"").append(esc(data.getVa())).append("\",")
                .append("\"sectionName\":\"").append(esc(data.getSectionName())).append("\",")
                .append("\"value\":\"").append(esc(data.getValue())).append("\",")
                .append("\"note\":\"").append(esc(data.getNote())).append("\"")
                .append("}");
            if (i < relatedData.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("  },\n");
    }

    private void appendWorldHtml(StringBuilder h, WorldAnalysisResult worldAnalysis) {
        if (worldAnalysis == null) return;
        h.append("<h2>世界数组与相关全局数据</h2>\n");
        h.append("<p>").append(escH(worldAnalysis.getSummary())).append("</p>\n");
        if (worldAnalysis.getPriorityHints() != null && !worldAnalysis.getPriorityHints().isEmpty()) {
            h.append("<ul>\n");
            for (String hint : worldAnalysis.getPriorityHints()) {
                h.append("<li>").append(escH(hint)).append("</li>\n");
            }
            h.append("</ul>\n");
        }
        if (worldAnalysis.getWorldArrayCandidates() != null && !worldAnalysis.getWorldArrayCandidates().isEmpty()) {
            h.append("<table>\n<tr><th>名称</th><th>类型</th><th>RVA</th><th>VA</th><th>段</th><th>指针数</th><th>置信度</th><th>证据</th></tr>\n");
            for (WorldArrayCandidate c : worldAnalysis.getWorldArrayCandidates()) {
                h.append("<tr><td>").append(escH(c.getName())).append("</td>");
                h.append("<td>").append(escH(c.getKind())).append("</td>");
                h.append("<td>").append(escH(c.getRva())).append("</td>");
                h.append("<td>").append(escH(c.getVa())).append("</td>");
                h.append("<td>").append(escH(c.getSectionName())).append("</td>");
                h.append("<td>").append(c.getPointerCount()).append("</td>");
                h.append("<td>").append(String.format(Locale.ROOT, "%.2f", c.getConfidence())).append("</td>");
                h.append("<td>").append(escH(String.join(" | ", c.getEvidence() != null ? c.getEvidence() : List.of()))).append("</td></tr>\n");
            }
            h.append("</table>\n");
        }
        if (worldAnalysis.getRelatedData() != null && !worldAnalysis.getRelatedData().isEmpty()) {
            h.append("<h3>相关证据</h3>\n<table>\n<tr><th>类型</th><th>名称</th><th>RVA</th><th>段</th><th>值</th><th>说明</th></tr>\n");
            for (WorldRelatedData data : worldAnalysis.getRelatedData().stream().limit(40).toList()) {
                h.append("<tr><td>").append(escH(data.getKind())).append("</td>");
                h.append("<td>").append(escH(data.getName())).append("</td>");
                h.append("<td>").append(escH(data.getRva())).append("</td>");
                h.append("<td>").append(escH(data.getSectionName())).append("</td>");
                h.append("<td>").append(escH(data.getValue())).append("</td>");
                h.append("<td>").append(escH(data.getNote())).append("</td></tr>\n");
            }
            h.append("</table>\n");
        }
    }

    private String tr(String label, String val) {
        return "<tr><td>" + label + "</td><td>" + (val != null ? val : "-") + "</td></tr>\n";
    }

    private String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1048576.0);
    }

    private void appendJsonStringArray(StringBuilder sb, List<String> values) {
        sb.append("[");
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(esc(values.get(i))).append("\"");
            }
        }
        sb.append("]");
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String escH(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
