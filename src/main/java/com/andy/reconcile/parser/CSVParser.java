package com.andy.reconcile.parser;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.domain.SystemTransaction;
import com.andy.reconcile.domain.TransactionType;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CSV Parser for reconciliation input files.
 *
 * Handles parsing of:
 * - System transaction CSV files (from internal system)
 * - Bank statement CSV files (from multiple banks)
 *
 * Features:
 * - Date range filtering (basic requirement)
 * - Error handling (file not found, invalid format)
 * - Validation (required fields)
 * - Skips invalid rows with logging
 */
@Component
public class CSVParser {

    /**
     * Parses system transactions from a CSV file.
     *
     * Expected CSV format:
     * trxID,amount,type,transactionTime
     * TRX001,5000000,DEBIT,2024-01-10T14:30:00
     *
     * @param filePath Path to the system transaction CSV file
     * @param startDate Start date for filtering (inclusive)
     * @param endDate End date for filtering (inclusive)
     * @return List of SystemTransaction objects within the date range
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if CSV format is invalid
     */
    public List<SystemTransaction> parseSystemTransactions(
            String filePath,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        List<SystemTransaction> transactions = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Validate header
            String[] header = rows.get(0);
            validateSystemTransactionHeader(header);

            // Parse data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                try {
                    SystemTransaction trx = parseSystemTransactionRow(row);

                    // Filter by date range
                    LocalDate trxDate = trx.getTransactionTime().toLocalDate();
                    if (isWithinDateRange(trxDate, startDate, endDate)) {
                        transactions.add(trx);
                    }
                } catch (Exception e) {
                    // Skip invalid rows, log warning
                    System.err.println("Warning: Skipping invalid row " + (i + 1) +
                            ": " + Arrays.toString(row) + " - " + e.getMessage());
                }
            }
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        return transactions;
    }

    /**
     * Parses bank statements from a CSV file.
     *
     * Expected CSV format:
     * uniqueIdentifier,amount,date,bankName
     * BCA2024011001,-5000000,2024-01-10,BCA
     *
     * @param filePath Path to the bank statement CSV file
     * @param startDate Start date for filtering (inclusive)
     * @param endDate End date for filtering (inclusive)
     * @return List of BankStatement objects within the date range
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if CSV format is invalid
     */
    public List<BankStatement> parseBankStatements(
            String filePath,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        List<BankStatement> statements = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Validate header
            String[] header = rows.get(0);
            validateBankStatementHeader(header);

            // Parse data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                try {
                    BankStatement stmt = parseBankStatementRow(row);

                    // Filter by date range
                    if (isWithinDateRange(stmt.getDate(), startDate, endDate)) {
                        statements.add(stmt);
                    }
                } catch (Exception e) {
                    // Skip invalid rows, log warning
                    System.err.println("Warning: Skipping invalid row " + (i + 1) +
                            ": " + Arrays.toString(row) + " - " + e.getMessage());
                }
            }
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        return statements;
    }

    /**
     * Validates the header of a system transaction CSV file.
     */
    private void validateSystemTransactionHeader(String[] header) {
        List<String> expectedHeaders = Arrays.asList("trxID", "amount", "type", "transactionTime");
        List<String> actualHeaders = Arrays.asList(header);

        if (!actualHeaders.equals(expectedHeaders)) {
            throw new IllegalArgumentException(
                    "Invalid CSV header. Expected: " + expectedHeaders +
                    ", but got: " + actualHeaders
            );
        }
    }

    /**
     * Validates the header of a bank statement CSV file.
     */
    private void validateBankStatementHeader(String[] header) {
        List<String> expectedHeaders = Arrays.asList("uniqueIdentifier", "amount", "date", "bankName");
        List<String> actualHeaders = Arrays.asList(header);

        if (!actualHeaders.equals(expectedHeaders)) {
            throw new IllegalArgumentException(
                    "Invalid CSV header. Expected: " + expectedHeaders +
                    ", but got: " + actualHeaders
            );
        }
    }

    /**
     * Parses a single row into a SystemTransaction object.
     */
    private SystemTransaction parseSystemTransactionRow(String[] row) {
        if (row.length < 4) {
            throw new IllegalArgumentException("Row must have 4 columns");
        }

        return SystemTransaction.builder()
                .trxID(row[0].trim())
                .amount(new BigDecimal(row[1].trim()))
                .type(TransactionType.valueOf(row[2].trim().toUpperCase()))
                .transactionTime(LocalDateTime.parse(row[3].trim()))
                .build();
    }

    /**
     * Parses a single row into a BankStatement object.
     */
    private BankStatement parseBankStatementRow(String[] row) {
        if (row.length < 4) {
            throw new IllegalArgumentException("Row must have 4 columns");
        }

        return BankStatement.builder()
                .uniqueIdentifier(row[0].trim())
                .amount(new BigDecimal(row[1].trim()))
                .date(LocalDate.parse(row[2].trim()))
                .bankName(row[3].trim())
                .build();
    }

    /**
     * Checks if a date is within the specified date range (inclusive).
     */
    private boolean isWithinDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
