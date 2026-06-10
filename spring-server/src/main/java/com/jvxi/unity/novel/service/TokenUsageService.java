package com.jvxi.unity.novel.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.auth.UserContext;
import com.jvxi.unity.novel.model.AiSettings;

/**
 * Token 使用量追踪服务
 *
 * 功能：
 * - 记录每个用户的 token 使用量
 * - 提供 token 使用统计和限额信息
 * - 支持查询远程 API 的 Token 配额
 */
@Component
public class TokenUsageService {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageService.class);
    private static final ConcurrentHashMap<String, UsageCounters> counters = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TokenUsageService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录 token 使用量
     */
    public void recordUsage(int promptTokens, int completionTokens, int totalTokens) {
        String userId = UserContext.requireUserId();
        counters.computeIfAbsent(userId, k -> new UsageCounters())
            .add(promptTokens, completionTokens, totalTokens);
    }

    /**
     * 获取当前用户的 token 使用情况
     */
    public TokenUsageSnapshot getUsage() {
        String userId = UserContext.requireUserId();
        UsageCounters c = counters.get(userId);
        if (c == null) {
            return new TokenUsageSnapshot(0, 0, 0);
        }
        return c.snapshot();
    }

    /**
     * 查询远程 API 的 Token 配额
     * @return 配额信息，如果查询失败返回 null
     */
    public RemoteQuotaInfo queryRemoteQuota(AiSettings settings) {
        if (settings == null || settings.apiKey() == null || settings.apiKey().isBlank()) {
            log.debug("跳过配额查询：API Key 未配置");
            return null;
        }

        String baseUrl = settings.baseUrl();
        String apiKey = settings.apiKey();
        String provider = settings.provider();

        if (baseUrl == null || baseUrl.isBlank()) {
            log.debug("跳过配额查询：Base URL 未配置");
            return null;
        }

        log.info("查询远程配额: provider={}", provider);

        try {
            RemoteQuotaInfo result = null;
            // 根据不同的 provider 调用不同的余额查询接口
            // 注意：小米 MiMo 没有提供 Token 查询接口，需要用户手动配置
            if ("deepseek".equals(provider)) {
                result = queryDeepSeekQuota(baseUrl, apiKey);
            } else if ("moonshot".equals(provider)) {
                result = queryMoonshotQuota(baseUrl, apiKey);
            } else if (!"xiaomi-mimo".equals(provider)) {
                // 对于 openai-compatible 和其他未知 provider，尝试通用查询
                // 小米 MiMo 没有查询接口，跳过
                result = queryGenericQuota(baseUrl, apiKey);
            }

            if (result != null) {
                log.info("配额查询成功: totalTokens={}, currency={}", result.totalTokens(), result.currency());
            } else {
                log.info("配额查询返回空结果");
            }
            return result;
        } catch (Exception e) {
            log.warn("查询远程配额失败: provider={}", provider);
            return null;
        }
    }

    private RemoteQuotaInfo queryDeepSeekQuota(String baseUrl, String apiKey) throws IOException, InterruptedException {
        String url = baseUrl.endsWith("/") ? baseUrl + "user/balance" : baseUrl + "/user/balance";
        log.debug("查询 DeepSeek 配额");

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + apiKey)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("DeepSeek 响应: status={}", response.statusCode());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode balanceInfo = root.path("balance_info");
            if (!balanceInfo.isMissingNode()) {
                double balance = balanceInfo.path("balance").asDouble(0);
                double grantedBalance = balanceInfo.path("granted_balance").asDouble(0);
                // DeepSeek 返回的是美元余额，转换为 token 估算（1美元约等于 100万 token）
                long totalTokens = (long) ((balance + grantedBalance) * 1000000);
                return new RemoteQuotaInfo(totalTokens, totalTokens, "USD", balance + grantedBalance);
            }
        }
        return null;
    }

    private RemoteQuotaInfo queryMoonshotQuota(String baseUrl, String apiKey) throws IOException, InterruptedException {
        String url = baseUrl.endsWith("/") ? baseUrl + "users/me/balance" : baseUrl + "/users/me/balance";
        log.debug("查询 Moonshot 配额");

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + apiKey)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("Moonshot 响应: status={}", response.statusCode());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) {
                double availableBalance = data.path("available_balance").asDouble(0);
                double voucherBalance = data.path("voucher_balance").asDouble(0);
                // Moonshot 返回的是元，转换为 token 估算（1元约等于 100万 token）
                long totalTokens = (long) ((availableBalance + voucherBalance) * 1000000);
                return new RemoteQuotaInfo(totalTokens, totalTokens, "CNY", availableBalance + voucherBalance);
            }
        }
        return null;
    }

    private RemoteQuotaInfo queryXiaomiMimoQuota(String baseUrl, String apiKey) throws IOException, InterruptedException {
        // 尝试多个可能的小米 MiMo 余额查询接口
        // 注意：baseUrl 可能已经包含 /v1，所以路径不要加 v1/ 前缀
        String[] paths = {
            "user/balance",
            "user/info",
            "user/subscription",
            "user/quota",
            "billing/usage",
            "billing/subscription",
            "dashboard/billing/subscription",
            "account/balance",
            "account/info"
        };

        log.info("小米 MiMo 配额查询，尝试 {} 个接口路径", paths.length);

        for (String path : paths) {
            String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
            log.debug("尝试小米 MiMo 配额接口路径: {}", path);

            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("小米 MiMo 响应 [{}]: status={}", path, response.statusCode());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    JsonNode root = objectMapper.readTree(body);

                    // 尝试不同的响应格式
                    // 格式1: {"balance": 100.5}
                    if (root.has("balance")) {
                        double balance = root.path("balance").asDouble(0);
                        long totalTokens = (long) (balance * 1000000);
                        log.info("解析到余额 (balance): {}", balance);
                        return new RemoteQuotaInfo(totalTokens, totalTokens, "CNY", balance);
                    }

                    // 格式2: {"data": {"balance": 100.5}}
                    JsonNode data = root.path("data");
                    if (!data.isMissingNode()) {
                        if (data.has("balance")) {
                            double balance = data.path("balance").asDouble(0);
                            long totalTokens = (long) (balance * 1000000);
                            log.info("解析到余额 (data.balance): {}", balance);
                            return new RemoteQuotaInfo(totalTokens, totalTokens, "CNY", balance);
                        }
                        if (data.has("total_granted")) {
                            long totalGranted = data.path("total_granted").asLong(0);
                            log.info("解析到总额度 (data.total_granted): {}", totalGranted);
                            return new RemoteQuotaInfo(totalGranted, totalGranted, "token", totalGranted);
                        }
                        if (data.has("quota")) {
                            long quota = data.path("quota").asLong(0);
                            log.info("解析到配额 (data.quota): {}", quota);
                            return new RemoteQuotaInfo(quota, quota, "token", quota);
                        }
                        if (data.has("total_tokens")) {
                            long totalTokens = data.path("total_tokens").asLong(0);
                            log.info("解析到总token (data.total_tokens): {}", totalTokens);
                            return new RemoteQuotaInfo(totalTokens, totalTokens, "token", totalTokens);
                        }
                    }

                    // 格式3: {"total_granted": 1000000, "total_used": 500000}
                    if (root.has("total_granted")) {
                        long totalGranted = root.path("total_granted").asLong(0);
                        log.info("解析到总额度 (total_granted): {}", totalGranted);
                        return new RemoteQuotaInfo(totalGranted, totalGranted, "token", totalGranted);
                    }

                    // 格式4: {"total_tokens": 1000000}
                    if (root.has("total_tokens")) {
                        long totalTokens = root.path("total_tokens").asLong(0);
                        log.info("解析到总token (total_tokens): {}", totalTokens);
                        return new RemoteQuotaInfo(totalTokens, totalTokens, "token", totalTokens);
                    }

                    // 格式5: {"quota": {"total": 1000000, "used": 500000}}
                    JsonNode quotaNode = root.path("quota");
                    if (!quotaNode.isMissingNode()) {
                        long total = quotaNode.path("total").asLong(0);
                        if (total > 0) {
                            log.info("解析到配额 (quota.total): {}", total);
                            return new RemoteQuotaInfo(total, total, "token", total);
                        }
                    }

                    // 格式6: {"subscription": {"total_tokens": 1000000}}
                    JsonNode subNode = root.path("subscription");
                    if (!subNode.isMissingNode()) {
                        long totalTokens = subNode.path("total_tokens").asLong(0);
                        if (totalTokens > 0) {
                            log.info("解析到订阅额度 (subscription.total_tokens): {}", totalTokens);
                            return new RemoteQuotaInfo(totalTokens, totalTokens, "token", totalTokens);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("小米 MiMo 接口 {} 查询失败", path);
            }
        }
        return null;
    }

    /**
     * 通用配额查询 - 尝试常见的余额接口路径
     */
    private RemoteQuotaInfo queryGenericQuota(String baseUrl, String apiKey) throws IOException, InterruptedException {
        String[] paths = {"user/balance", "billing/usage", "billing/subscription", "account/balance"};

        for (String path : paths) {
            String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
            log.debug("通用配额查询路径: {}", path);

            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.debug("通用配额响应 [{}]: status={}", path, response.statusCode());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());

                    // 尝试解析常见的余额格式
                    if (root.has("balance")) {
                        double balance = root.path("balance").asDouble(0);
                        long totalTokens = (long) (balance * 1000000);
                        return new RemoteQuotaInfo(totalTokens, totalTokens, "CNY", balance);
                    }

                    JsonNode data = root.path("data");
                    if (!data.isMissingNode()) {
                        if (data.has("balance")) {
                            double balance = data.path("balance").asDouble(0);
                            long totalTokens = (long) (balance * 1000000);
                            return new RemoteQuotaInfo(totalTokens, totalTokens, "CNY", balance);
                        }
                        if (data.has("total_granted")) {
                            long totalGranted = data.path("total_granted").asLong(0);
                            return new RemoteQuotaInfo(totalGranted, totalGranted, "token", totalGranted);
                        }
                    }

                    if (root.has("total_granted")) {
                        long totalGranted = root.path("total_granted").asLong(0);
                        return new RemoteQuotaInfo(totalGranted, totalGranted, "token", totalGranted);
                    }
                }
            } catch (Exception e) {
                log.debug("通用接口 {} 查询失败", path);
            }
        }
        return null;
    }

    /**
     * 获取包含远程配额信息的 token 使用情况
     */
    public TokenUsageSnapshot getUsageWithRemoteQuota(AiSettings settings) {
        TokenUsageSnapshot localSnapshot = getUsage();

        // 尝试查询远程配额
        RemoteQuotaInfo quota = queryRemoteQuota(settings);
        if (quota != null && quota.totalTokens() > 0) {
            log.info("使用远程配额: {} tokens", quota.totalTokens());
            return new TokenUsageSnapshot(
                localSnapshot.promptTokens(),
                localSnapshot.completionTokens(),
                localSnapshot.totalTokens(),
                quota.totalTokens()
            );
        }

        // 如果没有远程配额，使用本地配置的 contextWindowSize
        long contextWindowSize = settings != null ? settings.contextWindowSize() : 0;
        if (contextWindowSize > 0) {
            log.info("使用本地配置的上下文窗口大小: {} tokens", contextWindowSize);
            return new TokenUsageSnapshot(
                localSnapshot.promptTokens(),
                localSnapshot.completionTokens(),
                localSnapshot.totalTokens(),
                contextWindowSize
            );
        }

        log.info("未获取到配额信息，远程查询和本地配置均为空");
        return localSnapshot;
    }

    /**
     * 远程配额信息
     */
    public record RemoteQuotaInfo(
        long totalTokens,
        long remainingTokens,
        String currency,
        double rawBalance
    ) {}

    /**
     * Token 使用快照
     */
    public record TokenUsageSnapshot(
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long maxTokens,
        long remainingTokens,
        double usagePercent
    ) {
        public TokenUsageSnapshot(long promptTokens, long completionTokens, long totalTokens) {
            this(promptTokens, completionTokens, totalTokens, 0, 0, 0);
        }

        public TokenUsageSnapshot(long promptTokens, long completionTokens, long totalTokens, long maxTokens) {
            this(
                promptTokens,
                completionTokens,
                totalTokens,
                maxTokens,
                Math.max(0, maxTokens - totalTokens),
                maxTokens > 0 ? Math.min(100.0, (double) totalTokens / maxTokens * 100) : 0
            );
        }
    }

    /**
     * 使用量计数器
     */
    private static final class UsageCounters {
        final AtomicLong prompt = new AtomicLong();
        final AtomicLong completion = new AtomicLong();
        final AtomicLong total = new AtomicLong();

        void add(long p, long c, long t) {
            prompt.addAndGet(p);
            completion.addAndGet(c);
            total.addAndGet(t);
        }

        TokenUsageSnapshot snapshot() {
            return new TokenUsageSnapshot(prompt.get(), completion.get(), total.get());
        }
    }
}

