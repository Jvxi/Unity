package com.jvxi.unity.novel.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jvxi.unity.novel.persistence.entity.BookEntity;

public interface BookRepository extends JpaRepository<BookEntity, String> {
    List<BookEntity> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<BookEntity> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);
}

