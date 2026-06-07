package com.jvxi.unity.model;

import java.util.List;

/**
 * 虚表信息
 */
public class VtableInfo {
    private String rva;
    private long rvaValue;
    private String va;
    private long vaValue;
    private int functionCount;
    private String detectionMethod;  // RTTI / POINTER_SCAN / EXPORT_REF
    private String relatedSymbol;
    private String rttiTypeName;
    private List<VFunctionInfo> functions;
    private String aiNote;

    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getRvaValue() { return rvaValue; }
    public void setRvaValue(long rvaValue) { this.rvaValue = rvaValue; }
    public String getVa() { return va; }
    public void setVa(String va) { this.va = va; }
    public long getVaValue() { return vaValue; }
    public void setVaValue(long vaValue) { this.vaValue = vaValue; }
    public int getFunctionCount() { return functionCount; }
    public void setFunctionCount(int functionCount) { this.functionCount = functionCount; }
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    public String getRelatedSymbol() { return relatedSymbol; }
    public void setRelatedSymbol(String relatedSymbol) { this.relatedSymbol = relatedSymbol; }
    public String getRttiTypeName() { return rttiTypeName; }
    public void setRttiTypeName(String rttiTypeName) { this.rttiTypeName = rttiTypeName; }
    public List<VFunctionInfo> getFunctions() { return functions; }
    public void setFunctions(List<VFunctionInfo> functions) { this.functions = functions; }
    public String getAiNote() { return aiNote; }
    public void setAiNote(String aiNote) { this.aiNote = aiNote; }
}
