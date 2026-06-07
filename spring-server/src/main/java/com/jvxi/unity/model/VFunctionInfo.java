package com.jvxi.unity.model;

/**
 * 虚函数信息
 */
public class VFunctionInfo {
    private int index;
    private String rva;
    private long rvaValue;
    private String va;
    private long vaValue;
    private String note;

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getRvaValue() { return rvaValue; }
    public void setRvaValue(long rvaValue) { this.rvaValue = rvaValue; }
    public String getVa() { return va; }
    public void setVa(String va) { this.va = va; }
    public long getVaValue() { return vaValue; }
    public void setVaValue(long vaValue) { this.vaValue = vaValue; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
