package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.VectorEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorEmbeddingRepository extends JpaRepository<VectorEmbeddingEntity, String> {

    List<VectorEmbeddingEntity> findByBookId(String bookId);

    Optional<VectorEmbeddingEntity> findByBookIdAndChunkId(String bookId, String chunkId);

    List<VectorEmbeddingEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    @Query("SELECT COUNT(v) FROM VectorEmbeddingEntity v WHERE v.bookId = :bookId")
    long countByBookId(@Param("bookId") String bookId);

    void deleteByBookId(String bookId);
}

