package com.jvxi.unity.novel.model.commit;

import java.util.List;
import java.util.Map;

public record ChapterCommit(
    String id,
    int chapterNumber,
    String status,  // "accepted", "rejected"
    Map<String, Object> contractRefs,
    Map<String, Object> outlineSnapshot,
    Map<String, Object> reviewResult,
    Map<String, Object> fulfillmentResult,
    Map<String, Object> disambiguationResult,
    String summaryText,
    String dominantStrand,
    Map<String, String> projectionStatus
) {
    public ChapterCommit(int chapterNumber) {
        this(
            "commit-" + chapterNumber,
            chapterNumber,
            "pending",
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            "",
            "",
            Map.of()
        );
    }
}

