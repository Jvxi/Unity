package com.jvxi.unity.novel.model.commit;

import java.util.Map;

public record StoryEvent(
    String id,
    int chapterNumber,
    String eventType,
    String subject,
    String field,
    String oldValue,
    String newValue,
    String reason,
    Map<String, Object> payload
) {
    public StoryEvent(String eventType, String subject, String field, String oldValue, String newValue, int chapterNumber) {
        this(
            "event-" + System.currentTimeMillis(),
            chapterNumber,
            eventType,
            subject,
            field,
            oldValue,
            newValue,
            "",
            Map.of()
        );
    }
}

