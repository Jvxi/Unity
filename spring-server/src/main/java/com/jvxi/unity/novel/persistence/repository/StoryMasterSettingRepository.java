package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.StoryMasterSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryMasterSettingRepository extends JpaRepository<StoryMasterSettingEntity, String> {

    Optional<StoryMasterSettingEntity> findByBookId(String bookId);

    void deleteByBookId(String bookId);
}

