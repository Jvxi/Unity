package com.jvxi.unity.novel.service;

import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.ComplianceReport;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.service.PromptBuilder.GenerationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplianceCheckerTest {

    private final ComplianceChecker checker = new ComplianceChecker();

    @Test
    void flagsMissingMandatoryBeatAndForbiddenContent() {
        Chapter chapter = new Chapter(
            "chapter-1",
            1,
            "First",
            "Hero opens the locked gate",
            "Hero enters the tower",
            List.of("outline-1"),
            List.of(),
            List.of(),
            List.of("must-find-key"),
            List.of("forbidden-spoiler"),
            "",
            ""
        );
        OutlineNode outline = new OutlineNode(
            "outline-1",
            1,
            "Tower",
            "The gate opens",
            "Reach the tower",
            "Guard blocks the path",
            List.of(),
            List.of("outline-forbidden")
        );

        ComplianceReport report = checker.evaluate(
            new GenerationContext(chapter, List.of(outline), List.of(), List.of()),
            "The hero reaches the tower but mentions forbidden-spoiler.",
            true
        );

        assertFalse(report.passed());
        assertTrue(report.missingMandatoryBeats().contains("must-find-key"));
        assertTrue(report.forbiddenHits().contains("forbidden-spoiler"));
    }

    @Test
    void strictModeFlagsMissingChapterAnchors() {
        Chapter chapter = new Chapter(
            "chapter-2",
            2,
            "Anchor",
            "unique-summary-anchor",
            "unique-purpose-anchor",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            "",
            ""
        );

        ComplianceReport report = checker.evaluate(
            new GenerationContext(chapter, List.of(), List.of(), List.of()),
            "The scene only describes weather and footsteps.",
            true
        );

        assertFalse(report.passed());
        assertFalse(report.missingChapterAnchors().isEmpty());
    }

    @Test
    void acceptsDraftWhenRequiredAnchorsArePresent() {
        Chapter chapter = new Chapter(
            "chapter-3",
            3,
            "Clean",
            "unique-summary-anchor",
            "unique-purpose-anchor",
            List.of(),
            List.of(),
            List.of(),
            List.of("must-find-key"),
            List.of("forbidden-spoiler"),
            "",
            ""
        );

        ComplianceReport report = checker.evaluate(
            new GenerationContext(chapter, List.of(), List.of(), List.of()),
            "unique-summary-anchor. unique-purpose-anchor. must-find-key.",
            true
        );

        assertTrue(report.passed());
    }
}
