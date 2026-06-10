package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.ChapterCommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterCommitRepository extends JpaRepository<ChapterCommitEntity, String> {

    List<ChapterCommitEntity> findByBookId(String bookId);

    Optional<ChapterCommitEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    void deleteByBookId(String bookId);
}

