package com.jvxi.unity.novel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.auth.UserContext;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.LibraryIndex;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.persistence.ProjectJsonMapper;
import com.jvxi.unity.novel.persistence.entity.BookEntity;
import com.jvxi.unity.novel.persistence.entity.UserLibraryEntity;
import com.jvxi.unity.novel.persistence.repository.BookRepository;
import com.jvxi.unity.novel.persistence.repository.UserLibraryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookLibraryServiceTest {

    private final BookRepository bookRepository = mock(BookRepository.class);
    private final UserLibraryRepository userLibraryRepository = mock(UserLibraryRepository.class);
    private final ProjectNormalizer normalizer = new ProjectNormalizer(
        new PublishPlatformCatalog(),
        new NovelTypeCatalog(),
        new SystemPromptComposer()
    );
    private final ProjectJsonMapper projectJsonMapper = new ProjectJsonMapper(new ObjectMapper());
    private final ProjectFactory projectFactory = new ProjectFactory();
    private final BookLibraryService service = new BookLibraryService(
        bookRepository,
        userLibraryRepository,
        projectJsonMapper,
        projectFactory,
        normalizer,
        new NovelTypeCatalog()
    );

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void loadLibraryUsesCurrentUserAndKeepsExistingActiveBook() {
        UserContext.setUserId("user-1");
        UserLibraryEntity library = library("user-1", "book-1");
        BookEntity book = book("book-1", "user-1", "第一本", "2026-06-12T10:00:00Z");
        when(userLibraryRepository.existsById("user-1")).thenReturn(true);
        when(userLibraryRepository.findById("user-1")).thenReturn(Optional.of(library));
        when(bookRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(book));

        LibraryIndex index = service.loadLibrary();

        assertEquals("book-1", index.activeBookId());
        assertEquals(1, index.books().size());
        assertEquals("第一本", index.books().getFirst().title());
        verify(bookRepository).findByUserIdOrderByUpdatedAtDesc("user-1");
    }

    @Test
    void deleteActiveBookSwitchesToMostRecentRemainingBook() {
        UserContext.setUserId("user-1");
        UserLibraryEntity library = library("user-1", "book-1");
        when(bookRepository.findByIdAndUserId("book-1", "user-1")).thenReturn(Optional.of(book("book-1", "user-1", "旧书", "2026-06-10T00:00:00Z")));
        when(userLibraryRepository.findById("user-1")).thenReturn(Optional.of(library));
        when(bookRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(book("book-2", "user-1", "新书", "2026-06-12T00:00:00Z")));

        service.deleteBook("book-1");

        verify(bookRepository).deleteByIdAndUserId("book-1", "user-1");
        assertEquals("book-2", library.getActiveBookId());
        verify(userLibraryRepository).save(library);
    }

    @Test
    void deleteLastBookClearsActiveBook() {
        UserContext.setUserId("user-1");
        UserLibraryEntity library = library("user-1", "book-1");
        when(bookRepository.findByIdAndUserId("book-1", "user-1")).thenReturn(Optional.of(book("book-1", "user-1", "旧书", "2026-06-10T00:00:00Z")));
        when(userLibraryRepository.findById("user-1")).thenReturn(Optional.of(library));
        when(bookRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of());

        service.deleteBook("book-1");

        assertEquals("", library.getActiveBookId());
        verify(userLibraryRepository).save(library);
    }

    @Test
    void saveProjectPersistsVersionedJsonWithoutTransientAiKey() {
        UserContext.setUserId("user-1");
        BookEntity entity = book("book-1", "user-1", "旧书", "2026-06-10T00:00:00Z");
        when(bookRepository.findByIdAndUserId("book-1", "user-1")).thenReturn(Optional.of(entity));

        Project project = normalizer.withTransientAiSettings(
            projectFactory.createBlankProject(),
            new AiSettings(true, "custom", "https://relay.example.com/v1", "sk-secret", "model-a", 0.7, 2000, 8000, "")
        );

        service.saveProjectForBook("book-1", project);

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository).save(captor.capture());
        String json = captor.getValue().getProjectJson();
        assertTrue(json.contains("\"schemaVersion\""));
        assertTrue(json.contains("\"revision\""));
        assertFalse(json.contains("sk-secret"));
        assertFalse(json.contains("relay.example.com"));
        assertFalse(json.contains("model-a"));
    }

    @Test
    void createBookCreatesUserLibraryWhenMissing() {
        UserContext.setUserId("user-1");
        when(userLibraryRepository.existsById("user-1")).thenReturn(false);
        when(userLibraryRepository.findById("user-1")).thenReturn(Optional.of(library("user-1", "")));

        service.createBook("新书", "male", "xuanyi");

        verify(userLibraryRepository, times(2)).save(any(UserLibraryEntity.class));
        verify(bookRepository).save(any(BookEntity.class));
        verify(userLibraryRepository).findById(eq("user-1"));
    }

    private UserLibraryEntity library(String userId, String activeBookId) {
        UserLibraryEntity library = new UserLibraryEntity();
        library.setUserId(userId);
        library.setActiveBookId(activeBookId);
        return library;
    }

    private BookEntity book(String bookId, String userId, String title, String updatedAt) {
        BookEntity book = new BookEntity();
        book.setId(bookId);
        book.setUserId(userId);
        book.setTitle(title);
        book.setGenre("悬疑");
        book.setUpdatedAt(updatedAt);
        book.setChapterCount(1);
        book.setOnboardingCompleted(false);
        book.setProjectJson(projectJsonMapper.toJson(normalizer.normalize(projectFactory.createBlankProject())));
        return book;
    }
}
