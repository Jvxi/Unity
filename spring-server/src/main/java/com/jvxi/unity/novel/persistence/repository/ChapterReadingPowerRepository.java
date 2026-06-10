package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.ChapterReadingPowerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterReadingPowerRepository extends JpaRepository<ChapterReadingPowerEntity, String> {

    List<ChapterReadingPowerEntity> findByBookId(String bookId);

    Optional<ChapterReadingPowerEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    void deleteByBookId(String bookId);
}

