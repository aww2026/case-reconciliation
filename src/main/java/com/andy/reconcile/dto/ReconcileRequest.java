package com.andy.reconcile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for reconciliation request.
 * Used with multipart file uploads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    // Note: Files will be uploaded as MultipartFile parameters in controller
    // This DTO only contains the JSON metadata
}
