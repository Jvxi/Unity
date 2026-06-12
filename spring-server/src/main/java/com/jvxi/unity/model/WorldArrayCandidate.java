package com.jvxi.unity.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界数组、全局对象数组、名称池等高价值全局数据候选。
 */
public class WorldArrayCandidate {
    private String name;
    private String kind;
    private String rva;
    private long rvaValue;
    private String va;
    private long vaValue;
    private String sectionName;
    private String detectionMethod;
    private int pointerCount;
    private double confidence;
    private List<String> evidence = new ArrayList<>();
    private List<String> relatedStrings = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getRvaValue() { return rvaValue; }
    public void setRvaValue(long rvaValue) { this.rvaValue = rvaValue; }
    public String getVa() { return va; }
    public void setVa(String va) { this.va = va; }
    public long getVaValue() { return vaValue; }
    public void setVaValue(long vaValue) { this.vaValue = vaValue; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    public int getPointerCount() { return pointerCount; }
    public void setPointerCount(int pointerCount) { this.pointerCount = pointerCount; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public List<String> getRelatedStrings() { return relatedStrings; }
    public void setRelatedStrings(List<String> relatedStrings) { this.relatedStrings = relatedStrings; }
}
