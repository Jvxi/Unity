package com.jvxi.unity.novel.persistence.repository;

import com.jvxi.unity.novel.persistence.entity.MemoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoryItemRepository extends JpaRepository<MemoryItemEntity, String> {

    List<MemoryItemEntity> findByBookId(String bookId);

    List<MemoryItemEntity> findByBookIdAndCategory(String bookId, MemoryItemEntity.MemoryCategory category);

    List<MemoryItemEntity> findByBookIdAndSubject(String bookId, String subject);

    List<MemoryItemEntity> findByBookIdAndStatus(String bookId, MemoryItemEntity.MemoryStatus status);

    Optional<MemoryItemEntity> findByBookIdAndDedupKey(String bookId, String dedupKey);

    @Query("SELECT m FROM MemoryItemEntity m WHERE m.bookId = :bookId AND m.status = 'active' ORDER BY m.updatedAt DESC")
    List<MemoryItemEntity> findActiveByBookId(@Param("bookId") String bookId);

    @Query("SELECT m FROM MemoryItemEntity m WHERE m.bookId = :bookId AND m.category = :category AND m.status = 'active' ORDER BY m.updatedAt DESC")
    List<MemoryItemEntity> findActiveByBookIdAndCategory(@Param("bookId") String bookId, @Param("category") MemoryItemEntity.MemoryCategory category);

    @Query("SELECT COUNT(m) FROM MemoryItemEntity m WHERE m.bookId = :bookId")
    long countByBookId(@Param("bookId") String bookId);

    @Modifying
    @Query("UPDATE MemoryItemEntity m SET m.status = 'outdated' WHERE m.bookId = :bookId AND m.dedupKey = :dedupKey AND m.status = 'active'")
    void markOutdatedByDedupKey(@Param("bookId") String bookId, @Param("dedupKey") String dedupKey);

    @Modifying
    @Query("DELETE FROM MemoryItemEntity m WHERE m.bookId = :bookId AND m.status = 'outdated' AND m.sourceChapter < :chapterThreshold")
    void deleteOldOutdated(@Param("bookId") String bookId, @Param("chapterThreshold") int chapterThreshold);

    void deleteByBookId(String bookId);
}

