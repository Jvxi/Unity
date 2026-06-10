package com.jvxi.unity.novel.model.story;

import java.util.List;
import java.util.Map;

public record MasterSetting(
    ContractMeta meta,
    Map<String, Object> route,
    Map<String, Object> masterConstraints,
    List<Map<String, Object>> baseContext,
    List<Map<String, Object>> sourceTrace,
    Map<String, List<String>> overridePolicy
) {
    public MasterSetting(String genre) {
        this(
            new ContractMeta("master_setting"),
            Map.of("primary_genre", genre),
            Map.of(),
            List.of(),
            List.of(),
            Map.of("locked", List.of(), "append_only", List.of(), "override_allowed", List.of())
        );
    }
}

