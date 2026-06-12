package com.jvxi.unity.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 十六进制查看服务 - 生成分段的 Hex dump
 */
@Service
public class HexViewService {

    private static final int BYTES_PER_LINE = 16;

    /**
     * Hex dump 行
     */
    public static class HexLine {
        private long offset;
        private String hex;
        private String ascii;

        public HexLine(long offset, String hex, String ascii) {
            this.offset = offset;
            this.hex = hex;
            this.ascii = ascii;
        }

        public long getOffset() { return offset; }
        public String getHex() { return hex; }
        public String getAscii() { return ascii; }
    }

    /**
     * Hex dump 结果
     */
    public static class HexResult {
        private List<HexLine> lines;
        private long totalBytes;
        private long startOffset;
        private long endOffset;
        private int totalLines;

        public HexResult(List<HexLine> lines, long totalBytes, long startOffset, long endOffset) {
            this.lines = lines;
            this.totalBytes = totalBytes;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.totalLines = lines.size();
        }

        public List<HexLine> getLines() { return lines; }
        public long getTotalBytes() { return totalBytes; }
        public long getStartOffset() { return startOffset; }
        public long getEndOffset() { return endOffset; }
        public int getTotalLines() { return totalLines; }
    }

    /**
     * 生成 Hex dump
     * @param data 二进制数据
     * @param offset 起始偏移（字节）
     * @param length 读取长度（字节），0 表示全部
     * @param searchHex 搜索十六进制字符串（可选，如 "4D5A"）
     */
    public HexResult dump(byte[] data, long offset, int length, String searchHex) {
        long totalBytes = data.length;
        int start = (int) Math.min(Math.max(0, offset), totalBytes);
        int end;

        if (length <= 0) {
            // 限制前端加载量，最多 64KB
            end = Math.min(start + 65536, (int) totalBytes);
        } else {
            end = Math.min(start + Math.min(length, 65536), (int) totalBytes);
        }

        // 搜索模式：找到匹配位置
        if (searchHex != null && !searchHex.isBlank()) {
            byte[] pattern = hexStringToBytes(searchHex.replaceAll("\\s+", ""));
            if (pattern != null && pattern.length > 0) {
                List<Integer> matches = findPattern(data, pattern);
                if (matches.isEmpty()) {
                    return new HexResult(List.of(), totalBytes, 0, 0);
                }
                // 展示第一个匹配位置周围的上下文
                int matchStart = Math.max(0, matches.get(0) - 256);
                int matchEnd = Math.min((int) totalBytes, matches.get(0) + pattern.length + 256);
                start = matchStart;
                end = matchEnd;
            }
        }

        List<HexLine> lines = new ArrayList<>();
        for (int i = start; i < end; i += BYTES_PER_LINE) {
            int lineEnd = Math.min(i + BYTES_PER_LINE, end);
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            for (int j = i; j < i + BYTES_PER_LINE; j++) {
                if (j < lineEnd) {
                    int b = data[j] & 0xFF;
                    hexPart.append(String.format("%02X", b));
                    asciiPart.append(b >= 0x20 && b < 0x7F ? (char) b : '.');
                } else {
                    hexPart.append("  ");
                    asciiPart.append(' ');
                }
                if ((j - i + 1) % 8 == 0 && j < i + BYTES_PER_LINE - 1) {
                    hexPart.append("  ");
                } else if (j < i + BYTES_PER_LINE - 1) {
                    hexPart.append(' ');
                }
            }

            lines.add(new HexLine(i, hexPart.toString(), asciiPart.toString()));
        }

        return new HexResult(lines, totalBytes, start, end);
    }

    /** 在数据中搜索字节模式 */
    private List<Integer> findPattern(byte[] data, byte[] pattern) {
        List<Integer> matches = new ArrayList<>();
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            matches.add(i);
            if (matches.size() >= 10) break; // 最多 10 个匹配
        }
        return matches;
    }

    /** 十六进制字符串转字节数组 */
    private byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) return null;
        try {
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < hex.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
