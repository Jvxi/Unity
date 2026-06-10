package com.jvxi.unity.novel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * 并行执行多批 AI 任务，任一批次完成即回调（用于 SSE 尽快推送）。
 */
final class ParallelAiRunner {
    private ParallelAiRunner() {
    }

    static <T> void runIndexedBatches(
        int batchCount,
        IntFunction<T> batchTask,
        Consumer<T> onBatchComplete,
        int poolSize,
        long timeoutPerBatchSeconds
    ) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(poolSize, batchCount));
        CompletionService<T> completion = new ExecutorCompletionService<>(pool);
        List<Future<T>> submitted = new ArrayList<>(batchCount);
        try {
            for (int index = 0; index < batchCount; index++) {
                final int batchIndex = index;
                submitted.add(completion.submit(() -> batchTask.apply(batchIndex)));
            }
            for (int completed = 0; completed < batchCount; completed++) {
                T result = completion.take().get(timeoutPerBatchSeconds, TimeUnit.SECONDS);
                onBatchComplete.accept(result);
            }
        } catch (Exception exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(cause);
        } finally {
            pool.shutdownNow();
        }
    }
}

