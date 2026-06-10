package com.jvxi.unity.novel.model.story;

import java.util.Map;

public record OverrideBundle(
    Map<String, Object> locked,
    Map<String, Object> appendOnly,
    Map<String, Object> overrideAllowed
) {
    public OverrideBundle() {
        this(Map.of(), Map.of(), Map.of());
    }
}

