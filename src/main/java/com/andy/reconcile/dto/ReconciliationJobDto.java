package com.andy.reconcile.dto;

import com.andy.reconcile.entity.ReconciliationJob.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for reconciliation job summary (for list endpoints).
 * Contains job metadata without detailed results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationJobDto {

    private Long id;
    private JobStatus status;
    private String systemFileName;
    private String bankFileNames;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    // Summary counts (if available)
    private Integer totalSystemTransactions;
    private Integer totalBankTransactions;
    private Integer matchedCount;
    private Integer unmatchedCount;
}
