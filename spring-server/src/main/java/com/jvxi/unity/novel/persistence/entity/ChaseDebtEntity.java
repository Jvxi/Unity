package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_chase_debts")
public class ChaseDebtEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_type", nullable = false)
    private DebtType debtType;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_chapter", nullable = false)
    private Integer createdChapter;

    @Column(nullable = false)
    private Integer urgency = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtStatus status = DebtStatus.pending;

    @Column(name = "resolved_chapter")
    private Integer resolvedChapter;

    @Column(name = "resolved_reason", columnDefinition = "TEXT")
    private String resolvedReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ChaseDebtEntity() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public DebtType getDebtType() { return debtType; }
    public void setDebtType(DebtType debtType) { this.debtType = debtType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCreatedChapter() { return createdChapter; }
    public void setCreatedChapter(Integer createdChapter) { this.createdChapter = createdChapter; }

    public Integer getUrgency() { return urgency; }
    public void setUrgency(Integer urgency) { this.urgency = urgency; }

    public DebtStatus getStatus() { return status; }
    public void setStatus(DebtStatus status) { this.status = status; }

    public Integer getResolvedChapter() { return resolvedChapter; }
    public void setResolvedChapter(Integer resolvedChapter) { this.resolvedChapter = resolvedChapter; }

    public String getResolvedReason() { return resolvedReason; }
    public void setResolvedReason(String resolvedReason) { this.resolvedReason = resolvedReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum DebtType {
        promise, open_loop, foreshadowing
    }

    public enum DebtStatus {
        pending, paid_off, abandoned
    }
}


