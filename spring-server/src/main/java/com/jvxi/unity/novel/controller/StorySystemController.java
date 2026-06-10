package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.story.*;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.story.StorySystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/novels/story-system")
public class StorySystemController {

    private final StorySystemService storySystemService;
    private final BookLibraryService bookLibraryService;

    public StorySystemController(StorySystemService storySystemService, BookLibraryService bookLibraryService) {
        this.storySystemService = storySystemService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 题材路由
     */
    @PostMapping("/{bookId}/route")
    public ResponseEntity<Map<String, Object>> routeGenre(
            @PathVariable String bookId,
            @RequestBody Map<String, String> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String query = request.getOrDefault("query", "");
        String genre = request.get("genre");
        Map<String, Object> result = storySystemService.routeGenre(bookId, query, genre);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成主设定合同
     */
    @PostMapping("/{bookId}/master-setting")
    public ResponseEntity<MasterSetting> generateMasterSetting(
            @PathVariable String bookId,
            @RequestBody Map<String, String> request
    ) {
        bookLibraryService.requireBookAccess(bookId);
        String query = request.getOrDefault("query", "");
        String genre = request.get("genre");
        MasterSetting result = storySystemService.generateMasterSetting(bookId, query, genre);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取主设定合同
     */
    @GetMapping("/{bookId}/master-setting")
    public ResponseEntity<MasterSetting> getMasterSetting(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        return storySystemService.getMasterSetting(bookId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 生成卷级合同
     */
    @PostMapping("/{bookId}/volume/{volume}/brief")
    public ResponseEntity<VolumeBrief> generateVolumeBrief(
            @PathVariable String bookId,
            @PathVariable int volume
    ) {
        bookLibraryService.requireBookAccess(bookId);
        VolumeBrief result = storySystemService.generateVolumeBrief(bookId, volume);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取卷级合同
     */
    @GetMapping("/{bookId}/volume/{volume}/brief")
    public ResponseEntity<VolumeBrief> getVolumeBrief(
            @PathVariable String bookId,
            @PathVariable int volume
    ) {
        bookLibraryService.requireBookAccess(bookId);
        return storySystemService.getVolumeBrief(bookId, volume)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 生成章节级合同
     */
    @PostMapping("/{bookId}/chapter/{chapter}/brief")
    public ResponseEntity<ChapterBrief> generateChapterBrief(
            @PathVariable String bookId,
            @PathVariable int chapter,
            @RequestBody(required = false) Map<String, Object> chapterDirective
    ) {
        bookLibraryService.requireBookAccess(bookId);
        ChapterBrief result = storySystemService.generateChapterBrief(bookId, chapter, chapterDirective);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取章节级合同
     */
    @GetMapping("/{bookId}/chapter/{chapter}/brief")
    public ResponseEntity<ChapterBrief> getChapterBrief(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        return storySystemService.getChapterBrief(bookId, chapter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 生成审查合同
     */
    @PostMapping("/{bookId}/chapter/{chapter}/review-contract")
    public ResponseEntity<ReviewContract> generateReviewContract(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        ReviewContract result = storySystemService.generateReviewContract(bookId, chapter);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取审查合同
     */
    @GetMapping("/{bookId}/chapter/{chapter}/review-contract")
    public ResponseEntity<ReviewContract> getReviewContract(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        return storySystemService.getReviewContract(bookId, chapter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}


