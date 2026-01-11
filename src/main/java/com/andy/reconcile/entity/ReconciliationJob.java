package com.andy.reconcile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for reconciliation job metadata.
 * Stores information about reconciliation jobs for tracking and history.
 *
 * Database: reconciliation_jobs table
 * Note: Aggregates (counts, totals) are calculated on-demand via repository queries.
 */
@Entity
@Table(name = "reconciliation_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "system_file_name")
    private String systemFileName;

    @Column(name = "bank_file_names", length = 1000)
    private String bankFileNames; // Comma-separated list

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.PENDING;
        }
    }

    public enum JobStatus {
        PENDING,    // Job created, not yet started
        PROCESSING, // Job is currently being processed
        COMPLETED,  // Job completed successfully
        FAILED      // Job failed with error
    }
}
