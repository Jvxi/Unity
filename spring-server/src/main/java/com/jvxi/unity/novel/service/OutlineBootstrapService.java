package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.NovelTypeInfo;
import com.jvxi.unity.novel.model.OnboardingAnswer;
import com.jvxi.unity.novel.model.OnboardingQuestion;
import com.jvxi.unity.novel.model.OutlineBootstrapCharacter;
import com.jvxi.unity.novel.model.OutlineBootstrapOutlineNode;
import com.jvxi.unity.novel.model.OutlineBootstrapProposal;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectMeta;
import com.jvxi.unity.novel.model.PublishPlatformInfo;

@Component
public class OutlineBootstrapService {
    private static final int QUESTION_COUNT = 8;
    private static final int BOOTSTRAP_BATCH_COUNT = 2;
    private static final int BOOTSTRAP_BATCH_SIZE = 4;
    private static final Pattern QUESTION_ITEM_PATTERN = Pattern.compile(
        "\\{\\s*\"id\"\\s*:\\s*\"(bq\\d{2})\"\\s*,\\s*\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"hint\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"placeholder\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\}",
        Pattern.DOTALL
    );

    private final NovelTypeCatalog novelTypeCatalog;
    private final PublishPlatformCatalog publishPlatformCatalog;
    private final GenerationService generationService;
    private final AiJsonRepairService aiJsonRepairService;
    private final ObjectMapper objectMapper;

    public OutlineBootstrapService(
        NovelTypeCatalog novelTypeCatalog,
        PublishPlatformCatalog publishPlatformCatalog,
        GenerationService generationService,
        AiJsonRepairService aiJsonRepairService,
        ObjectMapper objectMapper
    ) {
        this.novelTypeCatalog = novelTypeCatalog;
        this.publishPlatformCatalog = publishPlatformCatalog;
        this.generationService = generationService;
        this.aiJsonRepairService = aiJsonRepairService;
        this.objectMapper = objectMapper;
    }

    public void streamQuestions(Project project, Consumer<OnboardingQuestion> onQuestion) {
        ensureRemoteReady(project);
        String bookContext = buildBookContext(project);
        try {
            ParallelAiRunner.runIndexedBatches(
                BOOTSTRAP_BATCH_COUNT,
                batchIndex -> generateQuestionBatch(project, bookContext, batchIndex),
                batch -> {
                    for (OnboardingQuestion question : batch) {
                        onQuestion.accept(question);
                    }
                },
                BOOTSTRAP_BATCH_COUNT,
                80
            );
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                cause.getMessage() == null ? "生成灵感问题失败" : cause.getMessage()
            );
        }
    }

    public List<OutlineBootstrapProposal> generateProposals(Project project, List<OnboardingAnswer> answers) {
        ensureRemoteReady(project);
        if (answers == null || answers.size() < QUESTION_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先完成全部 " + QUESTION_COUNT + " 个灵感问题。");
        }
        for (OnboardingAnswer answer : answers) {
            if (answer.answer() == null || answer.answer().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "请完成所有问题后再生成大纲方案。");
            }
        }

        String bookContext = buildBookContext(project);
        StringBuilder answerBlock = new StringBuilder("作者回答：\n");
        for (OnboardingAnswer answer : answers) {
            answerBlock.append("- ").append(answer.question()).append("：").append(answer.answer().trim()).append("\n");
        }

        String systemPrompt = "你是资深网络小说策划。只输出 JSON，禁止 Markdown 与解释。" +
            "格式：{\"proposals\":[{\"id\":\"p1\",\"name\":\"方案名<=16字\",\"pitch\":\"卖点<=40字\",\"premise\":\"故事前提<=200字\",\"tone\":\"基调<=20字\",\"targetLength\":\"篇幅\",\"styleRules\":[\"规则\"],\"worldRules\":[\"规则\"],\"outlineNodes\":[{\"title\":\"阶段名\",\"summary\":\"梗概\",\"objective\":\"目标\",\"keyConflict\":\"冲突\",\"mustKeep\":[\"要点\"],\"forbidden\":[\"禁写\"]}],\"characters\":[{\"name\":\"姓名\",\"role\":\"主角/反派等\",\"profile\":\"简介\",\"motivation\":\"动机\",\"constraint\":\"限制\",\"relationships\":\"关系\"}]}]} " +
            "proposals 长度必须为 3，id 依次为 p1、p2、p3。每个方案 4-6 个 outlineNodes、2-4 个 characters。字符串单行，禁止英文双引号。" +
            "三个方案方向须明显不同（如基调、主角路径、核心矛盾各不相同）。";

        String userPrompt = bookContext + "\n\n" + answerBlock + "\n请给出 3 套互异的大纲方案。";

        ApiException lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String raw = generationService.completeWithPrompts(
                    project,
                    systemPrompt,
                    attempt == 1 ? userPrompt : userPrompt + "\n【重试】仅输出合法 JSON，proposals 长度=3。",
                    AiCompletionOptions.jsonProposals()
                );
                return parseProposalsFromAi(raw);
            } catch (ApiException exception) {
                lastError = exception;
            } catch (Exception exception) {
                lastError = new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    exception.getMessage() == null ? "解析方案失败" : exception.getMessage()
                );
            }
        }
        throw lastError == null
            ? new ApiException(HttpStatus.BAD_GATEWAY, "生成大纲方案失败")
            : lastError;
    }

    public Project applyProposal(Project project, OutlineBootstrapProposal proposal) {
        if (proposal == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择一个大纲方案。");
        }

        ProjectMeta meta = project.meta();
        String title = meta.title();
        if (title == null || title.isBlank()) {
            title = "未命名作品";
        }

        String synopsis = meta.synopsis();
        if (synopsis == null || synopsis.isBlank()) {
            synopsis = textOrDefault(proposal.pitch(), synopsis);
        }

        ProjectMeta updatedMeta = new ProjectMeta(
            title,
            synopsis,
            meta.genre(),
            textOrDefault(proposal.premise(), meta.premise()),
            textOrDefault(proposal.tone(), meta.tone()),
            textOrDefault(proposal.targetLength(), meta.targetLength()),
            mergeLines(meta.styleRules(), proposal.styleRules()),
            mergeLines(meta.worldRules(), proposal.worldRules()),
            meta.strictMode(),
            meta.publishPlatform(),
            meta.audienceChannel(),
            meta.novelType()
        );

        List<OutlineNode> outlineNodes = new ArrayList<>();
        int order = 1;
        for (OutlineBootstrapOutlineNode node : safeOutlineNodes(proposal)) {
            outlineNodes.add(new OutlineNode(
                UUID.randomUUID().toString(),
                order++,
                textOrDefault(node.title(), "剧情阶段 " + order),
                text(node.summary()),
                text(node.objective()),
                text(node.keyConflict()),
                safeLines(node.mustKeep()),
                safeLines(node.forbidden())
            ));
        }

        List<CharacterProfile> characters = new ArrayList<>();
        for (OutlineBootstrapCharacter character : safeCharacters(proposal)) {
            if (text(character.name()).isBlank()) {
                continue;
            }
            characters.add(new CharacterProfile(
                UUID.randomUUID().toString(),
                text(character.name()),
                text(character.role()),
                text(character.profile()),
                text(character.motivation()),
                text(character.constraint()),
                text(character.relationships())
            ));
        }

        List<Chapter> chapters = project.chapters() == null || project.chapters().isEmpty()
            ? List.of(defaultChapter())
            : project.chapters();

        return new Project(
            updatedMeta,
            project.aiSettings(),
            project.onboarding(),
            outlineNodes,
            characters,
            project.foreshadowing() == null ? List.of() : project.foreshadowing(),
            chapters,
            project.updatedAt()
        );
    }

    private Chapter defaultChapter() {
        return new Chapter(
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
    }

    private List<OnboardingQuestion> generateQuestionBatch(Project project, String bookContext, int batchIndex) {
        int start = batchIndex * BOOTSTRAP_BATCH_SIZE + 1;
        int end = Math.min(start + BOOTSTRAP_BATCH_SIZE - 1, QUESTION_COUNT);
        int count = end - start + 1;
        String firstId = String.format(Locale.ROOT, "bq%02d", start);
        String lastId = String.format(Locale.ROOT, "bq%02d", end);

        StringBuilder focusBlock = new StringBuilder();
        for (int number = start; number <= end; number++) {
            focusBlock.append("- ").append(focusForBootstrapQuestion(number)).append('\n');
        }

        String systemPrompt = ("你是网络小说策划编辑，帮助还没想好大纲的作者厘清创作方向。只输出 JSON，禁止 Markdown。" +
            "格式：{\"questions\":[{\"id\":\"bq01\",\"title\":\"...\",\"hint\":\"...\",\"placeholder\":\"...\"}]} " +
            "questions 长度=%d，id 从 %s 到 %s 连续。title<=24字，hint/placeholder各<=32字，禁止英文双引号。")
            .formatted(count, firstId, lastId);

        String userPrompt = "生成 %d 个开书灵感问题。\n%s\n\n%s".formatted(count, focusBlock, bookContext);

        ApiException lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String raw = generationService.completeWithPrompts(
                    project,
                    systemPrompt,
                    attempt == 1 ? userPrompt : userPrompt + "\n【重试】仅输出合法 JSON，questions 长度=" + count + "。",
                    AiCompletionOptions.jsonQuestions(count)
                );
                return parseQuestionsFromAi(raw, count);
            } catch (ApiException exception) {
                lastError = exception;
            } catch (Exception exception) {
                lastError = new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    exception.getMessage() == null ? "解析失败" : exception.getMessage()
                );
            }
        }
        throw lastError == null
            ? new ApiException(HttpStatus.BAD_GATEWAY, "生成问题失败")
            : lastError;
    }

    private String focusForBootstrapQuestion(int questionNumber) {
        return switch (questionNumber) {
            case 1 -> "聚焦：最想写的核心爽点或情绪体验。";
            case 2 -> "聚焦：主角起点处境与首要目标。";
            case 3 -> "聚焦：金手指、能力体系或差异化设定。";
            case 4 -> "聚焦：主要反派/对立面与矛盾来源。";
            case 5 -> "聚焦：前期 30 章左右的剧情走向。";
            case 6 -> "聚焦：中期升级或反转期待。";
            case 7 -> "聚焦：结局倾向（HE/BE/开放式等）。";
            case 8 -> "聚焦：绝对不想写的雷点或禁忌。";
            default -> "聚焦：全书卖点与差异化。";
        };
    }

    private List<OutlineBootstrapProposal> parseProposalsFromAi(String raw) throws JsonProcessingException {
        JsonNode root = aiJsonRepairService.readTree(raw);
        JsonNode proposalsNode = root.path("proposals");
        if (!proposalsNode.isArray() || proposalsNode.size() != 3) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 应返回 3 个大纲方案，请重试。");
        }

        List<OutlineBootstrapProposal> proposals = new ArrayList<>();
        int index = 1;
        for (JsonNode node : proposalsNode) {
            String id = text(node.path("id"));
            if (id.isBlank()) {
                id = "p" + index;
            }
            proposals.add(new OutlineBootstrapProposal(
                id,
                textOrDefault(node.path("name"), "方案 " + index),
                text(node.path("pitch")),
                text(node.path("premise")),
                text(node.path("tone")),
                textOrDefault(node.path("targetLength"), "长篇连载"),
                readStringArray(node.path("styleRules")),
                readStringArray(node.path("worldRules")),
                parseOutlineNodes(node.path("outlineNodes")),
                parseCharacters(node.path("characters"))
            ));
            index++;
        }
        return List.copyOf(proposals);
    }

    private List<OutlineBootstrapOutlineNode> parseOutlineNodes(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<OutlineBootstrapOutlineNode> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            result.add(new OutlineBootstrapOutlineNode(
                text(node.path("title")),
                text(node.path("summary")),
                text(node.path("objective")),
                text(node.path("keyConflict")),
                readStringArray(node.path("mustKeep")),
                readStringArray(node.path("forbidden"))
            ));
        }
        return result;
    }

    private List<OutlineBootstrapCharacter> parseCharacters(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<OutlineBootstrapCharacter> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            result.add(new OutlineBootstrapCharacter(
                text(node.path("name")),
                text(node.path("role")),
                text(node.path("profile")),
                text(node.path("motivation")),
                text(node.path("constraint")),
                text(node.path("relationships"))
            ));
        }
        return result;
    }

    private List<OnboardingQuestion> parseQuestionsFromAi(String raw, int expectedCount) throws JsonProcessingException {
        try {
            JsonNode questionsNode = locateQuestionsNode(raw);
            return mapQuestionNodes(questionsNode, expectedCount);
        } catch (JsonProcessingException | ApiException exception) {
            List<OnboardingQuestion> recovered = parseQuestionsByRegex(raw);
            if (recovered.size() >= expectedCount) {
                return recovered.subList(0, expectedCount);
            }
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回的 JSON 格式有误，请重试。");
        }
    }

    private List<OnboardingQuestion> parseQuestionsByRegex(String raw) {
        String payload = aiJsonRepairService.extractJsonPayload(raw);
        Matcher matcher = QUESTION_ITEM_PATTERN.matcher(payload);
        List<OnboardingQuestion> questions = new ArrayList<>();
        while (matcher.find()) {
            questions.add(new OnboardingQuestion(
                matcher.group(1),
                unescape(matcher.group(2)),
                defaultHint(unescape(matcher.group(3))),
                defaultPlaceholder(unescape(matcher.group(4)))
            ));
        }
        questions.sort(Comparator.comparing(OnboardingQuestion::id));
        return questions;
    }

    private List<OnboardingQuestion> mapQuestionNodes(JsonNode questionsNode, int expectedCount) {
        if (!questionsNode.isArray() || questionsNode.size() != expectedCount) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "应返回 " + expectedCount + " 个问题，实际 " + (questionsNode.isArray() ? questionsNode.size() : 0) + " 个。"
            );
        }
        List<OnboardingQuestion> questions = new ArrayList<>();
        int index = 1;
        for (JsonNode node : questionsNode) {
            String id = text(node.path("id"));
            if (id.isBlank()) {
                id = String.format(Locale.ROOT, "bq%02d", index);
            }
            String title = text(node.path("title"));
            if (title.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "第 " + index + " 个问题缺少标题。");
            }
            questions.add(new OnboardingQuestion(
                id,
                title,
                defaultHint(text(node.path("hint"))),
                defaultPlaceholder(text(node.path("placeholder")))
            ));
            index++;
        }
        return List.copyOf(questions);
    }

    private JsonNode locateQuestionsNode(String raw) throws JsonProcessingException {
        JsonNode root = aiJsonRepairService.readTree(raw);
        JsonNode questions = root.path("questions");
        if (questions.isArray()) {
            return questions;
        }
        if (root.isArray()) {
            return root;
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, "缺少 questions 数组。");
    }

    private void ensureRemoteReady(Project project) {
        if (!generationService.isRemoteModelReady(project.aiSettings())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "请先在 AI 设置中启用远程模型并填写 API Key，再使用灵感向导。"
            );
        }
    }

    private String buildBookContext(Project project) {
        ProjectMeta meta = project.meta();
        NovelTypeInfo novelType = novelTypeCatalog.resolveType(meta.audienceChannel(), meta.novelType());
        PublishPlatformInfo platform = publishPlatformCatalog.resolve(meta.publishPlatform());
        String channel = "female".equals(meta.audienceChannel()) ? "女频" : "男频";

        StringBuilder builder = new StringBuilder();
        builder.append("书名：").append(blank(meta.title(), "未命名")).append("\n");
        builder.append("频道：").append(channel).append("；类型：").append(novelType.label()).append("\n");
        builder.append("平台：").append(platform.label()).append("\n");
        if (!text(meta.synopsis()).isBlank()) {
            builder.append("作品简介：").append(truncate(meta.synopsis(), 500)).append("\n");
        }
        if (!text(meta.premise()).isBlank()) {
            builder.append("核心设定：").append(truncate(meta.premise(), 300)).append("\n");
        }
        if (!project.outlineNodes().isEmpty()) {
            builder.append("已有大纲节点数：").append(project.outlineNodes().size()).append("\n");
        }
        return builder.toString();
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
        return List.copyOf(lines);
    }

    private List<String> mergeLines(List<String> existing, List<String> extra) {
        List<String> merged = new ArrayList<>();
        if (existing != null) {
            merged.addAll(existing.stream().filter(line -> line != null && !line.isBlank()).toList());
        }
        if (extra != null) {
            for (String line : extra) {
                if (line != null && !line.isBlank() && !merged.contains(line)) {
                    merged.add(line);
                }
            }
        }
        return List.copyOf(merged);
    }

    private List<OutlineBootstrapOutlineNode> safeOutlineNodes(OutlineBootstrapProposal proposal) {
        return proposal.outlineNodes() == null ? List.of() : proposal.outlineNodes();
    }

    private List<OutlineBootstrapCharacter> safeCharacters(OutlineBootstrapProposal proposal) {
        return proposal.characters() == null ? List.of() : proposal.characters();
    }

    private List<String> safeLines(List<String> lines) {
        return lines == null ? List.of() : lines.stream().filter(line -> line != null && !line.isBlank()).toList();
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String textOrDefault(JsonNode node, String fallback) {
        String value = text(node);
        return value.isBlank() ? fallback : value;
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultHint(String hint) {
        return hint.isBlank() ? "结合你的真实创作想法回答，越具体越好。" : hint;
    }

    private String defaultPlaceholder(String placeholder) {
        return placeholder.isBlank() ? "例如：主角因家族变故踏上复仇之路…" : placeholder;
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", "\n");
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }
}

