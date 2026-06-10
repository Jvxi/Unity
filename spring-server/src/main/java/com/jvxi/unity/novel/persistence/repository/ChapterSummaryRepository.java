package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.ChapterSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterSummaryRepository extends JpaRepository<ChapterSummaryEntity, String> {

    List<ChapterSummaryEntity> findByBookId(String bookId);

    Optional<ChapterSummaryEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    @Query("SELECT cs FROM ChapterSummaryEntity cs WHERE cs.bookId = :bookId ORDER BY cs.chapterNumber DESC")
    List<ChapterSummaryEntity> findRecentByBookId(@Param("bookId") String bookId);

    @Query(value = "SELECT * FROM novel_chapter_summaries WHERE book_id = :bookId ORDER BY chapter_number DESC LIMIT :limit", nativeQuery = true)
    List<ChapterSummaryEntity> findRecentByBookIdWithLimit(@Param("bookId") String bookId, @Param("limit") int limit);

    void deleteByBookId(String bookId);
}

