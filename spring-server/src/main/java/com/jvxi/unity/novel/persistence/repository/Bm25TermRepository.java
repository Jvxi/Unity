package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.Bm25TermEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Bm25TermRepository extends JpaRepository<Bm25TermEntity, Long> {

    List<Bm25TermEntity> findByBookIdAndTerm(String bookId, String term);

    List<Bm25TermEntity> findByBookIdAndChunkId(String bookId, String chunkId);

    @Query("SELECT DISTINCT b.term FROM Bm25TermEntity b WHERE b.bookId = :bookId")
    List<String> findDistinctTermsByBookId(@Param("bookId") String bookId);

    @Query("SELECT COUNT(DISTINCT b.chunkId) FROM Bm25TermEntity b WHERE b.bookId = :bookId")
    long countDistinctChunksByBookId(@Param("bookId") String bookId);

    void deleteByBookId(String bookId);
}

