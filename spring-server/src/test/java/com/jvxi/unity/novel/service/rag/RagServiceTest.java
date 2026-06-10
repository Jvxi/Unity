package com.jvxi.unity.novel.service.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.rag.RagResult;
import com.jvxi.unity.novel.persistence.entity.Bm25TermEntity;
import com.jvxi.unity.novel.persistence.entity.VectorEmbeddingEntity;
import com.jvxi.unity.novel.persistence.repository.Bm25TermRepository;
import com.jvxi.unity.novel.persistence.repository.RagQueryLogRepository;
import com.jvxi.unity.novel.persistence.repository.VectorEmbeddingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagServiceTest {

    private final VectorEmbeddingRepository vectorRepository = mock(VectorEmbeddingRepository.class);
    private final Bm25TermRepository bm25Repository = mock(Bm25TermRepository.class);
    private final RagQueryLogRepository queryLogRepository = mock(RagQueryLogRepository.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final RagService ragService = new RagService(
            vectorRepository,
            bm25Repository,
            queryLogRepository,
            embeddingService,
            new ObjectMapper()
    );

    @Test
    void indexChapterWithoutEmbeddingBuildsBm25Only() {
        when(bm25Repository.findByBookIdAndChunkId(eq("book-1"), any())).thenReturn(List.of());

        ragService.indexChapter("book-1", 3, "主角突破成功，敌人出现。", null);

        ArgumentCaptor<Bm25TermEntity> captor = ArgumentCaptor.forClass(Bm25TermEntity.class);
        verify(bm25Repository, atLeastOnce()).save(captor.capture());
        verify(vectorRepository, never()).save(any(VectorEmbeddingEntity.class));
        Bm25TermEntity saved = captor.getAllValues().getFirst();
        assertEquals("book-1", saved.getBookId());
        assertEquals(3, saved.getChapterNumber());
        assertEquals("主角突破成功，敌人出现。", saved.getChunkText());
    }

    @Test
    void bm25SearchReturnsChunkTextWithoutVectorIndex() {
        Bm25TermEntity entity = new Bm25TermEntity();
        entity.setBookId("book-1");
        entity.setChunkId("ch0003_p0");
        entity.setTerm("突破");
        entity.setTermFrequency(2);
        entity.setChunkText("主角突破成功，敌人出现。");
        entity.setChapterNumber(3);

        when(bm25Repository.countDistinctChunksByBookId("book-1")).thenReturn(1L);
        when(bm25Repository.findByBookIdAndTerm("book-1", "突破")).thenReturn(List.of(entity));
        when(vectorRepository.findByBookIdAndChunkId("book-1", "ch0003_p0")).thenReturn(Optional.empty());

        List<RagResult> results = ragService.bm25Search("book-1", "突破", 5);

        assertFalse(results.isEmpty());
        assertEquals("主角突破成功，敌人出现。", results.get(0).chunkText());
        assertEquals(3, results.get(0).chapterNumber());
        assertEquals("bm25", results.get(0).source());
    }
}
