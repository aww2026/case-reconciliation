package com.andy.reconcile.service;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.parser.CSVParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 🟢 GREEN Phase: Parallel CSV Parser implementation.
 *
 * Parses multiple bank statement files in parallel using CompletableFuture.
 *
 * Performance:
 * - Sequential: 3 files × 2 seconds = 6 seconds
 * - Parallel: max(2, 2, 2) = 2 seconds (3x faster!)
 *
 * Algorithm:
 * 1. Create CompletableFuture for each file
 * 2. Execute all futures concurrently using ExecutorService
 * 3. Wait for all to complete (join)
 * 4. Aggregate results
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelCSVParser {

    private final CSVParser csvParser;
    private final ExecutorService executorService;

    /**
     * Parse multiple bank statement files in parallel.
     *
     * @param bankFiles List of bank file paths
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @return Combined list of all bank statements
     * @throws IOException if any file parsing fails
     */
    public List<BankStatement> parseBankStatementsParallel(
            List<String> bankFiles,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        if (bankFiles == null || bankFiles.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Parsing {} bank files in parallel", bankFiles.size());

        // Create CompletableFuture for each file
        List<CompletableFuture<List<BankStatement>>> futures = bankFiles.stream()
                .map(filePath -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("Parsing file: {}", filePath);
                        return csvParser.parseBankStatements(filePath, startDate, endDate);
                    } catch (IOException e) {
                        log.error("Failed to parse file: {}", filePath, e);
                        throw new RuntimeException("Failed to parse file: " + filePath, e);
                    }
                }, executorService))
                .collect(Collectors.toList());

        // Wait for all futures to complete and collect results
        try {
            List<BankStatement> allStatements = futures.stream()
                    .map(CompletableFuture::join)  // Wait for completion
                    .flatMap(List::stream)         // Flatten results
                    .collect(Collectors.toList());

            log.info("Parsed {} total bank statements from {} files",
                    allStatements.size(), bankFiles.size());

            return allStatements;

        } catch (Exception e) {
            throw new IOException("Failed to parse bank files in parallel: " + e.getMessage(), e);
        }
    }
}
