package com.jvxi.unity.novel.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jvxi.unity.novel.auth.UserContext;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.BookSummary;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.LibraryIndex;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.persistence.ProjectJsonMapper;
import com.jvxi.unity.novel.persistence.entity.BookEntity;
import com.jvxi.unity.novel.persistence.entity.UserLibraryEntity;
import com.jvxi.unity.novel.persistence.repository.BookRepository;
import com.jvxi.unity.novel.persistence.repository.UserLibraryRepository;

@Component
public class BookLibraryService {
    private final BookRepository bookRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final ProjectJsonMapper projectJsonMapper;
    private final ProjectFactory projectFactory;
    private final ProjectNormalizer normalizer;
    private final NovelTypeCatalog novelTypeCatalog;

    public BookLibraryService(
        BookRepository bookRepository,
        UserLibraryRepository userLibraryRepository,
        ProjectJsonMapper projectJsonMapper,
        ProjectFactory projectFactory,
        ProjectNormalizer normalizer,
        NovelTypeCatalog novelTypeCatalog
    ) {
        this.bookRepository = bookRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.projectJsonMapper = projectJsonMapper;
        this.projectFactory = projectFactory;
        this.normalizer = normalizer;
        this.novelTypeCatalog = novelTypeCatalog;
    }

    @Transactional
    public LibraryIndex loadLibrary() {
        String userId = UserContext.requireUserId();
        ensureLibraryExists(userId);
        UserLibraryEntity library = userLibraryRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取书库索引。"));
        List<BookEntity> books = bookRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<BookSummary> summaries = books.stream().map(this::toSummary).toList();
        String activeBookId = library.getActiveBookId() == null ? "" : library.getActiveBookId();
        if (summaries.isEmpty()) {
            if (!activeBookId.isBlank()) {
                library.setActiveBookId("");
                userLibraryRepository.save(library);
            }
            activeBookId = "";
        } else if (activeBookId.isBlank() || findBook(summaries, activeBookId) == null) {
            activeBookId = summaries.getFirst().id();
            library.setActiveBookId(activeBookId);
            userLibraryRepository.save(library);
        }
        return new LibraryIndex(activeBookId, summaries);
    }

    public String getActiveBookId() {
        String userId = UserContext.requireUserId();
        return userLibraryRepository.findById(userId)
            .map(UserLibraryEntity::getActiveBookId)
            .orElse("");
    }

    @Transactional
    public void setActiveBookId(String bookId) {
        String userId = UserContext.requireUserId();
        if (bookRepository.findByIdAndUserId(bookId, userId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "书籍不存在：" + bookId);
        }
        UserLibraryEntity library = requireLibrary(userId);
        library.setActiveBookId(bookId);
        userLibraryRepository.save(library);
    }

    @Transactional
    public BookSummary createBook(String title, String audienceChannel, String novelTypeId) {
        String userId = UserContext.requireUserId();
        ensureLibraryExists(userId);
        String bookId = UUID.randomUUID().toString();
        String resolvedTitle = title == null || title.isBlank() ? "未命名作品" : title.trim();
        Project project = normalizer.normalize(projectFactory.createBlankProject());
        project = withGenre(project, audienceChannel, novelTypeId);
        project = withTitle(project, resolvedTitle);
        project = ensureDraftDefaults(project);
        BookEntity entity = new BookEntity();
        entity.setId(bookId);
        entity.setUserId(userId);
        applyProject(entity, project);
        bookRepository.save(entity);

        UserLibraryEntity library = requireLibrary(userId);
        library.setActiveBookId(bookId);
        userLibraryRepository.save(library);
        return toSummary(entity);
    }

    @Transactional
    public void deleteBook(String bookId) {
        String userId = UserContext.requireUserId();
        if (bookRepository.findByIdAndUserId(bookId, userId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "书籍不存在。");
        }

        UserLibraryEntity library = requireLibrary(userId);
        bookRepository.deleteByIdAndUserId(bookId, userId);

        List<BookEntity> remaining = bookRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (library.getActiveBookId().equals(bookId)) {
            library.setActiveBookId(remaining.isEmpty() ? "" : remaining.getFirst().getId());
            userLibraryRepository.save(library);
        }
    }

    @Transactional
    public void registerBookAfterSave(String bookId, Project project) {
        String userId = UserContext.requireUserId();
        BookEntity entity = bookRepository.findByIdAndUserId(bookId, userId)
            .orElseGet(() -> {
                BookEntity created = new BookEntity();
                created.setId(bookId);
                created.setUserId(userId);
                return created;
            });
        applyProject(entity, project);
        bookRepository.save(entity);
    }

    @Transactional
    public Project loadProjectForBook(String bookId) {
        String userId = UserContext.requireUserId();
        return bookRepository.findByIdAndUserId(bookId, userId)
            .map(entity -> projectJsonMapper.fromJson(entity.getProjectJson()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "书籍不存在。"));
    }

    @Transactional
    public void saveProjectForBook(String bookId, Project project) {
        String userId = UserContext.requireUserId();
        BookEntity entity = bookRepository.findByIdAndUserId(bookId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "书籍不存在。"));
        applyProject(entity, project);
        bookRepository.save(entity);
    }

    @Transactional
    public void ensureUserLibrary(String userId) {
        ensureLibraryExists(userId);
    }

    @Transactional(readOnly = true)
    public void requireBookAccess(String bookId) {
        String userId = UserContext.requireUserId();
        if (bookId == null || bookId.isBlank() || bookRepository.findByIdAndUserId(bookId, userId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "书籍不存在或无权访问。");
        }
    }

    @Transactional
    public void importBook(String userId, String bookId, Project project, boolean setActive) {
        ensureLibraryExists(userId);
        BookEntity entity = new BookEntity();
        entity.setId(bookId);
        entity.setUserId(userId);
        applyProject(entity, project);
        bookRepository.save(entity);
        if (setActive) {
            UserLibraryEntity library = requireLibrary(userId);
            library.setActiveBookId(bookId);
            userLibraryRepository.save(library);
        }
    }

    @Transactional
    public void setActiveBookForUser(String userId, String bookId) {
        ensureLibraryExists(userId);
        UserLibraryEntity library = requireLibrary(userId);
        library.setActiveBookId(bookId);
        userLibraryRepository.save(library);
    }

    private void createBookForUser(String userId, String title, Project project, boolean forceActive) {
        String bookId = UUID.randomUUID().toString();
        BookEntity entity = new BookEntity();
        entity.setId(bookId);
        entity.setUserId(userId);
        if (title != null && !title.isBlank()) {
            applyProject(entity, withTitle(project, title.trim()));
        } else {
            applyProject(entity, project);
        }
        bookRepository.save(entity);
        if (forceActive) {
            UserLibraryEntity library = requireLibrary(userId);
            library.setActiveBookId(bookId);
            userLibraryRepository.save(library);
        }
    }

    private void ensureLibraryExists(String userId) {
        if (userLibraryRepository.existsById(userId)) {
            return;
        }
        UserLibraryEntity library = new UserLibraryEntity();
        library.setUserId(userId);
        library.setActiveBookId("");
        userLibraryRepository.save(library);
    }

    private UserLibraryEntity requireLibrary(String userId) {
        return userLibraryRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取书库索引。"));
    }

    private void applyProject(BookEntity entity, Project project) {
        String title = project.meta().title() == null || project.meta().title().isBlank()
            ? "未命名作品"
            : project.meta().title().trim();
        String genre = project.meta().genre() == null ? "" : project.meta().genre().trim();
        boolean onboardingCompleted = project.onboarding() != null && project.onboarding().completed();
        int chapterCount = project.chapters() == null ? 0 : project.chapters().size();
        String updatedAt = project.updatedAt() == null || project.updatedAt().isBlank()
            ? Instant.now().toString()
            : project.updatedAt();
        entity.setTitle(title);
        entity.setGenre(genre);
        entity.setOnboardingCompleted(onboardingCompleted);
        entity.setChapterCount(chapterCount);
        entity.setUpdatedAt(updatedAt);
        long nextRevision = projectJsonMapper.nextRevision(entity.getProjectJson());
        entity.setProjectJson(projectJsonMapper.toJson(normalizer.sanitizeForPersistence(project), nextRevision));
    }

    private BookSummary toSummary(BookEntity entity) {
        return new BookSummary(
            entity.getId(),
            entity.getTitle(),
            entity.getGenre(),
            entity.getUpdatedAt(),
            entity.getChapterCount(),
            entity.isOnboardingCompleted()
        );
    }

    private BookSummary findBook(List<BookSummary> books, String bookId) {
        if (bookId == null || bookId.isBlank()) {
            return null;
        }
        return books.stream().filter(book -> book.id().equals(bookId)).findFirst().orElse(null);
    }

    private Project withGenre(Project project, String audienceChannel, String novelTypeId) {
        if ((audienceChannel == null || audienceChannel.isBlank())
            && (novelTypeId == null || novelTypeId.isBlank())) {
            return project;
        }
        String audience = novelTypeCatalog.normalizeAudience(
            audienceChannel == null || audienceChannel.isBlank()
                ? project.meta().audienceChannel()
                : audienceChannel
        );
        String novelType = novelTypeCatalog.normalizeNovelType(
            audience,
            novelTypeId == null || novelTypeId.isBlank() ? project.meta().novelType() : novelTypeId
        );
        String genre = novelTypeCatalog.formatGenreLabel(audience, novelType);
        var meta = project.meta();
        return new Project(
            new com.jvxi.unity.novel.model.ProjectMeta(
                meta.title(),
                meta.synopsis(),
                genre,
                meta.premise(),
                meta.tone(),
                meta.targetLength(),
                meta.styleRules(),
                meta.worldRules(),
                meta.strictMode(),
                meta.publishPlatform(),
                audience,
                novelType
            ),
            project.aiSettings(),
            project.onboarding(),
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            project.chapters(),
            project.updatedAt()
        );
    }

    private Project ensureDraftDefaults(Project project) {
        if (project.chapters() != null && !project.chapters().isEmpty()) {
            return project;
        }
        Chapter chapter = new Chapter(
            UUID.randomUUID().toString(),
            1,
            "第 1 章",
            "",
            "",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            "",
            ""
        );
        return new Project(
            project.meta(),
            project.aiSettings(),
            project.onboarding(),
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            List.of(chapter),
            project.updatedAt()
        );
    }

    private Project withTitle(Project project, String title) {
        var meta = project.meta();
        return new Project(
            new com.jvxi.unity.novel.model.ProjectMeta(
                title,
                meta.synopsis(),
                meta.genre(),
                meta.premise(),
                meta.tone(),
                meta.targetLength(),
                meta.styleRules(),
                meta.worldRules(),
                meta.strictMode(),
                meta.publishPlatform(),
                meta.audienceChannel(),
                meta.novelType()
            ),
            project.aiSettings(),
            project.onboarding(),
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            project.chapters(),
            project.updatedAt()
        );
    }
}

