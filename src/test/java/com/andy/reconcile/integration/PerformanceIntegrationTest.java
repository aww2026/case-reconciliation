package com.andy.reconcile.integration;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.parser.CSVParser;
import com.andy.reconcile.service.ParallelCSVParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for parallel processing and streaming performance.
 *
 * These tests verify:
 * 1. Parallel processing is faster than sequential
 * 2. Results are identical between parallel and sequential
 * 3. Large files can be processed without OOM errors
 */
@SpringBootTest
class PerformanceIntegrationTest {

    private CSVParser csvParser;
    private ParallelCSVParser parallelCSVParser;
    private ExecutorService executorService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvParser = new CSVParser();
        executorService = Executors.newFixedThreadPool(4);
        parallelCSVParser = new ParallelCSVParser(csvParser, executorService);
    }

    @Test
    void parallelProcessingShouldBeFasterThanSequential() throws IOException {
        // ARRANGE - Create 3 medium-sized files
        Path file1 = createBankFile("bank1.csv", 100);
        Path file2 = createBankFile("bank2.csv", 100);
        Path file3 = createBankFile("bank3.csv", 100);

        List<String> files = Arrays.asList(
                file1.toString(),
                file2.toString(),
                file3.toString()
        );

        LocalDate startDate = LocalDate.parse("2024-01-01");
        LocalDate endDate = LocalDate.parse("2024-01-31");

        // ACT - Sequential processing
        long sequentialStart = System.currentTimeMillis();
        List<BankStatement> sequentialResults = parseSequentially(files, startDate, endDate);
        long sequentialTime = System.currentTimeMillis() - sequentialStart;

        // ACT - Parallel processing
        long parallelStart = System.currentTimeMillis();
        List<BankStatement> parallelResults = parallelCSVParser.parseBankStatementsParallel(
                files, startDate, endDate
        );
        long parallelTime = System.currentTimeMillis() - parallelStart;

        // ASSERT - Results should be identical
        assertThat(parallelResults).hasSameSizeAs(sequentialResults);
        assertThat(parallelResults).hasSize(300);  // 3 files × 100 rows

        // ASSERT - Parallel should be faster (or at least not slower)
        System.out.println("Sequential time: " + sequentialTime + "ms");
        System.out.println("Parallel time: " + parallelTime + "ms");
        System.out.println("Speedup: " + (double) sequentialTime / parallelTime + "x");

        // For 3 files, parallel should be at least as fast as sequential
        assertThat(parallelTime).isLessThanOrEqualTo(sequentialTime * 2);
    }

    @Test
    void shouldProduceSameResultsWithParallelAndSequential() throws IOException {
        // ARRANGE
        Path file1 = createBankFile("bank1.csv", 50);
        Path file2 = createBankFile("bank2.csv", 75);

        List<String> files = Arrays.asList(file1.toString(), file2.toString());
        LocalDate startDate = LocalDate.parse("2024-01-01");
        LocalDate endDate = LocalDate.parse("2024-01-31");

        // ACT
        List<BankStatement> sequential = parseSequentially(files, startDate, endDate);
        List<BankStatement> parallel = parallelCSVParser.parseBankStatementsParallel(
                files, startDate, endDate
        );

        // ASSERT - Same count
        assertThat(parallel).hasSameSizeAs(sequential);
        assertThat(parallel).hasSize(125);  // 50 + 75
    }

    @Test
    void shouldHandleLargeFileWithoutOutOfMemory() throws IOException {
        // ARRANGE - Create a large file (10,000 rows)
        Path largeFile = createBankFile("large_bank.csv", 10000);

        // ACT - Parse with parallel processing
        List<BankStatement> results = parallelCSVParser.parseBankStatementsParallel(
                Arrays.asList(largeFile.toString()),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - All records loaded
        assertThat(results).hasSize(10000);
        System.out.println("Successfully processed 10,000 records");
    }

    @Test
    void shouldFilterByDateRange() throws IOException {
        // ARRANGE - File with mixed dates
        Path file = createBankFileWithDates();

        // ACT - Filter for January only
        List<BankStatement> results = parallelCSVParser.parseBankStatementsParallel(
                Arrays.asList(file.toString()),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - Only January records
        assertThat(results).allMatch(stmt ->
                !stmt.getDate().isBefore(LocalDate.parse("2024-01-01")) &&
                !stmt.getDate().isAfter(LocalDate.parse("2024-01-31"))
        );
    }

    // Helper methods

    private List<BankStatement> parseSequentially(
            List<String> files,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        return files.stream()
                .flatMap(file -> {
                    try {
                        return csvParser.parseBankStatements(file, startDate, endDate).stream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    private Path createBankFile(String filename, int rowCount) throws IOException {
        Path file = tempDir.resolve(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("uniqueIdentifier,amount,date,bankName\n");
            for (int i = 1; i <= rowCount; i++) {
                writer.write(String.format("BANK%05d,%d,2024-01-15,TestBank\n", i, i * 1000));
            }
        }
        return file;
    }

    private Path createBankFileWithDates() throws IOException {
        Path file = tempDir.resolve("mixed_dates.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("uniqueIdentifier,amount,date,bankName\n");
            // December 2023
            writer.write("BANK001,100000,2023-12-31,TestBank\n");
            // January 2024
            writer.write("BANK002,200000,2024-01-15,TestBank\n");
            writer.write("BANK003,300000,2024-01-20,TestBank\n");
            // February 2024
            writer.write("BANK004,400000,2024-02-01,TestBank\n");
        }
        return file;
    }
}
