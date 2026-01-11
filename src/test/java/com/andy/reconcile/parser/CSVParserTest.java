package com.andy.reconcile.parser;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.domain.SystemTransaction;
import com.andy.reconcile.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CSV Parser - handles input file parsing for reconciliation.
 *
 * CSV Parser is critical for basic requirements:
 * - Parse system transaction CSV files
 * - Parse bank statement CSV files (multiple banks)
 * - Filter by date range (start date, end date)
 * - Handle file errors gracefully
 * - Validate required fields
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
class CSVParserTest {

    @TempDir
    Path tempDir;

    private CSVParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CSVParser();
    }

    @Test
    void shouldParseSystemTransactions_FromValidCSV() throws IOException {
        // ARRANGE - Create test CSV file
        String csvContent = """
                trxID,amount,type,transactionTime
                TRX001,5000000,DEBIT,2024-01-10T14:30:00
                TRX002,110000,CREDIT,2024-01-10T09:15:00
                TRX003,2500000,DEBIT,2024-01-11T16:45:00
                """;

        Path csvFile = tempDir.resolve("system_transactions.csv");
        Files.writeString(csvFile, csvContent);

        // ACT
        List<SystemTransaction> transactions = csvParser.parseSystemTransactions(
                csvFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(transactions).hasSize(3);
        assertThat(transactions.get(0).getTrxID()).isEqualTo("TRX001");
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo("5000000");
        assertThat(transactions.get(0).getType()).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void shouldParseBankStatements_FromValidCSV() throws IOException {
        // ARRANGE
        String csvContent = """
                uniqueIdentifier,amount,date,bankName
                BCA2024011001,-5000000,2024-01-10,BCA
                BCA2024011002,110000,2024-01-10,BCA
                BCA2024011003,-2500000,2024-01-11,BCA
                """;

        Path csvFile = tempDir.resolve("bca_statements.csv");
        Files.writeString(csvFile, csvContent);

        // ACT
        List<BankStatement> statements = csvParser.parseBankStatements(
                csvFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(statements).hasSize(3);
        assertThat(statements.get(0).getUniqueIdentifier()).isEqualTo("BCA2024011001");
        assertThat(statements.get(0).getAmount()).isEqualByComparingTo("-5000000");
        assertThat(statements.get(0).getBankName()).isEqualTo("BCA");
    }

    @Test
    void shouldFilterByDateRange_SystemTransactions() throws IOException {
        // ARRANGE
        String csvContent = """
                trxID,amount,type,transactionTime
                TRX001,1000000,DEBIT,2024-01-05T10:00:00
                TRX002,2000000,DEBIT,2024-01-15T10:00:00
                TRX003,3000000,DEBIT,2024-01-25T10:00:00
                """;

        Path csvFile = tempDir.resolve("system_transactions.csv");
        Files.writeString(csvFile, csvContent);

        // ACT - Filter only Jan 10-20
        List<SystemTransaction> transactions = csvParser.parseSystemTransactions(
                csvFile.toString(),
                LocalDate.parse("2024-01-10"),
                LocalDate.parse("2024-01-20")
        );

        // ASSERT - Only TRX002 should be included
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getTrxID()).isEqualTo("TRX002");
    }

    @Test
    void shouldFilterByDateRange_BankStatements() throws IOException {
        // ARRANGE
        String csvContent = """
                uniqueIdentifier,amount,date,bankName
                BANK001,-1000000,2024-01-05,BCA
                BANK002,-2000000,2024-01-15,BCA
                BANK003,-3000000,2024-01-25,BCA
                """;

        Path csvFile = tempDir.resolve("bank_statements.csv");
        Files.writeString(csvFile, csvContent);

        // ACT - Filter only Jan 10-20
        List<BankStatement> statements = csvParser.parseBankStatements(
                csvFile.toString(),
                LocalDate.parse("2024-01-10"),
                LocalDate.parse("2024-01-20")
        );

        // ASSERT - Only BANK002 should be included
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getUniqueIdentifier()).isEqualTo("BANK002");
    }

    @Test
    void shouldThrowException_WhenFileNotFound() {
        // ACT & ASSERT
        assertThatThrownBy(() ->
                csvParser.parseSystemTransactions(
                        "/nonexistent/file.csv",
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-01-31")
                )
        ).isInstanceOf(IOException.class)
         .hasMessageContaining("file");
    }

    @Test
    void shouldThrowException_WhenInvalidCSVFormat() throws IOException {
        // ARRANGE - CSV with wrong columns
        String csvContent = """
                wrong,headers,here
                value1,value2,value3
                """;

        Path csvFile = tempDir.resolve("invalid.csv");
        Files.writeString(csvFile, csvContent);

        // ACT & ASSERT
        assertThatThrownBy(() ->
                csvParser.parseSystemTransactions(
                        csvFile.toString(),
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-01-31")
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("header");
    }

    @Test
    void shouldSkipInvalidRows_AndContinueParsing() throws IOException {
        // ARRANGE - Mix of valid and invalid rows
        String csvContent = """
                trxID,amount,type,transactionTime
                TRX001,5000000,DEBIT,2024-01-10T14:30:00
                TRX002,INVALID_AMOUNT,DEBIT,2024-01-10T09:15:00
                TRX003,2500000,DEBIT,2024-01-11T16:45:00
                """;

        Path csvFile = tempDir.resolve("system_transactions.csv");
        Files.writeString(csvFile, csvContent);

        // ACT
        List<SystemTransaction> transactions = csvParser.parseSystemTransactions(
                csvFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - Should have 2 valid rows (skipped the invalid one)
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getTrxID()).isEqualTo("TRX001");
        assertThat(transactions.get(1).getTrxID()).isEqualTo("TRX003");
    }

    @Test
    void shouldHandleEmptyCSVFile() throws IOException {
        // ARRANGE - CSV with only header
        String csvContent = """
                trxID,amount,type,transactionTime
                """;

        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, csvContent);

        // ACT
        List<SystemTransaction> transactions = csvParser.parseSystemTransactions(
                csvFile.toString(),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(transactions).isEmpty();
    }
}
