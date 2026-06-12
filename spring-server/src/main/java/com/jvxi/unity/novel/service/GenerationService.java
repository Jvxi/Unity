package com.jvxi.unity.novel.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.jvxi.unity.novel.exception.GenerationCancelledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ForeshadowingItem;
import com.jvxi.unity.novel.model.OnboardingState;
import com.jvxi.unity.novel.model.ProjectMeta;
import com.jvxi.unity.novel.model.ChapterGenerationResponse;
import com.jvxi.unity.novel.model.ComplianceReport;
import com.jvxi.unity.novel.model.NovelTypeInfo;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectEnvelope;
import com.jvxi.unity.novel.model.PublishPlatformInfo;
import com.jvxi.unity.novel.service.PromptContextAugmenter.PromptAugmentation;
import com.jvxi.unity.novel.service.PromptBuilder.GenerationContext;

@Service
public class GenerationService {
    private final ProjectStore projectStore;
    private final ProjectNormalizer normalizer;
    private final ProjectValidator validator;
    private final PromptBuilder promptBuilder;
    private final ComplianceChecker complianceChecker;
    private final SystemPromptComposer systemPromptComposer;
    private final PublishPlatformCatalog publishPlatformCatalog;
    private final NovelTypeCatalog novelTypeCatalog;
    private final TokenUsageService tokenUsageService;
    private final AiGatewayService aiGatewayService;
    private final AiJsonRepairService aiJsonRepairService;
    private final PromptContextAugmenter promptContextAugmenter;
    private final ObjectMapper objectMapper;

    public GenerationService(
        ProjectStore projectStore,
        ProjectNormalizer normalizer,
        ProjectValidator validator,
        PromptBuilder promptBuilder,
        ComplianceChecker complianceChecker,
        SystemPromptComposer systemPromptComposer,
        PublishPlatformCatalog publishPlatformCatalog,
        NovelTypeCatalog novelTypeCatalog,
        TokenUsageService tokenUsageService,
        AiGatewayService aiGatewayService,
        AiJsonRepairService aiJsonRepairService,
        PromptContextAugmenter promptContextAugmenter,
        ObjectMapper objectMapper
    ) {
        this.projectStore = projectStore;
        this.normalizer = normalizer;
        this.validator = validator;
        this.promptBuilder = promptBuilder;
        this.complianceChecker = complianceChecker;
        this.systemPromptComposer = systemPromptComposer;
        this.publishPlatformCatalog = publishPlatformCatalog;
        this.novelTypeCatalog = novelTypeCatalog;
        this.tokenUsageService = tokenUsageService;
        this.aiGatewayService = aiGatewayService;
        this.aiJsonRepairService = aiJsonRepairService;
        this.promptContextAugmenter = promptContextAugmenter;
        this.objectMapper = objectMapper;
    }

    public ChapterGenerationResponse generateChapter(String chapterId) {
        return generateChapter(chapterId, null);
    }

    public ChapterGenerationResponse generateChapter(String chapterId, AiSettings aiSettings) {
        try {
            List<String> warnings = new ArrayList<>();
            GenerationSession session = prepareSession(chapterId, aiSettings, warnings);
            StringBuilder draftBuilder = new StringBuilder();
            String provider = generateDraftContent(session, draftBuilder, null, warnings, null);
            return finalizeGeneration(session, chapterId, draftBuilder.toString(), provider, warnings);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void streamGenerateChapter(String chapterId, SseEmitter emitter) {
        streamGenerateChapter(chapterId, emitter, false);
    }

    public void streamGenerateChapter(String chapterId, SseEmitter emitter, boolean continueMode) {
        streamGenerateChapter(chapterId, emitter, continueMode, null);
    }

    public void streamGenerateChapter(String chapterId, SseEmitter emitter, boolean continueMode, AiSettings aiSettings) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(throwable -> cancelled.set(true));

        try {
            List<String> warnings = new ArrayList<>();
            GenerationSession session = prepareSession(chapterId, aiSettings, warnings);
            StringBuilder draftBuilder = new StringBuilder();

            // 续写模式：预填已有内容
            String existingDraft = "";
            if (continueMode) {
                Chapter chapter = session.context.chapter();
                existingDraft = chapter.draft() == null ? "" : chapter.draft().trim();
                if (!existingDraft.isEmpty()) {
                    draftBuilder.append(existingDraft);
                    // 通知前端已有内容
                    sendStreamEvent(emitter, "delta", Map.of("content", existingDraft));
                }
            }

            Consumer<String> onDelta = chunk -> {
                ensureNotCancelled(cancelled);
                draftBuilder.append(chunk);
                // 续写模式下检查上限 4500 字
                if (continueMode && countPureText(draftBuilder.toString()) > 4500) {
                    return;
                }
                try {
                    sendStreamEvent(emitter, "delta", Map.of("content", chunk));
                } catch (IOException ioException) {
                    throw new GenerationCancelledException();
                }
            };

            String provider = continueMode && !existingDraft.isEmpty()
                ? generateDraftContent(session, draftBuilder, onDelta, warnings, cancelled, existingDraft)
                : generateDraftContent(session, draftBuilder, onDelta, warnings, cancelled);
            if (cancelled.get()) {
                completeCancelledStream(emitter);
                return;
            }

            // 截断到上限
            String fullDraft = draftBuilder.toString();
            if (continueMode && countPureText(fullDraft) > 4500) {
                fullDraft = truncateToPureTextLimit(fullDraft, 4500);
                warnings.add("续写内容已达上限(4500字)，已自动截断。");
            }

            ChapterGenerationResponse response = finalizeGeneration(
                session,
                chapterId,
                fullDraft,
                provider,
                warnings
            );
            Map<String, Object> donePayload = new HashMap<>();
            donePayload.put("result", response);
            sendStreamEvent(emitter, "done", donePayload);
            emitter.complete();
        } catch (GenerationCancelledException cancelledException) {
            completeCancelledStream(emitter);
        } catch (Exception exception) {
            if (cancelled.get()) {
                completeCancelledStream(emitter);
                return;
            }
            try {
                sendStreamEvent(emitter, "error", Map.of(
                    "message",
                    safeErrorMessage(exception, "Stream generation failed.")
                ));
            } catch (IOException ignored) {
                // Ignore secondary failures while reporting the original error.
            }
            emitter.complete();
        }
    }

    public void streamReviewChapters(SseEmitter emitter) {
        streamReviewChapters(emitter, null);
    }

    public void streamReviewChapters(SseEmitter emitter, AiSettings aiSettings) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(throwable -> cancelled.set(true));

        try {
            Project project = normalizer.normalize(projectStore.loadActiveProject().project());
            if (aiSettings != null) {
                project = normalizer.withTransientAiSettings(project, aiSettings);
            }
            List<Chapter> chapters = project.chapters();
            int total = chapters.size();
            int issueCount = 0;
            boolean remoteReviewReady = isRemoteModelReady(project.aiSettings());

            for (int i = 0; i < total; i++) {
                ensureNotCancelled(cancelled);
                Chapter chapter = chapters.get(i);
                String draft = chapter.draft() == null ? "" : chapter.draft().trim();

                sendStreamEvent(emitter, "progress", Map.of(
                    "chapterId", chapter.id(),
                    "chapterTitle", chapter.title() == null ? "" : chapter.title(),
                    "chapterIndex", i,
                    "totalChapters", total
                ));

                if (draft.isEmpty() || draft.length() < 20) {
                    sendStreamEvent(emitter, "result", Map.of(
                        "chapterId", chapter.id(),
                        "chapterTitle", chapter.title() == null ? "" : chapter.title(),
                        "issues", List.of()
                    ));
                    continue;
                }

                List<Map<String, String>> issues;
                if (remoteReviewReady) {
                    issues = reviewDraftWithRemoteModel(project, draft, cancelled);
                } else {
                    issues = localReviewIssues(draft);
                }
                issueCount += issues.size();

                sendStreamEvent(emitter, "result", Map.of(
                    "chapterId", chapter.id(),
                    "chapterTitle", chapter.title() == null ? "" : chapter.title(),
                    "issues", issues
                ));
            }

            sendStreamEvent(emitter, "done", Map.of(
                "reviewedCount", total,
                "issueCount", issueCount
            ));
            emitter.complete();
        } catch (GenerationCancelledException e) {
            completeCancelledStream(emitter);
        } catch (Exception e) {
            try {
                sendStreamEvent(emitter, "error", Map.of(
                    "message", safeErrorMessage(e, "审查失败")
                ));
            } catch (IOException ignored) {
            }
            emitter.complete();
        }
    }

    private List<Map<String, String>> reviewDraftWithRemoteModel(
        Project project,
        String draft,
        AtomicBoolean cancelled
    ) {
        String prompt = "你是一名专业网文编辑。请审查以下小说章节正文，只找出不属于小说正文的内容，" +
            "例如作者备注、AI残留、写作说明、大纲混入、Markdown格式、重复段落或明显逻辑错误。" +
            "请严格输出 JSON 数组：[{\"original\":\"原文片段\",\"description\":\"问题说明\",\"suggestion\":\"修改建议\"}]。" +
            "没有问题时输出 []。\n\n章节正文：\n" + draft;
        try {
            StringBuilder resultBuilder = new StringBuilder();
            streamRemoteModel(project, prompt, null, resultBuilder::append, cancelled);
            return parseReviewIssues(resultBuilder.toString().trim());
        } catch (GenerationCancelledException e) {
            throw e;
        } catch (Exception e) {
            return localReviewIssues(draft);
        }
    }

    private List<Map<String, String>> parseReviewIssues(String raw) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            var array = aiJsonRepairService.readTree(raw);
            for (var node : array) {
                String orig = node.path("original").asText("").trim();
                String desc = node.path("description").asText("").trim();
                String sugg = node.path("suggestion").asText("").trim();
                if (!orig.isBlank()) {
                    result.add(Map.of("original", orig, "description", desc, "suggestion", sugg));
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private List<Map<String, String>> localReviewIssues(String draft) {
        List<Map<String, String>> issues = new ArrayList<>();
        List<String> lines = draft.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        for (String line : lines) {
            if (line.startsWith("#") || line.startsWith("- ") || line.matches("^\\d+\\..*")) {
                issues.add(Map.of(
                    "original", line,
                    "description", "疑似 Markdown、列表或写作说明混入正文。",
                    "suggestion", "删除格式标记，把内容改写为自然的小说正文。"
                ));
            } else if (line.matches("^(大纲|角色设定|角色列表|伏笔|章节说明|写作要求|创作提示|注释|说明|设定)[:：].*")) {
                issues.add(Map.of(
                    "original", line,
                    "description", "疑似大纲或设定说明混入正文。",
                    "suggestion", "只保留角色行动、对话和场景描写。"
                ));
            } else if (ComplianceChecker.NARRATION_META_PHRASES.stream().anyMatch(line::contains)) {
                issues.add(Map.of(
                    "original", line,
                    "description", "疑似作者备注、AI 残留或旁白式说明。",
                    "suggestion", "删除该句，改用具体事件推进剧情。"
                ));
            }
            if (issues.size() >= 20) {
                break;
            }
        }
        return issues;
    }
    /** 统计纯文字数（排除标点空白） */
    private int countPureText(String text) {
        return text.replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "").length();
    }

    /** 按纯文字上限截断（保留到最近的句号/换行） */
    private String truncateToPureTextLimit(String text, int limit) {
        int pureCount = 0;
        int cutIndex = text.length();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isIdeographic(c) && !Character.isLetterOrDigit(c)) {
                // punctuation, skip counting
            } else if (!Character.isWhitespace(c)) {
                pureCount++;
            }
            if (pureCount >= limit) {
                // 找到最近的句号或换行
                for (int j = i; j < Math.min(i + 50, text.length()); j++) {
                    char ch = text.charAt(j);
                    if (ch == '\n' || ch == '。' || ch == '！' || ch == '？' || ch == '…') {
                        cutIndex = j + 1;
                        break;
                    }
                }
                break;
            }
        }
        return text.substring(0, cutIndex);
    }

    private void completeCancelledStream(SseEmitter emitter) {
        try {
            sendStreamEvent(emitter, "cancelled", Map.of("message", "生成已取消"));
            emitter.complete();
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    private void ensureNotCancelled(AtomicBoolean cancelled) {
        if (cancelled != null && cancelled.get()) {
            throw new GenerationCancelledException();
        }
    }

    public String previewSystemPrompt(Project project) {
        return buildSystemPrompt(project, "");
    }

    public boolean isRemoteModelReady(AiSettings settings) {
        return aiGatewayService.isRemoteModelReady(settings);
    }

    public String completeWithPrompts(Project project, String systemPrompt, String userPrompt)
        throws IOException, InterruptedException {
        return completeWithPrompts(project, systemPrompt, userPrompt, null);
    }

    public String completeWithPrompts(
        Project project,
        String systemPrompt,
        String userPrompt,
        AiCompletionOptions options
    ) throws IOException, InterruptedException {
        if (!isRemoteModelReady(project.aiSettings())) {
            throw new IOException("请先启用远程 AI，并填写 Base URL、API Key 与模型名称。");
        }
        return callRemoteModelWithSystem(project, systemPrompt, userPrompt, null, options).content();
    }

    public Map<String, Object> testConnection(AiSettings settings) {
        AiSettings normalized = normalizer.normalizeAiSettingsOnly(settings);

        if (!isRemoteModelReady(normalized)) {
            return Map.of("ok", false, "message", "请先启用远程 AI，并填写 Base URL、API Key 与模型名称。");
        }

        try {
            long started = System.currentTimeMillis();
            AiCompletionOptions probeOptions = AiCompletionOptions.connectionProbe();
            AiGatewayService.CompletionResult probeResult = aiGatewayService.complete(
                normalized,
                "",
                "OK",
                probeOptions
            );
            String content = probeResult.content();
            long elapsed = System.currentTimeMillis() - started;
            String summary = summarizeConnectivityTest(content);
            if (summary.isBlank()) {
                return Map.of(
                    "ok",
                    false,
                    "message",
                    "模型已响应但无法读取回复内容，请检查 Base URL、模型名与 API Key。（" + elapsed + " ms）"
                );
            }
            if ("OK".equals(summary)) {
                return Map.of("ok", true, "message", "OK（" + elapsed + " ms）");
            }
            return Map.of("ok", true, "message", summary + "（" + elapsed + " ms）");
        } catch (Exception exception) {
            return Map.of("ok", false, "message", safeErrorMessage(exception, "连接失败"));
        }
    }

    private GenerationSession prepareSession(String chapterId) {
        return prepareSession(chapterId, null);
    }

    private GenerationSession prepareSession(String chapterId, AiSettings aiSettings) {
        return prepareSession(chapterId, aiSettings, new ArrayList<>());
    }

    private GenerationSession prepareSession(String chapterId, AiSettings aiSettings, List<String> warnings) {
        ProjectEnvelope envelope = projectStore.loadActiveProject();
        String bookId = envelope.bookId();
        Project project = normalizer.normalize(envelope.project());
        if (aiSettings != null) {
            project = normalizer.withTransientAiSettings(project, aiSettings);
        }
        validator.validate(project);
        validator.validateChapterForGeneration(project, chapterId);

        project = autoFillChapterMetadata(project, chapterId);

        GenerationContext context = promptBuilder.resolveContext(project, chapterId);
        String prompt = promptBuilder.buildPrompt(project, context);
        PromptAugmentation augmentation = promptContextAugmenter.augment(bookId, project, context);
        if (!augmentation.text().isBlank()) {
            prompt = prompt + "\n" + augmentation.text();
        }
        warnings.addAll(augmentation.warnings());
        return new GenerationSession(project, context, prompt);
    }

    private Project autoFillChapterMetadata(Project project, String chapterId) {
        Chapter chapter = project.chapters().stream()
            .filter(entry -> entry.id().equals(chapterId))
            .findFirst()
            .orElse(null);
        if (chapter == null) {
            return project;
        }

        boolean needsSummary = chapter.summary() == null || chapter.summary().isBlank();
        boolean needsPurpose = chapter.purpose() == null || chapter.purpose().isBlank();
        boolean needsOutlineNodes = chapter.outlineNodeIds().isEmpty() && !project.outlineNodes().isEmpty();
        boolean needsCharacters = chapter.characterIds().isEmpty() && !project.characters().isEmpty();
        boolean needsForeshadowing = chapter.foreshadowingIds().isEmpty() && !project.foreshadowing().isEmpty();

        if (!needsSummary && !needsPurpose && !needsOutlineNodes && !needsCharacters && !needsForeshadowing) {
            return project;
        }

        List<String> outlineNodeIds = needsOutlineNodes
            ? project.outlineNodes().stream().map(OutlineNode::id).toList()
            : chapter.outlineNodeIds();
        List<String> characterIds = needsCharacters
            ? project.characters().stream().map(CharacterProfile::id).toList()
            : chapter.characterIds();
        List<String> foreshadowingIds = needsForeshadowing
            ? project.foreshadowing().stream().map(ForeshadowingItem::id).toList()
            : chapter.foreshadowingIds();

        List<OutlineNode> boundNodes = project.outlineNodes().stream()
            .filter(node -> outlineNodeIds.contains(node.id()))
            .toList();

        Chapter previousChapter = project.chapters().stream()
            .filter(entry -> entry.order() < chapter.order())
            .max((a, b) -> Integer.compare(a.order(), b.order()))
            .orElse(null);

        String autoSummary = needsSummary ? buildAutoSummary(chapter, boundNodes, previousChapter) : chapter.summary();
        String autoPurpose = needsPurpose ? buildAutoPurpose(chapter, boundNodes) : chapter.purpose();

        Chapter updated = new Chapter(
            chapter.id(), chapter.order(), chapter.title(),
            autoSummary, autoPurpose,
            outlineNodeIds, characterIds, foreshadowingIds,
            chapter.mandatoryBeats(), chapter.forbiddenContent(), chapter.notes(), chapter.draft()
        );

        List<Chapter> updatedChapters = project.chapters().stream()
            .map(entry -> entry.id().equals(chapterId) ? updated : entry)
            .toList();

        return new Project(
            project.meta(), project.aiSettings(), project.onboarding(),
            project.outlineNodes(), project.characters(), project.foreshadowing(),
            updatedChapters, project.updatedAt()
        );
    }

    private String buildAutoSummary(Chapter chapter, List<OutlineNode> boundNodes, Chapter previousChapter) {
        if (!boundNodes.isEmpty()) {
            String nodeSummaries = boundNodes.stream()
                .map(node -> {
                    String title = node.title().isBlank() ? "" : node.title() + "：";
                    String summary = node.summary().isBlank() ? "" : node.summary();
                    return title + summary;
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("；"));
            if (!nodeSummaries.isBlank()) {
                return nodeSummaries;
            }
        }

        if (previousChapter != null && previousChapter.summary() != null && !previousChapter.summary().isBlank()) {
            return "承接上章「" + previousChapter.title() + "」";
        }

        return "第" + chapter.order() + "章：" + chapter.title();
    }

    private String buildAutoPurpose(Chapter chapter, List<OutlineNode> boundNodes) {
        if (!boundNodes.isEmpty()) {
            String objectives = boundNodes.stream()
                .map(OutlineNode::objective)
                .filter(obj -> obj != null && !obj.isBlank())
                .collect(Collectors.joining("；"));
            if (!objectives.isBlank()) {
                return objectives;
            }
        }

        if (chapter.order() > 1) {
            return "推进剧情，承接前文";
        }
        return "推进剧情";
    }

    private Project autoGenerateMetadata(Project project, String chapterId, String draft, List<String> warnings) {
        if (draft.isBlank()) return project;

        try {
            String truncated = draft.length() > 3000 ? draft.substring(0, 3000) : draft;
            String metaPrompt = "根据以下小说正文，生成该章节的标题、摘要和目的。\n\n"
                + "要求：\n"
                + "- 标题：10字以内，概括核心冲突，带追读感\n"
                + "- 摘要：50字以内，概括本章主要剧情\n"
                + "- 目的：30字以内，说明本章在全书中的作用\n\n"
                + "正文：\n" + truncated + "\n\n"
                + "请严格按以下JSON格式输出，不要输出其他内容：\n"
                + "{\"title\":\"...\",\"summary\":\"...\",\"purpose\":\"...\"}";

            StringBuilder metaBuilder = new StringBuilder();
            streamRemoteModel(project, metaPrompt, null, metaBuilder::append, null);
            String raw = metaBuilder.toString().trim();

            if (raw.isBlank()) {
                warnings.add("自动生成标题/摘要/目的失败：AI返回为空。");
                return project;
            }

            String title = "";
            String summary = "";
            String purpose = "";
            try {
                var node = aiJsonRepairService.readTree(raw);
                title = node.path("title").asText("").trim();
                summary = node.path("summary").asText("").trim();
                purpose = node.path("purpose").asText("").trim();
            } catch (Exception parseException) {
                warnings.add("自动生成标题/摘要/目的失败：JSON解析错误。原始内容：" + raw.substring(0, Math.min(100, raw.length())));
                return project;
            }

            if (title.isBlank() && summary.isBlank() && purpose.isBlank()) {
                warnings.add("自动生成标题/摘要/目的失败：AI返回的值为空。");
                return project;
            }

            Chapter oldChapter = project.chapters().stream()
                .filter(ch -> ch.id().equals(chapterId))
                .findFirst()
                .orElse(null);
            if (oldChapter == null) return project;

            List<String> generated = new ArrayList<>();
            String finalTitle = title.isBlank() ? oldChapter.title() : title;
            String finalSummary = summary.isBlank() ? oldChapter.summary() : summary;
            String finalPurpose = purpose.isBlank() ? oldChapter.purpose() : purpose;

            if (!title.isBlank()) generated.add("标题");
            if (!summary.isBlank()) generated.add("摘要");
            if (!purpose.isBlank()) generated.add("目的");

            Chapter newChapter = new Chapter(
                oldChapter.id(), oldChapter.order(),
                finalTitle, finalSummary, finalPurpose,
                oldChapter.outlineNodeIds(), oldChapter.characterIds(), oldChapter.foreshadowingIds(),
                oldChapter.mandatoryBeats(), oldChapter.forbiddenContent(), oldChapter.notes(), oldChapter.draft()
            );

            List<Chapter> updatedChapters = project.chapters().stream()
                .map(ch -> ch.id().equals(chapterId) ? newChapter : ch)
                .toList();

            warnings.add("已自动生成" + String.join("/", generated) + "。");
            return new Project(
                project.meta(), project.aiSettings(), project.onboarding(),
                project.outlineNodes(), project.characters(), project.foreshadowing(),
                updatedChapters, project.updatedAt()
            );
        } catch (Exception e) {
            warnings.add("自动生成标题/摘要/目的失败：" + safeErrorMessage(e, "未知错误"));
            return project;
        }
    }

    private String generateDraftContent(
        GenerationSession session,
        StringBuilder draftBuilder,
        Consumer<String> onDelta,
        List<String> warnings,
        AtomicBoolean cancelled
    ) throws IOException, InterruptedException {
        return generateDraftContent(session, draftBuilder, onDelta, warnings, cancelled, "");
    }

    private String generateDraftContent(
        GenerationSession session,
        StringBuilder draftBuilder,
        Consumer<String> onDelta,
        List<String> warnings,
        AtomicBoolean cancelled,
        String existingDraft
    ) throws IOException, InterruptedException {
        ensureNotCancelled(cancelled);
        String provider = "rule-based-prototype";

        // 续写模式：修改 prompt 指示 AI 继续
        String prompt = session.prompt;
        if (!existingDraft.isEmpty()) {
            String tail = existingDraft.length() > 1500
                ? existingDraft.substring(existingDraft.length() - 1500)
                : existingDraft;
            prompt = "以下是该章节已有的正文内容，请从最后一句自然续写，不要重复已有内容，直接接续剧情。\n\n"
                + "---已有正文(尾部)---\n" + tail + "\n---续写开始---\n\n"
                + prompt;
        }

        if (isRemoteModelReady(session.project.aiSettings())) {
            try {
                if (onDelta != null) {
                    ContentWithUsage streamResult = streamRemoteModel(session.project, prompt, "", onDelta, cancelled);
                    ensureNotCancelled(cancelled);
                    session.lastPromptTokens = streamResult.promptTokens();
                    session.lastCompletionTokens = streamResult.completionTokens();
                    session.lastTotalTokens = streamResult.totalTokens();
                    if (!draftBuilder.isEmpty()) {
                        return aiGatewayService.providerLabel(session.project.aiSettings(), true);
                    }
                } else {
                    ContentWithUsage result = callRemoteModel(session.project, prompt, "", null);
                    draftBuilder.append(result.content());
                    session.lastPromptTokens = result.promptTokens();
                    session.lastCompletionTokens = result.completionTokens();
                    session.lastTotalTokens = result.totalTokens();
                    if (!draftBuilder.isEmpty()) {
                        return aiGatewayService.providerLabel(session.project.aiSettings(), false);
                    }
                }
            } catch (GenerationCancelledException cancelledException) {
                throw cancelledException;
            } catch (Exception exception) {
                warnings.add("远程生成失败，已回退本地模板：" + safeErrorMessage(exception, "未知错误"));
            }
        }

        ensureNotCancelled(cancelled);
        String fallbackDraft = buildRuleBasedDraft(session.project, session.context);
        emitFallbackDraft(fallbackDraft, onDelta, draftBuilder, cancelled);
        return provider;
    }

    private ChapterGenerationResponse finalizeGeneration(
        GenerationSession session,
        String chapterId,
        String rawDraft,
        String provider,
        List<String> warnings
    ) {
        Project project = session.project;
        GenerationContext context = session.context;
        String prompt = session.prompt;

        String sanitizedDraft = sanitizeDraft(rawDraft);
        boolean strictOutline = project.meta().strictMode();
        ComplianceReport compliance = complianceChecker.evaluate(context, sanitizedDraft, strictOutline);

        if (project.meta().strictMode() && !compliance.passed() && !provider.startsWith("rule-based")) {
            try {
                String retryInstruction = promptBuilder.buildRepairInstruction(compliance);
                StringBuilder retryBuilder = new StringBuilder();
                streamRemoteModel(project, prompt, retryInstruction, retryBuilder::append, null);
                String repairedDraft = retryBuilder.toString();
                String repairedSanitizedDraft = sanitizeDraft(repairedDraft);
                ComplianceReport repairedCompliance = complianceChecker.evaluate(context, repairedSanitizedDraft, strictOutline);
                if (repairedCompliance.passed()) {
                    sanitizedDraft = repairedSanitizedDraft;
                    compliance = repairedCompliance;
                    warnings.add("严格模式重试已通过。");
                } else {
                    warnings.add("严格模式重试仍未通过合规检查。");
                }
            } catch (Exception exception) {
                warnings.add("严格模式重试失败：" + safeErrorMessage(exception, "未知错误"));
            }
        }

        boolean accepted = !project.meta().strictMode() || compliance.passed();
        String rejectionReason = accepted ? "" : "严格模式拒绝了本次草稿：存在旁白解说、大纲偏离或禁写项命中。";

        if (accepted) {
            // 自动生成标题/摘要/目的
            project = autoGenerateMetadata(project, chapterId, sanitizedDraft, warnings);
            Project updatedProject = updateChapterDraft(project, chapterId, sanitizedDraft);
            projectStore.saveActiveProject(updatedProject);
        }

        int promptTokens = session.lastPromptTokens;
        int completionTokens = session.lastCompletionTokens;
        int totalTokens = session.lastTotalTokens;
        if (totalTokens > 0) {
            tokenUsageService.recordUsage(promptTokens, completionTokens, totalTokens);
        }

        return new ChapterGenerationResponse(
            chapterId,
            provider,
            accepted,
            sanitizedDraft,
            prompt,
            compliance,
            rejectionReason,
            warnings,
            promptTokens,
            completionTokens,
            totalTokens
        );
    }

    private void emitFallbackDraft(
        String draft,
        Consumer<String> onDelta,
        StringBuilder draftBuilder,
        AtomicBoolean cancelled
    ) {
        if (onDelta == null) {
            draftBuilder.append(draft);
            return;
        }

        String[] chunks = draft.split("(?<=\n\n)");
        for (String chunk : chunks) {
            ensureNotCancelled(cancelled);
            if (chunk.isBlank()) {
                continue;
            }
            onDelta.accept(chunk);
            draftBuilder.append(chunk);
        }
    }

    private void sendStreamEvent(SseEmitter emitter, String type, Map<String, Object> payload) throws IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.putAll(payload);
        try {
            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(event)));
        } catch (IOException | IllegalStateException exception) {
            throw new GenerationCancelledException();
        }
    }

    private ContentWithUsage streamRemoteModel(
        Project project,
        String prompt,
        String extraSystemInstruction,
        Consumer<String> onDelta,
        AtomicBoolean cancelled
    ) throws IOException, InterruptedException {
        String systemPrompt = buildSystemPrompt(project, extraSystemInstruction);
        return streamRemoteModelWithSystem(project, systemPrompt, prompt, onDelta, cancelled);
    }

    private ContentWithUsage callRemoteModel(
        Project project,
        String prompt,
        String extraSystemInstruction,
        Consumer<String> onDelta
    ) throws IOException, InterruptedException {
        String systemPrompt = buildSystemPrompt(project, extraSystemInstruction);
        return callRemoteModelWithSystem(project, systemPrompt, prompt, onDelta);
    }

    private ContentWithUsage callRemoteModelWithSystem(
        Project project,
        String systemPrompt,
        String userPrompt,
        Consumer<String> onDelta
    ) throws IOException, InterruptedException {
        return callRemoteModelWithSystem(project, systemPrompt, userPrompt, onDelta, null);
    }

    private ContentWithUsage callRemoteModelWithSystem(
        Project project,
        String systemPrompt,
        String userPrompt,
        Consumer<String> onDelta,
        AiCompletionOptions options
    ) throws IOException, InterruptedException {
        AiSettings settings = project.aiSettings();

        if (onDelta != null) {
            StringBuilder builder = new StringBuilder();
            ContentWithUsage streamResult = streamRemoteModelWithSystem(project, systemPrompt, userPrompt, chunk -> {
                builder.append(chunk);
                onDelta.accept(chunk);
            }, null);
            return new ContentWithUsage(builder.toString(), streamResult.promptTokens(), streamResult.completionTokens(), streamResult.totalTokens());
        }

        AiGatewayService.CompletionResult result = aiGatewayService.complete(settings, systemPrompt, userPrompt, options);
        return new ContentWithUsage(result.content(), result.promptTokens(), result.completionTokens(), result.totalTokens());
    }

    private String summarizeConnectivityTest(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("OK") || trimmed.matches("(?is).*\\bOK\\b.*")) {
            return "OK";
        }
        if (trimmed.length() > 24) {
            return trimmed.substring(0, 24) + "…";
        }
        return trimmed;
    }

    private ContentWithUsage streamRemoteModelWithSystem(
        Project project,
        String systemPrompt,
        String userPrompt,
        Consumer<String> onDelta,
        AtomicBoolean cancelled
    ) throws IOException, InterruptedException {
        AiSettings settings = project.aiSettings();
        if (onDelta == null) {
            return new ContentWithUsage("", 0, 0, 0);
        }

        AiGatewayService.CompletionResult result = aiGatewayService.stream(
            settings,
            systemPrompt,
            userPrompt,
            null,
            onDelta,
            () -> ensureNotCancelled(cancelled)
        );
        return new ContentWithUsage(result.content(), result.promptTokens(), result.completionTokens(), result.totalTokens());
    }

    private String buildSystemPrompt(Project project, String extraSystemInstruction) {
        PublishPlatformInfo platform = publishPlatformCatalog.resolve(project.meta().publishPlatform());
        NovelTypeInfo novelType = novelTypeCatalog.resolveType(
            project.meta().audienceChannel(),
            project.meta().novelType()
        );
        String systemPrompt = systemPromptComposer.compose(project, platform, novelType);
        if (!text(extraSystemInstruction).isBlank()) {
            systemPrompt = systemPrompt + "\n" + text(extraSystemInstruction);
        }
        return systemPrompt;
    }

    private String safeErrorMessage(Exception exception, String fallback) {
        return aiGatewayService.safeErrorMessage(exception, fallback);
    }

    private Project updateChapterDraft(Project project, String chapterId, String draft) {
        List<Chapter> updatedChapters = project.chapters().stream()
            .map(chapter -> chapter.id().equals(chapterId)
                ? new Chapter(
                    chapter.id(),
                    chapter.order(),
                    chapter.title(),
                    chapter.summary(),
                    chapter.purpose(),
                    chapter.outlineNodeIds(),
                    chapter.characterIds(),
                    chapter.foreshadowingIds(),
                    chapter.mandatoryBeats(),
                    chapter.forbiddenContent(),
                    chapter.notes(),
                    draft
                )
                : chapter)
            .toList();

        return new Project(
            project.meta(),
            project.aiSettings(),
            project.onboarding(),
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            updatedChapters,
            project.updatedAt()
        );
    }

    private String buildRuleBasedDraft(Project project, GenerationContext context) {
        Chapter chapter = context.chapter();
        String lead = context.characters().isEmpty() ? "主角" : context.characters().getFirst().name();
        String premise = text(chapter.summary()).isBlank() ? project.meta().premise() : chapter.summary();

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(lead + "踏进现场时，潮气仍黏在衣领上。" + premise + " 这些碎片信息并不完整，却足以让人意识到局面已经偏离常轨。");
        paragraphs.add("没有人开口解释来龙去脉，只有物证、脚印和彼此回避的目光在无声角力。");

        if (!chapter.mandatoryBeats().isEmpty()) {
            for (String beat : chapter.mandatoryBeats()) {
                paragraphs.add(beat + "。那一瞬间，所有迟疑都被推到了边缘。");
            }
        }

        for (OutlineNode node : context.outlineNodes()) {
            if (!text(node.summary()).isBlank()) {
                paragraphs.add(node.summary() + "。相关细节在暗处彼此咬合，逼得人只能继续向下追。");
            }
            for (String mustKeep : node.mustKeep()) {
                if (!mustKeep.isBlank()) {
                    paragraphs.add(mustKeep + "。这一点在现场留下了无法忽视的痕迹。");
                }
            }
        }

        if (!context.foreshadowing().isEmpty()) {
            paragraphs.add(context.foreshadowing().getFirst().setup() + "。它尚未成形答案，却已经在角落里发出隐约回响。");
        }

        paragraphs.add(lead + "收住话头，把判断压进心里。更重的代价还在后头，而眼前这一步已没有退路。");
        return String.join("\n\n", paragraphs);
    }

    private String sanitizeDraft(String rawDraft) {
        String normalized = rawDraft == null ? "" : rawDraft.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "").trim();
        }

        List<String> lines = normalized.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .filter(line -> !line.startsWith("#"))
            .filter(line -> !line.startsWith("- "))
            .filter(line -> !line.startsWith("* "))
            .filter(line -> !line.matches("^\\d+\\..*"))
            .filter(line -> !line.matches("^(大纲|角色设定|角色列表|伏笔|章节说明|写作要求|创作提示|注释|说明|设定)[:：].*"))
            .filter(line -> !ComplianceChecker.NARRATION_META_PHRASES.stream().anyMatch(line::contains))
            .toList();
        return String.join("\n\n", lines);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private record ContentWithUsage(String content, int promptTokens, int completionTokens, int totalTokens) {
    }

    private static final class GenerationSession {
        Project project;
        GenerationContext context;
        final String prompt;
        int lastPromptTokens;
        int lastCompletionTokens;
        int lastTotalTokens;

        GenerationSession(Project project, GenerationContext context, String prompt) {
            this.project = project;
            this.context = context;
            this.prompt = prompt;
        }
    }
}

