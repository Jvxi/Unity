package com.jvxi.unity.novel.service.readingpower;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.readingpower.ChapterReadingPower;
import com.jvxi.unity.novel.model.readingpower.ChaseDebt;
import com.jvxi.unity.novel.persistence.entity.ChapterReadingPowerEntity;
import com.jvxi.unity.novel.persistence.entity.ChaseDebtEntity;
import com.jvxi.unity.novel.persistence.repository.ChapterReadingPowerRepository;
import com.jvxi.unity.novel.persistence.repository.ChaseDebtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReadingPowerService {

    private static final Logger log = LoggerFactory.getLogger(ReadingPowerService.class);

    // 钩子类型关键词映射
    private static final Map<String, List<String>> HOOK_KEYWORDS = Map.of(
            "crisis_hook", List.of("危险", "危机", "生死", "威胁", "毁灭", "死亡"),
            "mystery_hook", List.of("悬念", "谜团", "秘密", "未知", "神秘", "奇怪"),
            "desire_hook", List.of("渴望", "追求", "目标", "梦想", "愿望", "想要"),
            "emotion_hook", List.of("感动", "悲伤", "愤怒", "喜悦", "爱", "恨"),
            "choice_hook", List.of("选择", "抉择", "决定", "两难", "取舍")
    );

    // 爽点模式关键词映射
    private static final Map<String, List<String>> COOL_POINT_KEYWORDS = Map.of(
            "flex_and_counter", List.of("装逼", "打脸", "反杀", "逆袭"),
            "underdog_reveal", List.of("扮猪吃虎", "隐藏实力", "深藏不露"),
            "underdog_victory", List.of("越级", "以弱胜强", "以下克上"),
            "authority_challenge", List.of("挑战权威", "打脸", "教训"),
            "villain_downfall", List.of("反派", "覆灭", "失败", "报应"),
            "sweet_surprise", List.of("甜蜜", "惊喜", "超预期"),
            "misunderstanding_elevation", List.of("误解", "误会", "迪化"),
            "identity_reveal", List.of("身份", "掉马", "暴露", "揭晓")
    );

    // 硬性违规检测模式
    private static final Pattern READABILITY_PATTERN = Pattern.compile(".{20,}");
    private static final Pattern CONFLICT_PATTERN = Pattern.compile("(问题|目标|冲突|矛盾|困难|挑战|敌人|对手)");

    private final ChapterReadingPowerRepository readingPowerRepository;
    private final ChaseDebtRepository chaseDebtRepository;
    private final ObjectMapper objectMapper;

    public ReadingPowerService(
            ChapterReadingPowerRepository readingPowerRepository,
            ChaseDebtRepository chaseDebtRepository,
            ObjectMapper objectMapper
    ) {
        this.readingPowerRepository = readingPowerRepository;
        this.chaseDebtRepository = chaseDebtRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 分析章节追读力
     */
    @Transactional
    public ChapterReadingPower analyzeChapter(String bookId, int chapterNumber, String chapterText) {
        // 检查是否已存在
        Optional<ChapterReadingPowerEntity> existing = readingPowerRepository.findByBookIdAndChapterNumber(bookId, chapterNumber);
        if (existing.isPresent()) {
            return toModel(existing.get());
        }

        // 1. 分析钩子
        String hookType = identifyHookType(chapterText);
        String hookStrength = assessHookStrength(chapterText);
        String hookContent = extractHookContent(chapterText);

        // 2. 分析爽点
        List<Map<String, Object>> coolPoints = identifyCoolPoints(chapterText);

        // 3. 分析微兑现
        List<Map<String, Object>> microPayoffs = identifyMicroPayoffs(chapterText);

        // 4. 检查硬性违规
        List<Map<String, Object>> hardViolations = checkHardViolations(chapterText);

        // 5. 检查软性违规
        List<Map<String, Object>> softViolations = checkSoftViolations(chapterText);

        // 6. 计算综合评分
        double overallScore = calculateOverallScore(hookStrength, coolPoints, microPayoffs, hardViolations);

        // 持久化
        ChapterReadingPowerEntity entity = new ChapterReadingPowerEntity();
        entity.setBookId(bookId);
        entity.setChapterNumber(chapterNumber);
        entity.setHookType(hookType);
        entity.setHookStrength(ChapterReadingPowerEntity.HookStrength.valueOf(hookStrength));
        entity.setHookContent(hookContent);
        entity.setCoolPointsJson(serialize(coolPoints));
        entity.setMicroPayoffsJson(serialize(microPayoffs));
        entity.setHardViolationsJson(serialize(hardViolations));
        entity.setSoftViolationsJson(serialize(softViolations));
        entity.setOverallScore(BigDecimal.valueOf(overallScore));
        readingPowerRepository.save(entity);

        log.info("章节追读力分析完成: bookId={}, chapter={}, score={}", bookId, chapterNumber, overallScore);

        return new ChapterReadingPower(hookType, hookStrength, hookContent, coolPoints, microPayoffs, hardViolations, softViolations, overallScore);
    }

    /**
     * 获取章节追读力评分
     */
    public Optional<ChapterReadingPower> getChapterReadingPower(String bookId, int chapterNumber) {
        return readingPowerRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .map(this::toModel);
    }

    /**
     * 检查硬性约束
     */
    public List<Map<String, Object>> checkHardInvariants(String bookId, int chapterNumber) {
        // 从已有数据中获取
        return readingPowerRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .map(entity -> {
                    try {
                        return objectMapper.readValue(entity.getHardViolationsJson(), new TypeReference<List<Map<String, Object>>>() {});
                    } catch (IOException e) {
                        return List.<Map<String, Object>>of();
                    }
                })
                .orElse(List.of());
    }

    /**
     * 追踪债务
     */
    @Transactional
    public void trackDebt(String bookId, String debtType, String subject, String description, int chapterNumber, int urgency) {
        ChaseDebtEntity entity = new ChaseDebtEntity();
        entity.setBookId(bookId);
        entity.setDebtType(ChaseDebtEntity.DebtType.valueOf(debtType));
        entity.setSubject(subject);
        entity.setDescription(description);
        entity.setCreatedChapter(chapterNumber);
        entity.setUrgency(urgency);
        chaseDebtRepository.save(entity);

        log.info("追踪债务: bookId={}, type={}, subject={}, chapter={}", bookId, debtType, subject, chapterNumber);
    }

    /**
     * 兑现债务
     */
    @Transactional
    public void payOffDebt(String bookId, String debtId, int chapterNumber, String reason) {
        Optional<ChaseDebtEntity> debt = chaseDebtRepository.findById(debtId);
        if (debt.isPresent()) {
            ChaseDebtEntity entity = debt.get();
            entity.setStatus(ChaseDebtEntity.DebtStatus.paid_off);
            entity.setResolvedChapter(chapterNumber);
            entity.setResolvedReason(reason);
            chaseDebtRepository.save(entity);

            log.info("兑现债务: debtId={}, chapter={}", debtId, chapterNumber);
        }
    }

    /**
     * 获取待处理债务（按紧急度排序）
     */
    public List<ChaseDebt> getPendingDebts(String bookId, int limit) {
        return chaseDebtRepository.findPendingByBookIdWithLimit(bookId, limit)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /**
     * 追读力统计
     */
    public Map<String, Object> getStats(String bookId) {
        List<ChapterReadingPowerEntity> allPowers = readingPowerRepository.findByBookId(bookId);
        List<ChaseDebtEntity> pendingDebts = chaseDebtRepository.findByBookIdAndStatus(bookId, ChaseDebtEntity.DebtStatus.pending);

        double avgScore = allPowers.stream()
                .mapToDouble(e -> e.getOverallScore().doubleValue())
                .average()
                .orElse(0.0);

        Map<String, Long> hookDistribution = allPowers.stream()
                .filter(e -> e.getHookType() != null)
                .collect(Collectors.groupingBy(ChapterReadingPowerEntity::getHookType, Collectors.counting()));

        return Map.of(
                "chapter_count", allPowers.size(),
                "average_score", avgScore,
                "hook_distribution", hookDistribution,
                "pending_debts", pendingDebts.size()
        );
    }

    // ============ 私有辅助方法 ============

    private String identifyHookType(String text) {
        for (Map.Entry<String, List<String>> entry : HOOK_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "unknown";
    }

    private String assessHookStrength(String text) {
        // 简单的强度评估：基于关键词密度和位置
        int hookCount = 0;
        for (List<String> keywords : HOOK_KEYWORDS.values()) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    hookCount++;
                }
            }
        }

        if (hookCount >= 3) return "strong";
        if (hookCount >= 1) return "medium";
        return "weak";
    }

    private String extractHookContent(String text) {
        // 提取包含钩子关键词的句子
        String[] sentences = text.split("[。！？]");
        for (String sentence : sentences) {
            for (List<String> keywords : HOOK_KEYWORDS.values()) {
                for (String keyword : keywords) {
                    if (sentence.contains(keyword)) {
                        return sentence.trim();
                    }
                }
            }
        }
        return "";
    }

    private List<Map<String, Object>> identifyCoolPoints(String text) {
        List<Map<String, Object>> coolPoints = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : COOL_POINT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    coolPoints.add(Map.of(
                            "type", entry.getKey(),
                            "keyword", keyword,
                            "context", extractContext(text, keyword)
                    ));
                }
            }
        }

        return coolPoints;
    }

    private List<Map<String, Object>> identifyMicroPayoffs(String text) {
        List<Map<String, Object>> microPayoffs = new ArrayList<>();

        // 信息型微兑现
        if (text.contains("发现") || text.contains("得知") || text.contains("了解")) {
            microPayoffs.add(Map.of("type", "information", "description", "信息获取"));
        }

        // 关系型微兑现
        if (text.contains("和好") || text.contains("结盟") || text.contains("信任")) {
            microPayoffs.add(Map.of("type", "relationship", "description", "关系进展"));
        }

        // 能力型微兑现
        if (text.contains("突破") || text.contains("领悟") || text.contains("学会")) {
            microPayoffs.add(Map.of("type", "ability", "description", "能力提升"));
        }

        return microPayoffs;
    }

    private List<Map<String, Object>> checkHardViolations(String text) {
        List<Map<String, Object>> violations = new ArrayList<>();

        // HARD-001: 可读性基线
        if (text.length() < 100) {
            violations.add(Map.of(
                    "rule", "HARD-001",
                    "description", "章节内容过短，关键信息缺失",
                    "severity", "critical"
            ));
        }

        // HARD-004: 冲突真空
        Matcher conflictMatcher = CONFLICT_PATTERN.matcher(text);
        if (!conflictMatcher.find()) {
            violations.add(Map.of(
                    "rule", "HARD-004",
                    "description", "章节缺乏冲突、问题或目标",
                    "severity", "warning"
            ));
        }

        return violations;
    }

    private List<Map<String, Object>> checkSoftViolations(String text) {
        List<Map<String, Object>> violations = new ArrayList<>();

        // 可以添加更多软性违规检查
        // 例如：节奏问题、角色行为不一致等

        return violations;
    }

    private double calculateOverallScore(String hookStrength, List<Map<String, Object>> coolPoints,
                                          List<Map<String, Object>> microPayoffs, List<Map<String, Object>> hardViolations) {
        double score = 50.0; // 基础分

        // 钩子加分
        switch (hookStrength) {
            case "strong" -> score += 20;
            case "medium" -> score += 10;
            case "weak" -> score += 5;
        }

        // 爽点加分
        score += Math.min(coolPoints.size() * 5, 20);

        // 微兑现加分
        score += Math.min(microPayoffs.size() * 3, 15);

        // 硬性违规扣分
        for (Map<String, Object> violation : hardViolations) {
            if ("critical".equals(violation.get("severity"))) {
                score -= 20;
            } else {
                score -= 10;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    private String extractContext(String text, String keyword) {
        int index = text.indexOf(keyword);
        if (index == -1) return "";

        int start = Math.max(0, index - 20);
        int end = Math.min(text.length(), index + keyword.length() + 20);
        return text.substring(start, end);
    }

    private ChapterReadingPower toModel(ChapterReadingPowerEntity entity) {
        try {
            List<Map<String, Object>> coolPoints = deserialize(entity.getCoolPointsJson());
            List<Map<String, Object>> microPayoffs = deserialize(entity.getMicroPayoffsJson());
            List<Map<String, Object>> hardViolations = deserialize(entity.getHardViolationsJson());
            List<Map<String, Object>> softViolations = deserialize(entity.getSoftViolationsJson());

            return new ChapterReadingPower(
                    entity.getHookType(),
                    entity.getHookStrength() != null ? entity.getHookStrength().name() : null,
                    entity.getHookContent(),
                    coolPoints,
                    microPayoffs,
                    hardViolations,
                    softViolations,
                    entity.getOverallScore() != null ? entity.getOverallScore().doubleValue() : 0.0
            );
        } catch (Exception e) {
            log.error("反序列化ChapterReadingPower失败", e);
            return new ChapterReadingPower();
        }
    }

    private ChaseDebt toModel(ChaseDebtEntity entity) {
        return new ChaseDebt(
                entity.getId(),
                entity.getDebtType().name(),
                entity.getSubject(),
                entity.getDescription(),
                entity.getCreatedChapter(),
                entity.getUrgency(),
                entity.getStatus().name(),
                entity.getResolvedChapter(),
                entity.getResolvedReason()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }

    private List<Map<String, Object>> deserialize(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("JSON反序列化失败", e);
            return List.of();
        }
    }
}

