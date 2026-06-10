package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.StoryReviewContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryReviewContractRepository extends JpaRepository<StoryReviewContractEntity, String> {

    List<StoryReviewContractEntity> findByBookId(String bookId);

    Optional<StoryReviewContractEntity> findByBookIdAndChapterNumber(String bookId, Integer chapterNumber);

    void deleteByBookId(String bookId);
}

