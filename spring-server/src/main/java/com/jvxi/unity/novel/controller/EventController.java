package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.commit.StoryEvent;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.story.EventAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels/events")
public class EventController {

    private final EventAuditService eventAuditService;
    private final BookLibraryService bookLibraryService;

    public EventController(EventAuditService eventAuditService, BookLibraryService bookLibraryService) {
        this.eventAuditService = eventAuditService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 获取所有事件
     */
    @GetMapping("/{bookId}")
    public ResponseEntity<List<StoryEvent>> getAllEvents(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        List<StoryEvent> events = eventAuditService.getAllEvents(bookId);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取章节事件
     */
    @GetMapping("/{bookId}/chapter/{chapter}")
    public ResponseEntity<List<StoryEvent>> getChapterEvents(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<StoryEvent> events = eventAuditService.getChapterEvents(bookId, chapter);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取实体相关事件
     */
    @GetMapping("/{bookId}/entity/{entityId}")
    public ResponseEntity<List<StoryEvent>> getEntityEvents(
            @PathVariable String bookId,
            @PathVariable String entityId
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<StoryEvent> events = eventAuditService.getEntityEvents(bookId, entityId);
        return ResponseEntity.ok(events);
    }

    /**
     * 按类型获取事件
     */
    @GetMapping("/{bookId}/type/{eventType}")
    public ResponseEntity<List<StoryEvent>> getEventsByType(
            @PathVariable String bookId,
            @PathVariable String eventType
    ) {
        bookLibraryService.requireBookAccess(bookId);
        List<StoryEvent> events = eventAuditService.getEventsByType(bookId, eventType);
        return ResponseEntity.ok(events);
    }
}


