package com.jvxi.unity.model;

/**
 * Data Directory 条目
 */
public class DataDirectoryEntry {
    private String name;
    private int index;
    private String rva;
    private long rvaValue;
    private long size;
    private boolean present;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getRvaValue() { return rvaValue; }
    public void setRvaValue(long rvaValue) { this.rvaValue = rvaValue; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
