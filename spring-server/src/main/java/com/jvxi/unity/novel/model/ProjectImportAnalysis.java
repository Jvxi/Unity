package com.jvxi.unity.novel.model;

import java.util.List;

public record ProjectImportAnalysis(
    String title,
    String synopsis,
    String premise,
    String tone,
    String targetLength,
    List<String> styleRules,
    List<String> worldRules,
    List<ImportedOutlineNode> outlineNodes,
    List<ImportedChapter> chapters,
    List<ImportedCharacter> characters,
    List<ImportedForeshadowing> foreshadowing
) {
    public static ProjectImportAnalysis empty() {
        return new ProjectImportAnalysis("", "", "", "", "", List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public record ImportedForeshadowing(
        String title,
        String setup,
        String payoff,
        String plannedReveal,
        String status
    ) {
    }
}

