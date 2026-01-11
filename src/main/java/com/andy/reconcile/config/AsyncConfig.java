package com.andy.reconcile.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for async processing.
 * Enables @Async annotation support for background job processing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Async configuration is in application.properties:
    // spring.task.execution.pool.core-size=5
    // spring.task.execution.pool.max-size=10
    // spring.task.execution.pool.queue-capacity=25
}
