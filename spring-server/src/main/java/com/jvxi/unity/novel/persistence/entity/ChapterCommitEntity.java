package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_chapter_commits")
public class ChapterCommitEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitStatus status;

    @Column(name = "contract_refs_json", columnDefinition = "JSON")
    private String contractRefsJson;

    @Column(name = "outline_snapshot_json", columnDefinition = "JSON")
    private String outlineSnapshotJson;

    @Column(name = "review_result_json", columnDefinition = "JSON")
    private String reviewResultJson;

    @Column(name = "fulfillment_result_json", columnDefinition = "JSON")
    private String fulfillmentResultJson;

    @Column(name = "disambiguation_result_json", columnDefinition = "JSON")
    private String disambiguationResultJson;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "dominant_strand", length = 50)
    private String dominantStrand;

    @Column(name = "projection_status_json", columnDefinition = "JSON")
    private String projectionStatusJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChapterCommitEntity() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public CommitStatus getStatus() { return status; }
    public void setStatus(CommitStatus status) { this.status = status; }

    public String getContractRefsJson() { return contractRefsJson; }
    public void setContractRefsJson(String contractRefsJson) { this.contractRefsJson = contractRefsJson; }

    public String getOutlineSnapshotJson() { return outlineSnapshotJson; }
    public void setOutlineSnapshotJson(String outlineSnapshotJson) { this.outlineSnapshotJson = outlineSnapshotJson; }

    public String getReviewResultJson() { return reviewResultJson; }
    public void setReviewResultJson(String reviewResultJson) { this.reviewResultJson = reviewResultJson; }

    public String getFulfillmentResultJson() { return fulfillmentResultJson; }
    public void setFulfillmentResultJson(String fulfillmentResultJson) { this.fulfillmentResultJson = fulfillmentResultJson; }

    public String getDisambiguationResultJson() { return disambiguationResultJson; }
    public void setDisambiguationResultJson(String disambiguationResultJson) { this.disambiguationResultJson = disambiguationResultJson; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public String getDominantStrand() { return dominantStrand; }
    public void setDominantStrand(String dominantStrand) { this.dominantStrand = dominantStrand; }

    public String getProjectionStatusJson() { return projectionStatusJson; }
    public void setProjectionStatusJson(String projectionStatusJson) { this.projectionStatusJson = projectionStatusJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum CommitStatus {
        accepted, rejected
    }
}


