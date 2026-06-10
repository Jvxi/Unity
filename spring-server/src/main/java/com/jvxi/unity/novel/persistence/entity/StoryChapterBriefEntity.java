package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_story_chapter_briefs")
public class StoryChapterBriefEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "chapter_directive_json", columnDefinition = "JSON")
    private String chapterDirectiveJson;

    @Column(name = "dynamic_context_json", columnDefinition = "JSON")
    private String dynamicContextJson;

    @Column(name = "reasoning_json", columnDefinition = "JSON")
    private String reasoningJson;

    @Column(name = "override_allowed_json", columnDefinition = "JSON")
    private String overrideAllowedJson;

    @Column(name = "source_trace_json", columnDefinition = "JSON")
    private String sourceTraceJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoryChapterBriefEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public StoryChapterBriefEntity(String bookId, Integer chapterNumber) {
        this();
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getChapterDirectiveJson() { return chapterDirectiveJson; }
    public void setChapterDirectiveJson(String chapterDirectiveJson) { this.chapterDirectiveJson = chapterDirectiveJson; }

    public String getDynamicContextJson() { return dynamicContextJson; }
    public void setDynamicContextJson(String dynamicContextJson) { this.dynamicContextJson = dynamicContextJson; }

    public String getReasoningJson() { return reasoningJson; }
    public void setReasoningJson(String reasoningJson) { this.reasoningJson = reasoningJson; }

    public String getOverrideAllowedJson() { return overrideAllowedJson; }
    public void setOverrideAllowedJson(String overrideAllowedJson) { this.overrideAllowedJson = overrideAllowedJson; }

    public String getSourceTraceJson() { return sourceTraceJson; }
    public void setSourceTraceJson(String sourceTraceJson) { this.sourceTraceJson = sourceTraceJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}


