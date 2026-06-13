package com.jvxi.unity.repository;

import com.jvxi.unity.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSession> findByUserIdAndTargetUserId(Long userId, Long targetUserId);

    Optional<ChatSession> findByUserIdAndTargetGroupId(Long userId, Long targetGroupId);

    @Modifying
    void deleteByTargetGroupId(Long targetGroupId);

    @Modifying
    void deleteByUserIdAndTargetGroupId(Long userId, Long targetGroupId);

    @Modifying
    @Query("UPDATE ChatSession s SET s.unreadCount = 0 WHERE s.userId = :userId AND s.targetUserId = :targetUserId")
    void clearPrivateUnread(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Modifying
    @Query("UPDATE ChatSession s SET s.unreadCount = 0 WHERE s.userId = :userId AND s.targetGroupId = :groupId")
    void clearGroupUnread(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE ChatSession s SET s.unreadCount = s.unreadCount + 1, s.lastMessageId = :messageId, s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.userId = :userId AND ((s.targetUserId = :targetUserId AND :targetUserId IS NOT NULL) OR (s.targetGroupId = :groupId AND :groupId IS NOT NULL))")
    int incrementUnread(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId, @Param("groupId") Long groupId, @Param("messageId") Long messageId);
}
