package com.jvxi.unity.novel.model.memory;

import java.util.List;
import java.util.Map;

public record MemoryItem(
    String id,
    String layer,      // "semantic" | "episodic"
    String category,   // character_state, story_fact, world_rule, timeline, open_loop, reader_promise, relationship
    String subject,
    String field,
    String value,
    Map<String, Object> payload,
    String status,     // "active", "outdated", "contradicted", "tentative"
    int sourceChapter,
    List<String> evidence,
    String updatedAt
) {
    public MemoryItem(String category, String subject, String field, String value, int sourceChapter) {
        this(
            "mem-" + category + "-" + System.currentTimeMillis(),
            "semantic",
            category,
            subject,
            field,
            value,
            Map.of(),
            "active",
            sourceChapter,
            List.of(),
            java.time.Instant.now().toString()
        );
    }
}

