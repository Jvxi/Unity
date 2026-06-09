package com.jvxi.unity.repository;

import com.jvxi.unity.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "((m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1)) " +
           "AND m.groupId IS NULL ORDER BY m.createdAt ASC")
    List<ChatMessage> findPrivateMessages(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "((m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1)) " +
           "AND m.groupId IS NULL ORDER BY m.createdAt DESC")
    Page<ChatMessage> findPrivateMessagesDesc(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);

    List<ChatMessage> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    List<ChatMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.groupId = :groupId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchGroupMessages(@Param("groupId") Long groupId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "((m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1)) " +
           "AND m.groupId IS NULL AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchPrivateMessages(@Param("user1") Long user1, @Param("user2") Long user2, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiverId = :userId AND m.groupId IS NULL AND m.createdAt > :since")
    long countUnreadPrivateSince(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);

    @Query(value = "SELECT * FROM chat_messages WHERE group_id = :groupId AND id > :lastReadId ORDER BY created_at ASC", nativeQuery = true)
    List<ChatMessage> findNewGroupMessages(@Param("groupId") Long groupId, @Param("lastReadId") Long lastReadId);
}