package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_chapter_reading_power")
public class ChapterReadingPowerEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "hook_type", length = 50)
    private String hookType;

    @Enumerated(EnumType.STRING)
    @Column(name = "hook_strength")
    private HookStrength hookStrength;

    @Column(name = "hook_content", columnDefinition = "TEXT")
    private String hookContent;

    @Column(name = "cool_points_json", columnDefinition = "JSON")
    private String coolPointsJson;

    @Column(name = "micro_payoffs_json", columnDefinition = "JSON")
    private String microPayoffsJson;

    @Column(name = "hard_violations_json", columnDefinition = "JSON")
    private String hardViolationsJson;

    @Column(name = "soft_violations_json", columnDefinition = "JSON")
    private String softViolationsJson;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChapterReadingPowerEntity() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getHookType() { return hookType; }
    public void setHookType(String hookType) { this.hookType = hookType; }

    public HookStrength getHookStrength() { return hookStrength; }
    public void setHookStrength(HookStrength hookStrength) { this.hookStrength = hookStrength; }

    public String getHookContent() { return hookContent; }
    public void setHookContent(String hookContent) { this.hookContent = hookContent; }

    public String getCoolPointsJson() { return coolPointsJson; }
    public void setCoolPointsJson(String coolPointsJson) { this.coolPointsJson = coolPointsJson; }

    public String getMicroPayoffsJson() { return microPayoffsJson; }
    public void setMicroPayoffsJson(String microPayoffsJson) { this.microPayoffsJson = microPayoffsJson; }

    public String getHardViolationsJson() { return hardViolationsJson; }
    public void setHardViolationsJson(String hardViolationsJson) { this.hardViolationsJson = hardViolationsJson; }

    public String getSoftViolationsJson() { return softViolationsJson; }
    public void setSoftViolationsJson(String softViolationsJson) { this.softViolationsJson = softViolationsJson; }

    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum HookStrength {
        strong, medium, weak
    }
}


