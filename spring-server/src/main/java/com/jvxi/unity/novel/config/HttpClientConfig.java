package com.jvxi.unity.novel.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP 客户端配置：连接池、超时、虚拟线程
 *
 * 性能优化：
 * - 使用 HTTP/2 协议支持多路复用
 * - 虚拟线程执行器避免线程池瓶颈
 * - 优化连接超时设置平衡响应速度和稳定性
 */
@Configuration
public class HttpClientConfig {

    /**
     * AI 模型调用专用的 HttpClient
     * 使用 HTTP/2 + 虚拟线程，支持高并发 AI 请求
     */
    @Bean(destroyMethod = "close")
    public HttpClient aiHttpClient() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(8))  // 优化：从 10s 调整为 8s，平衡连接速度和稳定性
            .executor(executor)
            .build();
    }
}

