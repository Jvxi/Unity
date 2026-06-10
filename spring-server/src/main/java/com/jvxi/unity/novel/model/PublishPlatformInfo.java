package com.jvxi.unity.novel.model;

import java.util.List;

public record PublishPlatformInfo(
    String id,
    String label,
    String description,
    List<String> writingRules
) {
}

