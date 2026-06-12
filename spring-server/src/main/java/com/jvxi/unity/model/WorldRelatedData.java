package com.jvxi.unity.model;

/**
 * 与世界数组分析相关的字符串、导出符号、指针引用等证据。
 */
public class WorldRelatedData {
    private String kind;
    private String name;
    private String rva;
    private long rvaValue;
    private String va;
    private long vaValue;
    private String sectionName;
    private String value;
    private String note;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
