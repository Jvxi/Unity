package com.jvxi.unity.service;

import com.jvxi.unity.model.ExportInfo;
import com.jvxi.unity.model.PeInfo;
import com.jvxi.unity.model.SectionInfo;
import com.jvxi.unity.model.VtableInfo;
import com.jvxi.unity.service.pe.RawBinaryPeParser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VtableDetectorServiceTest {
    private static final long IMAGE_BASE = 0x140000000L;

    @Test
    void detectsPointerScanRttiAndExportReferencedVtables() {
        SyntheticPe pe = syntheticPe();

        putPtr(pe.data, off(0x2000), IMAGE_BASE + 0x1010);
        putPtr(pe.data, off(0x2008), IMAGE_BASE + 0x1020);
        putPtr(pe.data, off(0x2010), IMAGE_BASE + 0x1030);

        putPtr(pe.data, off(0x2040), IMAGE_BASE + 0x2080);
        putPtr(pe.data, off(0x2048), IMAGE_BASE + 0x1040);
        putPtr(pe.data, off(0x2050), IMAGE_BASE + 0x1050);
        putCol(pe.data, 0x2080, 0x20B0);
        putTypeDescriptor(pe.data, 0x20B0, ".?AVDemoClass@@");

        putPtr(pe.data, off(0x2120), IMAGE_BASE + 0x1060);
        putPtr(pe.data, off(0x2128), IMAGE_BASE + 0x1070);
        ExportInfo exportInfo = new ExportInfo();
        exportInfo.setName("ExportedVtable");
        exportInfo.setRvaValue(0x2120);
        pe.info.setExports(List.of(exportInfo));

        List<VtableInfo> vtables = new VtableDetectorService(new RawBinaryPeParser()).detect(pe.data, pe.info);

        VtableInfo scanned = requireVtable(vtables, 0x2000);
        assertEquals("POINTER_SCAN", scanned.getDetectionMethod());
        assertEquals(3, scanned.getFunctionCount());

        VtableInfo rtti = requireVtable(vtables, 0x2048);
        assertEquals("RTTI", rtti.getDetectionMethod());
        assertEquals("DemoClass", rtti.getRttiTypeName());
        assertEquals(2, rtti.getFunctionCount());

        VtableInfo exported = requireVtable(vtables, 0x2120);
        assertEquals("EXPORT_REF", exported.getDetectionMethod());
        assertEquals("ExportedVtable", exported.getRelatedSymbol());
        assertEquals(2, exported.getFunctionCount());
    }

    @Test
    void rejectsShortPointerRunsAsNoise() {
        SyntheticPe pe = syntheticPe();
        putPtr(pe.data, off(0x2000), IMAGE_BASE + 0x1010);
        putPtr(pe.data, off(0x2008), IMAGE_BASE + 0x1020);

        List<VtableInfo> vtables = new VtableDetectorService(new RawBinaryPeParser()).detect(pe.data, pe.info);

        assertTrue(vtables.isEmpty());
    }

    @Test
    void detectsPe32RttiAndExportRvaReferencedVtables() {
        long imageBase = 0x400000L;
        SyntheticPe pe = syntheticPe32(imageBase);

        putPtr32(pe.data, off(0x2040), imageBase + 0x2080);
        putPtr32(pe.data, off(0x2044), imageBase + 0x1010);
        putPtr32(pe.data, off(0x2048), imageBase + 0x1020);
        putCol32(pe.data, 0x2080, imageBase + 0x20B0);
        putTypeDescriptor32(pe.data, 0x20B0, ".?AUWin32Class@@");

        putPtr32(pe.data, off(0x2120), 0x2140);
        putPtr32(pe.data, off(0x2140), imageBase + 0x1030);
        putPtr32(pe.data, off(0x2144), imageBase + 0x1040);
        ExportInfo exportInfo = new ExportInfo();
        exportInfo.setName("ExportedRvaVtable");
        exportInfo.setRvaValue(0x2120);
        pe.info.setExports(List.of(exportInfo));

        List<VtableInfo> vtables = new VtableDetectorService(new RawBinaryPeParser()).detect(pe.data, pe.info);

        VtableInfo rtti = requireVtable(vtables, 0x2044);
        assertEquals("RTTI", rtti.getDetectionMethod());
        assertEquals("Win32Class", rtti.getRttiTypeName());
        assertEquals(2, rtti.getFunctionCount());

        VtableInfo exported = requireVtable(vtables, 0x2140);
        assertEquals("EXPORT_REF", exported.getDetectionMethod());
        assertEquals("ExportedRvaVtable", exported.getRelatedSymbol());
        assertEquals(2, exported.getFunctionCount());
    }

    private static VtableInfo requireVtable(List<VtableInfo> vtables, long rva) {
        return vtables.stream()
            .filter(item -> item.getRvaValue() == rva)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing vtable at 0x" + Long.toHexString(rva)));
    }

    private static SyntheticPe syntheticPe() {
        byte[] data = new byte[0xA00];
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
        putSection(buf, sec + 40, ".rdata", 0x2000, 0x400, 0x600, 0x400, 0x40000040);

        PeInfo info = new PeInfo();
        info.setMagic("PE32+");
        info.setImageBase(IMAGE_BASE);
        info.setFileSize(data.length);
        info.setSections(List.of(
            section(".text", 0x1000, 0x200, 0x200, 0x400, "CODE|EXECUTE|READ"),
            section(".rdata", 0x2000, 0x400, 0x400, 0x600, "INITIALIZED_DATA|READ")
        ));
        info.setExports(List.of());
        return new SyntheticPe(data, info);
    }

    private static SyntheticPe syntheticPe32(long imageBase) {
        byte[] data = new byte[0xA00];
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        data[0] = 'M';
        data[1] = 'Z';
        buf.putInt(0x3C, 0x80);
        buf.putInt(0x80, 0x4550);

        int coff = 0x84;
        buf.putShort(coff, (short) 0x14C);
        buf.putShort(coff + 2, (short) 2);
        buf.putShort(coff + 16, (short) 0xE0);

        int opt = coff + 20;
        buf.putShort(opt, (short) 0x10B);
        buf.putInt(opt + 28, (int) imageBase);

        int sec = opt + 0xE0;
        putSection(buf, sec, ".text", 0x1000, 0x200, 0x400, 0x200, 0x60000020);
        putSection(buf, sec + 40, ".rdata", 0x2000, 0x400, 0x600, 0x400, 0x40000040);

        PeInfo info = new PeInfo();
        info.setMagic("PE32");
        info.setImageBase(imageBase);
        info.setFileSize(data.length);
        info.setSections(List.of(
            section(".text", 0x1000, 0x200, 0x200, 0x400, "CODE|EXECUTE|READ"),
            section(".rdata", 0x2000, 0x400, 0x400, 0x600, "INITIALIZED_DATA|READ")
        ));
        info.setExports(List.of());
        return new SyntheticPe(data, info);
    }

    private static SectionInfo section(String name, long va, long vSize, long rawSize, long rawPtr, String chars) {
        SectionInfo section = new SectionInfo();
        section.setName(name);
        section.setVirtualAddress(va);
        section.setVirtualSize(vSize);
        section.setRawSize(rawSize);
        section.setRawPointer(rawPtr);
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

    private static void putCol(byte[] data, int rva, int typeDescriptorRva) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int fileOff = off(rva);
        buf.putInt(fileOff, 1);
        buf.putInt(fileOff + 4, 0);
        buf.putInt(fileOff + 8, 0);
        buf.putInt(fileOff + 12, typeDescriptorRva);
        buf.putInt(fileOff + 16, 0);
        buf.putInt(fileOff + 20, 0);
    }

    private static void putTypeDescriptor(byte[] data, int rva, String name) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, data, off(rva) + 16, bytes.length);
    }

    private static void putPtr(byte[] data, int off, long value) {
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putLong(off, value);
    }

    private static void putCol32(byte[] data, int rva, long typeDescriptorVa) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int fileOff = off(rva);
        buf.putInt(fileOff, 0);
        buf.putInt(fileOff + 4, 0);
        buf.putInt(fileOff + 8, 0);
        buf.putInt(fileOff + 12, (int) typeDescriptorVa);
        buf.putInt(fileOff + 16, 0);
    }

    private static void putTypeDescriptor32(byte[] data, int rva, String name) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, data, off(rva) + 8, bytes.length);
    }

    private static void putPtr32(byte[] data, int off, long value) {
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(off, (int) value);
    }

    private static int off(int rva) {
        if (rva >= 0x1000 && rva < 0x1200) return 0x400 + (rva - 0x1000);
        if (rva >= 0x2000 && rva < 0x2400) return 0x600 + (rva - 0x2000);
        throw new IllegalArgumentException("RVA out of synthetic sections: 0x" + Integer.toHexString(rva));
    }

    private record SyntheticPe(byte[] data, PeInfo info) {
    }
}
