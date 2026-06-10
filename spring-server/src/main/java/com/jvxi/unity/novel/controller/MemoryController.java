package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.memory.MemoryItem;
import com.jvxi.unity.novel.model.memory.MemoryPack;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.memory.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novels/memory")
public class MemoryController {

    private final MemoryService memoryService;
    private final BookLibraryService bookLibraryService;

    public MemoryController(MemoryService memoryService, BookLibraryService bookLibraryService) {
        this.memoryService = memoryService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 获取记忆包（写前注入）
     */
    @GetMapping("/{bookId}/pack")
    public ResponseEntity<MemoryPack> getMemoryPack(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "0") int chapter,
            @RequestParam(defaultValue = "write") String taskType
    ) {
        bookLibraryService.requireBookAccess(bookId);
        MemoryPack pack = memoryService.buildMemoryPack(bookId, chapter, taskType);
        return ResponseEntity.ok(pack);
    }

    /**
     * 按类别查询记忆
     */
    @GetMapping("/{bookId}/category/{category}")
    public ResponseEntity<List<MemoryItem>> queryByCategory(
            @PathVariable String bookId,
            @PathVariable String category
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<MemoryItem> items = memoryService.queryMemory(bookId, category, null);
        return ResponseEntity.ok(items);
    }

    /**
     * 按主体查询记忆
     */
    @GetMapping("/{bookId}/subject/{subject}")
    public ResponseEntity<List<MemoryItem>> queryBySubject(
            @PathVariable String bookId,
            @PathVariable String subject
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<MemoryItem> items = memoryService.queryMemory(bookId, null, subject);
        return ResponseEntity.ok(items);
    }

    /**
     * 压缩记忆
     */
    @PostMapping("/{bookId}/compact")
    public ResponseEntity<Map<String, Object>> compactMemory(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> result = memoryService.compactMemory(bookId);
        return ResponseEntity.ok(result);
    }

    /**
     * 记忆统计
     */
    @GetMapping("/{bookId}/stats")
    public ResponseEntity<Map<String, Object>> getMemoryStats(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> stats = memoryService.getMemoryStats(bookId);
        return ResponseEntity.ok(stats);
    }
}


