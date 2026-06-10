package com.jvxi.unity.novel.service.context;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContextRanker {

    private static final double RECENCY_WEIGHT = 0.7;
    private static final double FREQUENCY_WEIGHT = 0.3;
    private static final double HOOK_BONUS = 0.2;

    // 钩子提示关键词
    private static final List<String> HOOK_HINTS = List.of(
            "?", "？", "悬念", "钩子", "反转", "冲突", "危机", "秘密"
    );

    /**
     * 计算上下文段落的相关性分数
     */
    public double score(String text, int sourceChapter, int currentChapter, int frequency) {
        // 时效性分数
        int gap = currentChapter - sourceChapter;
        double recency = 1.0 / (1.0 + gap);

        // 频率分数（对数缩放）
        double frequencyScore = Math.min(1.0, Math.log(1 + frequency) / Math.log(11));

        // 钩子加分
        double hookBonus = containsHookHints(text) ? HOOK_BONUS : 0;

        return recency * RECENCY_WEIGHT + frequencyScore * FREQUENCY_WEIGHT + hookBonus;
    }

    /**
     * 对上下文段落列表进行排序
     */
    public List<ScoredContext> rankContexts(List<ContextEntry> entries, int currentChapter) {
        List<ScoredContext> scored = new ArrayList<>();

        for (ContextEntry entry : entries) {
            double score = score(entry.text(), entry.sourceChapter(), currentChapter, entry.frequency());
            scored.add(new ScoredContext(entry, score));
        }

        // 按分数降序排序
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));

        return scored;
    }

    /**
     * 检查文本是否包含钩子提示
     */
    private boolean containsHookHints(String text) {
        if (text == null) return false;
        return HOOK_HINTS.stream().anyMatch(text::contains);
    }

    /**
     * 上下文条目
     */
    public record ContextEntry(
            String text,
            int sourceChapter,
            int frequency,
            String category
    ) {}

    /**
     * 带分数的上下文
     */
    public record ScoredContext(
            ContextEntry entry,
            double score
    ) {}

    /**
     * 动态预算配置
     */
    public Map<String, Double> getDynamicBudget(int chapterNumber) {
        if (chapterNumber <= 30) {
            // 早期章节：重视核心和场景
            return Map.of(
                    "core_weight", 1.2,
                    "scene_weight", 1.1,
                    "global_weight", 0.9,
                    "detail_weight", 0.8
            );
        } else if (chapterNumber <= 119) {
            // 中期章节：平衡权重
            return Map.of(
                    "core_weight", 1.0,
                    "scene_weight", 1.0,
                    "global_weight", 1.0,
                    "detail_weight", 1.0
            );
        } else {
            // 后期章节：重视全局，降低场景
            return Map.of(
                    "core_weight", 1.1,
                    "scene_weight", 0.9,
                    "global_weight", 1.2,
                    "detail_weight", 0.8
            );
        }
    }

    /**
     * 应用动态预算调整分数
     */
    public double applyBudget(double baseScore, String category, int chapterNumber) {
        Map<String, Double> budget = getDynamicBudget(chapterNumber);

        return switch (category) {
            case "core" -> baseScore * budget.get("core_weight");
            case "scene" -> baseScore * budget.get("scene_weight");
            case "global" -> baseScore * budget.get("global_weight");
            case "detail" -> baseScore * budget.get("detail_weight");
            default -> baseScore;
        };
    }
}

