package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ForeshadowingItem;
import com.jvxi.unity.novel.model.NovelTypeInfo;
import com.jvxi.unity.novel.model.OnboardingAnswer;
import com.jvxi.unity.novel.model.OnboardingQuestion;
import com.jvxi.unity.novel.model.OnboardingState;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectMeta;
import com.jvxi.unity.novel.model.PublishPlatformInfo;

@Component
public class OpeningQuestionnaireService {
    private static final int BATCH_SIZE = 5;
    private static final int BATCH_COUNT = 3;
    private static final int CONTEXT_FIELD_LIMIT = 200;
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION_ITEM_PATTERN = Pattern.compile(
        "\\{\\s*\"id\"\\s*:\\s*\"(q\\d{2})\"\\s*,\\s*\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"hint\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"placeholder\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\}",
        Pattern.DOTALL
    );

    private final NovelTypeCatalog novelTypeCatalog;
    private final PublishPlatformCatalog publishPlatformCatalog;
    private final GenerationService generationService;
    private final ObjectMapper objectMapper;

    public OpeningQuestionnaireService(
        NovelTypeCatalog novelTypeCatalog,
        PublishPlatformCatalog publishPlatformCatalog,
        GenerationService generationService,
        ObjectMapper objectMapper
    ) {
        this.novelTypeCatalog = novelTypeCatalog;
        this.publishPlatformCatalog = publishPlatformCatalog;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    public List<OnboardingQuestion> generateQuestions(Project project) {
        List<OnboardingQuestion> merged = new ArrayList<>();
        streamQuestions(project, merged::add);
        if (merged.size() != 15) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "仅生成 " + merged.size() + " 个问题，请重试。");
        }
        merged.sort(Comparator.comparing(OnboardingQuestion::id));
        return List.copyOf(merged);
    }

    /** 3 批并行（每批 5 题），任一批完成即推送，减少 API 往返次数。 */
    public void streamQuestions(Project project, Consumer<OnboardingQuestion> onQuestion) {
        ensureRemoteReady(project);
        String bookContext = buildBookContext(project);
        try {
            ParallelAiRunner.runIndexedBatches(
                BATCH_COUNT,
                batchIndex -> generateBatch(project, bookContext, batchIndex),
                batch -> {
                    for (OnboardingQuestion question : batch) {
                        onQuestion.accept(question);
                    }
                },
                BATCH_COUNT,
                75
            );
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                cause.getMessage() == null ? "生成开书问题失败" : cause.getMessage()
            );
        }
    }

    private void ensureRemoteReady(Project project) {
        if (!generationService.isRemoteModelReady(project.aiSettings())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "请先在 AI 设置中启用远程模型，并填写 Base URL、API Key 与模型名称，再获取开书问题。"
            );
        }
    }

    private List<OnboardingQuestion> generateBatch(Project project, String bookContext, int batchIndex) {
        int start = batchIndex * BATCH_SIZE + 1;
        int end = start + BATCH_SIZE - 1;
        String idRange = String.format(Locale.ROOT, "q%02d 到 q%02d", start, end);

        String systemPrompt = "你是网络小说策划编辑。只输出 JSON 对象，禁止 Markdown 与任何解释文字。" +
            "格式：{\"questions\":[{\"id\":\"q01\",\"title\":\"...\",\"hint\":\"...\",\"placeholder\":\"...\"}]} " +
            "字符串规则：每项单行；禁止英文双引号、禁止换行；title 不超过 28 字；hint、placeholder 各不超过 36 字。";

        String focus = switch (batchIndex) {
            case 0 -> "本批聚焦：核心矛盾、主角、开篇钩子、世界观规则、前期目标。";
            case 1 -> "本批聚焦：对立面、人物关系、伏笔规划、节奏基调、中期升级。";
            default -> "本批聚焦：高潮前置、结局形态、情感落点、禁写项、全书收束。";
        };

        String userPrompt = "根据资料生成恰好 5 个开书问题，id 必须为 " + idRange + "。\n"
            + focus + "\n\n" + bookContext;

        ApiException lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String prompt = attempt == 1
                    ? userPrompt
                    : userPrompt + "\n【重试】上次 JSON 无效。请确保 questions 长度=5，且字符串内无未转义双引号。";
                String raw = generationService.completeWithPrompts(
                    project,
                    systemPrompt,
                    prompt,
                    AiCompletionOptions.jsonQuestions(BATCH_SIZE)
                );
                return parseQuestionsFromAi(raw, BATCH_SIZE);
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
            ? new ApiException(HttpStatus.BAD_GATEWAY, "生成失败")
            : lastError;
    }

    public OnboardingState validateAndBuildState(List<OnboardingAnswer> submittedAnswers, List<OnboardingQuestion> questions) {
        if (questions == null || questions.size() != 15) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先点击「获取问题」生成 15 个问题后再提交。");
        }

        List<OnboardingAnswer> normalized = new ArrayList<>();
        for (OnboardingQuestion question : questions) {
            String answer = submittedAnswers.stream()
                .filter(entry -> question.id().equals(entry.questionId()))
                .map(OnboardingAnswer::answer)
                .findFirst()
                .orElse("");
            if (answer == null || answer.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "请完成全部 15 个问题后再开书：" + question.title());
            }
            normalized.add(new OnboardingAnswer(question.id(), question.title(), answer.trim()));
        }
        return new OnboardingState(true, List.copyOf(questions), List.copyOf(normalized));
    }

    public OnboardingState withGeneratedQuestions(List<OnboardingQuestion> questions) {
        return new OnboardingState(false, List.copyOf(questions), List.of());
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
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "AI 返回的 JSON 格式有误，请重试。建议使用支持 JSON 模式的模型。"
            );
        }
    }

    private List<OnboardingQuestion> parseQuestionsByRegex(String raw) {
        String payload = extractJsonPayload(raw);
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
        if (!questionsNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回格式不正确：缺少 questions 数组。");
        }
        if (questionsNode.size() != expectedCount) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "本批应返回 " + expectedCount + " 个问题，实际 " + questionsNode.size() + " 个。"
            );
        }

        List<OnboardingQuestion> questions = new ArrayList<>();
        int index = 1;
        for (JsonNode node : questionsNode) {
            String id = text(node.path("id"));
            if (id.isBlank()) {
                id = String.format(Locale.ROOT, "q%02d", index);
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
        String json = extractJsonPayload(raw);
        json = normalizeJsonText(json);
        JsonNode root = objectMapper.readTree(json);
        JsonNode questions = root.path("questions");
        if (questions.isArray()) {
            return questions;
        }
        if (root.isArray()) {
            return root;
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回内容不是有效 JSON。");
    }

    private String extractJsonPayload(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 未返回内容。");
        }
        String trimmed = sanitizeRaw(raw);
        Matcher matcher = JSON_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            trimmed = matcher.group(1).trim();
        }
        String balanced = extractBalancedJson(trimmed, '{', '}');
        if (balanced != null) {
            return balanced;
        }
        balanced = extractBalancedJson(trimmed, '[', ']');
        if (balanced != null) {
            return balanced;
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回内容不是有效 JSON。");
    }

    private String normalizeJsonText(String json) {
        return json
            .replaceAll(",\\s*}", "}")
            .replaceAll(",\\s*]", "]");
    }

    private String extractBalancedJson(String text, char open, char close) {
        int start = text.indexOf(open);
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }
        return null;
    }

    private String sanitizeRaw(String raw) {
        return raw
            .replace('\uFEFF', ' ')
            .replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .trim();
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", " ").trim();
    }

    private String defaultHint(String hint) {
        return hint.isBlank() ? "请结合你已填写的书籍与大纲信息作答。" : hint;
    }

    private String defaultPlaceholder(String placeholder) {
        return placeholder.isBlank() ? "请写出具体、可验证的情节或设定。" : placeholder;
    }

    private String buildBookContext(Project project) {
        ProjectMeta meta = project.meta();
        NovelTypeInfo novelType = novelTypeCatalog.resolveType(meta.audienceChannel(), meta.novelType());
        PublishPlatformInfo platform = publishPlatformCatalog.resolve(meta.publishPlatform());
        String channel = "female".equals(meta.audienceChannel()) ? "女频" : "男频";

        StringBuilder builder = new StringBuilder();
        builder.append("书名：").append(blank(meta.title(), "未命名")).append("；类型：").append(novelType.label());
        builder.append("；平台：").append(platform.label()).append("；受众：").append(channel).append('\n');
        if (!blank(meta.synopsis(), "").isBlank()) {
            builder.append("简介：").append(limit(meta.synopsis(), CONTEXT_FIELD_LIMIT)).append('\n');
        }
        builder.append("核心设定：").append(limit(meta.premise(), CONTEXT_FIELD_LIMIT)).append('\n');

        List<OutlineNode> outlineNodes = project.outlineNodes() == null ? List.of() : project.outlineNodes();
        if (!outlineNodes.isEmpty()) {
            builder.append("大纲：");
            outlineNodes.stream().sorted((a, b) -> Integer.compare(a.order(), b.order())).limit(6).forEach(node -> {
                builder.append(" [").append(node.order()).append(']').append(node.title());
                if (!node.summary().isBlank()) {
                    builder.append(':').append(limit(node.summary(), 80));
                }
            });
            builder.append('\n');
        }

        List<CharacterProfile> characters = project.characters() == null ? List.of() : project.characters();
        if (!characters.isEmpty()) {
            builder.append("角色：");
            characters.stream().limit(5).forEach(character -> {
                builder.append(' ').append(character.name());
                if (!character.profile().isBlank()) {
                    builder.append('(').append(limit(character.profile(), 40)).append(')');
                }
            });
            builder.append('\n');
        }

        List<ForeshadowingItem> foreshadowing = project.foreshadowing() == null ? List.of() : project.foreshadowing();
        if (!foreshadowing.isEmpty()) {
            builder.append("伏笔：");
            foreshadowing.stream().limit(4).forEach(item -> builder.append(' ').append(item.title()));
            builder.append('\n');
        }

        return builder.toString().trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim().replace('\n', ' ');
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "…";
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }
}

