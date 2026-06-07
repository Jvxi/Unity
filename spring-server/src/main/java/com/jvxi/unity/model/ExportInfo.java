package com.jvxi.unity.model;

/**
 * 导出函数信息
 */
public class ExportInfo {
    private String name;
    private String rva;
    private long rvaValue;
    private int ordinal;
    private boolean forwarder;
    private String forwarderName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRva() { return rva; }
    public void setRva(String rva) { this.rva = rva; }
    public long getRvaValue() { return rvaValue; }
    public void setRvaValue(long rvaValue) { this.rvaValue = rvaValue; }
    public int getOrdinal() { return ordinal; }
    public void setOrdinal(int ordinal) { this.ordinal = ordinal; }
    public boolean isForwarder() { return forwarder; }
    public void setForwarder(boolean forwarder) { this.forwarder = forwarder; }
    public String getForwarderName() { return forwarderName; }
    public void setForwarderName(String forwarderName) { this.forwarderName = forwarderName; }
}
