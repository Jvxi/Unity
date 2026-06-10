package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "novel_user_libraries")
public class UserLibraryEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "active_book_id", nullable = false, length = 36)
    private String activeBookId = "";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActiveBookId() {
        return activeBookId;
    }

    public void setActiveBookId(String activeBookId) {
        this.activeBookId = activeBookId;
    }
}


