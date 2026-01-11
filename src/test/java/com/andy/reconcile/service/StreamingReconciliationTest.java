package com.andy.reconcile.service;

import com.andy.reconcile.domain.*;
import com.andy.reconcile.entity.ReconciliationJob;
import com.andy.reconcile.matcher.ExactMatcher;
import com.andy.reconcile.parser.CSVParser;
import com.andy.reconcile.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 🔴 RED Phase: Tests for streaming reconciliation.
 *
 * Goal: Handle large files (>100MB) without loading everything into memory.
 *
 * Strategy:
 * - Build HashMap from system transactions (smaller dataset)
 * - Stream bank statements line-by-line
 * - Match and save to database immediately
 * - Don't accumulate results in memory
 *
 * Memory efficiency:
 * - Before: O(n + m) - load all system + all bank
 * - After: O(n) - only load system transactions
 */
@ExtendWith(MockitoExtension.class)
class StreamingReconciliationTest {

    @Mock
    private CSVParser csvParser;

    @Mock
    private ExactMatcher exactMatcher;

    @Mock
    private MatchedTransactionRepository matchedTransactionRepository;

    @Mock
    private UnmatchedSystemTransactionRepository unmatchedSystemTransactionRepository;

    @Mock
    private UnmatchedBankStatementRepository unmatchedBankStatementRepository;

    private StreamingReconciliationService streamingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        streamingService = new StreamingReconciliationService(
                csvParser,
                exactMatcher,
                matchedTransactionRepository,
                unmatchedSystemTransactionRepository,
                unmatchedBankStatementRepository
        );
    }

    @Test
    void shouldProcessLargeFileWithStreaming() throws IOException {
        // ARRANGE - Create a large CSV file with at least one matching record
        Path largeBankFile = createLargeBankStatementFileWithMatch();

        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        List<SystemTransaction> systemTrx = Arrays.asList(sys1);

        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(systemTrx);

        ReconciliationJob job = ReconciliationJob.builder()
                .id(1L)
                .build();

        // ACT
        streamingService.reconcileWithStreaming(
                "system.csv",
                largeBankFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31"),
                job
        );

        // ASSERT - Verify database writes happened (streaming saves incrementally)
        verify(matchedTransactionRepository, atLeastOnce()).save(any());
    }

    @Test
    void shouldMatchTransactionsWhileStreaming() throws IOException {
        // ARRANGE
        Path bankFile = createSmallBankStatementFile();

        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        List<SystemTransaction> systemTrx = Arrays.asList(sys1);

        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(systemTrx);

        ReconciliationJob job = ReconciliationJob.builder()
                .id(1L)
                .build();

        // ACT
        streamingService.reconcileWithStreaming(
                "system.csv",
                bankFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31"),
                job
        );

        // ASSERT - Matched transaction should be saved
        verify(matchedTransactionRepository, times(1)).save(any());
    }

    @Test
    void shouldSaveUnmatchedBankStatements() throws IOException {
        // ARRANGE
        Path bankFile = createSmallBankStatementFile();

        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(new ArrayList<>());

        ReconciliationJob job = ReconciliationJob.builder()
                .id(1L)
                .build();

        // ACT
        streamingService.reconcileWithStreaming(
                "system.csv",
                bankFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31"),
                job
        );

        // ASSERT - Unmatched bank statements should be saved
        verify(unmatchedBankStatementRepository, times(1)).save(any());
    }

    @Test
    void shouldSaveUnmatchedSystemTransactions() throws IOException {
        // ARRANGE
        Path bankFile = tempDir.resolve("empty_bank.csv");
        Files.writeString(bankFile, "uniqueIdentifier,amount,date,bankName\n");

        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        List<SystemTransaction> systemTrx = Arrays.asList(sys1);

        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(systemTrx);

        ReconciliationJob job = ReconciliationJob.builder()
                .id(1L)
                .build();

        // ACT
        streamingService.reconcileWithStreaming(
                "system.csv",
                bankFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31"),
                job
        );

        // ASSERT - Unmatched system transaction should be saved
        verify(unmatchedSystemTransactionRepository, times(1)).save(any());
    }

    // Helper methods

    private Path createLargeBankStatementFileWithMatch() throws IOException {
        Path file = tempDir.resolve("large_bank.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("uniqueIdentifier,amount,date,bankName\n");
            // Add one matching record (matches TRX001 which is DEBIT of 1000000 = -1000000)
            writer.write("BANK001,-1000000,2024-01-10,TestBank\n");
            // Add more records
            for (int i = 2; i <= 1000; i++) {
                writer.write(String.format("BANK%03d,%d,2024-01-10,TestBank\n", i, i * 1000));
            }
        }
        return file;
    }

    private Path createSmallBankStatementFile() throws IOException {
        Path file = tempDir.resolve("bank.csv");
        Files.writeString(file,
                "uniqueIdentifier,amount,date,bankName\n" +
                "BANK001,-1000000,2024-01-10,BCA\n");
        return file;
    }

    private SystemTransaction createSystemTransaction(String trxID, String amount, TransactionType type) {
        return SystemTransaction.builder()
                .trxID(trxID)
                .amount(new BigDecimal(amount))
                .type(type)
                .transactionTime(LocalDateTime.of(2024, 1, 10, 10, 0))
                .build();
    }
}
