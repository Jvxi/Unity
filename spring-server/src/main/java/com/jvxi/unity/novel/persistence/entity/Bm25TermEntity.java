package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "novel_bm25_terms")
public class Bm25TermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "chunk_id", nullable = false, length = 100)
    private String chunkId;

    @Column(nullable = false, length = 50)
    private String term;

    @Column(name = "term_frequency", nullable = false)
    private Integer termFrequency;

    @Column(name = "chunk_text", columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "chapter_number")
    private Integer chapterNumber;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public Integer getTermFrequency() { return termFrequency; }
    public void setTermFrequency(Integer termFrequency) { this.termFrequency = termFrequency; }

    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }
}


