package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.readingpower.ChapterReadingPower;
import com.jvxi.unity.novel.model.readingpower.ChaseDebt;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.readingpower.ReadingPowerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novels/reading-power")
public class ReadingPowerController {

    private final ReadingPowerService readingPowerService;
    private final BookLibraryService bookLibraryService;

    public ReadingPowerController(ReadingPowerService readingPowerService, BookLibraryService bookLibraryService) {
        this.readingPowerService = readingPowerService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 分析章节追读力
     */
    @PostMapping("/{bookId}/chapter/{chapter}/analyze")
    public ResponseEntity<ChapterReadingPower> analyzeChapter(
            @PathVariable String bookId,
            @PathVariable int chapter,
            @RequestBody Map<String, String> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String chapterText = request.getOrDefault("text", "");
        ChapterReadingPower result = readingPowerService.analyzeChapter(bookId, chapter, chapterText);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取章节追读力评分
     */
    @GetMapping("/{bookId}/chapter/{chapter}")
    public ResponseEntity<ChapterReadingPower> getChapterReadingPower(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        return readingPowerService.getChapterReadingPower(bookId, chapter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取待处理债务
     */
    @GetMapping("/{bookId}/debts")
    public ResponseEntity<List<ChaseDebt>> getPendingDebts(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<ChaseDebt> debts = readingPowerService.getPendingDebts(bookId, limit);
        return ResponseEntity.ok(debts);
    }

    /**
     * 兑现债务
     */
    @PostMapping("/{bookId}/debts/{debtId}/payoff")
    public ResponseEntity<Map<String, Object>> payOffDebt(
            @PathVariable String bookId,
            @PathVariable String debtId,
            @RequestBody Map<String, Object> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        int chapter = (int) request.getOrDefault("chapter", 0);
        String reason = (String) request.getOrDefault("reason", "");

        readingPowerService.payOffDebt(bookId, debtId, chapter, reason);
        return ResponseEntity.ok(Map.of("status", "success", "debtId", debtId));
    }

    /**
     * 追读力统计
     */
    @GetMapping("/{bookId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> stats = readingPowerService.getStats(bookId);
        return ResponseEntity.ok(stats);
    }
}


