package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "novel_books")
public class BookEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 255)
    private String title = "";

    @Column(nullable = false, length = 128)
    private String genre = "";

    @Column(name = "updated_at", nullable = false, length = 30)
    private String updatedAt;

    @Column(name = "chapter_count", nullable = false)
    private int chapterCount;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Lob
    @Column(name = "project_json", nullable = false, columnDefinition = "LONGTEXT")
    private String projectJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public String getProjectJson() {
        return projectJson;
    }

    public void setProjectJson(String projectJson) {
        this.projectJson = projectJson;
    }
}


