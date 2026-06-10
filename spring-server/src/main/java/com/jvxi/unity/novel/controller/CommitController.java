package com.jvxi.unity.novel.controller;

import com.jvxi.unity.novel.model.commit.ChapterCommit;
import com.jvxi.unity.novel.service.BookLibraryService;
import com.jvxi.unity.novel.service.story.ChapterCommitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novels/commit")
public class CommitController {

    private final ChapterCommitService chapterCommitService;
    private final BookLibraryService bookLibraryService;

    public CommitController(ChapterCommitService chapterCommitService, BookLibraryService bookLibraryService) {
        this.chapterCommitService = chapterCommitService;
        this.bookLibraryService = bookLibraryService;
    }

    /**
     * 提交章节
     */
    @PostMapping("/{bookId}/chapter/{chapter}")
    public ResponseEntity<ChapterCommit> commitChapter(
            @PathVariable String bookId,
            @PathVariable int chapter,
            @RequestBody Map<String, Object> commitData
    ) {
        bookLibraryService.requireBookAccess(bookId);
        ChapterCommit result = chapterCommitService.commitChapter(bookId, chapter, commitData);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取提交记录
     */
    @GetMapping("/{bookId}/chapter/{chapter}")
    public ResponseEntity<ChapterCommit> getCommit(
            @PathVariable String bookId,
            @PathVariable int chapter
    ) {
        bookLibraryService.requireBookAccess(bookId);
        return chapterCommitService.getCommit(bookId, chapter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有提交记录
     */
    @GetMapping("/{bookId}/chapters")
    public ResponseEntity<List<ChapterCommit>> getAllCommits(@PathVariable String bookId) {
        bookLibraryService.requireBookAccess(bookId);
        List<ChapterCommit> commits = chapterCommitService.getAllCommits(bookId);
        return ResponseEntity.ok(commits);
    }
}


