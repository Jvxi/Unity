package com.jvxi.unity.model;

/**
 * 调试信息
 */
public class DebugInfo {
    private String type;
    private String pdbPath;
    private String guid;
    private int age;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPdbPath() { return pdbPath; }
    public void setPdbPath(String pdbPath) { this.pdbPath = pdbPath; }
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
