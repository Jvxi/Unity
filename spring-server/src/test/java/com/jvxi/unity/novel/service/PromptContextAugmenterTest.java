package com.jvxi.unity.novel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectMeta;
import com.jvxi.unity.novel.model.memory.MemoryItem;
import com.jvxi.unity.novel.model.memory.MemoryPack;
import com.jvxi.unity.novel.model.rag.RagResult;
import com.jvxi.unity.novel.model.story.ChapterBrief;
import com.jvxi.unity.novel.model.story.MasterSetting;
import com.jvxi.unity.novel.service.PromptBuilder.GenerationContext;
import com.jvxi.unity.novel.service.PromptContextAugmenter.PromptAugmentation;
import com.jvxi.unity.novel.service.context.ContextService;
import com.jvxi.unity.novel.service.rag.RagService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptContextAugmenterTest {

    private final ContextService contextService = mock(ContextService.class);
    private final RagService ragService = mock(RagService.class);
    private final PromptContextAugmenter augmenter = new PromptContextAugmenter(
        contextService,
        ragService,
        new ObjectMapper()
    );

    @Test
    void buildsKnowledgeAugmentationWithContractsMemoryRagAndGuidance() {
        GenerationContext generationContext = generationContext();
        when(contextService.assembleContext("book-1", 3, "write")).thenReturn(Map.of(
            "story_contracts", Map.of("master", new MasterSetting("悬疑")),
            "chapter_brief", new ChapterBrief(3),
            "memory_pack", new MemoryPack(
                List.of(new MemoryItem("story_fact", "银匙", "state", "银匙藏在井底，只有主角知道。", 1)),
                List.of(new MemoryItem("open_loop", "失踪信件", "debt", "读者还不知道信件被谁带走。", 2)),
                List.of(),
                Map.of()
            ),
            "writing_guidance", Map.of(
                "focus", "线索递进与反转",
                "pacing", "紧张推进",
                "key_elements", List.of("误导线索", "嫌疑人", "反转"),
                "avoid", List.of("直接解释真相")
            ),
            "writing_checklist", Map.of(
                "pre_write", List.of("确认章节目标"),
                "during_write", List.of("保持视角一致"),
                "post_write", List.of("检查线索闭环")
            )
        ));
        when(ragService.hybridSearch(eq("book-1"), eq("迷雾镇 银匙悬案 悬疑 银匙失踪 主角发现井底刻痕 找到井底银匙"), eq(5), isNull()))
            .thenReturn(List.of(new RagResult("chunk-1", "上一章结尾，主角在井边发现新的刻痕。", 0.8, 0.2, 2, "bm25")));

        PromptAugmentation result = augmenter.augment("book-1", project(), generationContext);

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.text().contains("知识增强提示包"));
        assertTrue(result.text().contains("故事合同摘要"));
        assertTrue(result.text().contains("银匙藏在井底"));
        assertTrue(result.text().contains("RAG 命中内容"));
        assertTrue(result.text().contains("上一章结尾"));
        assertTrue(result.text().contains("题材写法卡"));
        assertTrue(result.text().contains("写作检查清单"));
    }

    @Test
    void returnsWarningsWhenContextOrRagFailsWithoutThrowing() {
        GenerationContext generationContext = generationContext();
        when(contextService.assembleContext("book-1", 3, "write"))
            .thenThrow(new IllegalStateException("Bearer sk-secret https://relay.example.com/v1 failed"));
        when(ragService.hybridSearch(eq("book-1"), eq("迷雾镇 银匙悬案 悬疑 银匙失踪 主角发现井底刻痕 找到井底银匙"), eq(5), isNull()))
            .thenThrow(new IllegalStateException("api_key=sk-secret failed"));

        PromptAugmentation result = augmenter.augment("book-1", project(), generationContext);

        assertEquals("", result.text());
        assertEquals(2, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("知识增强提示包加载失败"));
        assertTrue(result.warnings().get(1).contains("RAG 检索失败"));
        assertFalse(result.warnings().toString().contains("sk-secret"));
        assertFalse(result.warnings().toString().contains("relay.example.com"));
    }

    private Project project() {
        return new Project(
            new ProjectMeta(
                "迷雾镇",
                "",
                "银匙悬案",
                "悬疑",
                "冷峻",
                "30万字",
                List.of(),
                List.of(),
                false,
                "fanqie",
                "male",
                "mystery"
            ),
            new AiSettings(false, "", "", "", "", 0.7, 2000, 8000, ""),
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of(generationContext().chapter()),
            "2026-06-12T00:00:00Z"
        );
    }

    private GenerationContext generationContext() {
        Chapter chapter = new Chapter(
            "chapter-3",
            3,
            "银匙失踪",
            "",
            "主角发现井底刻痕",
            List.of(),
            List.of(),
            List.of(),
            List.of("找到井底银匙"),
            List.of(),
            "",
            ""
        );
        return new GenerationContext(chapter, List.of(), List.of(), List.of());
    }
}
