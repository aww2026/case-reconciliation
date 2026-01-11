package com.andy.reconcile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Reconciliation Service.
 * Provides REST API for transaction reconciliation.
 */
@SpringBootApplication
public class ReconcileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconcileServiceApplication.class, args);
    }
}
