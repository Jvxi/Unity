package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.ImportedChapter;
import com.jvxi.unity.novel.model.ImportedCharacter;
import com.jvxi.unity.novel.model.ImportedOutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectImportAnalysis;
import com.jvxi.unity.novel.model.ProjectMeta;

@Component
public class ProjectImportService {
    private static final int MAX_TEXT_LENGTH = 80_000;
    private static final int OUTLINE_AI_INPUT = 32_000;
    private static final int CHAPTER_META_BATCH = 20;
    private static final int CHAPTER_PREVIEW_CHARS = 180;
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final ExecutorService IMPORT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final GenerationService generationService;
    private final ObjectMapper objectMapper;

    public ProjectImportService(GenerationService generationService, ObjectMapper objectMapper) {
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    public ProjectImportAnalysis analyze(Project project, String outlineText, String chaptersText) {
        ProjectImportAnalysis merged = ProjectImportAnalysis.empty();
        String outline = normalizeInput(outlineText);
        String chapters = normalizeInput(chaptersText);
        if (!outline.isBlank()) {
            merged = mergeAnalysis(merged, analyzeOutline(project, outline));
        }
        if (!chapters.isBlank()) {
            merged = mergeAnalysis(merged, analyzeChapters(project, chapters));
        }
        if (outline.isBlank() && chapters.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请上传或粘贴大纲、章节正文至少一项。");
        }
        ensureHasContent(merged);
        return merged;
    }

    public ProjectImportAnalysis analyzeOutline(Project project, String outlineText) {
        ensureRemoteReady(project);
        String outline = normalizeInput(outlineText);
        if (outline.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请粘贴或上传大纲/设定文本。");
        }
        String bookHint = bookHint(project.meta());
        String outlineForAi = truncate(outline, OUTLINE_AI_INPUT);
        CompletableFuture<BookMetaSlice> bookFuture = CompletableFuture.supplyAsync(
            () -> safeFetchBookMeta(project, bookHint, outlineForAi),
            IMPORT_EXECUTOR
        );
        CompletableFuture<List<ImportedCharacter>> charactersFuture = CompletableFuture.supplyAsync(
            () -> safeFetchCharacters(project, bookHint, outlineForAi),
            IMPORT_EXECUTOR
        );
        CompletableFuture<OutlineSlice> outlineFuture = CompletableFuture.supplyAsync(
            () -> fetchOutlineSlice(project, bookHint, outlineForAi),
            IMPORT_EXECUTOR
        );
        try {
            CompletableFuture.allOf(bookFuture, charactersFuture, outlineFuture).join();
            MetaSlice meta = MetaSlice.merge(bookFuture.get(), charactersFuture.get());
            OutlineSlice outlineSlice = outlineFuture.get();
            ProjectImportAnalysis analysis = new ProjectImportAnalysis(
                meta.title(),
                meta.synopsis(),
                meta.premise(),
                meta.tone(),
                meta.targetLength(),
                meta.styleRules(),
                meta.worldRules(),
                outlineSlice.outlineNodes(),
                List.of(),
                meta.characters(),
                outlineSlice.foreshadowing()
            );
            ensureHasContent(analysis);
            return analysis;
        } catch (Exception exception) {
            throw wrapFailure(exception);
        }
    }

    public ProjectImportAnalysis analyzeChapters(Project project, String chaptersText) {
        ensureRemoteReady(project);
        String chapters = normalizeInput(chaptersText);
        if (chapters.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请粘贴或上传章节正文。");
        }
        String bookHint = bookHint(project.meta());
        List<ImportedChapter> importedChapters = fetchChapters(project, bookHint, chapters);
        ProjectImportAnalysis analysis = new ProjectImportAnalysis(
            "",
            "",
            "",
            "",
            "",
            List.of(),
            List.of(),
            List.of(),
            importedChapters,
            List.of(),
            List.of()
        );
        if (importedChapters.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "未能从章节正文中提取到章节，请检查是否用「第X章」等标题分隔。");
        }
        return analysis;
    }

    private ProjectImportAnalysis mergeAnalysis(ProjectImportAnalysis base, ProjectImportAnalysis patch) {
        return new ProjectImportAnalysis(
            pick(base.title(), patch.title()),
            pick(base.synopsis(), patch.synopsis()),
            pick(base.premise(), patch.premise()),
            pick(base.tone(), patch.tone()),
            pick(base.targetLength(), patch.targetLength()),
            patch.styleRules().isEmpty() ? base.styleRules() : patch.styleRules(),
            patch.worldRules().isEmpty() ? base.worldRules() : patch.worldRules(),
            patch.outlineNodes().isEmpty() ? base.outlineNodes() : patch.outlineNodes(),
            patch.chapters().isEmpty() ? base.chapters() : patch.chapters(),
            patch.characters().isEmpty() ? base.characters() : patch.characters(),
            patch.foreshadowing().isEmpty() ? base.foreshadowing() : patch.foreshadowing()
        );
    }

    private String pick(String base, String patch) {
        return patch == null || patch.isBlank() ? base : patch;
    }

    private BookMetaSlice safeFetchBookMeta(Project project, String bookHint, String outline) {
        try {
            return fetchBookMeta(project, bookHint, outline);
        } catch (Exception ignored) {
            return BookMetaSlice.empty();
        }
    }

    private List<ImportedCharacter> safeFetchCharacters(Project project, String bookHint, String outline) {
        try {
            return fetchCharacters(project, bookHint, outline);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private BookMetaSlice fetchBookMeta(Project project, String bookHint, String outline) {
        String systemPrompt = "你是资深网络小说编辑。只输出 JSON，禁止 Markdown 与解释。" +
            "格式：{\"title\":\"书名或空\",\"synopsis\":\"作品简介<=500字\",\"premise\":\"核心设定<=300字\",\"tone\":\"基调\",\"targetLength\":\"篇幅预期\",\"styleRules\":[\"文风规则\"],\"worldRules\":[\"世界观规则\"]} " +
            "规则：根据文本提取书籍信息；不要输出 characters、outlineNodes、foreshadowing；字符串单行，禁止英文双引号。";

        String userPrompt = bookHint + "\n\n【大纲/设定文本】\n" + outline + "\n\n请提取书籍信息，输出 JSON。";
        return parseBookMeta(callWithRetry(project, systemPrompt, userPrompt, AiCompletionOptions.jsonImportBookMeta()));
    }

    private List<ImportedCharacter> fetchCharacters(Project project, String bookHint, String outline) {
        String systemPrompt = "你是资深网络小说编辑。只输出 JSON，禁止 Markdown 与解释。" +
            "格式：{\"characters\":[{\"name\":\"姓名\",\"role\":\"主角/配角等\",\"profile\":\"人物简介\",\"motivation\":\"动机\",\"constraint\":\"限制\",\"relationships\":\"关系\"}]} " +
            "规则：仅提取主要角色 2-8 个；无则空数组；字符串单行，禁止英文双引号。";

        String userPrompt = bookHint + "\n\n【大纲/设定文本】\n" + outline + "\n\n请仅提取角色列表，输出 JSON。";
        return parseCharactersOnly(
            callWithRetry(project, systemPrompt, userPrompt, AiCompletionOptions.jsonImportCharacters())
        );
    }

    private OutlineSlice fetchOutlineSlice(Project project, String bookHint, String outline) {
        String systemPrompt = "你是资深网络小说编辑。只输出 JSON，禁止 Markdown 与解释。" +
            "格式：{\"outlineNodes\":[{\"title\":\"阶段名\",\"summary\":\"梗概\",\"objective\":\"目标\",\"keyConflict\":\"冲突\",\"mustKeep\":[\"要点\"],\"forbidden\":[\"禁写\"]}],\"foreshadowing\":[{\"title\":\"伏笔标题\",\"setup\":\"埋设说明\",\"payoff\":\"揭示/回收方式\",\"plannedReveal\":\"计划在何处揭示\",\"status\":\"planned\"}]} " +
            "规则：outlineNodes 按剧情阶段拆分 4-10 个；foreshadowing 提取文本中已构思的伏笔/悬念线 0-12 条，无则空数组；status 用 planned/revealed/paid_off 之一；字符串单行，禁止英文双引号。";

        String userPrompt = bookHint + "\n\n【大纲/设定文本】\n" + outline + "\n\n请提取大纲阶段与伏笔，输出 JSON。";

        return parseOutlineSlice(callWithRetry(project, systemPrompt, userPrompt, AiCompletionOptions.jsonImportOutline()));
    }

    private List<ImportedChapter> fetchChapters(Project project, String bookHint, String chapters) {
        List<ChapterImportSplitter.LocalChapter> localChapters = ChapterImportSplitter.split(chapters);
        if (localChapters.isEmpty()) {
            localChapters = List.of(
                new ChapterImportSplitter.LocalChapter(
                    1,
                    "第 1 章",
                    ChapterImportSplitter.truncateContent(chapters)
                )
            );
        }

        Map<Integer, ChapterMeta> metaByOrder = fetchChapterMetadata(project, bookHint, localChapters);
        List<ImportedChapter> result = new ArrayList<>();
        for (ChapterImportSplitter.LocalChapter local : localChapters) {
            ChapterMeta meta = metaByOrder.getOrDefault(local.order(), ChapterMeta.empty());
            String title = meta.title().isBlank() ? local.title() : meta.title();
            result.add(new ImportedChapter(
                local.order(),
                title,
                meta.summary(),
                meta.purpose(),
                local.content()
            ));
        }
        return result;
    }

    private Map<Integer, ChapterMeta> fetchChapterMetadata(
        Project project,
        String bookHint,
        List<ChapterImportSplitter.LocalChapter> localChapters
    ) {
        Map<Integer, ChapterMeta> merged = new HashMap<>();
        for (int offset = 0; offset < localChapters.size(); offset += CHAPTER_META_BATCH) {
            int end = Math.min(offset + CHAPTER_META_BATCH, localChapters.size());
            List<ChapterImportSplitter.LocalChapter> batch = localChapters.subList(offset, end);
            try {
                merged.putAll(fetchChapterMetadataBatch(project, bookHint, batch));
            } catch (ApiException exception) {
                for (ChapterImportSplitter.LocalChapter local : batch) {
                    merged.putIfAbsent(local.order(), ChapterMeta.fromTitle(local.title()));
                }
            }
        }
        return merged;
    }

    private Map<Integer, ChapterMeta> fetchChapterMetadataBatch(
        Project project,
        String bookHint,
        List<ChapterImportSplitter.LocalChapter> batch
    ) {
        String systemPrompt = "你是资深网络小说编辑。只输出 JSON，禁止 Markdown 与解释。" +
            "格式：{\"chapters\":[{\"order\":1,\"title\":\"章节标题\",\"summary\":\"梗概<=120字\",\"purpose\":\"本章目的\"}]} " +
            "规则：根据给出的章节序号、标题与开头摘录写 summary 与 purpose；禁止输出 content 字段；order 必须与输入一致；字符串单行，禁止英文双引号。";

        StringBuilder listing = new StringBuilder();
        for (ChapterImportSplitter.LocalChapter local : batch) {
            String preview = previewForMeta(local.content());
            listing.append(local.order())
                .append(". ")
                .append(local.title())
                .append("：")
                .append(preview)
                .append("\n");
        }

        String userPrompt = bookHint
            + "\n\n【章节列表（仅根据摘录写梗概，不要复述正文）】\n"
            + listing
            + "\n请输出 JSON。";

        List<ImportedChapter> parsed = parseChapters(
            callWithRetry(project, systemPrompt, userPrompt, AiCompletionOptions.jsonImportChapters())
        );
        Map<Integer, ChapterMeta> meta = new HashMap<>();
        for (ImportedChapter chapter : parsed) {
            meta.put(chapter.order(), new ChapterMeta(chapter.title(), chapter.summary(), chapter.purpose()));
        }
        return meta;
    }

    private String previewForMeta(String content) {
        String trimmed = content == null ? "" : content.replace('\n', ' ').trim();
        if (trimmed.length() <= CHAPTER_PREVIEW_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, CHAPTER_PREVIEW_CHARS) + "…";
    }

    private String callWithRetry(Project project, String systemPrompt, String userPrompt, AiCompletionOptions options) {
        ApiException lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return generationService.completeWithPrompts(
                    project,
                    systemPrompt,
                    attempt == 1 ? userPrompt : userPrompt + "\n【重试】仅输出合法 JSON。",
                    options
                );
            } catch (ApiException exception) {
                lastError = exception;
            } catch (Exception exception) {
                lastError = new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    exception.getMessage() == null ? "解析导入内容失败" : exception.getMessage()
                );
            }
        }
        throw lastError == null
            ? new ApiException(HttpStatus.BAD_GATEWAY, "分析导入内容失败")
            : lastError;
    }

    private BookMetaSlice parseBookMeta(String raw) {
        String json = normalizeJsonText(extractJsonPayload(raw));
        try {
            return toBookMetaSlice(objectMapper.readTree(json));
        } catch (JsonProcessingException exception) {
            try {
                return toBookMetaSlice(objectMapper.readTree(repairJson(json)));
            } catch (JsonProcessingException ignored) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "解析书籍信息失败，请缩短大纲后重试。");
            }
        }
    }

    private BookMetaSlice toBookMetaSlice(JsonNode root) {
        return new BookMetaSlice(
            text(root.path("title")),
            text(root.path("synopsis")),
            text(root.path("premise")),
            text(root.path("tone")),
            text(root.path("targetLength")),
            readStringArray(root.path("styleRules")),
            readStringArray(root.path("worldRules"))
        );
    }

    private List<ImportedCharacter> parseCharactersOnly(String raw) {
        String json = normalizeJsonText(extractJsonPayload(raw));
        try {
            JsonNode root = objectMapper.readTree(json);
            return parseCharacters(root.path("characters"));
        } catch (JsonProcessingException exception) {
            try {
                JsonNode root = objectMapper.readTree(repairJson(json));
                return parseCharacters(root.path("characters"));
            } catch (JsonProcessingException ignored) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "解析角色列表失败，请缩短大纲后重试。");
            }
        }
    }

    private OutlineSlice parseOutlineSlice(String raw) {
        String json = normalizeJsonText(extractJsonPayload(raw));
        try {
            JsonNode root = objectMapper.readTree(json);
            return toOutlineSlice(root);
        } catch (JsonProcessingException exception) {
            try {
                JsonNode root = objectMapper.readTree(repairJson(json));
                return toOutlineSlice(root);
            } catch (JsonProcessingException ignored) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "解析大纲阶段失败：返回的 JSON 不完整，请缩短大纲文本后重试。"
                );
            }
        }
    }

    private OutlineSlice toOutlineSlice(JsonNode root) {
        return new OutlineSlice(
            parseOutlineNodeList(root.path("outlineNodes")),
            parseForeshadowingList(root.path("foreshadowing"))
        );
    }

    private List<ImportedChapter> parseChapters(String raw) {
        String json = normalizeJsonText(extractJsonPayload(raw));
        try {
            JsonNode root = objectMapper.readTree(json);
            return parseChapterList(root.path("chapters"));
        } catch (JsonProcessingException exception) {
            try {
                JsonNode root = objectMapper.readTree(repairJson(json));
                return parseChapterList(root.path("chapters"));
            } catch (JsonProcessingException ignored) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "解析章节失败：返回的 JSON 不完整。请减少章节数量或缩短单章长度后重试。"
                );
            }
        }
    }

    private List<ImportedOutlineNode> parseOutlineNodeList(JsonNode outlineArray) {
        List<ImportedOutlineNode> outlineNodes = new ArrayList<>();
        if (!outlineArray.isArray()) {
            return outlineNodes;
        }
        for (JsonNode node : outlineArray) {
            String title = text(node.path("title"));
            if (title.isBlank()) {
                continue;
            }
            outlineNodes.add(new ImportedOutlineNode(
                title,
                text(node.path("summary")),
                text(node.path("objective")),
                text(node.path("keyConflict")),
                readStringArray(node.path("mustKeep")),
                readStringArray(node.path("forbidden"))
            ));
        }
        return outlineNodes;
    }

    private List<ProjectImportAnalysis.ImportedForeshadowing> parseForeshadowingList(JsonNode array) {
        List<ProjectImportAnalysis.ImportedForeshadowing> items = new ArrayList<>();
        if (!array.isArray()) {
            return items;
        }
        for (JsonNode node : array) {
            String title = text(node.path("title"));
            String setup = text(node.path("setup"));
            if (title.isBlank() && setup.isBlank()) {
                continue;
            }
            items.add(new ProjectImportAnalysis.ImportedForeshadowing(
                title.isBlank() ? "伏笔" : title,
                setup,
                text(node.path("payoff")),
                text(node.path("plannedReveal")),
                normalizeForeshadowingStatus(text(node.path("status")))
            ));
        }
        return items;
    }

    private String normalizeForeshadowingStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase();
        if ("revealed".equals(value) || "planted".equals(value)) {
            return "revealed";
        }
        if ("paid_off".equals(value) || "resolved".equals(value)) {
            return "paid_off";
        }
        return "planned";
    }

    private List<ImportedChapter> parseChapterList(JsonNode chapterArray) {
        List<ImportedChapter> chapters = new ArrayList<>();
        if (!chapterArray.isArray()) {
            return chapters;
        }
        int fallbackOrder = 1;
        for (JsonNode node : chapterArray) {
            String title = text(node.path("title"));
            String content = text(node.path("content"));
            String summary = text(node.path("summary"));
            String purpose = text(node.path("purpose"));
            if (title.isBlank() && content.isBlank() && summary.isBlank() && purpose.isBlank()) {
                continue;
            }
            int order = node.path("order").asInt(fallbackOrder);
            fallbackOrder = Math.max(fallbackOrder, order) + 1;
            chapters.add(new ImportedChapter(
                order,
                title.isBlank() ? "第 " + order + " 章" : title,
                summary,
                purpose,
                content
            ));
        }
        return chapters;
    }

    private List<ImportedCharacter> parseCharacters(JsonNode characterArray) {
        List<ImportedCharacter> characters = new ArrayList<>();
        if (!characterArray.isArray()) {
            return characters;
        }
        for (JsonNode node : characterArray) {
            String name = text(node.path("name"));
            if (name.isBlank()) {
                continue;
            }
            characters.add(new ImportedCharacter(
                name,
                text(node.path("role")),
                text(node.path("profile")),
                text(node.path("motivation")),
                text(node.path("constraint")),
                text(node.path("relationships"))
            ));
        }
        return characters;
    }

    private void ensureHasContent(ProjectImportAnalysis analysis) {
        boolean hasMeta = !analysis.synopsis().isBlank()
            || !analysis.premise().isBlank()
            || !analysis.tone().isBlank()
            || !analysis.title().isBlank()
            || !analysis.styleRules().isEmpty()
            || !analysis.worldRules().isEmpty();
        boolean hasOutline = !analysis.outlineNodes().isEmpty();
        boolean hasChapters = !analysis.chapters().isEmpty();
        boolean hasCharacters = !analysis.characters().isEmpty();
        boolean hasForeshadowing = !analysis.foreshadowing().isEmpty();
        if (!hasMeta && !hasOutline && !hasChapters && !hasCharacters && !hasForeshadowing) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "未能从文本中提取到有效内容，请检查文本是否完整或稍后重试。"
            );
        }
    }

    private ApiException wrapFailure(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        if (cause instanceof ApiException apiException) {
            return apiException;
        }
        return new ApiException(
            HttpStatus.BAD_GATEWAY,
            cause.getMessage() == null ? "分析导入内容失败" : cause.getMessage()
        );
    }

    private String bookHint(ProjectMeta meta) {
        return """
            当前书籍：书名=%s，频道=%s，类型=%s
            """.formatted(
            blank(meta.title(), "未命名"),
            "female".equals(meta.audienceChannel()) ? "女频" : "男频",
            blank(meta.genre(), "未设")
        ).trim();
    }

    private void ensureRemoteReady(Project project) {
        if (!generationService.isRemoteModelReady(project.aiSettings())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "请先在 AI 设置中启用远程模型并填写 API Key，再使用导入分析。"
            );
        }
    }

    private String normalizeInput(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "\n…（已截断）";
    }

    private String extractJsonPayload(String raw) {
        if (raw == null) {
            return "{}";
        }
        var matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }

    private String normalizeJsonText(String json) {
        return json == null ? "{}" : json.replace('\u201c', '"').replace('\u201d', '"').trim();
    }

    private String repairJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        String repaired = json.trim();
        if (!repaired.endsWith("}")) {
            int lastBrace = repaired.lastIndexOf('}');
            if (lastBrace > 0) {
                repaired = repaired.substring(0, lastBrace + 1);
            } else {
                repaired = repaired + "}";
            }
        }
        repaired = repaired.replaceAll(",\\s*}", "}");
        repaired = repaired.replaceAll(",\\s*]", "]");
        return repaired;
    }

    private List<String> readStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode entry : node) {
            String value = text(entry);
            if (!value.isBlank()) {
                lines.add(value);
            }
        }
        return lines;
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record BookMetaSlice(
        String title,
        String synopsis,
        String premise,
        String tone,
        String targetLength,
        List<String> styleRules,
        List<String> worldRules
    ) {
        static BookMetaSlice empty() {
            return new BookMetaSlice("", "", "", "", "", List.of(), List.of());
        }
    }

    private record MetaSlice(
        String title,
        String synopsis,
        String premise,
        String tone,
        String targetLength,
        List<String> styleRules,
        List<String> worldRules,
        List<ImportedCharacter> characters
    ) {
        static MetaSlice merge(BookMetaSlice book, List<ImportedCharacter> characters) {
            return new MetaSlice(
                book.title(),
                book.synopsis(),
                book.premise(),
                book.tone(),
                book.targetLength(),
                book.styleRules(),
                book.worldRules(),
                characters == null ? List.of() : characters
            );
        }
    }

    private record OutlineSlice(
        List<ImportedOutlineNode> outlineNodes,
        List<ProjectImportAnalysis.ImportedForeshadowing> foreshadowing
    ) {
    }

    private record ChapterMeta(String title, String summary, String purpose) {
        static ChapterMeta empty() {
            return new ChapterMeta("", "", "");
        }

        static ChapterMeta fromTitle(String title) {
            return new ChapterMeta(title == null ? "" : title.trim(), "", "");
        }
    }
}

