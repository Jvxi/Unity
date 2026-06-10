package com.jvxi.unity.novel.model;

import java.util.List;

public record Project(
    ProjectMeta meta,
    AiSettings aiSettings,
    OnboardingState onboarding,
    List<OutlineNode> outlineNodes,
    List<CharacterProfile> characters,
    List<ForeshadowingItem> foreshadowing,
    List<Chapter> chapters,
    String updatedAt
) {
}

