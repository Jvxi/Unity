package com.jvxi.unity.model;

import java.util.List;

/**
 * PE 文件完整解析信息
 */
public class PeInfo {
    private String fileName;
    private long fileSize;
    private String machine;           // I386 / AMD64
    private int numberOfSections;
    private long timeDateStamp;
    private List<String> characteristics;  // DLL, EXECUTABLE_IMAGE 等

    // Optional Header
    private String magic;             // PE32 / PE32+
    private long imageBase;
    private int sectionAlignment;
    private int fileAlignment;
    private String subsystem;         // WINDOWS_GUI / WINDOWS_CUI / NATIVE
    private int sizeOfImage;
    private int sizeOfHeaders;
    private long checkSum;
    private List<String> dllCharacteristics;  // DYNAMIC_BASE, NX_COMPAT, GUARD_CF 等

    // Sections
    private List<SectionInfo> sections;

    // Exports
    private int exportCount;
    private List<ExportInfo> exports;

    // Imports
    private int importCount;
    private List<ImportInfo> imports;

    // Debug
    private List<DebugInfo> debugInfo;

    // TLS
    private List<Long> tlsCallbacks;

    // Data Directory 标记
    private List<DataDirectoryEntry> dataDirectories;

    // 签名
    private boolean hasCertificate;
    private String certificateInfo;

    // COFF Symbol Table
    private List<CoffSymbol> coffSymbols;

    // Guard CF
    private int guardCFFunctions;

    // 标记哪些解析器成功解析了
    private List<String> parserSources;

    // getters and setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getMachine() { return machine; }
    public void setMachine(String machine) { this.machine = machine; }
    public int getNumberOfSections() { return numberOfSections; }
    public void setNumberOfSections(int numberOfSections) { this.numberOfSections = numberOfSections; }
    public long getTimeDateStamp() { return timeDateStamp; }
    public void setTimeDateStamp(long timeDateStamp) { this.timeDateStamp = timeDateStamp; }
    public List<String> getCharacteristics() { return characteristics; }
    public void setCharacteristics(List<String> characteristics) { this.characteristics = characteristics; }
    public String getMagic() { return magic; }
    public void setMagic(String magic) { this.magic = magic; }
    public long getImageBase() { return imageBase; }
    public void setImageBase(long imageBase) { this.imageBase = imageBase; }
    public int getSectionAlignment() { return sectionAlignment; }
    public void setSectionAlignment(int sectionAlignment) { this.sectionAlignment = sectionAlignment; }
    public int getFileAlignment() { return fileAlignment; }
    public void setFileAlignment(int fileAlignment) { this.fileAlignment = fileAlignment; }
    public String getSubsystem() { return subsystem; }
    public void setSubsystem(String subsystem) { this.subsystem = subsystem; }
    public int getSizeOfImage() { return sizeOfImage; }
    public void setSizeOfImage(int sizeOfImage) { this.sizeOfImage = sizeOfImage; }
    public int getSizeOfHeaders() { return sizeOfHeaders; }
    public void setSizeOfHeaders(int sizeOfHeaders) { this.sizeOfHeaders = sizeOfHeaders; }
    public long getCheckSum() { return checkSum; }
    public void setCheckSum(long checkSum) { this.checkSum = checkSum; }
    public List<String> getDllCharacteristics() { return dllCharacteristics; }
    public void setDllCharacteristics(List<String> dllCharacteristics) { this.dllCharacteristics = dllCharacteristics; }
    public List<SectionInfo> getSections() { return sections; }
    public void setSections(List<SectionInfo> sections) { this.sections = sections; }
    public int getExportCount() { return exportCount; }
    public void setExportCount(int exportCount) { this.exportCount = exportCount; }
    public List<ExportInfo> getExports() { return exports; }
    public void setExports(List<ExportInfo> exports) { this.exports = exports; }
    public int getImportCount() { return importCount; }
    public void setImportCount(int importCount) { this.importCount = importCount; }
    public List<ImportInfo> getImports() { return imports; }
    public void setImports(List<ImportInfo> imports) { this.imports = imports; }
    public List<DebugInfo> getDebugInfo() { return debugInfo; }
    public void setDebugInfo(List<DebugInfo> debugInfo) { this.debugInfo = debugInfo; }
    public List<Long> getTlsCallbacks() { return tlsCallbacks; }
    public void setTlsCallbacks(List<Long> tlsCallbacks) { this.tlsCallbacks = tlsCallbacks; }
    public List<DataDirectoryEntry> getDataDirectories() { return dataDirectories; }
    public void setDataDirectories(List<DataDirectoryEntry> dataDirectories) { this.dataDirectories = dataDirectories; }
    public boolean isHasCertificate() { return hasCertificate; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }
    public String getCertificateInfo() { return certificateInfo; }
    public void setCertificateInfo(String certificateInfo) { this.certificateInfo = certificateInfo; }
    public List<CoffSymbol> getCoffSymbols() { return coffSymbols; }
    public void setCoffSymbols(List<CoffSymbol> coffSymbols) { this.coffSymbols = coffSymbols; }
    public int getGuardCFFunctions() { return guardCFFunctions; }
    public void setGuardCFFunctions(int guardCFFunctions) { this.guardCFFunctions = guardCFFunctions; }
    public List<String> getParserSources() { return parserSources; }
    public void setParserSources(List<String> parserSources) { this.parserSources = parserSources; }
}
