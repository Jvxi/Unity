package com.jvxi.unity.service;

import com.jvxi.unity.model.ExportInfo;
import com.jvxi.unity.model.PeInfo;
import com.jvxi.unity.model.SectionInfo;
import com.jvxi.unity.model.WorldAnalysisResult;
import com.jvxi.unity.service.pe.RawBinaryPeParser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldArrayAnalysisServiceTest {
    private static final long IMAGE_BASE = 0x140000000L;

    @Test
    void detectsWorldExportsStringsAndPointerArrays() {
        SyntheticPe pe = syntheticPe();
        putAscii(pe.data, off(0x2030), "GWorld");
        putAscii(pe.data, off(0x2050), "GUObjectArray");
        putAscii(pe.data, off(0x2070), "GNames");
        putPtr(pe.data, off(0x2100), IMAGE_BASE + 0x2200);
        putPtr(pe.data, off(0x2108), IMAGE_BASE + 0x2210);
        putPtr(pe.data, off(0x2110), IMAGE_BASE + 0x2220);
        putPtr(pe.data, off(0x2118), IMAGE_BASE + 0x2230);

        WorldAnalysisResult result = service().analyze(pe.data, pe.info);

        assertFalse(result.getWorldArrayCandidates().isEmpty());
        assertTrue(result.getWorldArrayCandidates().stream().anyMatch(item -> "world_array".equals(item.getKind())));
        assertTrue(result.getWorldArrayCandidates().stream().anyMatch(item -> item.getPointerCount() >= 4));
        assertTrue(result.getRelatedData().stream().anyMatch(item -> "GWorld".equals(item.getValue())));
        assertTrue(result.getPriorityHints().stream().anyMatch(hint -> hint.contains("世界数组") || hint.contains("WorldContext")));
    }

    @Test
    void returnsClearSummaryWhenNoWorldEvidenceExists() {
        SyntheticPe pe = syntheticPe();
        pe.info.setExports(List.of());
        pe.info.setExportCount(0);

        WorldAnalysisResult result = service().analyze(pe.data, pe.info);

        assertTrue(result.getWorldArrayCandidates().isEmpty());
        assertTrue(result.getRelatedData().isEmpty());
        assertTrue(result.getSummary().contains("未发现明显"));
    }

    private WorldArrayAnalysisService service() {
        return new WorldArrayAnalysisService(new RawBinaryPeParser(), new StringExtractService());
    }

    private static SyntheticPe syntheticPe() {
        byte[] data = new byte[0x1000];
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        data[0] = 'M';
        data[1] = 'Z';
        buf.putInt(0x3C, 0x80);
        buf.putInt(0x80, 0x4550);

        int coff = 0x84;
        buf.putShort(coff, (short) 0x8664);
        buf.putShort(coff + 2, (short) 2);
        buf.putShort(coff + 16, (short) 0xF0);

        int opt = coff + 20;
        buf.putShort(opt, (short) 0x20B);
        buf.putLong(opt + 24, IMAGE_BASE);

        int sec = opt + 0xF0;
        putSection(buf, sec, ".text", 0x1000, 0x200, 0x400, 0x200, 0x60000020);
        putSection(buf, sec + 40, ".rdata", 0x2000, 0x500, 0x600, 0x500, 0x40000040);

        ExportInfo exportInfo = new ExportInfo();
        exportInfo.setName("GWorld");
        exportInfo.setRva("0x2100");
        exportInfo.setRvaValue(0x2100);

        PeInfo info = new PeInfo();
        info.setMagic("PE32+");
        info.setImageBase(IMAGE_BASE);
        info.setFileName("GameAssembly.dll");
        info.setFileSize(data.length);
        info.setSections(List.of(
            section(".text", 0x1000, 0x200, 0x200, 0x400, "CODE|EXECUTE|READ"),
            section(".rdata", 0x2000, 0x500, 0x500, 0x600, "INITIALIZED_DATA|READ")
        ));
        info.setExports(List.of(exportInfo));
        info.setExportCount(1);
        return new SyntheticPe(data, info);
    }

    private static SectionInfo section(String name, long va, long vSize, long rawSize, long rawPtr, String chars) {
        SectionInfo section = new SectionInfo();
        section.setName(name);
        section.setVirtualAddress(va);
        section.setVirtualSize(vSize);
        section.setRawSize(rawSize);
        section.setRawPointer(rawPtr);
        section.setRva("0x" + Long.toHexString(va));
        section.setCharacteristics(chars);
        return section;
    }

    private static void putSection(ByteBuffer buf, int off, String name, int va, int vSize, int rawPtr, int rawSize, int chars) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < Math.min(bytes.length, 8); i++) {
            buf.put(off + i, bytes[i]);
        }
        buf.putInt(off + 8, vSize);
        buf.putInt(off + 12, va);
        buf.putInt(off + 16, rawSize);
        buf.putInt(off + 20, rawPtr);
        buf.putInt(off + 36, chars);
    }

    private static void putAscii(byte[] data, int off, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, data, off, bytes.length);
    }

    private static void putPtr(byte[] data, int off, long value) {
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putLong(off, value);
    }

    private static int off(int rva) {
        if (rva >= 0x1000 && rva < 0x1200) return 0x400 + (rva - 0x1000);
        if (rva >= 0x2000 && rva < 0x2500) return 0x600 + (rva - 0x2000);
        throw new IllegalArgumentException("RVA out of synthetic sections: 0x" + Integer.toHexString(rva));
    }

    private record SyntheticPe(byte[] data, PeInfo info) {
    }
}
