package com.jvxi.unity.novel.model.story;

import java.util.List;
import java.util.Map;

public record ContractMeta(
    String schemaVersion,
    String contractType,
    String generatorVersion,
    List<Map<String, Object>> sourceTrace
) {
    public ContractMeta(String contractType) {
        this("story-system/v1", contractType, "phase2", List.of());
    }
}

