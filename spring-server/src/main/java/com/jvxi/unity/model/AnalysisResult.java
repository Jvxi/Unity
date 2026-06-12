package com.jvxi.unity.model;

import java.util.List;

/**
 * 完整分析结果
 */
public class AnalysisResult {
    private PeInfo peInfo;
    private List<VtableInfo> vtables;
    private WorldAnalysisResult worldAnalysis;
    private String aiSummary;

    public PeInfo getPeInfo() { return peInfo; }
    public void setPeInfo(PeInfo peInfo) { this.peInfo = peInfo; }
    public List<VtableInfo> getVtables() { return vtables; }
    public void setVtables(List<VtableInfo> vtables) { this.vtables = vtables; }
    public WorldAnalysisResult getWorldAnalysis() { return worldAnalysis; }
    public void setWorldAnalysis(WorldAnalysisResult worldAnalysis) { this.worldAnalysis = worldAnalysis; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
}
