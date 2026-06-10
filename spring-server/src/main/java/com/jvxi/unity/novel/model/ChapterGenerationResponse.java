package com.jvxi.unity.novel.model;

import java.util.List;

public record ChapterGenerationResponse(
    String chapterId,
    String provider,
    boolean accepted,
    String draft,
    String promptPreview,
    ComplianceReport compliance,
    String rejectionReason,
    List<String> warnings,
    int promptTokens,
    int completionTokens,
    int totalTokens
) {
}

