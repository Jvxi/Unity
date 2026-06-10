package com.jvxi.unity.novel.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "novel_story_volume_briefs")
public class StoryVolumeBriefEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "book_id", nullable = false, length = 36)
    private String bookId;

    @Column(name = "volume_number", nullable = false)
    private Integer volumeNumber;

    @Column(name = "volume_goal_json", columnDefinition = "JSON")
    private String volumeGoalJson;

    @Column(name = "selected_tropes_json", columnDefinition = "JSON")
    private String selectedTropesJson;

    @Column(name = "selected_pacing_json", columnDefinition = "JSON")
    private String selectedPacingJson;

    @Column(name = "selected_scenes_json", columnDefinition = "JSON")
    private String selectedScenesJson;

    @Column(name = "anti_patterns_json", columnDefinition = "JSON")
    private String antiPatternsJson;

    @Column(name = "system_constraints_json", columnDefinition = "JSON")
    private String systemConstraintsJson;

    @Column(name = "overrides_json", columnDefinition = "JSON")
    private String overridesJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoryVolumeBriefEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public StoryVolumeBriefEntity(String bookId, Integer volumeNumber) {
        this();
        this.bookId = bookId;
        this.volumeNumber = volumeNumber;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Integer getVolumeNumber() { return volumeNumber; }
    public void setVolumeNumber(Integer volumeNumber) { this.volumeNumber = volumeNumber; }

    public String getVolumeGoalJson() { return volumeGoalJson; }
    public void setVolumeGoalJson(String volumeGoalJson) { this.volumeGoalJson = volumeGoalJson; }

    public String getSelectedTropesJson() { return selectedTropesJson; }
    public void setSelectedTropesJson(String selectedTropesJson) { this.selectedTropesJson = selectedTropesJson; }

    public String getSelectedPacingJson() { return selectedPacingJson; }
    public void setSelectedPacingJson(String selectedPacingJson) { this.selectedPacingJson = selectedPacingJson; }

    public String getSelectedScenesJson() { return selectedScenesJson; }
    public void setSelectedScenesJson(String selectedScenesJson) { this.selectedScenesJson = selectedScenesJson; }

    public String getAntiPatternsJson() { return antiPatternsJson; }
    public void setAntiPatternsJson(String antiPatternsJson) { this.antiPatternsJson = antiPatternsJson; }

    public String getSystemConstraintsJson() { return systemConstraintsJson; }
    public void setSystemConstraintsJson(String systemConstraintsJson) { this.systemConstraintsJson = systemConstraintsJson; }

    public String getOverridesJson() { return overridesJson; }
    public void setOverridesJson(String overridesJson) { this.overridesJson = overridesJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}


