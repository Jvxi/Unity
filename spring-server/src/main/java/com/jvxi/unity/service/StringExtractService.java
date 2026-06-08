package com.jvxi.unity.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

/**
 * 二进制字符串提取服务 - 从二进制数据中提取 ASCII/Unicode 字符串
 */
@Service
public class StringExtractService {

    /** 默认最小字符串长度 */
    private static final int DEFAULT_MIN_LENGTH = 4;

    /**
     * 提取结果条目
     */
    public static class StringEntry {
        private long offset;
        private String value;
        private String encoding; // "ascii" or "unicode"
        private int length;

        public StringEntry(long offset, String value, String encoding) {
            this.offset = offset;
            this.value = value;
            this.encoding = encoding;
            this.length = value.length();
        }

        public long getOffset() { return offset; }
        public String getValue() { return value; }
        public String getEncoding() { return encoding; }
        public int getLength() { return length; }
    }

    /**
     * 提取结果
     */
    public static class ExtractResult {
        private List<StringEntry> strings;
        private int totalCount;
        private int asciiCount;
        private int unicodeCount;

        public ExtractResult(List<StringEntry> strings) {
            this.strings = strings;
            this.totalCount = strings.size();
            this.asciiCount = (int) strings.stream().filter(s -> "ascii".equals(s.getEncoding())).count();
            this.unicodeCount = (int) strings.stream().filter(s -> "unicode".equals(s.getEncoding())).count();
        }

        public List<StringEntry> getStrings() { return strings; }
        public int getTotalCount() { return totalCount; }
        public int getAsciiCount() { return asciiCount; }
        public int getUnicodeCount() { return unicodeCount; }
    }

    /**
     * 从二进制数据中提取字符串
     * @param data 二进制数据
     * @param minLength 最小长度
     * @param encodingFilter 过滤编码类型: "all", "ascii", "unicode"
     * @param keyword 关键词过滤（可选）
     */
    public ExtractResult extract(byte[] data, int minLength, String encodingFilter, String keyword) {
        if (minLength <= 0) minLength = DEFAULT_MIN_LENGTH;
        if (encodingFilter == null) encodingFilter = "all";

        List<StringEntry> results = new ArrayList<>();

        if ("all".equals(encodingFilter) || "ascii".equals(encodingFilter)) {
            results.addAll(extractAscii(data, minLength));
        }
        if ("all".equals(encodingFilter) || "unicode".equals(encodingFilter)) {
            results.addAll(extractUnicode(data, minLength));
        }

        // 按偏移排序
        results.sort(Comparator.comparingLong(StringEntry::getOffset));

        // 关键词过滤
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            results.removeIf(e -> !e.getValue().toLowerCase().contains(kw));
        }

        return new ExtractResult(results);
    }

    /** 提取 ASCII 字符串 */
    private List<StringEntry> extractAscii(byte[] data, int minLength) {
        List<StringEntry> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        long startOffset = 0;

        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            if (b >= 0x20 && b < 0x7F) {
                if (sb.isEmpty()) startOffset = i;
                sb.append((char) b);
            } else {
                if (sb.length() >= minLength) {
                    result.add(new StringEntry(startOffset, sb.toString(), "ascii"));
                }
                sb.setLength(0);
            }
        }
        if (sb.length() >= minLength) {
            result.add(new StringEntry(startOffset, sb.toString(), "ascii"));
        }
        return result;
    }

    /** 提取 Unicode (UTF-16LE) 字符串 */
    private List<StringEntry> extractUnicode(byte[] data, int minLength) {
        List<StringEntry> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        long startOffset = 0;

        for (int i = 0; i < data.length - 1; i += 2) {
            char c = (char) ((data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8));
            if (c >= 0x20 && c < 0x7F) {
                if (sb.isEmpty()) startOffset = i;
                sb.append(c);
            } else {
                if (sb.length() >= minLength) {
                    result.add(new StringEntry(startOffset, sb.toString(), "unicode"));
                }
                sb.setLength(0);
            }
        }
        if (sb.length() >= minLength) {
            result.add(new StringEntry(startOffset, sb.toString(), "unicode"));
        }
        return result;
    }
}
