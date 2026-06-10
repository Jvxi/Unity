package com.jvxi.unity.novel.model.memory;

import java.util.List;
import java.util.Map;

public record MemoryPack(
    List<MemoryItem> workingMemory,
    List<MemoryItem> episodicMemory,
    List<MemoryItem> semanticMemory,
    Map<String, Object> metadata
) {
    public MemoryPack() {
        this(List.of(), List.of(), List.of(), Map.of());
    }

    public List<MemoryItem> all() {
        List<MemoryItem> all = new java.util.ArrayList<>();
        all.addAll(workingMemory);
        all.addAll(episodicMemory);
        all.addAll(semanticMemory);
        return all;
    }
}

