package com.jvxi.unity.novel.model;

import java.util.List;

public record LibraryIndex(
    String activeBookId,
    List<BookSummary> books
) {
}

