package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.rag.RagResult;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.rag.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novels/rag")
public class RagController {

    private final RagService ragService;
    private final BookLibraryService bookLibraryService;

    public RagController(RagService ragService, BookLibraryService bookLibraryService) {
        this.ragService = ragService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 混合检索
     */
    @PostMapping("/{bookId}/search")
    public ResponseEntity<List<RagResult>> hybridSearch(
            @PathVariable String bookId,
            @RequestBody Map<String, Object> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String query = (String) request.getOrDefault("query", "");
        int topK = (int) request.getOrDefault("topK", 10);

        List<RagResult> results = ragService.hybridSearch(bookId, query, topK);
        return ResponseEntity.ok(results);
    }

    /**
     * 向量检索
     */
    @PostMapping("/{bookId}/search/vector")
    public ResponseEntity<List<RagResult>> vectorSearch(
            @PathVariable String bookId,
            @RequestBody Map<String, Object> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String query = (String) request.getOrDefault("query", "");
        int topK = (int) request.getOrDefault("topK", 10);

        List<RagResult> results = ragService.vectorSearch(bookId, query, topK);
        return ResponseEntity.ok(results);
    }

    /**
     * BM25检索
     */
    @PostMapping("/{bookId}/search/bm25")
    public ResponseEntity<List<RagResult>> bm25Search(
            @PathVariable String bookId,
            @RequestBody Map<String, Object> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String query = (String) request.getOrDefault("query", "");
        int topK = (int) request.getOrDefault("topK", 10);

        List<RagResult> results = ragService.bm25Search(bookId, query, topK);
        return ResponseEntity.ok(results);
    }

    /**
     * 索引章节
     */
    @PostMapping("/{bookId}/index/chapter/{chapter}")
    public ResponseEntity<Map<String, Object>> indexChapter(
            @PathVariable String bookId,
            @PathVariable int chapter,
            @RequestBody Map<String, String> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String chapterText = request.getOrDefault("text", "");
        ragService.indexChapter(bookId, chapter, chapterText);
        return ResponseEntity.ok(Map.of("status", "success", "chapter", chapter));
    }

    /**
     * RAG统计
     */
    @GetMapping("/{bookId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> stats = ragService.getStats(bookId);
        return ResponseEntity.ok(stats);
    }
}


