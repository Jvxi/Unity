package com.jvxi.unity.novel.model;

public record BookSummary(
    String id,
    String title,
    String genre,
    String updatedAt,
    int chapterCount,
    boolean onboardingCompleted
) {
}

