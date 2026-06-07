package com.jvxi.unity.service.pe;

import com.jvxi.unity.model.*;
import org.springframework.stereotype.Service;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 手写 PE 二进制解析器（全面版）
 * 覆盖 DOS/PE 头、段表、Data Directory、导出表、导入表、调试、TLS
 */
@Service
public class RawBinaryPeParser {

    public PeInfo parse(byte[] data) {
        PeInfo info = new PeInfo();
        info.setFileSize(data.length);
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // DOS Header
        if ((buf.getShort(0) & 0xFFFF) != 0x5A4D) throw new RuntimeException("不是有效的 PE 文件（缺少 MZ 签名）");
        int peOff = buf.getInt(0x3C);
        if (peOff + 4 > data.length) throw new RuntimeException("PE 偏移越界");

        // PE Signature
        if (buf.getInt(peOff) != 0x4550) throw new RuntimeException("不是有效的 PE 文件（缺少 PE 签名）");

        // COFF Header
        int coff = peOff + 4;
        int machine = buf.getShort(coff) & 0xFFFF;
        int numSec = buf.getShort(coff + 2) & 0xFFFF;
        long ts = buf.getInt(coff + 4) & 0xFFFFFFFFL;
        int optSize = buf.getShort(coff + 16) & 0xFFFF;

        info.setMachine(machineStr(machine));
        info.setNumberOfSections(numSec);
        info.setTimeDateStamp(ts);
        info.setCharacteristics(coffChars(buf.getShort(coff + 18) & 0xFFFF));

        // Optional Header
        int opt = coff + 20;
        int magic = buf.getShort(opt) & 0xFFFF;
        boolean is64 = magic == 0x20B;
        info.setMagic(is64 ? "PE32+" : "PE32");

        long imageBase = is64 ? buf.getLong(opt + 24) : (buf.getInt(opt + 28) & 0xFFFFFFFFL);
        info.setImageBase(imageBase);
        info.setSectionAlignment(buf.getInt(opt + 32));
        info.setFileAlignment(buf.getInt(opt + 36));
        info.setSizeOfImage(buf.getInt(opt + 56));
        info.setSizeOfHeaders(buf.getInt(opt + 60));
        info.setCheckSum(buf.getInt(opt + 64) & 0xFFFFFFFFL);
        info.setSubsystem(subsysStr(buf.getShort(opt + 68) & 0xFFFF));
        info.setDllCharacteristics(dllChars(buf.getShort(opt + 70) & 0xFFFF));

        int ddBase, ddCount;
        if (is64) {
            ddCount = buf.getInt(opt + 108);
            ddBase = opt + 112;
        } else {
            ddCount = buf.getInt(opt + 92);
            ddBase = opt + 96;
        }

        // Data Directories
        List<DataDirectoryEntry> ddList = parseDataDirs(buf, ddBase, ddCount);
        info.setDataDirectories(ddList);

        // Sections
        int secStart = opt + optSize;
        List<SectionInfo> sections = parseSections(buf, secStart, numSec);
        info.setSections(sections);

        // Exports (DD index 0)
        if (ddList.size() > 0 && ddList.get(0).isPresent()) {
            List<ExportInfo> exports = parseExports(buf, data, ddList.get(0), imageBase, sections);
            info.setExports(exports);
            info.setExportCount(exports.size());
        } else {
            info.setExports(new ArrayList<>());
            info.setExportCount(0);
        }

        // Imports (DD index 1)
        if (ddList.size() > 1 && ddList.get(1).isPresent()) {
            List<ImportInfo> imports = parseImports(buf, data, ddList.get(1), imageBase, is64, sections);
            info.setImports(imports);
            info.setImportCount(imports.size());
        } else {
            info.setImports(new ArrayList<>());
            info.setImportCount(0);
        }

        // Debug (DD index 6)
        if (ddList.size() > 6 && ddList.get(6).isPresent()) {
            info.setDebugInfo(parseDebug(buf, data, ddList.get(6), sections));
        }

        // TLS (DD index 9)
        if (ddList.size() > 9 && ddList.get(9).isPresent()) {
            info.setTlsCallbacks(parseTls(buf, data, ddList.get(9), imageBase, is64, sections));
        }

        return info;
    }

    // ======== 工具方法 ========

    public long rvaToFileOff(byte[] data, long rva) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int peOff = buf.getInt(0x3C);
        int coff = peOff + 4;
        int numSec = buf.getShort(coff + 2) & 0xFFFF;
        int optSize = buf.getShort(coff + 16) & 0xFFFF;
        int secStart = peOff + 4 + 20 + optSize;
        for (int i = 0; i < numSec; i++) {
            int o = secStart + i * 40;
            int vSize = buf.getInt(o + 8);
            int vAddr = buf.getInt(o + 12);
            int rawPtr = buf.getInt(o + 20);
            if (rva >= vAddr && rva < vAddr + Math.max(vSize, buf.getInt(o + 16))) {
                return rawPtr + (rva - vAddr);
            }
        }
        return -1;
    }

    public long readPtr(ByteBuffer buf, int off, boolean is64) {
        return is64 ? buf.getLong(off) : (buf.getInt(off) & 0xFFFFFFFFL);
    }

    private String readNullTermStr(byte[] data, int off) {
        if (off < 0 || off >= data.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < data.length && i < off + 512; i++) {
            byte b = data[i];
            if (b == 0) break;
            sb.append((char)(b & 0xFF));
        }
        return sb.toString();
    }

    private List<DataDirectoryEntry> parseDataDirs(ByteBuffer buf, int off, int count) {
        String[] names = {"Export","Import","Resource","Exception","Certificate",
            "BaseReloc","Debug","Architecture","GlobalPtr","TLS",
            "LoadConfig","BoundImport","IAT","DelayImport","CLR","Reserved"};
        List<DataDirectoryEntry> list = new ArrayList<>();
        for (int i = 0; i < Math.min(count, 16); i++) {
            int o = off + i * 8;
            if (o + 8 > buf.capacity()) break;
            DataDirectoryEntry d = new DataDirectoryEntry();
            d.setIndex(i);
            d.setName(i < names.length ? names[i] : "Unknown_" + i);
            d.setRvaValue(buf.getInt(o) & 0xFFFFFFFFL);
            d.setSize(buf.getInt(o + 4) & 0xFFFFFFFFL);
            d.setRva("0x" + Long.toHexString(d.getRvaValue()));
            d.setPresent(d.getRvaValue() != 0 && d.getSize() != 0);
            list.add(d);
        }
        return list;
    }

    private List<SectionInfo> parseSections(ByteBuffer buf, int off, int count) {
        List<SectionInfo> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int o = off + i * 40;
            if (o + 40 > buf.capacity()) break;
            SectionInfo s = new SectionInfo();
            byte[] name = new byte[8];
            buf.position(o); buf.get(name);
            s.setName(new String(name).trim().replace("\0",""));
            s.setVirtualSize(buf.getInt(o+8) & 0xFFFFFFFFL);
            s.setVirtualAddress(buf.getInt(o+12) & 0xFFFFFFFFL);
            s.setRva("0x" + Long.toHexString(s.getVirtualAddress()));
            s.setRawSize(buf.getInt(o+16) & 0xFFFFFFFFL);
            s.setRawPointer(buf.getInt(o+20) & 0xFFFFFFFFL);
            s.setCharacteristics(secChars(buf.getInt(o+36)));
            list.add(s);
        }
        return list;
    }

    private List<ExportInfo> parseExports(ByteBuffer buf, byte[] data, DataDirectoryEntry dd, long imageBase, List<SectionInfo> sections) {
        List<ExportInfo> list = new ArrayList<>();
        long off = rvaToFileOff(data, dd.getRvaValue());
        if (off < 0 || off + 40 > data.length) return list;
        int numFuncs = buf.getInt((int)(off + 20));
        int numNames = buf.getInt((int)(off + 24));
        int funcTableRva = buf.getInt((int)(off + 28));
        int nameTableRva = buf.getInt((int)(off + 32));
        int ordTableRva = buf.getInt((int)(off + 36));
        int ordBase = buf.getInt((int)(off + 16));

        // 构建 ordinal->name 映射
        Map<Integer, String> ordNameMap = new HashMap<>();
        long nameTableOff = rvaToFileOff(data, nameTableRva & 0xFFFFFFFFL);
        long ordTableOff = rvaToFileOff(data, ordTableRva & 0xFFFFFFFFL);
        if (nameTableOff >= 0 && ordTableOff >= 0) {
            for (int i = 0; i < numNames && nameTableOff + 4 <= data.length; i++) {
                int nameRva = buf.getInt((int)(nameTableOff + i * 4));
                int ordinal = buf.getShort((int)(ordTableOff + i * 2)) & 0xFFFF;
                long nameOff = rvaToFileOff(data, nameRva & 0xFFFFFFFFL);
                String name = nameOff >= 0 ? readNullTermStr(data, (int)nameOff) : null;
                if (name != null) ordNameMap.put(ordinal, name);
            }
        }

        long funcTableOff = rvaToFileOff(data, funcTableRva & 0xFFFFFFFFL);
        if (funcTableOff < 0) return list;

        for (int i = 0; i < numFuncs && funcTableOff + 4 <= data.length; i++) {
            int funcRva = buf.getInt((int)(funcTableOff + i * 4));
            if (funcRva == 0) continue;
            ExportInfo e = new ExportInfo();
            e.setOrdinal(ordBase + i);
            e.setRvaValue(funcRva & 0xFFFFFFFFL);
            e.setRva("0x" + Long.toHexString(e.getRvaValue()));
            e.setName(ordNameMap.get(i));
            // 检测 forwarder
            long ddStart = dd.getRvaValue();
            long ddEnd = ddStart + dd.getSize();
            e.setForwarder(e.getRvaValue() >= ddStart && e.getRvaValue() < ddEnd);
            if (e.isForwarder()) {
                long fwdOff = rvaToFileOff(data, e.getRvaValue());
                if (fwdOff >= 0) e.setForwarderName(readNullTermStr(data, (int)fwdOff));
            }
            list.add(e);
        }
        return list;
    }

    private List<ImportInfo> parseImports(ByteBuffer buf, byte[] data, DataDirectoryEntry dd, long imageBase, boolean is64, List<SectionInfo> sections) {
        List<ImportInfo> list = new ArrayList<>();
        long off = rvaToFileOff(data, dd.getRvaValue());
        if (off < 0) return list;

        // 每个 Import Descriptor 20 字节
        for (int i = 0; ; i++) {
            int o = (int)(off + i * 20);
            if (o + 20 > data.length) break;
            int dllNameRva = buf.getInt(o + 12);
            int iltRva = buf.getInt(o);
            if (dllNameRva == 0 && iltRva == 0) break;

            ImportInfo imp = new ImportInfo();
            long dllOff = rvaToFileOff(data, dllNameRva & 0xFFFFFFFFL);
            imp.setDllName(dllOff >= 0 ? readNullTermStr(data, (int)dllOff) : "unknown");

            List<ImportInfo.ImportFunction> funcs = new ArrayList<>();
            long iltOff = rvaToFileOff(data, iltRva & 0xFFFFFFFFL);
            if (iltOff >= 0) {
                int ptrSz = is64 ? 8 : 4;
                for (int j = 0; ; j++) {
                    int fo = (int)(iltOff + j * ptrSz);
                    if (fo + ptrSz > data.length) break;
                    long entry = readPtr(buf, fo, is64);
                    if (entry == 0) break;
                    ImportInfo.ImportFunction f = new ImportInfo.ImportFunction();
                    if (is64 ? (entry & 0x8000000000000000L) != 0 : (entry & 0x80000000L) != 0) {
                        f.setName("ordinal_" + (entry & 0xFFFF));
                        f.setHint((int)(entry & 0xFFFF));
                    } else {
                        int hintNameRva = (int)(entry & 0x7FFFFFFFL);
                        long hintOff = rvaToFileOff(data, hintNameRva & 0xFFFFFFFFL);
                        if (hintOff >= 0 && hintOff + 2 < data.length) {
                            f.setHint(buf.getShort((int)hintOff) & 0xFFFF);
                            f.setName(readNullTermStr(data, (int)(hintOff + 2)));
                        }
                    }
                    funcs.add(f);
                }
            }
            imp.setFunctions(funcs);
            list.add(imp);
        }
        return list;
    }

    private List<DebugInfo> parseDebug(ByteBuffer buf, byte[] data, DataDirectoryEntry dd, List<SectionInfo> sections) {
        List<DebugInfo> list = new ArrayList<>();
        long off = rvaToFileOff(data, dd.getRvaValue());
        if (off < 0) return list;
        int count = (int)(dd.getSize() / 28);
        for (int i = 0; i < count && off + i * 28 + 28 <= data.length; i++) {
            int o = (int)(off + i * 28);
            int type = buf.getInt(o + 12);
            DebugInfo d = new DebugInfo();
            d.setType(switch (type) {
                case 2 -> "CODEVIEW";
                case 4 -> "FPO";
                case 9 -> "BORLAND";
                case 13 -> "REPRO";
                default -> "TYPE_" + type;
            });
            if (type == 2 && buf.getInt(o + 24) > 0) {
                int cvRva = buf.getInt(o + 24);
                int cvRawSize = buf.getInt(o + 16);
                long cvOff = rvaToFileOff(data, cvRva & 0xFFFFFFFFL);
                if (cvOff >= 0 && cvOff + 24 <= data.length) {
                    int sig = buf.getInt((int)cvOff);
                    if (sig == 0x53445352) { // "RSDS"
                        byte[] guid = new byte[16];
                        buf.position((int)(cvOff + 4)); buf.get(guid);
                        d.setGuid(String.format("%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                            buf.getInt((int)(cvOff+4)), buf.getShort((int)(cvOff+8))&0xFFFF, buf.getShort((int)(cvOff+10))&0xFFFF,
                            data[(int)(cvOff+12)]&0xFF, data[(int)(cvOff+13)]&0xFF,
                            data[(int)(cvOff+14)]&0xFF, data[(int)(cvOff+15)]&0xFF,
                            data[(int)(cvOff+16)]&0xFF, data[(int)(cvOff+17)]&0xFF,
                            data[(int)(cvOff+18)]&0xFF, data[(int)(cvOff+19)]&0xFF));
                        d.setAge(buf.getInt((int)(cvOff + 20)));
                        d.setPdbPath(readNullTermStr(data, (int)(cvOff + 24)));
                    }
                }
            }
            list.add(d);
        }
        return list;
    }

    private List<Long> parseTls(ByteBuffer buf, byte[] data, DataDirectoryEntry dd, long imageBase, boolean is64, List<SectionInfo> sections) {
        List<Long> callbacks = new ArrayList<>();
        long off = rvaToFileOff(data, dd.getRvaValue());
        if (off < 0) return callbacks;
        long callbackRva = readPtr(buf, (int)(off + (is64 ? 24 : 12)), is64);
        if (callbackRva == 0) return callbacks;
        long cbOff = rvaToFileOff(data, callbackRva - imageBase);
        if (cbOff < 0) return callbacks;
        int ptrSz = is64 ? 8 : 4;
        while (cbOff + ptrSz <= data.length) {
            long addr = readPtr(buf, (int)cbOff, is64);
            if (addr == 0) break;
            callbacks.add(addr);
            cbOff += ptrSz;
        }
        return callbacks;
    }

    // ======== 名称转换 ========

    private String machineStr(int m) {
        return switch (m) {
            case 0x14c -> "I386"; case 0x8664 -> "AMD64"; case 0xAA64 -> "ARM64";
            default -> "0x" + Integer.toHexString(m);
        };
    }
    private String subsysStr(int s) {
        return switch (s) {
            case 1->"NATIVE"; case 2->"WINDOWS_GUI"; case 3->"WINDOWS_CUI";
            case 7->"POSIX_CUI"; case 10->"EFI_APPLICATION"; default->"UNKNOWN("+s+")";
        };
    }
    private List<String> coffChars(int c) {
        List<String> l = new ArrayList<>();
        if ((c&0x0002)!=0) l.add("EXECUTABLE_IMAGE");
        if ((c&0x0020)!=0) l.add("LARGE_ADDRESS_AWARE");
        if ((c&0x2000)!=0) l.add("DLL");
        if ((c&0x0100)!=0) l.add("32BIT_MACHINE");
        return l;
    }
    private List<String> dllChars(int c) {
        List<String> l = new ArrayList<>();
        if ((c&0x0020)!=0) l.add("HIGH_ENTROPY_VA");
        if ((c&0x0040)!=0) l.add("DYNAMIC_BASE");
        if ((c&0x0100)!=0) l.add("NX_COMPAT");
        if ((c&0x0400)!=0) l.add("NO_SEH");
        if ((c&0x4000)!=0) l.add("GUARD_CF");
        if ((c&0x8000)!=0) l.add("TERMINAL_SERVER_AWARE");
        return l;
    }
    private String secChars(int c) {
        StringBuilder sb = new StringBuilder();
        if ((c&0x20)!=0) sb.append("CODE|");
        if ((c&0x40)!=0) sb.append("INITIALIZED_DATA|");
        if ((c&0x80)!=0) sb.append("UNINITIALIZED_DATA|");
        if ((c&0x20000000)!=0) sb.append("EXECUTE|");
        if ((c&0x40000000)!=0) sb.append("READ|");
        if ((c&0x80000000)!=0) sb.append("WRITE|");
        if ((c&0x10000000)!=0) sb.append("SHARE|");
        String s = sb.toString();
        return s.isEmpty() ? "NONE" : s.substring(0, s.length()-1);
    }
}
