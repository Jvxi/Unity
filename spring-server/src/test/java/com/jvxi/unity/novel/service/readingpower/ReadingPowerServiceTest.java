package com.jvxi.unity.novel.service.readingpower;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.readingpower.ChapterReadingPower;
import com.jvxi.unity.novel.model.readingpower.ChaseDebt;
import com.jvxi.unity.novel.persistence.entity.ChapterReadingPowerEntity;
import com.jvxi.unity.novel.persistence.entity.ChaseDebtEntity;
import com.jvxi.unity.novel.persistence.repository.ChapterReadingPowerRepository;
import com.jvxi.unity.novel.persistence.repository.ChaseDebtRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadingPowerServiceTest {

    private final ChapterReadingPowerRepository readingPowerRepository = mock(ChapterReadingPowerRepository.class);
    private final ChaseDebtRepository chaseDebtRepository = mock(ChaseDebtRepository.class);
    private final ReadingPowerService service = new ReadingPowerService(
        readingPowerRepository,
        chaseDebtRepository,
        new ObjectMapper()
    );

    @Test
    void analyzeChapterPersistsScoreAndViolations() {
        when(readingPowerRepository.findByBookIdAndChapterNumber("book-1", 1)).thenReturn(Optional.empty());
        when(readingPowerRepository.save(any(ChapterReadingPowerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChapterReadingPower result = service.analyzeChapter("book-1", 1, "short");

        assertTrue(result.overallScore() >= 0);
        assertFalse(result.hardViolations().isEmpty());

        ArgumentCaptor<ChapterReadingPowerEntity> captor = ArgumentCaptor.forClass(ChapterReadingPowerEntity.class);
        verify(readingPowerRepository).save(captor.capture());
        assertEquals("book-1", captor.getValue().getBookId());
        assertEquals(1, captor.getValue().getChapterNumber());
        assertTrue(captor.getValue().getHardViolationsJson().contains("HARD-001"));
    }

    @Test
    void trackAndPayOffDebtUpdatesDebtProjection() {
        service.trackDebt("book-1", "open_loop", "chapter_hook", "A locked room remains unexplained.", 2, 80);

        ArgumentCaptor<ChaseDebtEntity> createdCaptor = ArgumentCaptor.forClass(ChaseDebtEntity.class);
        verify(chaseDebtRepository).save(createdCaptor.capture());
        ChaseDebtEntity created = createdCaptor.getValue();
        assertEquals("book-1", created.getBookId());
        assertEquals(ChaseDebtEntity.DebtType.open_loop, created.getDebtType());
        assertEquals(80, created.getUrgency());
        assertEquals(ChaseDebtEntity.DebtStatus.pending, created.getStatus());

        when(chaseDebtRepository.findById(created.getId())).thenReturn(Optional.of(created));
        service.payOffDebt("book-1", created.getId(), 5, "Revealed the culprit.");

        assertEquals(ChaseDebtEntity.DebtStatus.paid_off, created.getStatus());
        assertEquals(5, created.getResolvedChapter());
        assertEquals("Revealed the culprit.", created.getResolvedReason());
    }

    @Test
    void returnsPendingDebtsAndStats() {
        ChaseDebtEntity debt = debt("book-1", "debt-1", 70);
        when(chaseDebtRepository.findPendingByBookIdWithLimit("book-1", 10)).thenReturn(List.of(debt));
        when(chaseDebtRepository.findByBookIdAndStatus("book-1", ChaseDebtEntity.DebtStatus.pending)).thenReturn(List.of(debt));

        ChapterReadingPowerEntity power = new ChapterReadingPowerEntity();
        power.setBookId("book-1");
        power.setChapterNumber(1);
        power.setHookType("mystery_hook");
        power.setHookStrength(ChapterReadingPowerEntity.HookStrength.medium);
        power.setCoolPointsJson("[]");
        power.setMicroPayoffsJson("[]");
        power.setHardViolationsJson("[]");
        power.setSoftViolationsJson("[]");
        power.setOverallScore(BigDecimal.valueOf(72));
        when(readingPowerRepository.findByBookId("book-1")).thenReturn(List.of(power));

        List<ChaseDebt> debts = service.getPendingDebts("book-1", 10);
        Map<String, Object> stats = service.getStats("book-1");

        assertEquals(1, debts.size());
        assertEquals("open_loop", debts.getFirst().debtType());
        assertEquals(1, stats.get("chapter_count"));
        assertEquals(72.0, (double) stats.get("average_score"));
        assertEquals(1, stats.get("pending_debts"));
    }

    private ChaseDebtEntity debt(String bookId, String id, int urgency) {
        ChaseDebtEntity debt = new ChaseDebtEntity();
        debt.setId(id);
        debt.setBookId(bookId);
        debt.setDebtType(ChaseDebtEntity.DebtType.open_loop);
        debt.setSubject("chapter_hook");
        debt.setDescription("A locked room remains unexplained.");
        debt.setCreatedChapter(2);
        debt.setUrgency(urgency);
        debt.setStatus(ChaseDebtEntity.DebtStatus.pending);
        return debt;
    }
}
