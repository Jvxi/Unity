package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import com.jvxi.unity.service.pe.RawBinaryPeParser;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 虚表检测服务：RTTI、连续代码指针、导出引用三种策略。
 */
@Service
public class VtableDetectorService {

    private static final int MAX_VFUNCTIONS = 128;
    private static final int MIN_RTTI_FUNCTIONS = 2;
    private static final int MIN_SCAN_FUNCTIONS = 3;

    private final RawBinaryPeParser rawParser;

    public VtableDetectorService(RawBinaryPeParser rawParser) {
        this.rawParser = rawParser;
    }

    public List<VtableInfo> detect(byte[] data, PeInfo peInfo) {
        if (data == null || peInfo == null || peInfo.getSections() == null) {
            return List.of();
        }

        boolean is64 = "PE32+".equals(peInfo.getMagic());
        int ptrSize = is64 ? 8 : 4;
        List<long[]> codeRanges = executableRanges(peInfo);
        if (codeRanges.isEmpty()) {
            return List.of();
        }

        List<long[]> dataRanges = dataRanges(peInfo);
        List<VtableInfo> all = new ArrayList<>();
        for (long[] range : dataRanges) {
            all.addAll(detectByRtti(data, peInfo, is64, ptrSize, range[0], range[1], codeRanges));
            all.addAll(detectByPointerScan(data, peInfo.getImageBase(), is64, ptrSize, range[0], range[1], codeRanges));
        }

        if (peInfo.getExports() != null && !peInfo.getExports().isEmpty()) {
            all.addAll(detectByExportRef(data, peInfo, is64, ptrSize, dataRanges, codeRanges));
        }

        return deduplicate(all);
    }

    private List<VtableInfo> detectByRtti(
            byte[] data,
            PeInfo peInfo,
            boolean is64,
            int ptrSize,
            long start,
            long end,
            List<long[]> codeRanges) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        Map<Long, String> colPointers = new HashMap<>();

        long minColSize = is64 ? 24 : 20;
        for (long rva = start; rva <= end - minColSize; rva += 4) {
            long fileOff = rawParser.rvaToFileOff(data, rva);
            if (!fits(fileOff, minColSize, data.length)) continue;

            int signature = buf.getInt((int) fileOff);
            if ((is64 && signature != 1) || (!is64 && signature != 0)) continue;

            int offsetToTop = buf.getInt((int) fileOff + 4);
            if (Math.abs(offsetToTop) > 0x100000) continue;

            long typeDescriptorRef = buf.getInt((int) fileOff + 12) & 0xFFFFFFFFL;
            long typeDescriptorRva = is64 ? typeDescriptorRef : toRva(typeDescriptorRef, peInfo.getImageBase());
            if (!validRva(data, typeDescriptorRva)) continue;

            String typeName = extractTypeName(data, typeDescriptorRva, is64);
            colPointers.put(peInfo.getImageBase() + rva, typeName);
            colPointers.put(rva, typeName);
        }

        if (colPointers.isEmpty()) return list;

        for (long refRva = start; refRva <= end - ptrSize; refRva += ptrSize) {
            long fileOff = rawParser.rvaToFileOff(data, refRva);
            if (!fits(fileOff, ptrSize, data.length)) continue;

            long value = readPointer(buf, (int) fileOff, is64);
            if (!colPointers.containsKey(value)) continue;

            long vtableRva = refRva + ptrSize;
            List<VFunctionInfo> funcs = readVtableFunctions(data, vtableRva, peInfo.getImageBase(), is64, ptrSize, codeRanges);
            if (funcs.size() < MIN_RTTI_FUNCTIONS) continue;

            VtableInfo vt = buildVtable(vtableRva, peInfo.getImageBase(), funcs, "RTTI");
            vt.setRttiTypeName(colPointers.get(value));
            list.add(vt);
        }
        return list;
    }

    private List<VtableInfo> detectByPointerScan(
            byte[] data,
            long imageBase,
            boolean is64,
            int ptrSize,
            long start,
            long end,
            List<long[]> codeRanges) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int consecutive = 0;
        long arrayStart = 0;

        for (long rva = start; rva <= end - ptrSize; rva += ptrSize) {
            long fileOff = rawParser.rvaToFileOff(data, rva);
            if (!fits(fileOff, ptrSize, data.length)) {
                consecutive = 0;
                continue;
            }

            long value = readPointer(buf, (int) fileOff, is64);
            if (isCodePointer(value, imageBase, codeRanges)) {
                if (consecutive == 0) arrayStart = rva;
                consecutive++;
            } else {
                addScannedVtable(list, data, arrayStart, consecutive, imageBase, is64, ptrSize, codeRanges);
                consecutive = 0;
            }
        }
        addScannedVtable(list, data, arrayStart, consecutive, imageBase, is64, ptrSize, codeRanges);
        return list;
    }

    private List<VtableInfo> detectByExportRef(
            byte[] data,
            PeInfo peInfo,
            boolean is64,
            int ptrSize,
            List<long[]> dataRanges,
            List<long[]> codeRanges) {
        List<VtableInfo> list = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        for (ExportInfo exp : peInfo.getExports()) {
            if (exp.isForwarder()) continue;
            long exportRva = exp.getRvaValue();
            if (!contains(dataRanges, exportRva)) continue;

            List<VFunctionInfo> direct = readVtableFunctions(data, exportRva, peInfo.getImageBase(), is64, ptrSize, codeRanges);
            if (direct.size() >= MIN_RTTI_FUNCTIONS) {
                VtableInfo vt = buildVtable(exportRva, peInfo.getImageBase(), direct, "EXPORT_REF");
                vt.setRelatedSymbol(exp.getName());
                list.add(vt);
                continue;
            }

            long fileOff = rawParser.rvaToFileOff(data, exportRva);
            if (!fits(fileOff, ptrSize, data.length)) continue;

            long pointedRva = pointerValueToRva(readPointer(buf, (int) fileOff, is64), peInfo.getImageBase());
            if (!contains(dataRanges, pointedRva)) continue;

            List<VFunctionInfo> funcs = readVtableFunctions(data, pointedRva, peInfo.getImageBase(), is64, ptrSize, codeRanges);
            if (funcs.size() >= MIN_RTTI_FUNCTIONS) {
                VtableInfo vt = buildVtable(pointedRva, peInfo.getImageBase(), funcs, "EXPORT_REF");
                vt.setRelatedSymbol(exp.getName());
                list.add(vt);
            }
        }
        return list;
    }

    private void addScannedVtable(
            List<VtableInfo> list,
            byte[] data,
            long arrayStart,
            int consecutive,
            long imageBase,
            boolean is64,
            int ptrSize,
            List<long[]> codeRanges) {
        if (consecutive < MIN_SCAN_FUNCTIONS) return;
        List<VFunctionInfo> funcs = readVtableFunctions(data, arrayStart, imageBase, is64, ptrSize, codeRanges);
        if (funcs.size() >= MIN_SCAN_FUNCTIONS) {
            list.add(buildVtable(arrayStart, imageBase, funcs, "POINTER_SCAN"));
        }
    }

    private List<VFunctionInfo> readVtableFunctions(
            byte[] data,
            long vtableRva,
            long imageBase,
            boolean is64,
            int ptrSize,
            List<long[]> codeRanges) {
        List<VFunctionInfo> funcs = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long fileOff = rawParser.rvaToFileOff(data, vtableRva);
        if (fileOff < 0) return funcs;

        for (int i = 0; i < MAX_VFUNCTIONS; i++) {
            long entryOff = fileOff + (long) i * ptrSize;
            if (!fits(entryOff, ptrSize, data.length)) break;

            long funcAddr = readPointer(buf, (int) entryOff, is64);
            if (!isCodePointer(funcAddr, imageBase, codeRanges)) break;

            VFunctionInfo vf = new VFunctionInfo();
            vf.setIndex(i);
            vf.setRvaValue(funcAddr - imageBase);
            vf.setRva(hex(vf.getRvaValue()));
            vf.setVaValue(funcAddr);
            vf.setVa(hex(funcAddr));
            funcs.add(vf);
        }
        return funcs;
    }

    private VtableInfo buildVtable(long rva, long imageBase, List<VFunctionInfo> funcs, String method) {
        VtableInfo vt = new VtableInfo();
        vt.setRvaValue(rva);
        vt.setRva(hex(rva));
        vt.setVaValue(imageBase + rva);
        vt.setVa(hex(vt.getVaValue()));
        vt.setFunctionCount(funcs.size());
        vt.setDetectionMethod(method);
        vt.setFunctions(funcs);
        return vt;
    }

    private List<long[]> executableRanges(PeInfo peInfo) {
        List<long[]> ranges = new ArrayList<>();
        for (SectionInfo sec : peInfo.getSections()) {
            if (sec.getCharacteristics() != null && sec.getCharacteristics().contains("EXECUTE")) {
                addRange(ranges, sec);
            }
        }
        return ranges;
    }

    private List<long[]> dataRanges(PeInfo peInfo) {
        List<long[]> ranges = new ArrayList<>();
        for (SectionInfo sec : peInfo.getSections()) {
            String name = Optional.ofNullable(sec.getName()).orElse("").toLowerCase(Locale.ROOT);
            String chars = Optional.ofNullable(sec.getCharacteristics()).orElse("");
            boolean readable = chars.contains("READ");
            boolean executable = chars.contains("EXECUTE");
            boolean knownNoise = name.contains("rsrc") || name.contains("reloc") || name.contains("pdata");
            boolean likelyData = name.contains("data") || name.contains("rdata") || name.contains("xdata") || name.contains("const");
            if (readable && !executable && !knownNoise && likelyData) {
                addRange(ranges, sec);
            }
        }
        return ranges;
    }

    private void addRange(List<long[]> ranges, SectionInfo sec) {
        long start = sec.getVirtualAddress();
        long size = Math.max(sec.getVirtualSize(), sec.getRawSize());
        if (size > 0) ranges.add(new long[] { start, start + size });
    }

    private boolean contains(List<long[]> ranges, long rva) {
        for (long[] range : ranges) {
            if (rva >= range[0] && rva < range[1]) return true;
        }
        return false;
    }

    private boolean isCodePointer(long addr, long imageBase, List<long[]> codeRanges) {
        if (addr == 0 || addr < imageBase) return false;
        long rva = addr - imageBase;
        return contains(codeRanges, rva);
    }

    private long readPointer(ByteBuffer buf, int off, boolean is64) {
        return is64 ? buf.getLong(off) : (buf.getInt(off) & 0xFFFFFFFFL);
    }

    private long toRva(long value, long imageBase) {
        long rva = value >= imageBase ? value - imageBase : value;
        return rva < 0 ? value : rva;
    }

    private long pointerValueToRva(long value, long imageBase) {
        return value >= imageBase ? value - imageBase : value;
    }

    private boolean validRva(byte[] data, long rva) {
        return rva > 0 && rawParser.rvaToFileOff(data, rva) >= 0;
    }

    private boolean fits(long off, long size, int length) {
        return off >= 0 && size >= 0 && off <= Integer.MAX_VALUE && off + size <= length;
    }

    private String extractTypeName(byte[] data, long tdRva, boolean is64) {
        try {
            long fileOff = rawParser.rvaToFileOff(data, tdRva);
            if (fileOff < 0) return null;
            long nameOffset = fileOff + (is64 ? 16 : 8);
            if (nameOffset >= data.length) return null;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 256 && nameOffset + i < data.length; i++) {
                byte b = data[(int) (nameOffset + i)];
                if (b == 0) break;
                sb.append((char) (b & 0xFF));
            }
            return demangleMsvcType(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String demangleMsvcType(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw;
        if (s.startsWith(".?A") && s.length() > 4) {
            s = s.substring(4);
        }
        if (s.endsWith("@@")) {
            s = s.substring(0, s.length() - 2);
        }
        s = s.replace("@", "::");
        return s.isBlank() ? null : s;
    }

    private List<VtableInfo> deduplicate(List<VtableInfo> all) {
        Map<Long, VtableInfo> map = new LinkedHashMap<>();
        for (VtableInfo vt : all) {
            VtableInfo existing = map.get(vt.getRvaValue());
            if (existing == null || shouldReplace(existing, vt)) {
                map.put(vt.getRvaValue(), vt);
            } else {
                mergeMetadata(existing, vt);
            }
        }
        return map.values().stream()
            .sorted(Comparator.comparingLong(VtableInfo::getRvaValue))
            .toList();
    }

    private boolean shouldReplace(VtableInfo existing, VtableInfo candidate) {
        int rankDelta = methodRank(candidate.getDetectionMethod()) - methodRank(existing.getDetectionMethod());
        if (rankDelta != 0) return rankDelta > 0;
        return candidate.getFunctionCount() > existing.getFunctionCount();
    }

    private void mergeMetadata(VtableInfo target, VtableInfo source) {
        if (target.getRttiTypeName() == null) target.setRttiTypeName(source.getRttiTypeName());
        if (target.getRelatedSymbol() == null) target.setRelatedSymbol(source.getRelatedSymbol());
    }

    private int methodRank(String method) {
        if ("RTTI".equals(method)) return 3;
        if ("EXPORT_REF".equals(method)) return 2;
        if ("POINTER_SCAN".equals(method)) return 1;
        return 0;
    }

    private String hex(long value) {
        return "0x" + Long.toHexString(value);
    }
}
