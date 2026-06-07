package com.jvxi.unity.model;

/**
 * 段表信息
 */
public class SectionInfo {
    private String name;
    private String rva;           // 相对虚拟地址（十六进制）
    private long virtualAddress;  // 原始数值
    private long virtualSize;
    private long rawSize;
    private long rawPointer;
    private String characteristics;  // CODE|EXECUTE|READ 等

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getVirtualAddress() { return virtualAddress; }
    public void setVirtualAddress(long virtualAddress) { this.virtualAddress = virtualAddress; }
    public long getVirtualSize() { return virtualSize; }
    public void setVirtualSize(long virtualSize) { this.virtualSize = virtualSize; }
    public long getRawSize() { return rawSize; }
    public void setRawSize(long rawSize) { this.rawSize = rawSize; }
    public long getRawPointer() { return rawPointer; }
    public void setRawPointer(long rawPointer) { this.rawPointer = rawPointer; }
    public String getCharacteristics() { return characteristics; }
    public void setCharacteristics(String characteristics) { this.characteristics = characteristics; }
}
