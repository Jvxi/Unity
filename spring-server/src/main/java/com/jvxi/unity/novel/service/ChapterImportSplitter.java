package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从粘贴文本中按章节标题切分正文，避免把整本书正文塞进 AI JSON。 */
public final class ChapterImportSplitter {
    public static final int MAX_CHAPTER_CHARS = 4500;

    private static final Pattern HEADING = Pattern.compile(
        "(?m)^\\s*(?:"
            + "第[\\s]*[0-9０-９一二三四五六七八九十百千万百千]+[\\s]*[章节回幕集]"
            + "|Chapter[\\s]*[0-9０-９]+"
            + "|[0-9０-９]+[\\s]*[、.．][\\s]*[^\\n]{0,48}"
            + ")\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private ChapterImportSplitter() {
    }

    public record LocalChapter(int order, String title, String content) {
    }

    public static List<LocalChapter> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher matcher = HEADING.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            spans.add(new int[] { matcher.start(), matcher.end() });
            titles.add(matcher.group().trim());
        }
        if (spans.isEmpty()) {
            return List.of();
        }

        List<LocalChapter> chapters = new ArrayList<>();
        for (int index = 0; index < spans.size(); index++) {
            int bodyStart = spans.get(index)[1];
            int bodyEnd = index + 1 < spans.size() ? spans.get(index + 1)[0] : text.length();
            String body = text.substring(bodyStart, bodyEnd).trim();
            chapters.add(new LocalChapter(index + 1, titles.get(index), truncateContent(body)));
        }
        return chapters;
    }

    public static String truncateContent(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        if (trimmed.length() <= MAX_CHAPTER_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_CHAPTER_CHARS) + "\n…（已截断）";
    }
}

