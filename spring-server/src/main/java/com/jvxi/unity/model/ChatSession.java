package com.jvxi.unity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "target_user_id", "target_group_id"})
})
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_group_id")
    private Long targetGroupId;

    private Long lastMessageId;

    @Column(nullable = false)
    private int unreadCount = 0;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public Long getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(Long targetGroupId) { this.targetGroupId = targetGroupId; }

    public Long getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(Long lastMessageId) { this.lastMessageId = lastMessageId; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
