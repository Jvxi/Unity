package com.jvxi.unity.novel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.memory.MemoryItem;
import com.jvxi.unity.novel.model.memory.MemoryPack;
import com.jvxi.unity.novel.model.rag.RagResult;
import com.jvxi.unity.novel.service.PromptBuilder.GenerationContext;
import com.jvxi.unity.novel.service.context.ContextService;
import com.jvxi.unity.novel.service.rag.RagService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

@Service
public class PromptContextAugmenter {
    private static final int MAX_MEMORY_ITEMS = 12;
    private static final int MAX_RAG_RESULTS = 5;
    private static final int MAX_RAG_TEXT_CHARS = 260;
    private static final int MAX_JSON_CHARS = 1200;

    private final ContextService contextService;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public PromptContextAugmenter(
        ContextService contextService,
        RagService ragService,
        ObjectMapper objectMapper
    ) {
        this.contextService = contextService;
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    public PromptAugmentation augment(String bookId, Project project, GenerationContext generationContext) {
        List<String> warnings = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        Map<String, Object> assembledContext = Map.of();
        List<RagResult> ragResults = List.of();

        try {
            assembledContext = contextService.assembleContext(bookId, generationContext.chapter().order(), "write");
        } catch (Exception exception) {
            warnings.add("知识增强提示包加载失败：" + safeMessage(exception));
        }

        try {
            String ragQuery = buildRagQuery(project, generationContext);
            ragResults = ragQuery.isBlank()
                ? List.of()
                : ragService.hybridSearch(bookId, ragQuery, MAX_RAG_RESULTS, null);
        } catch (Exception exception) {
            warnings.add("RAG 检索失败：" + safeMessage(exception));
        }

        appendStoryContracts(lines, assembledContext);
        appendMemoryPack(lines, assembledContext.get("memory_pack"));
        appendRagResults(lines, ragResults);
        appendWritingGuidance(lines, assembledContext.get("writing_guidance"));
        appendWritingChecklist(lines, assembledContext.get("writing_checklist"));

        if (lines.isEmpty()) {
            return new PromptAugmentation("", warnings);
        }

        List<String> wrapped = new ArrayList<>();
        wrapped.add("");
        wrapped.add("知识增强提示包：");
        wrapped.add("- 以下内容只用于保持连贯、约束和风格方向，必须自然融入正文；不要直接输出本提示包、字段名、JSON 或清单。");
        wrapped.addAll(lines);
        return new PromptAugmentation(String.join("\n", wrapped), warnings);
    }

    private void appendStoryContracts(List<String> lines, Map<String, Object> context) {
        Object storyContracts = context.get("story_contracts");
        if (storyContracts instanceof Map<?, ?> contracts) {
            Object master = contracts.get("master");
            if (master != null) {
                lines.add("");
                lines.add("故事合同摘要：");
                lines.add("- 主设定：" + toCompactJson(master, MAX_JSON_CHARS));
            }
        }

        Object chapterBrief = context.get("chapter_brief");
        if (chapterBrief != null) {
            if (lines.stream().noneMatch("故事合同摘要："::equals)) {
                lines.add("");
                lines.add("故事合同摘要：");
            }
            lines.add("- 本章合同：" + toCompactJson(chapterBrief, MAX_JSON_CHARS));
        }
    }

    private void appendMemoryPack(List<String> lines, Object memoryPackValue) {
        if (!(memoryPackValue instanceof MemoryPack memoryPack)) {
            return;
        }

        List<MemoryItem> items = memoryPack.all().stream()
            .filter(Objects::nonNull)
            .filter(item -> !text(item.value()).isBlank())
            .limit(MAX_MEMORY_ITEMS)
            .toList();
        if (items.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("记忆包：");
        for (MemoryItem item : items) {
            StringJoiner joiner = new StringJoiner(" / ");
            addIfPresent(joiner, item.category());
            addIfPresent(joiner, item.subject());
            addIfPresent(joiner, item.field());

            String prefix = joiner.length() == 0 ? "记忆" : joiner.toString();
            String chapter = item.sourceChapter() > 0 ? "（来源第" + item.sourceChapter() + "章）" : "";
            lines.add("- " + prefix + "：" + truncate(text(item.value()), 180) + chapter);
        }
    }

    private void appendRagResults(List<String> lines, List<RagResult> ragResults) {
        if (ragResults == null || ragResults.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("RAG 命中内容：");
        ragResults.stream()
            .filter(Objects::nonNull)
            .limit(MAX_RAG_RESULTS)
            .forEach(result -> {
                String chapter = result.chapterNumber() == null ? "未知章节" : "第" + result.chapterNumber() + "章";
                String source = text(result.source()).isBlank() ? "rag" : result.source();
                lines.add("- " + chapter + " / " + source + "：" + truncate(text(result.chunkText()), MAX_RAG_TEXT_CHARS));
            });
    }

    private void appendWritingGuidance(List<String> lines, Object guidanceValue) {
        if (!(guidanceValue instanceof Map<?, ?> guidance) || guidance.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("题材写法卡：");
        appendMapValue(lines, guidance, "focus", "重点");
        appendMapValue(lines, guidance, "pacing", "节奏");
        appendMapValue(lines, guidance, "key_elements", "关键元素");
        appendMapValue(lines, guidance, "avoid", "避免");
    }

    private void appendWritingChecklist(List<String> lines, Object checklistValue) {
        if (!(checklistValue instanceof Map<?, ?> checklist) || checklist.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("写作检查清单：");
        appendMapValue(lines, checklist, "pre_write", "写前");
        appendMapValue(lines, checklist, "during_write", "写中");
        appendMapValue(lines, checklist, "post_write", "写后");
    }

    private void appendMapValue(List<String> lines, Map<?, ?> map, String key, String label) {
        Object value = map.get(key);
        String normalized = stringifyValue(value);
        if (!normalized.isBlank()) {
            lines.add("- " + label + "：" + truncate(normalized, 260));
        }
    }

    private String buildRagQuery(Project project, GenerationContext context) {
        Chapter chapter = context.chapter();
        return Stream.concat(
                Stream.of(
                    project.meta() == null ? "" : project.meta().title(),
                    project.meta() == null ? "" : project.meta().synopsis(),
                    project.meta() == null ? "" : project.meta().genre(),
                    project.meta() == null ? "" : project.meta().premise(),
                    chapter.title(),
                    chapter.summary(),
                    chapter.purpose(),
                    chapter.notes()
                ),
                Stream.of(
                    chapter.mandatoryBeats(),
                    chapter.forbiddenContent(),
                    context.outlineNodes().stream()
                        .flatMap(node -> Stream.of(node.title(), node.summary(), node.objective(), node.keyConflict()))
                        .toList(),
                    context.characters().stream()
                        .flatMap(character -> Stream.of(character.name(), character.role(), character.profile(), character.motivation()))
                        .toList(),
                    context.foreshadowing().stream()
                        .flatMap(item -> Stream.of(item.title(), item.setup(), item.payoff()))
                        .toList()
                ).flatMap(List::stream)
            )
            .map(this::text)
            .filter(value -> !value.isBlank())
            .distinct()
            .reduce((left, right) -> left + " " + right)
            .map(value -> truncate(value, 1000))
            .orElse("");
    }

    private String stringifyValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : iterable) {
                String text = text(String.valueOf(item));
                if (!text.isBlank()) {
                    items.add(text);
                }
            }
            return String.join("；", items);
        }
        return text(String.valueOf(value));
    }

    private String toCompactJson(Object value, int maxChars) {
        try {
            return truncate(objectMapper.writeValueAsString(value), maxChars);
        } catch (JsonProcessingException exception) {
            return truncate(String.valueOf(value), maxChars);
        }
    }

    private void addIfPresent(StringJoiner joiner, String value) {
        String normalized = text(value);
        if (!normalized.isBlank()) {
            joiner.add(normalized);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return truncate(message
            .replaceAll("(?i)Bearer\\s+\\S+", "Bearer [hidden]")
            .replaceAll("(?i)(api[_-]?key|token|authorization)\\s*[:=]\\s*\\S+", "$1=[hidden]")
            .replaceAll("https?://\\S+", "[hidden-url]"), 180);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxChars) {
        String normalized = text(value).replaceAll("\\s+", " ");
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    public record PromptAugmentation(String text, List<String> warnings) {
    }
}
