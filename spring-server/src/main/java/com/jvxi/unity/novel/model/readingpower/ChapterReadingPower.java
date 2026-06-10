package com.jvxi.unity.novel.model.readingpower;

import java.util.List;
import java.util.Map;

public record ChapterReadingPower(
    String hookType,
    String hookStrength,  // "strong", "medium", "weak"
    String hookContent,
    List<Map<String, Object>> coolPoints,
    List<Map<String, Object>> microPayoffs,
    List<Map<String, Object>> hardViolations,
    List<Map<String, Object>> softViolations,
    double overallScore
) {
    public ChapterReadingPower() {
        this(null, null, null, List.of(), List.of(), List.of(), List.of(), 0.0);
    }
}

