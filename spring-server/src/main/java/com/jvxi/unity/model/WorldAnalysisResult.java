package com.jvxi.unity.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Unity / Unreal 等游戏 DLL 中世界数组和相关全局运行时数据的启发式分析结果。
 */
public class WorldAnalysisResult {
    private List<WorldArrayCandidate> worldArrayCandidates = new ArrayList<>();
    private List<WorldRelatedData> relatedData = new ArrayList<>();
    private List<String> priorityHints = new ArrayList<>();
    private String summary;

    public List<WorldArrayCandidate> getWorldArrayCandidates() { return worldArrayCandidates; }
    public void setWorldArrayCandidates(List<WorldArrayCandidate> worldArrayCandidates) { this.worldArrayCandidates = worldArrayCandidates; }
    public List<WorldRelatedData> getRelatedData() { return relatedData; }
    public void setRelatedData(List<WorldRelatedData> relatedData) { this.relatedData = relatedData; }
    public List<String> getPriorityHints() { return priorityHints; }
    public void setPriorityHints(List<String> priorityHints) { this.priorityHints = priorityHints; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
