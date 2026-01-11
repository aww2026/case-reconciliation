package com.andy.reconcile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for reconciliation service.
 *
 * Provides:
 * - ExecutorService for parallel processing
 * - Streaming threshold configuration
 * - Thread pool configuration
 */
@Configuration
@EnableAsync
public class ReconciliationConfig {

    @Value("${reconciliation.parallel.thread-pool-size:4}")
    private int threadPoolSize;

    @Value("${reconciliation.streaming.threshold-bytes:104857600}")  // 100MB default
    private long streamingThresholdBytes;

    @Value("${reconciliation.parallel.enabled:true}")
    private boolean parallelProcessingEnabled;

    /**
     * ExecutorService for parallel bank file processing.
     *
     * Default: 4 threads (good for 2-8 bank files)
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService reconciliationExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    public long getStreamingThresholdBytes() {
        return streamingThresholdBytes;
    }

    public boolean isParallelProcessingEnabled() {
        return parallelProcessingEnabled;
    }
}
