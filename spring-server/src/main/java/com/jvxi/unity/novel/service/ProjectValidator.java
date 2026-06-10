package com.jvxi.unity.novel.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ForeshadowingItem;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;

@Component
public class ProjectValidator {
    public void validateReadyForWriting(Project project) {
        boolean hasOnboarding = project.onboarding() != null && project.onboarding().completed();
        boolean hasOutline = project.outlineNodes() != null && !project.outlineNodes().isEmpty();
        if (!hasOnboarding && !hasOutline) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先完成开书 15 问或通过灵感向导设定大纲，再使用 AI 写作。");
        }
    }

    public void validateForOnboardingQuestions(Project project) {
        requireNotBlank(project.meta().title(), "请先填写书名，再开始开书问卷。");
        requireNotBlank(project.meta().synopsis(), "请先填写作品简介，再开始开书问卷。");
        requireNotBlank(project.meta().premise(), "请先填写核心设定，再开始开书问卷。");
        requireNotBlank(project.meta().audienceChannel(), "请先选择男频/女频。");
        requireNotBlank(project.meta().novelType(), "请先选择小说类型。");
    }

    /** 灵感向导：仅需书名与频道类型，不要求已有大纲或前提 */
    public void validateForOutlineBootstrap(Project project) {
        requireNotBlank(project.meta().audienceChannel(), "请先选择男频/女频。");
        requireNotBlank(project.meta().novelType(), "请先选择小说类型。");
    }

    /** 开书未完成时的草稿校验（允许空前提、无章节） */
    public void validateDraft(Project project) {
        requireNotBlank(project.meta().title(), "书名不能为空。");
        requireNotBlank(project.meta().audienceChannel(), "请先选择男频/女频。");
        requireNotBlank(project.meta().novelType(), "请先选择小说类型。");
        requireNotBlank(project.meta().genre(), "类型标签不能为空。");
        ensureUniqueIds(project.outlineNodes(), OutlineNode::id, "Duplicate outline node id detected.");
        ensureUniqueIds(project.characters(), CharacterProfile::id, "Duplicate character id detected.");
        ensureUniqueIds(project.foreshadowing(), ForeshadowingItem::id, "Duplicate foreshadowing id detected.");
        ensureUniqueIds(project.chapters(), Chapter::id, "Duplicate chapter id detected.");
    }

    public void validateChapterForGeneration(Project project, String chapterId) {
        validateReadyForWriting(project);
        Chapter chapter = project.chapters().stream()
            .filter(entry -> entry.id().equals(chapterId))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到该章节。"));

        if (project.meta().strictMode() && chapter.outlineNodeIds().isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "严格模式下需要绑定至少一个大纲节点。"
            );
        }
    }

    public void validate(Project project) {
        requireNotBlank(project.meta().title(), "Project title cannot be empty.");
        requireNotBlank(project.meta().audienceChannel(), "Audience channel cannot be empty.");
        requireNotBlank(project.meta().novelType(), "Novel type cannot be empty.");
        requireNotBlank(project.meta().genre(), "Project genre cannot be empty.");
        requireNotBlank(project.meta().synopsis(), "Project synopsis cannot be empty.");
        requireNotBlank(project.meta().premise(), "Project premise cannot be empty.");
        requireNotBlank(project.meta().tone(), "Project tone cannot be empty.");
        requireNotBlank(project.meta().targetLength(), "Project target length cannot be empty.");

        ensureUniqueIds(project.outlineNodes(), OutlineNode::id, "Duplicate outline node id detected.");
        ensureUniqueIds(project.characters(), CharacterProfile::id, "Duplicate character id detected.");
        ensureUniqueIds(project.foreshadowing(), ForeshadowingItem::id, "Duplicate foreshadowing id detected.");
        ensureUniqueIds(project.chapters(), Chapter::id, "Duplicate chapter id detected.");

        if (project.chapters().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one chapter is required.");
        }

        Set<String> outlineIds = collectIds(project.outlineNodes(), OutlineNode::id);
        Set<String> characterIds = collectIds(project.characters(), CharacterProfile::id);
        Set<String> foreshadowingIds = collectIds(project.foreshadowing(), ForeshadowingItem::id);

        for (Chapter chapter : project.chapters()) {
            if (chapter.order() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Chapter order must be positive.");
            }
            ensureReferenceExists(chapter.outlineNodeIds(), outlineIds, "Chapter references unknown outline node id.");
            ensureReferenceExists(chapter.characterIds(), characterIds, "Chapter references unknown character id.");
            ensureReferenceExists(chapter.foreshadowingIds(), foreshadowingIds, "Chapter references unknown foreshadowing id.");
        }
    }

    private <T> void ensureUniqueIds(List<T> items, Function<T, String> idGetter, String message) {
        Set<String> seen = new HashSet<>();
        for (T item : items) {
            String id = idGetter.apply(item);
            requireNotBlank(id, "Entity id cannot be empty.");
            if (!seen.add(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    private <T> Set<String> collectIds(List<T> items, Function<T, String> idGetter) {
        Set<String> ids = new HashSet<>();
        for (T item : items) {
            ids.add(idGetter.apply(item));
        }
        return ids;
    }

    private void ensureReferenceExists(List<String> ids, Set<String> allowedIds, String message) {
        for (String id : ids) {
            if (!allowedIds.contains(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, message + " (" + id + ")");
            }
        }
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }
}

