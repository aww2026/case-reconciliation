package com.andy.reconcile.service;

import com.andy.reconcile.domain.*;
import com.andy.reconcile.dto.*;
import com.andy.reconcile.entity.*;
import com.andy.reconcile.entity.ReconciliationJob.JobStatus;
import com.andy.reconcile.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing reconciliation jobs with async processing and database persistence.
 * Aggregates (counts, totals) are calculated on-demand from reconciliation items.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJobService {

    private final ReconciliationJobRepository jobRepository;
    private final MatchedTransactionRepository matchedTransactionRepository;
    private final UnmatchedSystemTransactionRepository unmatchedSystemTransactionRepository;
    private final UnmatchedBankStatementRepository unmatchedBankStatementRepository;
    private final ReconciliationService reconciliationService;

    private static final String UPLOAD_DIR = "uploads";

    /**
     * Create a new reconciliation job and process it asynchronously.
     *
     * @param systemFile System transaction CSV file
     * @param bankFiles List of bank statement CSV files
     * @param startDate Start date for reconciliation
     * @param endDate End date for reconciliation
     * @return Response with job ID
     */
    @Transactional
    public ReconcileResponse createReconciliationJob(
            MultipartFile systemFile,
            List<MultipartFile> bankFiles,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        // Create uploads directory if not exists
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Save uploaded files
        String systemFilePath = saveFile(systemFile);
        List<String> bankFilePaths = bankFiles.stream()
                .map(this::saveFileQuietly)
                .collect(Collectors.toList());

        // Extract file names
        String systemFileName = systemFile.getOriginalFilename();
        String bankFileNames = bankFiles.stream()
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.joining(", "));

        // Create job entity
        ReconciliationJob job = ReconciliationJob.builder()
                .status(JobStatus.PENDING)
                .systemFileName(systemFileName)
                .bankFileNames(bankFileNames)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        job = jobRepository.save(job);

        // Process asynchronously
        processReconciliationAsync(job.getId(), systemFilePath, bankFilePaths, startDate, endDate);

        return ReconcileResponse.builder()
                .jobId(job.getId())
                .status(JobStatus.PENDING)
                .message("Reconciliation job created successfully. Use job ID to poll for status.")
                .build();
    }

    /**
     * Process reconciliation asynchronously.
     */
    @Async
    @Transactional
    public void processReconciliationAsync(
            Long jobId,
            String systemFilePath,
            List<String> bankFilePaths,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ReconciliationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        try {
            log.info("Starting reconciliation job {} asynchronously", jobId);

            // Update status to PROCESSING
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            // Perform reconciliation
            ReconciliationSummary summary = reconciliationService.reconcile(
                    systemFilePath,
                    bankFilePaths,
                    startDate,
                    endDate
            );

            // Save results to database
            saveReconciliationResults(job, summary);

            // Update job status to COMPLETED
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Reconciliation job {} completed successfully", jobId);

        } catch (Exception e) {
            log.error("Reconciliation job {} failed", jobId, e);

            // Update job status to FAILED
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        } finally {
            // Clean up uploaded files
            cleanupFiles(systemFilePath, bankFilePaths);
        }
    }

    /**
     * Save reconciliation results to database.
     * Saves items directly to job (no intermediate summary entity).
     */
    private void saveReconciliationResults(ReconciliationJob job, ReconciliationSummary summary) {
        // Save matched transactions
        for (MatchedPair match : summary.getMatches()) {
            MatchedTransaction matchEntity = mapToMatchedTransaction(match, job);
            matchedTransactionRepository.save(matchEntity);
        }

        // Save unmatched system transactions
        for (SystemTransaction sys : summary.getUnmatchedSystem()) {
            UnmatchedSystemTransaction unmatchedSysEntity = mapToUnmatchedSystemTransaction(sys, job);
            unmatchedSystemTransactionRepository.save(unmatchedSysEntity);
        }

        // Save unmatched bank statements
        for (List<BankStatement> bankList : summary.getUnmatchedBankByBank().values()) {
            for (BankStatement bank : bankList) {
                UnmatchedBankStatement unmatchedBankEntity = mapToUnmatchedBankStatement(bank, job);
                unmatchedBankStatementRepository.save(unmatchedBankEntity);
            }
        }
    }

    /**
     * Get all reconciliation jobs.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationJobDto> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToJobDto)
                .collect(Collectors.toList());
    }

    /**
     * Get reconciliation job by ID with detailed results.
     */
    @Transactional(readOnly = true)
    public ReconciliationDetailDto getJobDetail(Long jobId) {
        ReconciliationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        ReconciliationJobDto jobDto = mapToJobDto(job);

        // Get summary if job is completed
        ReconciliationDetailDto.ReconciliationSummaryDto summaryDto = null;
        if (job.getStatus() == JobStatus.COMPLETED) {
            summaryDto = calculateSummaryDto(jobId, job.getStartDate(), job.getEndDate());
        }

        return ReconciliationDetailDto.builder()
                .job(jobDto)
                .summary(summaryDto)
                .build();
    }

    // =============== Helper Methods ===============

    private String saveFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    private String saveFileQuietly(MultipartFile file) {
        try {
            return saveFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + file.getOriginalFilename(), e);
        }
    }

    private void cleanupFiles(String systemFilePath, List<String> bankFilePaths) {
        try {
            Files.deleteIfExists(Paths.get(systemFilePath));
            for (String bankFilePath : bankFilePaths) {
                Files.deleteIfExists(Paths.get(bankFilePath));
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup files", e);
        }
    }

    private ReconciliationJobDto mapToJobDto(ReconciliationJob job) {
        ReconciliationJobDto dto = ReconciliationJobDto.builder()
                .id(job.getId())
                .status(job.getStatus())
                .systemFileName(job.getSystemFileName())
                .bankFileNames(job.getBankFileNames())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .build();

        // Calculate aggregates on-demand if job is completed
        if (job.getStatus() == JobStatus.COMPLETED) {
            long matchedCount = matchedTransactionRepository.findByJobId(job.getId()).size();
            long unmatchedSystemCount = unmatchedSystemTransactionRepository.findByJobId(job.getId()).size();
            long unmatchedBankCount = unmatchedBankStatementRepository.findByJobId(job.getId()).size();

            dto.setMatchedCount((int) matchedCount);
            dto.setUnmatchedCount((int) (unmatchedSystemCount + unmatchedBankCount));
            dto.setTotalSystemTransactions((int) (matchedCount + unmatchedSystemCount));
            dto.setTotalBankTransactions((int) (matchedCount + unmatchedBankCount));
        }

        return dto;
    }

    /**
     * Calculate summary DTO from database items.
     */
    private ReconciliationDetailDto.ReconciliationSummaryDto calculateSummaryDto(Long jobId, LocalDate startDate, LocalDate endDate) {
        // Query items from database
        List<MatchedTransaction> matches = matchedTransactionRepository.findByJobId(jobId);
        List<UnmatchedSystemTransaction> unmatchedSystem = unmatchedSystemTransactionRepository.findByJobId(jobId);
        List<UnmatchedBankStatement> unmatchedBank = unmatchedBankStatementRepository.findByJobId(jobId);

        // Calculate aggregates
        int totalSystemTransactions = matches.size() + unmatchedSystem.size();
        int totalBankTransactions = matches.size() + unmatchedBank.size();
        int matchedCount = matches.size();
        int unmatchedCount = unmatchedSystem.size() + unmatchedBank.size();

        double totalDiscrepancy = matches.stream()
                .map(MatchedTransaction::getDiscrepancy)
                .filter(Objects::nonNull)
                .mapToDouble(d -> d.doubleValue())
                .sum();

        double reconciliationRate = totalSystemTransactions > 0
                ? (double) matchedCount / totalSystemTransactions * 100.0
                : 0.0;

        // Map matched transactions
        List<ReconciliationDetailDto.MatchedPairDto> matchDtos = matches.stream()
                .map(this::mapToMatchedPairDto)
                .collect(Collectors.toList());

        // Map unmatched system transactions
        List<ReconciliationDetailDto.UnmatchedSystemTransactionDto> unmatchedSysDtos = unmatchedSystem.stream()
                .map(this::mapToUnmatchedSystemDto)
                .collect(Collectors.toList());

        // Map unmatched bank statements (grouped by bank)
        Map<String, List<ReconciliationDetailDto.UnmatchedBankStatementDto>> unmatchedBankByBank =
                unmatchedBank.stream()
                        .collect(Collectors.groupingBy(
                                bank -> bank.getBankName() != null ? bank.getBankName() : "Unknown",
                                Collectors.mapping(this::mapToUnmatchedBankDto, Collectors.toList())
                        ));

        return ReconciliationDetailDto.ReconciliationSummaryDto.builder()
                .totalSystemTransactions(totalSystemTransactions)
                .totalBankTransactions(totalBankTransactions)
                .matchedCount(matchedCount)
                .unmatchedCount(unmatchedCount)
                .totalDiscrepancy(java.math.BigDecimal.valueOf(totalDiscrepancy))
                .reconciliationRate(reconciliationRate)
                .startDate(startDate)
                .endDate(endDate)
                .matches(matchDtos)
                .unmatchedSystem(unmatchedSysDtos)
                .unmatchedBankByBank(unmatchedBankByBank)
                .build();
    }

    private MatchedTransaction mapToMatchedTransaction(MatchedPair match, ReconciliationJob job) {
        return MatchedTransaction.builder()
                .job(job)
                .systemTrxId(match.getSystemTransaction().getTrxID())
                .systemAmount(match.getSystemTransaction().getAmount())
                .systemType(match.getSystemTransaction().getType().name())
                .systemTransactionTime(match.getSystemTransaction().getTransactionTime())
                .bankUniqueIdentifier(match.getBankStatement().getUniqueIdentifier())
                .bankAmount(match.getBankStatement().getAmount())
                .bankDate(match.getBankStatement().getDate())
                .bankName(match.getBankStatement().getBankName())
                .discrepancy(match.getDiscrepancy())
                .confidence(match.getConfidence())
                .build();
    }

    private UnmatchedSystemTransaction mapToUnmatchedSystemTransaction(SystemTransaction sys, ReconciliationJob job) {
        return UnmatchedSystemTransaction.builder()
                .job(job)
                .trxId(sys.getTrxID())
                .amount(sys.getAmount())
                .type(sys.getType().name())
                .transactionTime(sys.getTransactionTime())
                .build();
    }

    private UnmatchedBankStatement mapToUnmatchedBankStatement(BankStatement bank, ReconciliationJob job) {
        return UnmatchedBankStatement.builder()
                .job(job)
                .uniqueIdentifier(bank.getUniqueIdentifier())
                .amount(bank.getAmount())
                .date(bank.getDate())
                .bankName(bank.getBankName())
                .build();
    }

    private ReconciliationDetailDto.MatchedPairDto mapToMatchedPairDto(MatchedTransaction match) {
        return ReconciliationDetailDto.MatchedPairDto.builder()
                .systemTrxId(match.getSystemTrxId())
                .systemAmount(match.getSystemAmount())
                .systemType(match.getSystemType())
                .systemTransactionTime(match.getSystemTransactionTime())
                .bankUniqueIdentifier(match.getBankUniqueIdentifier())
                .bankAmount(match.getBankAmount())
                .bankDate(match.getBankDate())
                .bankName(match.getBankName())
                .discrepancy(match.getDiscrepancy())
                .confidence(match.getConfidence())
                .build();
    }

    private ReconciliationDetailDto.UnmatchedSystemTransactionDto mapToUnmatchedSystemDto(UnmatchedSystemTransaction sys) {
        return ReconciliationDetailDto.UnmatchedSystemTransactionDto.builder()
                .trxId(sys.getTrxId())
                .amount(sys.getAmount())
                .type(sys.getType())
                .transactionTime(sys.getTransactionTime())
                .build();
    }

    private ReconciliationDetailDto.UnmatchedBankStatementDto mapToUnmatchedBankDto(UnmatchedBankStatement bank) {
        return ReconciliationDetailDto.UnmatchedBankStatementDto.builder()
                .uniqueIdentifier(bank.getUniqueIdentifier())
                .amount(bank.getAmount())
                .date(bank.getDate())
                .bankName(bank.getBankName())
                .build();
    }
}
