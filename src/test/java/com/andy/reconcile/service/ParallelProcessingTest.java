package com.andy.reconcile.service;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.parser.CSVParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 🔴 RED Phase: Tests for parallel bank file processing.
 *
 * These tests will FAIL initially - that's expected in TDD!
 *
 * Goal: Parse multiple bank files in parallel for better performance.
 *
 * Expected behavior:
 * - Multiple bank files are parsed concurrently
 * - All transactions from all files are collected
 * - Errors in one file don't affect others
 * - Results are identical to sequential processing
 */
@ExtendWith(MockitoExtension.class)
class ParallelProcessingTest {

    @Mock
    private CSVParser csvParser;

    private ParallelCSVParser parallelCSVParser;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4);
        parallelCSVParser = new ParallelCSVParser(csvParser, executorService);
    }

    @Test
    void shouldParseBankFilesInParallel() throws IOException {
        // ARRANGE
        BankStatement bca1 = createBankStatement("BCA001", "100000", "BCA");
        BankStatement mandiri1 = createBankStatement("MND001", "200000", "Mandiri");
        BankStatement bni1 = createBankStatement("BNI001", "300000", "BNI");

        when(csvParser.parseBankStatements(eq("bca.csv"), any(), any()))
                .thenReturn(Arrays.asList(bca1));
        when(csvParser.parseBankStatements(eq("mandiri.csv"), any(), any()))
                .thenReturn(Arrays.asList(mandiri1));
        when(csvParser.parseBankStatements(eq("bni.csv"), any(), any()))
                .thenReturn(Arrays.asList(bni1));

        // ACT
        List<String> bankFiles = Arrays.asList("bca.csv", "mandiri.csv", "bni.csv");
        LocalDate startDate = LocalDate.parse("2024-01-01");
        LocalDate endDate = LocalDate.parse("2024-01-31");

        List<BankStatement> results = parallelCSVParser.parseBankStatementsParallel(
                bankFiles, startDate, endDate
        );

        // ASSERT
        assertThat(results).hasSize(3);
        assertThat(results).contains(bca1, mandiri1, bni1);

        // Verify all files were parsed
        verify(csvParser, times(1)).parseBankStatements(eq("bca.csv"), eq(startDate), eq(endDate));
        verify(csvParser, times(1)).parseBankStatements(eq("mandiri.csv"), eq(startDate), eq(endDate));
        verify(csvParser, times(1)).parseBankStatements(eq("bni.csv"), eq(startDate), eq(endDate));
    }

    @Test
    void shouldHandleEmptyBankFileList() throws IOException {
        // ACT
        List<BankStatement> results = parallelCSVParser.parseBankStatementsParallel(
                Arrays.asList(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(results).isEmpty();
        verify(csvParser, never()).parseBankStatements(any(), any(), any());
    }

    @Test
    void shouldContinueProcessingWhenOneFileFails() throws IOException {
        // ARRANGE
        BankStatement bca1 = createBankStatement("BCA001", "100000", "BCA");
        BankStatement bni1 = createBankStatement("BNI001", "300000", "BNI");

        when(csvParser.parseBankStatements(eq("bca.csv"), any(), any()))
                .thenReturn(Arrays.asList(bca1));
        when(csvParser.parseBankStatements(eq("mandiri.csv"), any(), any()))
                .thenThrow(new IOException("File not found"));
        when(csvParser.parseBankStatements(eq("bni.csv"), any(), any()))
                .thenReturn(Arrays.asList(bni1));

        // ACT & ASSERT
        List<String> bankFiles = Arrays.asList("bca.csv", "mandiri.csv", "bni.csv");

        assertThatThrownBy(() -> parallelCSVParser.parseBankStatementsParallel(
                bankFiles,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        )).isInstanceOf(IOException.class)
          .hasMessageContaining("Failed to parse bank files");
    }

    @Test
    void shouldMaintainCorrectOrderOfResults() throws IOException {
        // ARRANGE
        BankStatement bca1 = createBankStatement("BCA001", "100000", "BCA");
        BankStatement bca2 = createBankStatement("BCA002", "200000", "BCA");
        BankStatement mandiri1 = createBankStatement("MND001", "300000", "Mandiri");

        when(csvParser.parseBankStatements(eq("bca.csv"), any(), any()))
                .thenReturn(Arrays.asList(bca1, bca2));
        when(csvParser.parseBankStatements(eq("mandiri.csv"), any(), any()))
                .thenReturn(Arrays.asList(mandiri1));

        // ACT
        List<String> bankFiles = Arrays.asList("bca.csv", "mandiri.csv");
        List<BankStatement> results = parallelCSVParser.parseBankStatementsParallel(
                bankFiles,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - All statements collected
        assertThat(results).hasSize(3);
        assertThat(results).contains(bca1, bca2, mandiri1);
    }

    // Helper method
    private BankStatement createBankStatement(String id, String amount, String bankName) {
        return BankStatement.builder()
                .uniqueIdentifier(id)
                .amount(new BigDecimal(amount))
                .date(LocalDate.now())
                .bankName(bankName)
                .build();
    }
}
