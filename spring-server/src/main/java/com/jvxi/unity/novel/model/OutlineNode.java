package com.jvxi.unity.novel.model;

import java.util.List;

public record OutlineNode(
    String id,
    int order,
    String title,
    String summary,
    String objective,
    String keyConflict,
    List<String> mustKeep,
    List<String> forbidden
) {
}

