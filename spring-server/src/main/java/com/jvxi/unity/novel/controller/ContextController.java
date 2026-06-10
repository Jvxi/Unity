package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.context.ContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/novels/context")
public class ContextController {

    private final ContextService contextService;
    private final BookLibraryService bookLibraryService;

    public ContextController(ContextService contextService, BookLibraryService bookLibraryService) {
        this.contextService = contextService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 组装上下文包
     */
    @PostMapping("/{bookId}/chapter/{chapter}/assemble")
    public ResponseEntity<Map<String, Object>> assembleContext(
            @PathVariable String bookId,
            @PathVariable int chapter,
            @RequestParam(defaultValue = "write") String taskType
    ) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> context = contextService.assembleContext(bookId, chapter, taskType);
        return ResponseEntity.ok(context);
    }

    /**
     * 获取写作指导
     */
    @GetMapping("/{bookId}/chapter/{chapter}/guidance")
    public ResponseEntity<Map<String, Object>> getWritingGuidance(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        Map<String, Object> guidance = contextService.getWritingGuidance(bookId, chapter);
        return ResponseEntity.ok(guidance);
    }
}


