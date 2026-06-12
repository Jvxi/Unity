package com.jvxi.unity.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BinaryToolServiceTest {

    @Test
    void extractsAsciiStringsWithKeywordFilter() {
        byte[] data = "\0MZ\0UnityPlayer\0GameAssembly.dll\0".getBytes(StandardCharsets.US_ASCII);

        StringExtractService.ExtractResult result = new StringExtractService()
            .extract(data, 4, "ascii", "Assembly");

        assertEquals(1, result.getTotalCount());
        assertEquals("GameAssembly.dll", result.getStrings().get(0).getValue());
        assertEquals("ascii", result.getStrings().get(0).getEncoding());
    }

    @Test
    void extractsUtf16LeStrings() {
        byte[] prefix = new byte[] {0, 1, 2, 3};
        byte[] unicode = new byte[] {
            'P', 0, 'l', 0, 'a', 0, 'y', 0, 'e', 0, 'r', 0, 0, 0
        };
        byte[] data = new byte[prefix.length + unicode.length];
        System.arraycopy(prefix, 0, data, 0, prefix.length);
        System.arraycopy(unicode, 0, data, prefix.length, unicode.length);

        StringExtractService.ExtractResult result = new StringExtractService()
            .extract(data, 4, "unicode", null);

        assertEquals(1, result.getTotalCount());
        assertEquals("Player", result.getStrings().get(0).getValue());
        assertEquals("unicode", result.getStrings().get(0).getEncoding());
        assertEquals(4, result.getStrings().get(0).getOffset());
    }

    @Test
    void hexDumpClampsNegativeOffset() {
        byte[] data = new byte[] {0x4D, 0x5A, 1, 2, 3, 4};

        HexViewService.HexResult result = new HexViewService().dump(data, -128, 4, null);

        assertEquals(0, result.getStartOffset());
        assertEquals(4, result.getEndOffset());
        assertEquals(1, result.getTotalLines());
        assertTrue(result.getLines().get(0).getHex().startsWith("4D 5A 01 02"));
    }

    @Test
    void hexDumpSearchReturnsContextAroundMatch() {
        byte[] data = new byte[600];
        data[300] = 0x4D;
        data[301] = 0x5A;

        HexViewService.HexResult result = new HexViewService().dump(data, 0, 0, "4D5A");

        assertFalse(result.getLines().isEmpty());
        assertTrue(result.getStartOffset() <= 300);
        assertTrue(result.getEndOffset() >= 302);
        assertTrue(result.getLines().stream().anyMatch(line -> line.getHex().contains("4D 5A")));
    }

    @Test
    void hexDumpReturnsEmptyWhenSearchDoesNotMatch() {
        byte[] data = new byte[] {1, 2, 3, 4};

        HexViewService.HexResult result = new HexViewService().dump(data, 0, 0, "4D5A");

        assertTrue(result.getLines().isEmpty());
        assertEquals(0, result.getTotalLines());
    }
}
