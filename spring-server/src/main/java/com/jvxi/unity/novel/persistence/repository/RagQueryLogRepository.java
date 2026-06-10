package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.RagQueryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagQueryLogRepository extends JpaRepository<RagQueryLogEntity, Long> {

    List<RagQueryLogEntity> findByBookId(String bookId);

    List<RagQueryLogEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    void deleteByBookId(String bookId);
}

