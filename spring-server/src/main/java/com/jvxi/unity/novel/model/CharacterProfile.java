package com.jvxi.unity.novel.model;

public record CharacterProfile(
    String id,
    String name,
    String role,
    String profile,
    String motivation,
    String constraint,
    String relationships
) {
}

