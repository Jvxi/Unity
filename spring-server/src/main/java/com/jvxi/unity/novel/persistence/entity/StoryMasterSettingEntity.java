package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_story_master_settings")
public class StoryMasterSettingEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "route_json", columnDefinition = "JSON")
    private String routeJson;

    @Column(name = "master_constraints_json", columnDefinition = "JSON")
    private String masterConstraintsJson;

    @Column(name = "base_context_json", columnDefinition = "JSON")
    private String baseContextJson;

    @Column(name = "override_policy_json", columnDefinition = "JSON")
    private String overridePolicyJson;

    @Column(name = "source_trace_json", columnDefinition = "JSON")
    private String sourceTraceJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public StoryMasterSettingEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public StoryMasterSettingEntity(String bookId) {
        this();
        this.bookId = bookId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getRouteJson() { return routeJson; }
    public void setRouteJson(String routeJson) { this.routeJson = routeJson; }

    public String getMasterConstraintsJson() { return masterConstraintsJson; }
    public void setMasterConstraintsJson(String masterConstraintsJson) { this.masterConstraintsJson = masterConstraintsJson; }

    public String getBaseContextJson() { return baseContextJson; }
    public void setBaseContextJson(String baseContextJson) { this.baseContextJson = baseContextJson; }

    public String getOverridePolicyJson() { return overridePolicyJson; }
    public void setOverridePolicyJson(String overridePolicyJson) { this.overridePolicyJson = overridePolicyJson; }

    public String getSourceTraceJson() { return sourceTraceJson; }
    public void setSourceTraceJson(String sourceTraceJson) { this.sourceTraceJson = sourceTraceJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}


