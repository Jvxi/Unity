package com.jvxi.unity.novel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.ChapterGenerationResponse;
import com.jvxi.unity.novel.model.OnboardingState;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectEnvelope;
import com.jvxi.unity.novel.model.ProjectMeta;
import com.jvxi.unity.novel.service.PromptContextAugmenter.PromptAugmentation;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectStore projectStore = mock(ProjectStore.class);
    private final PromptContextAugmenter promptContextAugmenter = mock(PromptContextAugmenter.class);
    private final ProjectNormalizer normalizer = new ProjectNormalizer(
        new PublishPlatformCatalog(),
        new NovelTypeCatalog(),
        new SystemPromptComposer()
    );
    private final GenerationService service = new GenerationService(
        projectStore,
        normalizer,
        new ProjectValidator(),
        new PromptBuilder(new PublishPlatformCatalog(), new NovelTypeCatalog()),
        new ComplianceChecker(),
        new SystemPromptComposer(),
        new PublishPlatformCatalog(),
        new NovelTypeCatalog(),
        mock(TokenUsageService.class),
        new AiGatewayService(objectMapper, HttpClient.newHttpClient()),
        new AiJsonRepairService(objectMapper),
        promptContextAugmenter,
        objectMapper
    );

    @Test
    void generateChapterIncludesKnowledgeAugmentationInPromptPreview() {
        Project project = project();
        when(projectStore.loadActiveProject()).thenReturn(new ProjectEnvelope("book-1", project));
        when(projectStore.saveActiveProject(any(Project.class))).thenAnswer(invocation -> new ProjectEnvelope("book-1", invocation.getArgument(0)));
        when(promptContextAugmenter.augment(eq("book-1"), any(Project.class), any()))
            .thenReturn(new PromptAugmentation("\n知识增强提示包：\n- 记忆包：主角知道井底银匙。", List.of("RAG 检索失败：timeout")));

        ChapterGenerationResponse response = service.generateChapter("chapter-1", disabledAi());

        assertTrue(response.promptPreview().contains("知识增强提示包"));
        assertTrue(response.promptPreview().contains("主角知道井底银匙"));
        assertTrue(response.warnings().contains("RAG 检索失败：timeout"));
        assertFalse(response.draft().isBlank());
        verify(projectStore).saveActiveProject(any(Project.class));
    }

    @Test
    void streamGenerateChapterSendsDeltaAndDoneEvents() throws Exception {
        Project project = project();
        when(projectStore.loadActiveProject()).thenReturn(new ProjectEnvelope("book-1", project));
        when(projectStore.saveActiveProject(any(Project.class))).thenAnswer(invocation -> new ProjectEnvelope("book-1", invocation.getArgument(0)));
        when(promptContextAugmenter.augment(eq("book-1"), any(Project.class), any()))
            .thenReturn(new PromptAugmentation("\n知识增强提示包：\n- RAG 命中内容：井边刻痕。", List.of()));

        CapturingSseEmitter emitter = new CapturingSseEmitter();

        service.streamGenerateChapter("chapter-1", emitter, false, disabledAi());

        assertTrue(emitter.completed);
        assertTrue(emitter.events.stream().anyMatch(event -> event.path("type").asText().equals("delta")));
        assertTrue(emitter.events.stream().anyMatch(event -> event.path("type").asText().equals("done")));
    }

    private Project project() {
        OutlineNode outline = new OutlineNode(
            "outline-1",
            1,
            "Locked Well",
            "The hero finds the silver key near the old well.",
            "Reveal the first clue",
            "The guard tries to stop the search",
            List.of("must-find-key"),
            List.of("forbidden-spoiler")
        );
        Chapter chapter = new Chapter(
            "chapter-1",
            1,
            "Silver Key",
            "The hero reaches the old well and finds the silver key.",
            "Reveal the first clue",
            List.of("outline-1"),
            List.of(),
            List.of(),
            List.of("must-find-key"),
            List.of("forbidden-spoiler"),
            "",
            ""
        );
        return new Project(
            new ProjectMeta(
                "Mist Town",
                "A detective follows clues in a coastal town.",
                "Mystery",
                "A vanished letter points to a hidden key.",
                "Tense",
                "300000",
                List.of(),
                List.of(),
                false,
                "qidian",
                "male",
                "xuanyi"
            ),
            disabledAi(),
            new OnboardingState(true, List.of(), List.of()),
            List.of(outline),
            List.of(),
            List.of(),
            List.of(chapter),
            "2026-06-12T00:00:00Z"
        );
    }

    private AiSettings disabledAi() {
        return new AiSettings(false, "rule-based", "", "", "", 0.7, 2000, 8000, "");
    }

    private final class CapturingSseEmitter extends SseEmitter {
        private final List<JsonNode> events = new ArrayList<>();
        private boolean completed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            Set<ResponseBodyEmitter.DataWithMediaType> items = builder.build();
            for (ResponseBodyEmitter.DataWithMediaType item : items) {
                Object data = item.getData();
                if (data instanceof String text && text.trim().startsWith("{")) {
                    events.add(objectMapper.readTree(text));
                }
            }
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }
    }
}
