package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.StoryChapterBriefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryChapterBriefRepository extends JpaRepository<StoryChapterBriefEntity, String> {

    List<StoryChapterBriefEntity> findByBookId(String bookId);

    Optional<StoryChapterBriefEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    void deleteByBookId(String bookId);
}

