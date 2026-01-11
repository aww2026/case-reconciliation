package com.andy.reconcile.service;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.domain.SystemTransaction;
import com.andy.reconcile.entity.MatchedTransaction;
import com.andy.reconcile.entity.ReconciliationJob;
import com.andy.reconcile.entity.UnmatchedBankStatement;
import com.andy.reconcile.entity.UnmatchedSystemTransaction;
import com.andy.reconcile.matcher.ExactMatcher;
import com.andy.reconcile.parser.CSVParser;
import com.andy.reconcile.repository.MatchedTransactionRepository;
import com.andy.reconcile.repository.UnmatchedBankStatementRepository;
import com.andy.reconcile.repository.UnmatchedSystemTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🟢 GREEN Phase: Streaming reconciliation service.
 *
 * Memory-efficient implementation for large files (>100MB).
 *
 * Algorithm:
 * 1. Load system transactions into HashMap (assume smaller dataset)
 * 2. Stream bank statements line-by-line (don't load all into memory)
 * 3. For each bank statement:
 *    - Check if matches system transaction in HashMap
 *    - Save match/unmatch to database immediately
 *    - DON'T accumulate in memory
 * 4. After streaming, save remaining unmatched system transactions
 *
 * Memory usage: O(n) where n = system transactions
 * (vs O(n+m) for in-memory where m = bank statements)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingReconciliationService {

    private final CSVParser csvParser;
    private final ExactMatcher exactMatcher;
    private final MatchedTransactionRepository matchedTransactionRepository;
    private final UnmatchedSystemTransactionRepository unmatchedSystemTransactionRepository;
    private final UnmatchedBankStatementRepository unmatchedBankStatementRepository;

    /**
     * Reconcile with streaming for memory efficiency.
     *
     * @param systemTransactionFile System transaction CSV path
     * @param bankStatementFile Bank statement CSV path (single file)
     * @param startDate Start date
     * @param endDate End date
     * @param job Reconciliation job entity
     */
    public void reconcileWithStreaming(
            String systemTransactionFile,
            String bankStatementFile,
            LocalDate startDate,
            LocalDate endDate,
            ReconciliationJob job
    ) throws IOException {
        log.info("Starting streaming reconciliation for job {}", job.getId());

        // Step 1: Load system transactions into HashMap (smaller dataset)
        List<SystemTransaction> systemTransactions = csvParser.parseSystemTransactions(
                systemTransactionFile,
                startDate,
                endDate
        );

        Map<String, SystemTransaction> systemMap = buildSystemMap(systemTransactions);
        log.info("Loaded {} system transactions into memory", systemMap.size());

        // Step 2: Stream bank statements and match on-the-fly
        int matchedCount = 0;
        int unmatchedBankCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(bankStatementFile))) {
            String headerLine = reader.readLine();  // Skip header
            if (headerLine == null) {
                throw new IOException("Empty bank statement file");
            }

            String line;
            int lineNumber = 2;

            while ((line = reader.readLine()) != null) {
                try {
                    BankStatement bankStmt = parseBankStatementLine(line);

                    // Filter by date range
                    if (isWithinDateRange(bankStmt.getDate(), startDate, endDate)) {
                        String key = buildMatchingKey(bankStmt);

                        if (systemMap.containsKey(key)) {
                            // Match found! Save immediately
                            SystemTransaction sysTrx = systemMap.remove(key);
                            saveMatch(sysTrx, bankStmt, job);
                            matchedCount++;
                        } else {
                            // Unmatched bank statement - save immediately
                            saveUnmatchedBank(bankStmt, job);
                            unmatchedBankCount++;
                        }
                    }

                } catch (Exception e) {
                    log.warn("Skipping invalid line {}: {}", lineNumber, e.getMessage());
                }

                lineNumber++;
            }
        }

        // Step 3: Save remaining unmatched system transactions
        int unmatchedSystemCount = 0;
        for (SystemTransaction sysTrx : systemMap.values()) {
            saveUnmatchedSystem(sysTrx, job);
            unmatchedSystemCount++;
        }

        log.info("Streaming reconciliation complete: {} matched, {} unmatched system, {} unmatched bank",
                matchedCount, unmatchedSystemCount, unmatchedBankCount);
    }

    private Map<String, SystemTransaction> buildSystemMap(List<SystemTransaction> systemTransactions) {
        Map<String, SystemTransaction> map = new HashMap<>();
        for (SystemTransaction trx : systemTransactions) {
            String key = trx.buildMatchingKey();
            map.putIfAbsent(key, trx);
        }
        return map;
    }

    private BankStatement parseBankStatementLine(String line) {
        String[] fields = line.split(",");
        return BankStatement.builder()
                .uniqueIdentifier(fields[0].trim())
                .amount(new BigDecimal(fields[1].trim()))
                .date(LocalDate.parse(fields[2].trim(), DateTimeFormatter.ISO_DATE))
                .bankName(fields.length > 3 ? fields[3].trim() : "Unknown")
                .build();
    }

    private String buildMatchingKey(BankStatement stmt) {
        return stmt.buildMatchingKey();
    }

    private boolean isWithinDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private void saveMatch(SystemTransaction sysTrx, BankStatement bankStmt, ReconciliationJob job) {
        MatchedTransaction entity = MatchedTransaction.builder()
                .job(job)
                .systemTrxId(sysTrx.getTrxID())
                .systemAmount(sysTrx.getAmount())
                .systemType(sysTrx.getType().name())
                .systemTransactionTime(sysTrx.getTransactionTime())
                .bankUniqueIdentifier(bankStmt.getUniqueIdentifier())
                .bankAmount(bankStmt.getAmount())
                .bankDate(bankStmt.getDate())
                .bankName(bankStmt.getBankName())
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        matchedTransactionRepository.save(entity);
    }

    private void saveUnmatchedBank(BankStatement bankStmt, ReconciliationJob job) {
        UnmatchedBankStatement entity = UnmatchedBankStatement.builder()
                .job(job)
                .uniqueIdentifier(bankStmt.getUniqueIdentifier())
                .amount(bankStmt.getAmount())
                .date(bankStmt.getDate())
                .bankName(bankStmt.getBankName())
                .build();

        unmatchedBankStatementRepository.save(entity);
    }

    private void saveUnmatchedSystem(SystemTransaction sysTrx, ReconciliationJob job) {
        UnmatchedSystemTransaction entity = UnmatchedSystemTransaction.builder()
                .job(job)
                .trxId(sysTrx.getTrxID())
                .amount(sysTrx.getAmount())
                .type(sysTrx.getType().name())
                .transactionTime(sysTrx.getTransactionTime())
                .build();

        unmatchedSystemTransactionRepository.save(entity);
    }
}
