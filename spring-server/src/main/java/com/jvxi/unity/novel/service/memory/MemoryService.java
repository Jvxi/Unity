package com.jvxi.unity.novel.service.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.memory.MemoryItem;
import com.jvxi.unity.novel.model.memory.MemoryPack;
import com.jvxi.unity.novel.model.memory.ScratchpadData;
import com.jvxi.unity.novel.persistence.entity.ChapterSummaryEntity;
import com.jvxi.unity.novel.persistence.entity.MemoryItemEntity;
import com.jvxi.unity.novel.persistence.repository.ChapterSummaryRepository;
import com.jvxi.unity.novel.persistence.repository.MemoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private static final Map<String, String> CATEGORY_TO_BUCKET = Map.of(
            "character_state", "character_state",
            "story_fact", "story_facts",
            "world_rule", "world_rules",
            "timeline", "timeline",
            "open_loop", "open_loops",
            "reader_promise", "reader_promises",
            "relationship", "relationships"
    );

    private static final Map<String, List<String>> CATEGORY_KEY_RULES = Map.of(
            "character_state", List.of("subject", "field"),
            "relationship", List.of("subject", "field"),
            "world_rule", List.of("subject", "field"),
            "story_fact", List.of("subject", "field"),
            "timeline", List.of("subject", "source_chapter"),
            "open_loop", List.of("subject"),
            "reader_promise", List.of("subject")
    );

    private static final Map<String, Map<String, Double>> DEFAULT_BUDGET = Map.of(
            "write", Map.of("max_items", 30.0, "working_ratio", 0.45, "episodic_ratio", 0.30, "semantic_ratio", 0.25),
            "review", Map.of("max_items", 40.0, "working_ratio", 0.35, "episodic_ratio", 0.35, "semantic_ratio", 0.30),
            "query", Map.of("max_items", 25.0, "working_ratio", 0.30, "episodic_ratio", 0.45, "semantic_ratio", 0.25)
    );

    private final MemoryItemRepository memoryItemRepository;
    private final ChapterSummaryRepository chapterSummaryRepository;
    private final ObjectMapper objectMapper;

    public MemoryService(
            MemoryItemRepository memoryItemRepository,
            ChapterSummaryRepository chapterSummaryRepository,
            ObjectMapper objectMapper
    ) {
        this.memoryItemRepository = memoryItemRepository;
        this.chapterSummaryRepository = chapterSummaryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 写前注入：构建记忆包
     */
    public MemoryPack buildMemoryPack(String bookId, int chapterNumber, String taskType) {
        Map<String, Double> budget = DEFAULT_BUDGET.getOrDefault(taskType, DEFAULT_BUDGET.get("write"));
        int maxItems = budget.get("max_items").intValue();

        // 1. 工作记忆：当前章节大纲、最近摘要、状态导出
        List<MemoryItem> workingMemory = buildWorkingMemory(bookId, chapterNumber, maxItems, budget.get("working_ratio"));

        // 2. 情景记忆：最近的状态变化、关系、出场
        List<MemoryItem> episodicMemory = buildEpisodicMemory(bookId, chapterNumber, maxItems, budget.get("episodic_ratio"));

        // 3. 语义记忆：长期事实
        List<MemoryItem> semanticMemory = buildSemanticMemory(bookId, chapterNumber, maxItems, budget.get("semantic_ratio"));

        return new MemoryPack(
                workingMemory,
                episodicMemory,
                semanticMemory,
                Map.of(
                        "chapter", chapterNumber,
                        "task_type", taskType,
                        "total_items", workingMemory.size() + episodicMemory.size() + semanticMemory.size()
                )
        );
    }

    /**
     * 写后沉淀：从章节结果提取记忆
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> updateFromChapterResult(String bookId, int chapterNumber, Map<String, Object> result) {
        int itemsAdded = 0;
        List<String> warnings = new ArrayList<>();

        // 1. 处理状态变化
        List<Map<String, Object>> stateChanges = (List<Map<String, Object>>) result.getOrDefault("state_changes", List.of());
        for (Map<String, Object> change : stateChanges) {
            try {
                MemoryItem item = new MemoryItem(
                        "character_state",
                        (String) change.getOrDefault("entity_id", ""),
                        (String) change.getOrDefault("field", ""),
                        (String) change.getOrDefault("new_value", ""),
                        chapterNumber
                );
                Map<String, Object> payload = new HashMap<>(change);
                payload.put("old_value", change.get("old_value"));
                upsertItem(bookId, item, payload);
                itemsAdded++;
            } catch (Exception e) {
                warnings.add("处理状态变化失败: " + e.getMessage());
            }
        }

        // 2. 处理新实体
        List<Map<String, Object>> entitiesNew = (List<Map<String, Object>>) result.getOrDefault("entities_new", List.of());
        for (Map<String, Object> entity : entitiesNew) {
            try {
                MemoryItem item = new MemoryItem(
                        "character_state",
                        (String) entity.getOrDefault("entity_id", ""),
                        "first_seen",
                        (String) entity.getOrDefault("name", ""),
                        chapterNumber
                );
                upsertItem(bookId, item, entity);
                itemsAdded++;
            } catch (Exception e) {
                warnings.add("处理新实体失败: " + e.getMessage());
            }
        }

        // 3. 处理新关系
        List<Map<String, Object>> relationshipsNew = (List<Map<String, Object>>) result.getOrDefault("relationships_new", List.of());
        for (Map<String, Object> rel : relationshipsNew) {
            try {
                MemoryItem item = new MemoryItem(
                        "relationship",
                        (String) rel.getOrDefault("from_entity", ""),
                        (String) rel.getOrDefault("type", ""),
                        (String) rel.getOrDefault("to_entity", ""),
                        chapterNumber
                );
                upsertItem(bookId, item, rel);
                itemsAdded++;
            } catch (Exception e) {
                warnings.add("处理新关系失败: " + e.getMessage());
            }
        }

        // 4. 处理章节钩子
        Map<String, Object> chapterMeta = (Map<String, Object>) result.getOrDefault("chapter_meta", Map.of());
        Map<String, Object> hook = (Map<String, Object>) chapterMeta.getOrDefault("hook", Map.of());
        if (!hook.isEmpty()) {
            try {
                MemoryItem item = new MemoryItem(
                        "story_fact",
                        "chapter_hook",
                        "ch" + chapterNumber,
                        (String) hook.getOrDefault("content", ""),
                        chapterNumber
                );
                upsertItem(bookId, item, hook);
                itemsAdded++;
            } catch (Exception e) {
                warnings.add("处理章节钩子失败: " + e.getMessage());
            }
        }

        log.info("记忆沉淀完成: bookId={}, chapter={}, itemsAdded={}", bookId, chapterNumber, itemsAdded);
        return Map.of("items_added", itemsAdded, "warnings", warnings);
    }

    /**
     * 查询记忆
     */
    public List<MemoryItem> queryMemory(String bookId, String category, String subject) {
        if (category != null && !category.isEmpty()) {
            MemoryItemEntity.MemoryCategory cat = MemoryItemEntity.MemoryCategory.valueOf(category);
            return memoryItemRepository.findActiveByBookIdAndCategory(bookId, cat)
                    .stream()
                    .map(this::toModel)
                    .collect(Collectors.toList());
        } else if (subject != null && !subject.isEmpty()) {
            return memoryItemRepository.findByBookIdAndSubject(bookId, subject)
                    .stream()
                    .map(this::toModel)
                    .collect(Collectors.toList());
        } else {
            return memoryItemRepository.findActiveByBookId(bookId)
                    .stream()
                    .map(this::toModel)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 压缩记忆
     */
    @Transactional
    public Map<String, Object> compactMemory(String bookId) {
        long totalBefore = memoryItemRepository.countByBookId(bookId);

        // 1. 删除旧的outdated项
        memoryItemRepository.deleteOldOutdated(bookId, 50);

        long totalAfter = memoryItemRepository.countByBookId(bookId);

        log.info("记忆压缩完成: bookId={}, before={}, after={}", bookId, totalBefore, totalAfter);
        return Map.of(
                "total_before", totalBefore,
                "total_after", totalAfter,
                "removed", totalBefore - totalAfter
        );
    }

    /**
     * 记忆统计
     */
    public Map<String, Object> getMemoryStats(String bookId) {
        long totalItems = memoryItemRepository.countByBookId(bookId);
        List<MemoryItemEntity> activeItems = memoryItemRepository.findActiveByBookId(bookId);

        Map<String, Long> byCategory = activeItems.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().name(), Collectors.counting()));

        Map<String, Long> byLayer = activeItems.stream()
                .collect(Collectors.groupingBy(e -> e.getLayer().name(), Collectors.counting()));

        return Map.of(
                "total_items", totalItems,
                "active_items", activeItems.size(),
                "by_category", byCategory,
                "by_layer", byLayer
        );
    }

    // ============ 私有辅助方法 ============

    private List<MemoryItem> buildWorkingMemory(String bookId, int chapterNumber, int maxItems, double ratio) {
        int limit = (int) (maxItems * ratio);
        List<MemoryItem> items = new ArrayList<>();

        // 最近3个章节摘要
        List<ChapterSummaryEntity> recentSummaries = chapterSummaryRepository.findRecentByBookIdWithLimit(bookId, 3);
        for (ChapterSummaryEntity summary : recentSummaries) {
            items.add(new MemoryItem(
                    "story_fact",
                    "chapter_summary",
                    "ch" + summary.getChapterNumber(),
                    summary.getSummaryText().substring(0, Math.min(200, summary.getSummaryText().length())),
                    summary.getChapterNumber()
            ));
        }

        // 限制数量
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        return items;
    }

    private List<MemoryItem> buildEpisodicMemory(String bookId, int chapterNumber, int maxItems, double ratio) {
        int limit = (int) (maxItems * ratio);

        // 最近的状态变化
        return memoryItemRepository.findByBookIdAndCategory(bookId, MemoryItemEntity.MemoryCategory.character_state)
                .stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(limit)
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    private List<MemoryItem> buildSemanticMemory(String bookId, int chapterNumber, int maxItems, double ratio) {
        int limit = (int) (maxItems * ratio);

        // 长期事实
        return memoryItemRepository.findByBookIdAndCategory(bookId, MemoryItemEntity.MemoryCategory.story_fact)
                .stream()
                .filter(e -> e.getStatus() == MemoryItemEntity.MemoryStatus.active)
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(limit)
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    private void upsertItem(String bookId, MemoryItem item, Map<String, Object> payload) {
        String dedupKey = generateDedupKey(item);

        // 标记旧项为outdated
        memoryItemRepository.markOutdatedByDedupKey(bookId, dedupKey);

        // 创建新项
        MemoryItemEntity entity = new MemoryItemEntity();
        entity.setBookId(bookId);
        entity.setLayer(MemoryItemEntity.MemoryLayer.semantic);
        entity.setCategory(MemoryItemEntity.MemoryCategory.valueOf(item.category()));
        entity.setSubject(item.subject());
        entity.setField(item.field());
        entity.setValue(item.value());
        entity.setPayloadJson(serialize(payload));
        entity.setStatus(MemoryItemEntity.MemoryStatus.active);
        entity.setSourceChapter(item.sourceChapter());
        entity.setDedupKey(dedupKey);

        memoryItemRepository.save(entity);
    }

    private String generateDedupKey(MemoryItem item) {
        List<String> keyParts = CATEGORY_KEY_RULES.getOrDefault(item.category(), List.of("id"));
        StringBuilder key = new StringBuilder();
        for (String part : keyParts) {
            switch (part) {
                case "subject" -> key.append(item.subject());
                case "field" -> key.append(":").append(item.field());
                case "source_chapter" -> key.append(":").append(item.sourceChapter());
                default -> key.append(item.id());
            }
        }
        return key.toString();
    }

    private MemoryItem toModel(MemoryItemEntity entity) {
        Map<String, Object> payload = Map.of();
        try {
            if (entity.getPayloadJson() != null) {
                payload = objectMapper.readValue(entity.getPayloadJson(), new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.warn("解析payload失败", e);
        }

        List<String> evidence = List.of();
        try {
            if (entity.getEvidenceJson() != null) {
                evidence = objectMapper.readValue(entity.getEvidenceJson(), new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.warn("解析evidence失败", e);
        }

        return new MemoryItem(
                entity.getId(),
                entity.getLayer().name(),
                entity.getCategory().name(),
                entity.getSubject(),
                entity.getField(),
                entity.getValue(),
                payload,
                entity.getStatus().name(),
                entity.getSourceChapter() != null ? entity.getSourceChapter() : 0,
                evidence,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : ""
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }
}

