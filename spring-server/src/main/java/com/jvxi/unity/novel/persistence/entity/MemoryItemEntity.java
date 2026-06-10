package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_memory_items")
public class MemoryItemEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryLayer layer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryCategory category;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 200)
    private String field;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryStatus status = MemoryStatus.active;

    @Column(name = "source_chapter")
    private Integer sourceChapter;

    @Column(name = "evidence_json", columnDefinition = "JSON")
    private String evidenceJson;

    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public MemoryItemEntity() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public MemoryLayer getLayer() { return layer; }
    public void setLayer(MemoryLayer layer) { this.layer = layer; }

    public MemoryCategory getCategory() { return category; }
    public void setCategory(MemoryCategory category) { this.category = category; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public MemoryStatus getStatus() { return status; }
    public void setStatus(MemoryStatus status) { this.status = status; }

    public Integer getSourceChapter() { return sourceChapter; }
    public void setSourceChapter(Integer sourceChapter) { this.sourceChapter = sourceChapter; }

    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 枚举类型
    public enum MemoryLayer {
        semantic, episodic
    }

    public enum MemoryCategory {
        character_state,
        story_fact,
        world_rule,
        timeline,
        open_loop,
        reader_promise,
        relationship
    }

    public enum MemoryStatus {
        active, outdated, contradicted, tentative
    }
}


