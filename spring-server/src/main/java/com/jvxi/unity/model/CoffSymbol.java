package com.jvxi.unity.model;

/**
 * COFF 符号表条目
 */
public class CoffSymbol {
    private String name;
    private long value;
    private int sectionNumber;
    private String type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }
    public int getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(int sectionNumber) { this.sectionNumber = sectionNumber; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
