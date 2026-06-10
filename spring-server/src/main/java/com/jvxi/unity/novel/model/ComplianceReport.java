package com.jvxi.unity.novel.model;

import java.util.List;

public record ComplianceReport(
    boolean passed,
    List<String> metaLabelHits,
    List<String> narrationMetaHits,
    List<String> missingMandatoryBeats,
    List<String> forbiddenHits,
    List<String> missingOutlineAnchors,
    List<String> missingChapterAnchors,
    List<String> groundedOutlineTitles,
    List<String> groundedCharacterNames
) {
}

