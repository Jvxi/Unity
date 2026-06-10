package com.jvxi.unity.novel.model.readingpower;

public record ChaseDebt(
    String id,
    String debtType,  // "promise", "open_loop", "foreshadowing"
    String subject,
    String description,
    int createdChapter,
    int urgency,      // 0-100
    String status,    // "pending", "paid_off", "abandoned"
    Integer resolvedChapter,
    String resolvedReason
) {
    public ChaseDebt(String debtType, String subject, String description, int createdChapter, int urgency) {
        this(
            "debt-" + System.currentTimeMillis(),
            debtType,
            subject,
            description,
            createdChapter,
            urgency,
            "pending",
            null,
            null
        );
    }
}

