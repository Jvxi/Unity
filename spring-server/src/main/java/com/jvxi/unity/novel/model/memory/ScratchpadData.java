package com.jvxi.unity.novel.model.memory;

import java.util.List;
import java.util.Map;

public record ScratchpadData(
    List<MemoryItem> characterState,
    List<MemoryItem> storyFacts,
    List<MemoryItem> worldRules,
    List<MemoryItem> timeline,
    List<MemoryItem> openLoops,
    List<MemoryItem> readerPromises,
    List<MemoryItem> relationships,
    Map<String, Object> meta
) {
    public ScratchpadData() {
        this(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    public List<MemoryItem> getByCategory(String category) {
        return switch (category) {
            case "character_state" -> characterState;
            case "story_fact" -> storyFacts;
            case "world_rule" -> worldRules;
            case "timeline" -> timeline;
            case "open_loop" -> openLoops;
            case "reader_promise" -> readerPromises;
            case "relationship" -> relationships;
            default -> List.of();
        };
    }
}

