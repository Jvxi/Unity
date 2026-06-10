package com.jvxi.unity.novel.model;

public record ForeshadowingItem(
    String id,
    String title,
    String setup,
    String payoff,
    String plannedReveal,
    String status,
    String notes
) {
}

