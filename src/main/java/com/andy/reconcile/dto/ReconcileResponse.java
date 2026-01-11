package com.andy.reconcile.dto;

import com.andy.reconcile.entity.ReconciliationJob.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for immediate response after creating a reconciliation job.
 * Contains job ID for polling status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileResponse {

    private Long jobId;
    private JobStatus status;
    private String message;
}
