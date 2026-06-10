package com.jvxi.unity.novel.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.LibraryIndex;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectEnvelope;
import com.jvxi.unity.novel.model.ProjectMeta;

@Component
public class ProjectStore {
    private final ProjectFactory projectFactory;
    private final ProjectNormalizer normalizer;
    private final ProjectValidator validator;
    private final BookLibraryService bookLibraryService;

    public ProjectStore(
        ProjectFactory projectFactory,
        ProjectNormalizer normalizer,
        ProjectValidator validator,
        BookLibraryService bookLibraryService
    ) {
        this.projectFactory = projectFactory;
        this.normalizer = normalizer;
        this.validator = validator;
        this.bookLibraryService = bookLibraryService;
    }

    public LibraryIndex loadLibrary() {
        return bookLibraryService.loadLibrary();
    }

    @Transactional
    public ProjectEnvelope loadActiveProject() {
        String bookId = bookLibraryService.getActiveBookId();
        if (bookId == null || bookId.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "书库为空，请先在「书库」中创建一本书。");
        }
        return loadProject(bookId);
    }

    @Transactional(readOnly = true)
    public ProjectEnvelope loadProject(String bookId) {
        if (bookId == null || bookId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前没有选中的书籍。");
        }
        Project parsed = bookLibraryService.loadProjectForBook(bookId);
        Project normalizedProject = normalizer.normalize(parsed);
        validateForLifecycle(normalizedProject);
        return new ProjectEnvelope(bookId, normalizedProject);
    }

    @Transactional
    public ProjectEnvelope saveActiveProject(Project project) {
        String bookId = bookLibraryService.getActiveBookId();
        return saveProject(bookId, project);
    }

    @Transactional
    public ProjectEnvelope saveProject(String bookId, Project project) {
        Project normalizedProject = normalizer.normalize(project);
        validateForLifecycle(normalizedProject);
        Project persisted = normalizer.withUpdatedTimestamp(normalizedProject);
        bookLibraryService.saveProjectForBook(bookId, persisted);
        bookLibraryService.registerBookAfterSave(bookId, persisted);
        return new ProjectEnvelope(bookId, persisted);
    }

    @Transactional
    public ProjectEnvelope switchActiveBook(String bookId) {
        bookLibraryService.setActiveBookId(bookId);
        return loadProject(bookId);
    }

    @Transactional
    public ProjectEnvelope createBook(String title, String audienceChannel, String novelTypeId) {
        var created = bookLibraryService.createBook(title, audienceChannel, novelTypeId);
        String bookId = created.id();
        bookLibraryService.setActiveBookId(bookId);
        return loadProject(bookId);
    }

    @Transactional
    public LibraryIndex deleteBook(String bookId) {
        bookLibraryService.deleteBook(bookId);
        return bookLibraryService.loadLibrary();
    }

    /** @deprecated 供内部兼容，请使用 loadActiveProject */
    @Deprecated
    public Project loadProject() {
        return loadActiveProject().project();
    }

    /** @deprecated 供内部兼容，请使用 saveActiveProject */
    @Deprecated
    public Project saveProject(Project project) {
        return saveActiveProject(project).project();
    }

    private void validateForLifecycle(Project project) {
        if (project.onboarding() != null && project.onboarding().completed()) {
            validator.validate(project);
            return;
        }
        validator.validateDraft(project);
    }
}

