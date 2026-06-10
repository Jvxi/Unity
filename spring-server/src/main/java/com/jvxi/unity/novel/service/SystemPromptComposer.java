package com.jvxi.unity.novel.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.OnboardingAnswer;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.PublishPlatformInfo;
import com.jvxi.unity.novel.model.NovelTypeInfo;

@Component
public class SystemPromptComposer {
    private static final int LEGACY_SHORT_PROMPT_MAX_LEN = 500;
    private static final String OVERRIDE_PREFIX = "#OVERRIDE";

    /**
     * 生成实际发送给模型的系统提示词：固定完整基底 + 可选作者追加约束。
     */
    public String compose(Project project, PublishPlatformInfo platform, NovelTypeInfo novelType) {
        String base = composeBase(project, platform, novelType);
        String custom = normalizeCustomPrompt(project.aiSettings().systemPrompt());
        if (custom.isBlank()) {
            return base;
        }
        if (custom.startsWith(OVERRIDE_PREFIX)) {
            String overrideBody = custom.substring(OVERRIDE_PREFIX.length()).trim();
            return overrideBody.isBlank() ? base : overrideBody;
        }
        return base + "\n\n【作者追加约束（须与上文一并遵守）】\n" + custom;
    }

    public String composeBase(Project project, PublishPlatformInfo platform, NovelTypeInfo novelType) {
        String audienceLabel = "female".equals(project.meta().audienceChannel()) ? "女频" : "男频";
        String onboardingBlock = buildOnboardingBlock(project.onboarding().answers());
        String bookRulesBlock = buildBookRulesBlock(project);
        String characterBlock = buildCharacterDisciplineBlock(project.characters());
        String strictBlock = project.meta().strictMode()
            ? """
            【严格正文模式】
            - 未落实绑定大纲节点、必写情节点或命中禁写项的草稿视为失败。
            - 不得用概括性旁白代替场景描写来「跳过」难写的情节。
            - 若资料冲突，以本章摘要/目的与绑定大纲节点的禁写项为最高优先级。
            """.trim()
            : """
            【标准模式】
            - 仍须遵守大纲与禁写项，但允许在合规前提下略作文学发挥。
            """.trim();

        return """
            你是「网络小说正文写作引擎」，唯一职责：将作者已锁定的设定转化为可连载发布的章节正文。

            ═══════════════════════════════════════
            一、身份与输出边界（违反任一条即失败）
            ═══════════════════════════════════════
            - 你不是编辑、策划、读者助理、评论家，禁止输出创作建议、写作分析、剧情讨论。
            - 禁止输出标题、小标题、章节号、Markdown、列表、注释、括号说明、分隔线。
            - 禁止「以下是正文」「本章将讲述」「作者要求」「根据大纲」等元话语。
            - 只输出连续的小说正文：通过场景、动作、对话、感官细节推进，不用说明书式旁白。

            ═══════════════════════════════════════
            二、最高优先级禁令（旁白与剧透）
            ═══════════════════════════════════════
            - 禁止帮读者总结、梳理、回顾、预告剧情；禁止「帮助读者理解」「故事线将会」「接下来」「上一章」「下一章」「综上所述」等跳出叙事的语句。
            - 禁止提及或暗示：大纲、角色设定、伏笔表、问卷、写作要求、平台规则、AI、作者。
            - 禁止提前写出大纲/章节标注为「禁写」「不可提前揭示」「禁止剧透」的内容。
            - 禁止用一句旁白概括局势（如「局面越来越复杂」）代替具体事件；每一段落须有可观察的情节推进。

            ═══════════════════════════════════════
            三、资料服从顺序（用户消息中提供）
            ═══════════════════════════════════════
            优先级从高到低：
            1) 本章「禁止出现」与绑定大纲节点的「禁写」
            2) 本章「必须发生」与绑定大纲节点的「必保留」
            3) 本章摘要、章节目的
            4) 绑定大纲节点的摘要/目标/冲突
            5) 开书问卷结论（须内化，禁止复述问卷原文）
            6) 出场角色的性格、行事作风、动机与限制
            7) 书籍世界观规则、文风规则、基调与前提
            8) 平台规则与类型写作提示

            ═══════════════════════════════════════
            四、当前书籍上下文
            ═══════════════════════════════════════
            - 书名：%s
            - 受众：%s
            - 类型：%s（%s）
            - 发布平台：%s
            - 基调：%s
            - 作品简介：%s
            - 核心设定：%s

            %s

            %s

            ═══════════════════════════════════════
            五、平台与类型规则
            ═══════════════════════════════════════
            【平台规则】
            %s

            【类型写作提示】
            %s

            ═══════════════════════════════════════
            六、开书问卷结论（禁止向读者解释，须融入正文）
            ═══════════════════════════════════════
            %s

            ═══════════════════════════════════════
            七、角色言行纪律（绑定出场角色时）
            ═══════════════════════════════════════
            %s

            ═══════════════════════════════════════
            八、正文技法
            ═══════════════════════════════════════
            - 开篇第一句即进入场景或动作，不写铺垫性说明。
            - 对话符合人物身份与性格，避免所有人同一口吻。
            - 伏笔只可埋设动作/细节，禁止解说伏笔含义。
            - 章节须完整覆盖用户消息中的本章要求，不得擅自改线或跳过大纲节点。
            - 单章建议 2000–3500 字（除非用户消息另有字数说明）。
            - 段落之间空一行；只输出正文，无前后缀。

            %s
            """.formatted(
            blank(project.meta().title(), "未命名"),
            audienceLabel,
            novelType.label(),
            project.meta().genre(),
            platform.label(),
            blank(project.meta().tone(), "未指定"),
            blank(project.meta().synopsis(), "见用户消息"),
            blank(project.meta().premise(), "见用户消息"),
            bookRulesBlock,
            strictBlock,
            platform.writingRules().stream().map(rule -> "- " + rule).collect(Collectors.joining("\n")),
            novelType.writingHints().stream().map(hint -> "- " + hint).collect(Collectors.joining("\n")),
            onboardingBlock,
            characterBlock,
            ""
        ).trim();
    }

    /**
     * 将旧版「整段替换用」的短提示词清空，避免用户误以为短句即全部约束。
     */
    public String normalizeCustomPrompt(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith(OVERRIDE_PREFIX)) {
            return trimmed;
        }
        if (trimmed.length() < LEGACY_SHORT_PROMPT_MAX_LEN && looksLikeLegacyReplacement(trimmed)) {
            return "";
        }
        return trimmed;
    }

    public String defaultCustomPromptHint() {
        return """
            系统会自动合成完整提示词（推荐）。此框仅填写「追加约束」，例如特殊人称、禁用词、口语风格等。
            若必须完全自定义，首行写 #OVERRIDE 后接全文（将替换自动提示词，不推荐）。
            """.trim();
    }

    private boolean looksLikeLegacyReplacement(String text) {
        return text.contains("作者助手")
            || text.contains("只输出小说正文")
            || text.contains("严格按提供的大纲");
    }

    private String buildBookRulesBlock(Project project) {
        StringBuilder builder = new StringBuilder();
        if (!project.meta().styleRules().isEmpty()) {
            builder.append("【文风规则】\n");
            project.meta().styleRules().forEach(rule -> builder.append("- ").append(rule).append('\n'));
        }
        if (!project.meta().worldRules().isEmpty()) {
            builder.append("【世界观规则】\n");
            project.meta().worldRules().forEach(rule -> builder.append("- ").append(rule).append('\n'));
        }
        if (builder.isEmpty()) {
            return "【书籍规则】\n（未单独列出，服从用户消息中的项目设定。）";
        }
        return builder.toString().trim();
    }

    private String buildCharacterDisciplineBlock(List<CharacterProfile> characters) {
        if (characters == null || characters.isEmpty()) {
            return "（本章未绑定角色时，按用户消息中的出场人物为准；仍须保持言行一致。）";
        }
        return characters.stream()
            .map(character -> {
                String name = blank(character.name(), "未命名");
                String role = blank(character.role(), "未标注定位");
                String personality = blank(character.profile(), "见用户消息");
                String conduct = blank(character.motivation(), "见用户消息");
                String constraint = blank(character.constraint(), "无");
                return "- " + name + "（" + role + "）\n"
                    + "  性格：" + personality + "\n"
                    + "  行事作风：" + conduct + "\n"
                    + "  底线/限制：" + constraint;
            })
            .collect(Collectors.joining("\n"));
    }

    private String buildOnboardingBlock(List<OnboardingAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return "（作者尚未完成开书问卷；按用户消息中的章节与大纲约束写作，勿擅自编造全书走向。）";
        }
        return answers.stream()
            .filter(answer -> answer.answer() != null && !answer.answer().isBlank())
            .map(answer -> "- " + answer.question() + " → " + answer.answer())
            .collect(Collectors.joining("\n"));
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

