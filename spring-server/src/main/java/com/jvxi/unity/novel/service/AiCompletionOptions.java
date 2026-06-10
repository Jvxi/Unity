package com.jvxi.unity.novel.service;

/**
 * AI 模型调用参数封装
 *
 * 性能优化说明：
 * - 降低 temperature 提高响应速度和一致性
 * - 优化超时设置平衡速度和稳定性
 * - 合理设置 maxTokens 避免不必要的长输出
 */
public record AiCompletionOptions(
    double temperature,
    int maxTokens,
    int timeoutSeconds,
    boolean jsonObjectMode,
    boolean connectivityProbe
) {
    public AiCompletionOptions(double temperature, int maxTokens, int timeoutSeconds, boolean jsonObjectMode) {
        this(temperature, maxTokens, timeoutSeconds, jsonObjectMode, false);
    }

    /** 导入等任务使用固定上限，不受用户设置页 maxTokens 限制 */
    public int effectiveMaxTokens() {
        return maxTokens;
    }

    /** 按题目数量生成 JSON（批量越少、越快） */
    public static AiCompletionOptions jsonQuestions(int questionCount) {
        int count = Math.max(1, questionCount);
        int tokens = Math.min(2200, 320 + count * 150);
        // 优化：降低超时时间，从 22+n*5 调整为 18+n*4，加快响应
        int timeout = Math.min(60, 18 + count * 4);
        return new AiCompletionOptions(0.1, tokens, timeout, true);
    }

    /** 开书问卷单批（5 题） */
    public static AiCompletionOptions jsonBatch() {
        return jsonQuestions(5);
    }

    public static AiCompletionOptions jsonTask() {
        return jsonBatch();
    }

    /** 三个大纲方案一次生成 */
    public static AiCompletionOptions jsonProposals() {
        // 优化：从 120s 降低到 90s，加快大纲方案生成
        return new AiCompletionOptions(0.15, 4096, 90, true);
    }

    /** 导入：书籍信息（不含角色） */
    public static AiCompletionOptions jsonImportBookMeta() {
        // 优化：从 75s 降低到 60s
        return new AiCompletionOptions(0.1, 2048, 60, true);
    }

    /** 导入：角色列表 */
    public static AiCompletionOptions jsonImportCharacters() {
        // 优化：从 75s 降低到 60s
        return new AiCompletionOptions(0.1, 2048, 60, true);
    }

    /** 导入：大纲阶段 + 伏笔 */
    public static AiCompletionOptions jsonImportOutline() {
        // 优化：从 90s 降低到 75s
        return new AiCompletionOptions(0.1, 3072, 75, true);
    }

    /** 导入：章节元数据（正文由本地切分，不写入 JSON） */
    public static AiCompletionOptions jsonImportChapters() {
        // 优化：从 180s 降低到 150s
        return new AiCompletionOptions(0.1, 8192, 150, true);
    }

    public static AiCompletionOptions connectionProbe() {
        // 优化：从 12s 降低到 10s，加快连接测试
        return new AiCompletionOptions(0.0, 16, 10, false, true);
    }

    public static AiCompletionOptions defaults(AiCompletionOptions source) {
        if (source == null) {
            // 优化：从 120s 降低到 90s，加快默认生成速度
            return new AiCompletionOptions(0.7, 1800, 90, false);
        }
        return source;
    }
}

