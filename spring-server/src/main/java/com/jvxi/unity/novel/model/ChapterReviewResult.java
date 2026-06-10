package com.jvxi.unity.novel.model;

import java.util.List;

public record ChapterReviewResult(
    String chapterId,
    String chapterTitle,
    List<ReviewIssue> issues
) {}

