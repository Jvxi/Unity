package com.jvxi.unity.novel.model.story;

import java.util.List;
import java.util.Map;

public record ReviewContract(
    ContractMeta meta,
    List<String> mustCheck,
    List<String> blockingRules,
    List<String> genreSpecificRisks,
    List<String> antiPatterns,
    List<String> systemConstraints,
    Map<String, Object> reviewThresholds,
    OverrideBundle overrides
) {
    public ReviewContract(int chapterNumber) {
        this(
            new ContractMeta("review_contract"),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            new OverrideBundle()
        );
    }
}

