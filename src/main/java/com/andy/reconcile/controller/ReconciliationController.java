package com.andy.reconcile.controller;

import com.andy.reconcile.dto.ReconcileResponse;
import com.andy.reconcile.dto.ReconciliationDetailDto;
import com.andy.reconcile.dto.ReconciliationJobDto;
import com.andy.reconcile.service.ReconciliationJobService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API Controller for Reconciliation Service.
 * Provides endpoints for uploading CSV files and retrieving reconciliation results.
 */
@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {

    private final ReconciliationJobService reconciliationJobService;

    /**
     * POST /api/reconciliations - Upload CSVs and create reconciliation job.
     *
     * @param systemFile System transaction CSV file
     * @param bankFiles Bank statement CSV files (supports multiple files)
     * @param startDate Start date for reconciliation (format: yyyy-MM-dd)
     * @param endDate End date for reconciliation (format: yyyy-MM-dd)
     * @return Response with job ID
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReconcileResponse> createReconciliation(
            @RequestParam("systemFile") @NotNull MultipartFile systemFile,
            @RequestParam("bankFiles") @NotNull List<MultipartFile> bankFiles,
            @RequestParam("startDate") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            log.info("Creating reconciliation job for period {} to {}", startDate, endDate);
            log.info("System file: {}, Bank files: {}",
                    systemFile.getOriginalFilename(),
                    bankFiles.stream().map(MultipartFile::getOriginalFilename).toList());

            // Validate files
            if (systemFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ReconcileResponse.builder()
                                .message("System file is empty")
                                .build());
            }

            if (bankFiles.isEmpty() || bankFiles.stream().anyMatch(MultipartFile::isEmpty)) {
                return ResponseEntity.badRequest()
                        .body(ReconcileResponse.builder()
                                .message("Bank files are empty or contain empty file")
                                .build());
            }

            // Validate date range
            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest()
                        .body(ReconcileResponse.builder()
                                .message("End date must be after start date")
                                .build());
            }

            // Create job
            ReconcileResponse response = reconciliationJobService.createReconciliationJob(
                    systemFile,
                    bankFiles,
                    startDate,
                    endDate
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IOException e) {
            log.error("Failed to create reconciliation job", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReconcileResponse.builder()
                            .message("Failed to save uploaded files: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error creating reconciliation job", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReconcileResponse.builder()
                            .message("Unexpected error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/reconciliations - Get all reconciliation jobs (with pagination).
     *
     * @return List of reconciliation jobs
     */
    @GetMapping
    public ResponseEntity<List<ReconciliationJobDto>> getAllReconciliations() {
        log.info("Retrieving all reconciliation jobs");
        List<ReconciliationJobDto> jobs = reconciliationJobService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/reconciliations/{id} - Get specific reconciliation job with detailed results.
     *
     * @param id Job ID
     * @return Detailed reconciliation results
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReconciliationById(@PathVariable Long id) {
        try {
            log.info("Retrieving reconciliation job {}", id);
            ReconciliationDetailDto detail = reconciliationJobService.getJobDetail(id);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Job not found: " + id));
        } catch (Exception e) {
            log.error("Failed to retrieve reconciliation job {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve job: " + e.getMessage()));
        }
    }

    /**
     * GET /api/reconciliations/{id}/export - Export reconciliation results as JSON.
     * (CSV export can be added later)
     *
     * @param id Job ID
     * @return Reconciliation results for export
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<?> exportReconciliation(@PathVariable Long id) {
        try {
            log.info("Exporting reconciliation job {}", id);
            ReconciliationDetailDto detail = reconciliationJobService.getJobDetail(id);

            if (detail.getSummary() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Job not completed yet or failed"));
            }

            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Job not found: " + id));
        } catch (Exception e) {
            log.error("Failed to export reconciliation job {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to export job: " + e.getMessage()));
        }
    }

    /**
     * Simple error response DTO.
     */
    private record ErrorResponse(String message) {}
}
