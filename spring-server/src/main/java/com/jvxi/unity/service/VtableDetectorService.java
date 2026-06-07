package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import com.jvxi.unity.service.pe.RawBinaryPeParser;
import org.springframework.stereotype.Service;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 虚表检测服务：三种策略扫描
 */
@Service
public class VtableDetectorService {

    private final RawBinaryPeParser rawParser;

    public VtableDetectorService(RawBinaryPeParser rawParser) {
        this.rawParser = rawParser;
    }

    public List<VtableInfo> detect(byte[] data, PeInfo peInfo) {
        List<VtableInfo> all = new ArrayList<>();
        boolean is64 = "PE32+".equals(peInfo.getMagic());
        int ptrSize = is64 ? 8 : 4;

        // 找到 .text 和 .rdata/.data 段的 RVA 范围
        long textStart = 0, textEnd = 0;
        long rdataStart = 0, rdataEnd = 0;
        long dataStart = 0, dataEnd = 0;

        for (SectionInfo sec : peInfo.getSections()) {
            long s = sec.getVirtualAddress();
            long e = s + Math.max(sec.getVirtualSize(), sec.getRawSize());
            if (".text".equals(sec.getName())) { textStart = s; textEnd = e; }
            if (".rdata".equals(sec.getName())) { rdataStart = s; rdataEnd = e; }
            if (".data".equals(sec.getName())) { dataStart = s; dataEnd = e; }
        }

        // 策略一：RTTI 引导
        all.addAll(detectByRtti(data, peInfo.getImageBase(), is64, ptrSize,
                rdataStart, rdataEnd, textStart, textEnd, peInfo));

        // 策略二：连续代码指针扫描
        all.addAll(detectByPointerScan(data, peInfo.getImageBase(), is64, ptrSize,
                rdataStart, rdataEnd, textStart, textEnd, peInfo));
        if (dataStart > 0) {
            all.addAll(detectByPointerScan(data, peInfo.getImageBase(), is64, ptrSize,
                    dataStart, dataEnd, textStart, textEnd, peInfo));
        }

        // 策略三：导出交叉引用
        if (peInfo.getExports() != null) {
            all.addAll(detectByExportRef(data, peInfo, is64, ptrSize,
                    rdataStart, rdataEnd, textStart, textEnd));
        }

        // 去重（按 RVA）
        return deduplicate(all);
    }

    /**
     * 策略一：RTTI Complete Object Locator 扫描
     */
    private List<VtableInfo> detectByRtti(byte[] data, long imageBase, boolean is64, int ptrSize,
                                           long rdataStart, long rdataEnd,
                                           long textStart, long textEnd, PeInfo peInfo) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // 扫描 .rdata 段中 COL 结构
        long scanStart = rdataStart;
        long scanEnd = rdataEnd - (is64 ? 24 : 20);

        for (long rva = scanStart; rva < scanEnd; rva += 4) {
            long fileOff = rawParser.rvaToFileOff(data, rva);
            if (fileOff < 0 || fileOff + 24 > data.length) continue;

            // COL signature: 64-bit = 1, 32-bit = 0
            int sig = buf.getInt((int) fileOff);
            if (sig != 0 && sig != 1) continue;
            boolean col64 = (sig == 1);
            if (col64 != is64) continue;

            // 验证 COL 字段合理性
            int offsetToTop = buf.getInt((int)(fileOff + 4));
            if (Math.abs(offsetToTop) > 0x100000) continue; // 太大的偏移不合理

            // TypeDescriptor RVA
            long tdRva;
            if (is64) {
                tdRva = buf.getInt((int)(fileOff + 12)) & 0xFFFFFFFFL;
            } else {
                tdRva = buf.getInt((int)(fileOff + 12)) & 0xFFFFFFFFL;
            }
            if (tdRva == 0 || tdRva > peInfo.getFileSize()) continue;

            // 提取类型名（TypeDescriptor 前 8/16 字节是 vtable 指针，然后是 name 字符串）
            String typeName = extractTypeName(data, tdRva, is64, peInfo.getImageBase());

            // vtable RVA = COL RVA - ptrSize (vtable pointer 在 COL 之前)
            long vtableRva = rva - ptrSize;
            if (vtableRva < rdataStart || vtableRva >= rdataEnd) continue;

            // 读取 vtable 内容
            long vtableFileOff = rawParser.rvaToFileOff(data, vtableRva);
            if (vtableFileOff < 0) continue;

            List<VFunctionInfo> funcs = new ArrayList<>();
            for (int i = 0; i < 50; i++) { // 最多扫描 50 个函数指针
                int fOff = (int)(vtableFileOff + i * ptrSize);
                if (fOff + ptrSize > data.length) break;
                long funcAddr = is64 ? buf.getLong(fOff) : (buf.getInt(fOff) & 0xFFFFFFFFL);
                if (!isCodePointer(funcAddr, imageBase, textStart, textEnd)) break;

                VFunctionInfo vf = new VFunctionInfo();
                vf.setIndex(i);
                vf.setRvaValue(funcAddr - imageBase);
                vf.setRva("0x" + Long.toHexString(vf.getRvaValue()));
                vf.setVaValue(funcAddr);
                vf.setVa("0x" + Long.toHexString(funcAddr));
                funcs.add(vf);
            }

            if (funcs.size() >= 2) {
                VtableInfo vt = new VtableInfo();
                vt.setRvaValue(vtableRva);
                vt.setRva("0x" + Long.toHexString(vtableRva));
                vt.setVaValue(imageBase + vtableRva);
                vt.setVa("0x" + Long.toHexString(vt.getVaValue()));
                vt.setFunctionCount(funcs.size());
                vt.setDetectionMethod("RTTI");
                vt.setRttiTypeName(typeName);
                vt.setFunctions(funcs);
                list.add(vt);
            }
        }
        return list;
    }

    /**
     * 策略二：连续代码指针数组扫描
     */
    private List<VtableInfo> detectByPointerScan(byte[] data, long imageBase, boolean is64, int ptrSize,
                                                  long start, long end,
                                                  long textStart, long textEnd, PeInfo peInfo) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        Set<Long> alreadyFound = new HashSet<>();

        long scanStart = start;
        long scanEnd = end - ptrSize;

        int consecutiveCount = 0;
        long arrayStart = 0;

        for (long rva = scanStart; rva < scanEnd; rva += ptrSize) {
            long fileOff = rawParser.rvaToFileOff(data, rva);
            if (fileOff < 0 || fileOff + ptrSize > data.length) {
                consecutiveCount = 0;
                continue;
            }
            long val = is64 ? buf.getLong((int) fileOff) : (buf.getInt((int) fileOff) & 0xFFFFFFFFL);

            if (isCodePointer(val, imageBase, textStart, textEnd)) {
                if (consecutiveCount == 0) arrayStart = rva;
                consecutiveCount++;
            } else {
                if (consecutiveCount >= 3 && !alreadyFound.contains(arrayStart)) {
                    alreadyFound.add(arrayStart);
                    list.add(buildVtableFromScan(data, arrayStart, consecutiveCount,
                            imageBase, is64, ptrSize, textStart, textEnd));
                }
                consecutiveCount = 0;
            }
        }
        // 处理末尾
        if (consecutiveCount >= 3 && !alreadyFound.contains(arrayStart)) {
            list.add(buildVtableFromScan(data, arrayStart, consecutiveCount,
                    imageBase, is64, ptrSize, textStart, textEnd));
        }
        return list;
    }

    /**
     * 策略三：导出符号交叉引用
     */
    private List<VtableInfo> detectByExportRef(byte[] data, PeInfo peInfo, boolean is64, int ptrSize,
                                                long rdataStart, long rdataEnd,
                                                long textStart, long textEnd) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        for (ExportInfo exp : peInfo.getExports()) {
            long rva = exp.getRvaValue();
            if (rva < rdataStart || rva >= rdataEnd) continue;

            long fileOff = rawParser.rvaToFileOff(data, rva);
            if (fileOff < 0 || fileOff + ptrSize > data.length) continue;

            long pointedAddr = is64 ? buf.getLong((int) fileOff) : (buf.getInt((int) fileOff) & 0xFFFFFFFFL);
            long pointedRva = pointedAddr - peInfo.getImageBase();

            if (pointedRva < rdataStart || pointedRva >= rdataEnd) continue;

            // 检查指向的地址是否是一个代码指针数组
            long pointedFileOff = rawParser.rvaToFileOff(data, pointedRva);
            if (pointedFileOff < 0) continue;

            List<VFunctionInfo> funcs = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                int fOff = (int)(pointedFileOff + i * ptrSize);
                if (fOff + ptrSize > data.length) break;
                long funcAddr = is64 ? buf.getLong(fOff) : (buf.getInt(fOff) & 0xFFFFFFFFL);
                if (!isCodePointer(funcAddr, peInfo.getImageBase(), textStart, textEnd)) break;

                VFunctionInfo vf = new VFunctionInfo();
                vf.setIndex(i);
                vf.setRvaValue(funcAddr - peInfo.getImageBase());
                vf.setRva("0x" + Long.toHexString(vf.getRvaValue()));
                vf.setVaValue(funcAddr);
                vf.setVa("0x" + Long.toHexString(funcAddr));
                funcs.add(vf);
            }

            if (funcs.size() >= 2) {
                VtableInfo vt = new VtableInfo();
                vt.setRvaValue(pointedRva);
                vt.setRva("0x" + Long.toHexString(pointedRva));
                vt.setVaValue(peInfo.getImageBase() + pointedRva);
                vt.setVa("0x" + Long.toHexString(vt.getVaValue()));
                vt.setFunctionCount(funcs.size());
                vt.setDetectionMethod("EXPORT_REF");
                vt.setRelatedSymbol(exp.getName());
                vt.setFunctions(funcs);
                list.add(vt);
            }
        }
        return list;
    }

    private VtableInfo buildVtableFromScan(byte[] data, long rva, int count,
                                            long imageBase, boolean is64, int ptrSize,
                                            long textStart, long textEnd) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long fileOff = rawParser.rvaToFileOff(data, rva);

        List<VFunctionInfo> funcs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int fOff = (int)(fileOff + i * ptrSize);
            if (fOff + ptrSize > data.length) break;
            long funcAddr = is64 ? buf.getLong(fOff) : (buf.getInt(fOff) & 0xFFFFFFFFL);
            if (!isCodePointer(funcAddr, imageBase, textStart, textEnd)) break;

            VFunctionInfo vf = new VFunctionInfo();
            vf.setIndex(i);
            vf.setRvaValue(funcAddr - imageBase);
            vf.setRva("0x" + Long.toHexString(vf.getRvaValue()));
            vf.setVaValue(funcAddr);
            vf.setVa("0x" + Long.toHexString(funcAddr));
            funcs.add(vf);
        }

        VtableInfo vt = new VtableInfo();
        vt.setRvaValue(rva);
        vt.setRva("0x" + Long.toHexString(rva));
        vt.setVaValue(imageBase + rva);
        vt.setVa("0x" + Long.toHexString(vt.getVaValue()));
        vt.setFunctionCount(funcs.size());
        vt.setDetectionMethod("POINTER_SCAN");
        vt.setFunctions(funcs);
        return vt;
    }

    private boolean isCodePointer(long addr, long imageBase, long textStart, long textEnd) {
        if (addr == 0) return false;
        long rva = addr - imageBase;
        return rva >= textStart && rva < textEnd;
    }

    private String extractTypeName(byte[] data, long tdRva, boolean is64, long imageBase) {
        try {
            // TypeDescriptor 结构：前 ptrSize*2 字节是 RTTI 指针，后面是 ".?AVClassName@@"
            long fileOff = rawParser.rvaToFileOff(data, tdRva);
            if (fileOff < 0) return null;
            long nameOffset = fileOff + (is64 ? 16 : 8);
            if (nameOffset >= data.length) return null;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 256 && nameOffset + i < data.length; i++) {
                byte b = data[(int)(nameOffset + i)];
                if (b == 0) break;
                sb.append((char)(b & 0xFF));
            }
            String raw = sb.toString();
            return demangleMsvcType(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String demangleMsvcType(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // ".?AVClassName@@" -> "ClassName"
        // ".?AUClassName@@" -> "ClassName"
        String s = raw;
        if (s.startsWith(".?A")) {
            s = s.substring(4); // remove ".?AV" or ".?AU"
        }
        if (s.endsWith("@@")) {
            s = s.substring(0, s.length() - 2);
        }
        // 替换嵌套命名空间
        s = s.replace("@", "::");
        return s;
    }

    private List<VtableInfo> deduplicate(List<VtableInfo> all) {
        Map<String, VtableInfo> map = new LinkedHashMap<>();
        for (VtableInfo vt : all) {
            String key = vt.getRva();
            if (!map.containsKey(key)) {
                map.put(key, vt);
            } else {
                // 合并：保留函数数量更多的
                VtableInfo existing = map.get(key);
                if (vt.getFunctionCount() > existing.getFunctionCount()) {
                    map.put(key, vt);
                }
            }
        }
        return new ArrayList<>(map.values());
    }
}
