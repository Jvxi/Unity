package com.jvxi.unity.novel.model.story;

import java.util.List;
import java.util.Map;

public record VolumeBrief(
    ContractMeta meta,
    Map<String, Object> volumeGoal,
    List<String> selectedTropes,
    Map<String, Object> selectedPacing,
    List<String> selectedScenes,
    List<String> antiPatterns,
    List<String> systemConstraints,
    OverrideBundle overrides
) {
    public VolumeBrief(int volumeNumber) {
        this(
            new ContractMeta("volume_brief"),
            Map.of("volume_number", volumeNumber),
            List.of(),
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            new OverrideBundle()
        );
    }
}

