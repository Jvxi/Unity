package com.jvxi.unity.novel.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectMeta(
    String title,
    @JsonProperty(defaultValue = "") String synopsis,
    String genre,
    String premise,
    String tone,
    String targetLength,
    List<String> styleRules,
    List<String> worldRules,
    boolean strictMode,
    String publishPlatform,
    String audienceChannel,
    String novelType
) {
}

