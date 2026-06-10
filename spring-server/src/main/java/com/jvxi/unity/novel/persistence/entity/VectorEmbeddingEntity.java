package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_vector_embeddings")
public class VectorEmbeddingEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chunk_id", nullable = false, length = 100)
    private String chunkId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "embedding", nullable = false, columnDefinition = "BLOB")
    private byte[] embedding;

    @Column(name = "entity_names_json", columnDefinition = "JSON")
    private String entityNamesJson;

    @Column(name = "chapter_number")
    private Integer chapterNumber;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public VectorEmbeddingEntity() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }

    public byte[] getEmbedding() { return embedding; }
    public void setEmbedding(byte[] embedding) { this.embedding = embedding; }

    public String getEntityNamesJson() { return entityNamesJson; }
    public void setEntityNamesJson(String entityNamesJson) { this.entityNamesJson = entityNamesJson; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}


