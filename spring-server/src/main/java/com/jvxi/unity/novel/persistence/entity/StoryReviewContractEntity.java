package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_story_review_contracts")
public class StoryReviewContractEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "must_check_json", columnDefinition = "JSON")
    private String mustCheckJson;

    @Column(name = "blocking_rules_json", columnDefinition = "JSON")
    private String blockingRulesJson;

    @Column(name = "genre_specific_risks_json", columnDefinition = "JSON")
    private String genreSpecificRisksJson;

    @Column(name = "anti_patterns_json", columnDefinition = "JSON")
    private String antiPatternsJson;

    @Column(name = "review_thresholds_json", columnDefinition = "JSON")
    private String reviewThresholdsJson;

    @Column(name = "overrides_json", columnDefinition = "JSON")
    private String overridesJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoryReviewContractEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public StoryReviewContractEntity(String bookId, Integer chapterNumber) {
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

    public String getMustCheckJson() { return mustCheckJson; }
    public void setMustCheckJson(String mustCheckJson) { this.mustCheckJson = mustCheckJson; }

    public String getBlockingRulesJson() { return blockingRulesJson; }
    public void setBlockingRulesJson(String blockingRulesJson) { this.blockingRulesJson = blockingRulesJson; }

    public String getGenreSpecificRisksJson() { return genreSpecificRisksJson; }
    public void setGenreSpecificRisksJson(String genreSpecificRisksJson) { this.genreSpecificRisksJson = genreSpecificRisksJson; }

    public String getAntiPatternsJson() { return antiPatternsJson; }
    public void setAntiPatternsJson(String antiPatternsJson) { this.antiPatternsJson = antiPatternsJson; }

    public String getReviewThresholdsJson() { return reviewThresholdsJson; }
    public void setReviewThresholdsJson(String reviewThresholdsJson) { this.reviewThresholdsJson = reviewThresholdsJson; }

    public String getOverridesJson() { return overridesJson; }
    public void setOverridesJson(String overridesJson) { this.overridesJson = overridesJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}


