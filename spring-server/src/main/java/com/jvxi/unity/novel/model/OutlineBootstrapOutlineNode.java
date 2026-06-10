package com.jvxi.unity.novel.model;

import java.util.List;

public record OutlineBootstrapOutlineNode(
    String title,
    String summary,
    String objective,
    String keyConflict,
    List<String> mustKeep,
    List<String> forbidden
) {
}

