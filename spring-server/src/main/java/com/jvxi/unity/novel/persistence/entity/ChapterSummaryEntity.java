package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_chapter_summaries")
public class ChapterSummaryEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "hook_json", columnDefinition = "JSON")
    private String hookJson;

    @Column(name = "pattern_json", columnDefinition = "JSON")
    private String patternJson;

    @Column(name = "ending_json", columnDefinition = "JSON")
    private String endingJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChapterSummaryEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public ChapterSummaryEntity(String bookId, Integer chapterNumber, String summaryText) {
        this();
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.summaryText = summaryText;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public String getHookJson() { return hookJson; }
    public void setHookJson(String hookJson) { this.hookJson = hookJson; }

    public String getPatternJson() { return patternJson; }
    public void setPatternJson(String patternJson) { this.patternJson = patternJson; }

    public String getEndingJson() { return endingJson; }
    public void setEndingJson(String endingJson) { this.endingJson = endingJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}


