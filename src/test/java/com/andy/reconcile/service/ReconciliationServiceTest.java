package com.andy.reconcile.service;

import com.andy.reconcile.domain.*;
import com.andy.reconcile.matcher.ExactMatcher;
import com.andy.reconcile.matcher.MatchResult;
import com.andy.reconcile.parser.CSVParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReconciliationService - THE CRITICAL SERVICE LAYER.
 *
 * ReconciliationService orchestrates the complete reconciliation flow:
 * 1. Parse system transactions (CSV)
 * 2. Parse bank statements (CSV, multiple files)
 * 3. Match transactions (ExactMatcher)
 * 4. Build summary with ALL 5 basic requirements
 *
 * These tests verify ALL BASIC REQUIREMENTS:
 * ✅ Requirement 1: Total transactions processed
 * ✅ Requirement 2: Total matched
 * ✅ Requirement 3: Total unmatched
 * ✅ Requirement 4: Unmatched details (system + bank, grouped by bank)
 * ✅ Requirement 5: Total discrepancies
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private CSVParser csvParser;

    @Mock
    private ExactMatcher exactMatcher;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(csvParser, exactMatcher);
    }

    @Test
    void shouldReconcile_WithAllMatched() throws IOException {
        // ARRANGE
        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        List<SystemTransaction> systemTrx = Arrays.asList(sys1);

        BankStatement bank1 = createBankStatement("BANK001", "-1000000", "BCA");
        List<BankStatement> bankStmt = Arrays.asList(bank1);

        MatchedPair match1 = createMatchedPair(sys1, bank1, "0");
        MatchResult matchResult = MatchResult.builder()
                .matches(Arrays.asList(match1))
                .unmatchedSystem(Arrays.asList())
                .unmatchedBank(Arrays.asList())
                .build();

        when(csvParser.parseSystemTransactions(eq("system.csv"), any(), any())).thenReturn(systemTrx);
        when(csvParser.parseBankStatements(eq("bank.csv"), any(), any())).thenReturn(bankStmt);
        when(exactMatcher.match(systemTrx, bankStmt)).thenReturn(matchResult);

        // ACT
        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bank.csv"),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - ALL BASIC REQUIREMENTS
        assertThat(summary.getTotalSystemTransactions()).isEqualTo(1);  // ✅ Requirement 1
        assertThat(summary.getTotalBankTransactions()).isEqualTo(1);    // ✅ Requirement 1
        assertThat(summary.getMatchedCount()).isEqualTo(1);             // ✅ Requirement 2
        assertThat(summary.getUnmatchedCount()).isEqualTo(0);           // ✅ Requirement 3
        assertThat(summary.getUnmatchedSystem()).isEmpty();             // ✅ Requirement 4a
        assertThat(summary.getUnmatchedBankByBank()).isEmpty();         // ✅ Requirement 4b
        assertThat(summary.getTotalDiscrepancy()).isEqualByComparingTo("0");  // ✅ Requirement 5
    }

    @Test
    void shouldReconcile_WithUnmatchedTransactions() throws IOException {
        // ARRANGE
        SystemTransaction matchedSys = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        SystemTransaction unmatchedSys = createSystemTransaction("TRX002", "500000", TransactionType.CREDIT);
        List<SystemTransaction> systemTrx = Arrays.asList(matchedSys, unmatchedSys);

        BankStatement matchedBank = createBankStatement("BANK001", "-1000000", "BCA");
        BankStatement unmatchedBank = createBankStatement("BANK002", "250000", "Mandiri");
        List<BankStatement> bankStmt = Arrays.asList(matchedBank, unmatchedBank);

        MatchedPair match1 = createMatchedPair(matchedSys, matchedBank, "0");
        MatchResult matchResult = MatchResult.builder()
                .matches(Arrays.asList(match1))
                .unmatchedSystem(Arrays.asList(unmatchedSys))
                .unmatchedBank(Arrays.asList(unmatchedBank))
                .build();

        when(csvParser.parseSystemTransactions(eq("system.csv"), any(), any())).thenReturn(systemTrx);
        when(csvParser.parseBankStatements(eq("bank.csv"), any(), any())).thenReturn(bankStmt);
        when(exactMatcher.match(systemTrx, bankStmt)).thenReturn(matchResult);

        // ACT
        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bank.csv"),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(summary.getTotalSystemTransactions()).isEqualTo(2);
        assertThat(summary.getTotalBankTransactions()).isEqualTo(2);
        assertThat(summary.getMatchedCount()).isEqualTo(1);
        assertThat(summary.getUnmatchedCount()).isEqualTo(2);  // 1 system + 1 bank
        assertThat(summary.getUnmatchedSystem()).hasSize(1);
        assertThat(summary.getUnmatchedBankByBank()).hasSize(1);
        assertThat(summary.getUnmatchedBankByBank().get("Mandiri")).hasSize(1);
    }

    @Test
    void shouldReconcile_WithMultipleBankFiles() throws IOException {
        // ARRANGE
        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        SystemTransaction sys2 = createSystemTransaction("TRX002", "2000000", TransactionType.DEBIT);
        List<SystemTransaction> systemTrx = Arrays.asList(sys1, sys2);

        BankStatement bcaStmt = createBankStatement("BCA001", "-1000000", "BCA");
        BankStatement mandiriStmt = createBankStatement("MND001", "-2000000", "Mandiri");

        when(csvParser.parseSystemTransactions(eq("system.csv"), any(), any())).thenReturn(systemTrx);
        when(csvParser.parseBankStatements(eq("bca.csv"), any(), any())).thenReturn(Arrays.asList(bcaStmt));
        when(csvParser.parseBankStatements(eq("mandiri.csv"), any(), any())).thenReturn(Arrays.asList(mandiriStmt));

        List<BankStatement> allBankStmt = Arrays.asList(bcaStmt, mandiriStmt);
        MatchResult matchResult = MatchResult.builder()
                .matches(Arrays.asList(
                        createMatchedPair(sys1, bcaStmt, "0"),
                        createMatchedPair(sys2, mandiriStmt, "0")
                ))
                .unmatchedSystem(Arrays.asList())
                .unmatchedBank(Arrays.asList())
                .build();

        when(exactMatcher.match(systemTrx, allBankStmt)).thenReturn(matchResult);

        // ACT
        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bca.csv", "mandiri.csv"),  // Multiple bank files!
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT
        assertThat(summary.getTotalBankTransactions()).isEqualTo(2);
        assertThat(summary.getMatchedCount()).isEqualTo(2);
    }

    @Test
    void shouldGroupUnmatchedBank_ByBankName() throws IOException {
        // ARRANGE
        BankStatement bcaUnmatched1 = createBankStatement("BCA001", "100000", "BCA");
        BankStatement bcaUnmatched2 = createBankStatement("BCA002", "200000", "BCA");
        BankStatement mandiriUnmatched = createBankStatement("MND001", "300000", "Mandiri");

        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(Arrays.asList());
        when(csvParser.parseBankStatements(any(), any(), any()))
                .thenReturn(Arrays.asList(bcaUnmatched1, bcaUnmatched2, mandiriUnmatched));

        MatchResult matchResult = MatchResult.builder()
                .matches(Arrays.asList())
                .unmatchedSystem(Arrays.asList())
                .unmatchedBank(Arrays.asList(bcaUnmatched1, bcaUnmatched2, mandiriUnmatched))
                .build();

        when(exactMatcher.match(any(), any())).thenReturn(matchResult);

        // ACT
        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bank.csv"),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - ✅ Requirement 4b: Grouped by bank!
        assertThat(summary.getUnmatchedBankByBank()).hasSize(2);
        assertThat(summary.getUnmatchedBankByBank().get("BCA")).hasSize(2);
        assertThat(summary.getUnmatchedBankByBank().get("Mandiri")).hasSize(1);
    }

    @Test
    void shouldCalculateTotalDiscrepancy() throws IOException {
        // ARRANGE
        SystemTransaction sys1 = createSystemTransaction("TRX001", "1000000", TransactionType.DEBIT);
        SystemTransaction sys2 = createSystemTransaction("TRX002", "2000000", TransactionType.DEBIT);

        BankStatement bank1 = createBankStatement("BANK001", "-999500", "BCA");  // 500 difference
        BankStatement bank2 = createBankStatement("BANK002", "-1999000", "BCA");  // 1000 difference

        MatchResult matchResult = MatchResult.builder()
                .matches(Arrays.asList(
                        createMatchedPair(sys1, bank1, "500"),   // Discrepancy
                        createMatchedPair(sys2, bank2, "1000")   // Discrepancy
                ))
                .unmatchedSystem(Arrays.asList())
                .unmatchedBank(Arrays.asList())
                .build();

        when(csvParser.parseSystemTransactions(any(), any(), any()))
                .thenReturn(Arrays.asList(sys1, sys2));
        when(csvParser.parseBankStatements(any(), any(), any()))
                .thenReturn(Arrays.asList(bank1, bank2));
        when(exactMatcher.match(any(), any())).thenReturn(matchResult);

        // ACT
        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bank.csv"),
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31")
        );

        // ASSERT - ✅ Requirement 5: Total discrepancies
        assertThat(summary.getTotalDiscrepancy()).isEqualByComparingTo("1500");  // 500 + 1000
    }

    @Test
    void shouldSetDateRangeInSummary() throws IOException {
        // ARRANGE
        when(csvParser.parseSystemTransactions(any(), any(), any())).thenReturn(Arrays.asList());
        when(csvParser.parseBankStatements(any(), any(), any())).thenReturn(Arrays.asList());
        when(exactMatcher.match(any(), any())).thenReturn(MatchResult.builder()
                .matches(Arrays.asList())
                .unmatchedSystem(Arrays.asList())
                .unmatchedBank(Arrays.asList())
                .build());

        // ACT
        LocalDate start = LocalDate.parse("2024-01-01");
        LocalDate end = LocalDate.parse("2024-01-31");

        ReconciliationSummary summary = reconciliationService.reconcile(
                "system.csv",
                Arrays.asList("bank.csv"),
                start,
                end
        );

        // ASSERT
        assertThat(summary.getStartDate()).isEqualTo(start);
        assertThat(summary.getEndDate()).isEqualTo(end);
    }

    // Helper methods

    private SystemTransaction createSystemTransaction(String trxID, String amount, TransactionType type) {
        return SystemTransaction.builder()
                .trxID(trxID)
                .amount(new BigDecimal(amount))
                .type(type)
                .transactionTime(LocalDateTime.now())
                .build();
    }

    private BankStatement createBankStatement(String id, String amount, String bankName) {
        return BankStatement.builder()
                .uniqueIdentifier(id)
                .amount(new BigDecimal(amount))
                .date(LocalDate.now())
                .bankName(bankName)
                .build();
    }

    private MatchedPair createMatchedPair(SystemTransaction sys, BankStatement bank, String discrepancy) {
        return MatchedPair.builder()
                .systemTransaction(sys)
                .bankStatement(bank)
                .discrepancy(new BigDecimal(discrepancy))
                .confidence(discrepancy.equals("0") ? 100.0 : 95.0)
                .build();
    }
}
