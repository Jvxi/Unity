package com.jvxi.unity.novel.model.rag;

public record RagResult(
    String chunkId,
    String chunkText,
    double score,
    double rrfScore,
    Integer chapterNumber,
    String source
) {
    public RagResult(String chunkId, String chunkText, double score) {
        this(chunkId, chunkText, score, 0.0, null, "unknown");
    }
}

