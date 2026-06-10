package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ComplianceReport;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.service.PromptBuilder.GenerationContext;

@Component
public class ComplianceChecker {
    private static final List<String> META_LABEL_CANDIDATES = List.of(
        "大纲",
        "角色设定",
        "角色列表",
        "伏笔",
        "章节说明",
        "写作要求",
        "创作提示",
        "注释",
        "说明：",
        "设定：",
        "Outline",
        "Character List",
        "Author Note"
    );

    public static final List<String> NARRATION_META_PHRASES = List.of(
        "帮助读者",
        "便于读者",
        "向读者",
        "告诉读者",
        "读者可以",
        "读者会",
        "大家会发现",
        "值得一提的是",
        "需要说明的是",
        "简单来说",
        "换句话说",
        "综上所述",
        "总而言之",
        "回顾前文",
        "上文提到",
        "前文说过",
        "正如前面",
        "正如前文",
        "本章将",
        "本章主要",
        "这一章将",
        "下一章",
        "后续章节",
        "故事线",
        "剧情线",
        "主线发展",
        "支线发展",
        "以下是正文",
        "正文如下",
        "开始正文"
    );

    private static final Pattern NARRATION_META_PATTERN = Pattern.compile(
        "(帮(助|忙)|便于).{0,6}读者|向读者|对读者|读者(可以|会|将)|"
            + "(故事|剧情|情节|主线|支线)(线|发展)|"
            + "(下一章|后续章节).{0,8}(将|会|即将)|"
            + "(回顾|总结|梳理).{0,4}(前文|上文|剧情|故事)"
    );

    public ComplianceReport evaluate(GenerationContext context, String draft, boolean strictOutline) {
        Chapter chapter = context.chapter();
        List<OutlineNode> outlineNodes = context.outlineNodes();
        List<CharacterProfile> characters = context.characters();

        List<String> metaLabelHits = META_LABEL_CANDIDATES.stream()
            .filter(draft::contains)
            .toList();

        List<String> narrationMetaHits = detectNarrationMetaHits(draft);

        List<String> missingMandatoryBeats = chapter.mandatoryBeats().stream()
            .filter(entry -> !draft.contains(entry))
            .toList();

        Set<String> forbidden = new LinkedHashSet<>();
        chapter.forbiddenContent().stream().filter(draft::contains).forEach(forbidden::add);
        outlineNodes.stream()
            .flatMap(node -> node.forbidden().stream())
            .filter(draft::contains)
            .forEach(forbidden::add);

        List<String> missingOutlineAnchors = strictOutline
            ? detectMissingOutlineAnchors(outlineNodes, draft)
            : List.of();

        List<String> missingChapterAnchors = List.of();
        if (strictOutline) {
            missingChapterAnchors = detectMissingChapterAnchors(chapter, draft);
        }

        List<String> groundedCharacterNames = characters.stream()
            .map(CharacterProfile::name)
            .filter(name -> !name.isBlank() && draft.contains(name))
            .toList();
        List<String> groundedOutlineTitles = outlineNodes.stream()
            .map(OutlineNode::title)
            .toList();

        boolean passed = metaLabelHits.isEmpty()
            && narrationMetaHits.isEmpty()
            && missingMandatoryBeats.isEmpty()
            && forbidden.isEmpty()
            && missingOutlineAnchors.isEmpty()
            && missingChapterAnchors.isEmpty();

        return new ComplianceReport(
            passed,
            metaLabelHits,
            narrationMetaHits,
            missingMandatoryBeats,
            List.copyOf(forbidden),
            missingOutlineAnchors,
            missingChapterAnchors,
            groundedOutlineTitles,
            groundedCharacterNames
        );
    }

    private List<String> detectNarrationMetaHits(String draft) {
        Set<String> hits = new LinkedHashSet<>();
        for (String phrase : NARRATION_META_PHRASES) {
            if (draft.contains(phrase)) {
                hits.add(phrase);
            }
        }
        if (NARRATION_META_PATTERN.matcher(draft).find()) {
            hits.add("检测到旁白/解说式句式");
        }
        return List.copyOf(hits);
    }

    private List<String> detectMissingOutlineAnchors(List<OutlineNode> outlineNodes, String draft) {
        List<String> missing = new ArrayList<>();
        for (OutlineNode node : outlineNodes) {
            List<String> violations = new ArrayList<>();

            // mustKeep, objective, keyConflict 均为剧情指导描述，
            // 不做自动子串校验（由 prompt 引导 AI 遵循）

            if (!violations.isEmpty()) {
                missing.add(node.title() + "：" + String.join("；", violations));
            }
        }
        return missing;
    }

    private List<String> detectMissingChapterAnchors(Chapter chapter, String draft) {
        List<String> missing = new ArrayList<>();
        String summary = chapter.summary() == null ? "" : chapter.summary().trim();
        String purpose = chapter.purpose() == null ? "" : chapter.purpose().trim();

        if (!summary.isBlank() && summary.length() >= 10) {
            List<String> summaryAnchors = extractAnchors(summary, 6);
            if (!summaryAnchors.isEmpty() && summaryAnchors.stream().filter(draft::contains).count() < 1) {
                missing.add("章节摘要关键信息未在正文中体现");
            }
        }

        if (!purpose.isBlank() && purpose.length() >= 8) {
            List<String> purposeAnchors = extractAnchors(purpose, 4);
            if (!purposeAnchors.isEmpty() && purposeAnchors.stream().noneMatch(draft::contains)) {
                missing.add("章节目的未在正文中体现");
            }
        }

        return missing;
    }

    private List<String> extractAnchors(String text, int minLength) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> anchors = new LinkedHashSet<>();
        String[] segments = text.split("[，。；、！？\\n]+");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.length() >= minLength) {
                anchors.add(trimmed);
            }
        }

        if (anchors.isEmpty() && text.trim().length() >= minLength) {
            anchors.add(text.trim());
        }

        return anchors.stream()
            .sorted((left, right) -> Integer.compare(right.length(), left.length()))
            .limit(6)
            .toList();
    }

}

