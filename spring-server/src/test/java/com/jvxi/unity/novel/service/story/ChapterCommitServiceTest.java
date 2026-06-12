package com.jvxi.unity.novel.service.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.commit.StoryEvent;
import com.jvxi.unity.novel.persistence.entity.ChapterCommitEntity;
import com.jvxi.unity.novel.persistence.repository.ChapterCommitRepository;
import com.jvxi.unity.novel.service.memory.MemoryService;
import com.jvxi.unity.novel.service.rag.RagService;
import com.jvxi.unity.novel.service.readingpower.ReadingPowerService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChapterCommitServiceTest {

    private final ChapterCommitRepository commitRepository = mock(ChapterCommitRepository.class);
    private final MemoryService memoryService = mock(MemoryService.class);
    private final RagService ragService = mock(RagService.class);
    private final ReadingPowerService readingPowerService = mock(ReadingPowerService.class);
    private final EventAuditService eventAuditService = mock(EventAuditService.class);
    private final ChapterCommitService commitService = new ChapterCommitService(
            commitRepository,
            memoryService,
            ragService,
            readingPowerService,
            eventAuditService,
            new ObjectMapper()
    );

    @Test
    void commitWithoutExtractionResultBuildsProjectionInputs() {
        when(commitRepository.findByBookIdAndChapterNumber("book-1", 3)).thenReturn(Optional.empty());
        when(commitRepository.save(any(ChapterCommitEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String chapterText = "主角发现密室里藏着一枚令牌。\n\n敌人暴露真实身份，谜团仍未解开。";

        commitService.commitChapter("book-1", 3, Map.of(
                "chapter_text", chapterText,
                "summary_text", "主角发现关键令牌"
        ));

        ArgumentCaptor<Map> extractionCaptor = ArgumentCaptor.forClass(Map.class);
        verify(memoryService).updateFromChapterResult(eq("book-1"), eq(3), extractionCaptor.capture());
        Map<?, ?> extraction = extractionCaptor.getValue();
        assertEquals(false, ((java.util.List<?>) extraction.get("state_changes")).isEmpty());

        verify(eventAuditService).recordEvent(eq("book-1"), any(StoryEvent.class));
        verify(readingPowerService).trackDebt(eq("book-1"), eq("open_loop"), eq("chapter_hook"), any(), eq(3), anyInt());
        verify(ragService).indexChapter(eq("book-1"), eq(3), eq(chapterText), any());
        verify(readingPowerService).analyzeChapter("book-1", 3, chapterText);
    }

    @Test
    void commitWithPartialExtractionResultBackfillsMissingHook() {
        when(commitRepository.findByBookIdAndChapterNumber("book-1", 4)).thenReturn(Optional.empty());
        when(commitRepository.save(any(ChapterCommitEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> partialExtraction = Map.of("chapter_meta", Map.of());

        commitService.commitChapter("book-1", 4, Map.of(
                "chapter_text", "他决定追查秘密，却发现新的线索指向旧案。",
                "extraction_result", partialExtraction
        ));

        ArgumentCaptor<Map> extractionCaptor = ArgumentCaptor.forClass(Map.class);
        verify(memoryService).updateFromChapterResult(eq("book-1"), eq(4), extractionCaptor.capture());
        Map<?, ?> chapterMeta = (Map<?, ?>) extractionCaptor.getValue().get("chapter_meta");
        Map<?, ?> hook = (Map<?, ?>) chapterMeta.get("hook");
        assertEquals("他决定追查秘密，却发现新的线索指向旧案。", hook.get("content"));
    }
}
