package com.jvxi.unity.novel.model.story;

import java.util.List;
import java.util.Map;

public record ChapterBrief(
    ContractMeta meta,
    Map<String, Object> overrideAllowed,
    Map<String, Object> chapterDirective,
    List<Map<String, Object>> dynamicContext,
    List<Map<String, Object>> sourceTrace,
    Map<String, Object> reasoning
) {
    public ChapterBrief(int chapterNumber) {
        this(
            new ContractMeta("chapter_brief"),
            Map.of(),
            Map.of("chapter_number", chapterNumber),
            List.of(),
            List.of(),
            Map.of()
        );
    }
}

