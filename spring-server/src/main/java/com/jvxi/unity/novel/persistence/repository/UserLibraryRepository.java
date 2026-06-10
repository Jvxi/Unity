package com.jvxi.unity.novel.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jvxi.unity.novel.persistence.entity.UserLibraryEntity;

public interface UserLibraryRepository extends JpaRepository<UserLibraryEntity, String> {
}

