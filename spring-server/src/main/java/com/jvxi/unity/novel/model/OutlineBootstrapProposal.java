package com.jvxi.unity.novel.model;

import java.util.List;

public record OutlineBootstrapProposal(
    String id,
    String name,
    String pitch,
    String premise,
    String tone,
    String targetLength,
    List<String> styleRules,
    List<String> worldRules,
    List<OutlineBootstrapOutlineNode> outlineNodes,
    List<OutlineBootstrapCharacter> characters
) {
}

