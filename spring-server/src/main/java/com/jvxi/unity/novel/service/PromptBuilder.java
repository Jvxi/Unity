package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ComplianceReport;
import com.jvxi.unity.novel.model.ForeshadowingItem;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.NovelTypeInfo;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.PublishPlatformInfo;

@Component
public class PromptBuilder {
    private final PublishPlatformCatalog publishPlatformCatalog;
    private final NovelTypeCatalog novelTypeCatalog;

    public PromptBuilder(PublishPlatformCatalog publishPlatformCatalog, NovelTypeCatalog novelTypeCatalog) {
        this.publishPlatformCatalog = publishPlatformCatalog;
        this.novelTypeCatalog = novelTypeCatalog;
    }
    public GenerationContext resolveContext(Project project, String chapterId) {
        Chapter chapter = project.chapters()
            .stream()
            .filter(entry -> entry.id().equals(chapterId))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chapter not found."));

        List<OutlineNode> outlineNodes = project.outlineNodes().stream()
            .filter(entry -> chapter.outlineNodeIds().contains(entry.id()))
            .toList();
        List<CharacterProfile> characters = project.characters().stream()
            .filter(entry -> chapter.characterIds().contains(entry.id()))
            .toList();
        List<ForeshadowingItem> foreshadowing = project.foreshadowing().stream()
            .filter(entry -> chapter.foreshadowingIds().contains(entry.id()))
            .toList();

        return new GenerationContext(chapter, outlineNodes, characters, foreshadowing);
    }

    public String buildPrompt(Project project, GenerationContext context) {
        List<String> lines = new ArrayList<>();
        Chapter chapter = context.chapter();

        lines.add("请根据以下章节要求生成小说正文。");
        lines.add("");

        appendPreviousChaptersContext(lines, project, chapter);

        lines.add("章节信息：");
        lines.add("章节：" + chapter.order() + " - " + chapter.title());
        lines.add("章节摘要：" + chapter.summary());
        lines.add("章节目的：" + chapter.purpose());
        lines.add("章节备注：" + (chapter.notes().isBlank() ? "无" : chapter.notes()));
        lines.add("");
        lines.add("本章必须发生：");
        lines.addAll(chapter.mandatoryBeats().stream().map(entry -> "- " + entry).toList());
        lines.add("");
        lines.add("本章禁止出现：");
        lines.addAll(chapter.forbiddenContent().stream().map(entry -> "- " + entry).toList());
        lines.add("");

        if (!context.outlineNodes().isEmpty()) {
            lines.add("绑定大纲节点：");
            for (OutlineNode node : context.outlineNodes()) {
                lines.add("- 标题：" + node.title());
                lines.add("  摘要：" + node.summary());
                lines.add("  目标：" + node.objective());
                lines.add("  冲突：" + node.keyConflict());
                if (!node.mustKeep().isEmpty()) {
                    lines.add("  必保留(必须通过正文场景,对话或心理描写自然体现):" + String.join(";", node.mustKeep()));
                }
                if (!node.forbidden().isEmpty()) {
                    lines.add("  禁提前写出:" + String.join(";", node.forbidden()));
                }
            }
            lines.add("");
        }

        if (!context.characters().isEmpty()) {
            lines.add("相关角色：");
            for (CharacterProfile character : context.characters()) {
                lines.add("- " + character.name() + " / " + character.role());
                lines.add("  人设：" + character.profile());
                lines.add("  动机：" + character.motivation());
                lines.add("  约束：" + character.constraint());
                lines.add("  关系：" + character.relationships());
            }
            lines.add("");
        }

        if (!context.foreshadowing().isEmpty()) {
            lines.add("相关伏笔：");
            for (ForeshadowingItem item : context.foreshadowing()) {
                lines.add("- " + item.title() + ":" + item.setup());
                lines.add("  回收方向:" + item.payoff());
                lines.add("  计划揭示:" + item.plannedReveal());
                lines.add("  当前状态:" + item.status());
            }
            lines.add("");
        }

        lines.add("输出要求：");
        lines.add("- 从正文第一句直接开始。");
        lines.add("- 语言流畅自然,以场景,动作,对话,心理推动剧情。");
        lines.add("- 不要出现\"以下是正文\"等元文本。");
        lines.add("- 字数建议:1200-2200字。");
        lines.add("- 大纲节点中的必保留项必须通过正文中的具体场景,角色对话或心理活动自然体现,不可遗漏。");
        lines.add("- 大纲节点中的目标和冲突必须在正文中有所体现。");

        return lines.stream().collect(Collectors.joining("\n"));
    }

    private void appendPreviousChaptersContext(List<String> lines, Project project, Chapter currentChapter) {
        List<Chapter> previousChapters = project.chapters().stream()
            .filter(entry -> entry.order() < currentChapter.order())
            .sorted((left, right) -> Integer.compare(left.order(), right.order()))
            .toList();

        if (previousChapters.isEmpty()) {
            return;
        }

        lines.add("已写章节上下文(仅供承接剧情,禁止复述,总结或预告):");
        Chapter lastChapter = previousChapters.getLast();
        for (Chapter previous : previousChapters) {
            lines.add("---");
            lines.add("第 " + previous.order() + " 章<" + previous.title() + ">");
            if (!previous.summary().isBlank()) {
                lines.add("摘要:" + previous.summary());
            }
            if (!previous.purpose().isBlank()) {
                lines.add("目的:" + previous.purpose());
            }
            if (previous.id().equals(lastChapter.id())) {
                String draft = previous.draft() == null ? "" : previous.draft().trim();
                if (!draft.isBlank()) {
                    lines.add("正文:");
                    lines.add(truncateTail(draft, 6000));
                }
            }
        }
        lines.add("---");
        lines.add("续写要求:从本章要求自然承接上文,时间线与因果不可矛盾,勿重复已写过的场景。");
        lines.add("");
    }

    private String truncateTail(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return "..." + text.substring(text.length() - maxChars);
    }

    public String buildRepairInstruction(ComplianceReport report) {
        List<String> lines = new ArrayList<>();
        lines.add("你上一版输出未通过合规检查,必须重写。");
        lines.add("重写要求:");
        lines.add("- 只输出小说正文。");

        if (!report.metaLabelHits().isEmpty()) {
            lines.add("- 删除所有元文本标签:" + String.join(",", report.metaLabelHits()));
        }
        if (!report.narrationMetaHits().isEmpty()) {
            lines.add("- 删除旁白/解说式语句:" + String.join(",", report.narrationMetaHits()));
        }
        if (!report.missingMandatoryBeats().isEmpty()) {
            lines.add("- 必须包含以下情节点:" + String.join(",", report.missingMandatoryBeats()));
        }
        if (!report.missingOutlineAnchors().isEmpty()) {
            lines.add("- 必须覆盖大纲约束:" + String.join(";", report.missingOutlineAnchors()));
        }
        if (!report.missingChapterAnchors().isEmpty()) {
            lines.add("- 必须体现章节锚点:" + String.join(";", report.missingChapterAnchors()));
        }
        if (!report.forbiddenHits().isEmpty()) {
            lines.add("- 禁止出现以下内容:" + String.join(",", report.forbiddenHits()));
        }
        return String.join("\n", lines);
    }

    public record GenerationContext(
        Chapter chapter,
        List<OutlineNode> outlineNodes,
        List<CharacterProfile> characters,
        List<ForeshadowingItem> foreshadowing
    ) {
    }
}

