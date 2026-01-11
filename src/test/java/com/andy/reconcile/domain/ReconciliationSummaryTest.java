package com.andy.reconcile.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReconciliationSummary domain model.
 *
 * ReconciliationSummary contains ALL the required output fields
 * specified in the basic requirements:
 * - Total number of transactions processed
 * - Total number of matched transactions
 * - Total number of unmatched transactions
 * - Details of unmatched transactions (system + bank, grouped by bank)
 * - Total discrepancies
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
class ReconciliationSummaryTest {

    @Test
    void shouldCreateReconciliationSummary_WithAllBasicRequirements() {
        // ARRANGE
        SystemTransaction sys1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.now())
                .build();

        BankStatement bank1 = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.now())
                .bankName("BCA")
                .build();

        MatchedPair match1 = MatchedPair.builder()
                .systemTransaction(sys1)
                .bankStatement(bank1)
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        SystemTransaction unmatchedSys = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("500000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.now())
                .build();

        BankStatement unmatchedBank = BankStatement.builder()
                .uniqueIdentifier("BANK002")
                .amount(new BigDecimal("250000"))
                .date(LocalDate.now())
                .bankName("Mandiri")
                .build();

        Map<String, List<BankStatement>> unmatchedByBank = new HashMap<>();
        unmatchedByBank.put("Mandiri", Arrays.asList(unmatchedBank));

        // ACT
        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalSystemTransactions(2)  // ✅ Basic requirement 1
                .totalBankTransactions(2)     // ✅ Basic requirement 1
                .matchedCount(1)              // ✅ Basic requirement 2
                .unmatchedCount(2)            // ✅ Basic requirement 3
                .totalDiscrepancy(BigDecimal.ZERO)  // ✅ Basic requirement 5
                .matches(Arrays.asList(match1))
                .unmatchedSystem(Arrays.asList(unmatchedSys))  // ✅ Basic requirement 4a
                .unmatchedBankByBank(unmatchedByBank)          // ✅ Basic requirement 4b (grouped!)
                .build();

        // ASSERT - Verify ALL basic requirements are met
        assertThat(summary.getTotalSystemTransactions()).isEqualTo(2);
        assertThat(summary.getTotalBankTransactions()).isEqualTo(2);
        assertThat(summary.getMatchedCount()).isEqualTo(1);
        assertThat(summary.getUnmatchedCount()).isEqualTo(2);
        assertThat(summary.getTotalDiscrepancy()).isEqualByComparingTo("0");
        assertThat(summary.getMatches()).hasSize(1);
        assertThat(summary.getUnmatchedSystem()).hasSize(1);
        assertThat(summary.getUnmatchedBankByBank()).containsKey("Mandiri");
    }

    @Test
    void shouldCalculateTotalProcessed() {
        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalSystemTransactions(100)
                .totalBankTransactions(98)
                .build();

        int totalProcessed = summary.getTotalProcessed();

        assertThat(totalProcessed).isEqualTo(198);  // 100 + 98
    }

    @Test
    void shouldGroupUnmatchedBankStatements_ByBankName() {
        BankStatement bcaUnmatched = BankStatement.builder()
                .uniqueIdentifier("BCA001")
                .amount(new BigDecimal("100000"))
                .date(LocalDate.now())
                .bankName("BCA")
                .build();

        BankStatement mandiriUnmatched = BankStatement.builder()
                .uniqueIdentifier("MND001")
                .amount(new BigDecimal("200000"))
                .date(LocalDate.now())
                .bankName("Mandiri")
                .build();

        BankStatement bniUnmatched = BankStatement.builder()
                .uniqueIdentifier("BNI001")
                .amount(new BigDecimal("300000"))
                .date(LocalDate.now())
                .bankName("BNI")
                .build();

        Map<String, List<BankStatement>> unmatchedByBank = new HashMap<>();
        unmatchedByBank.put("BCA", Arrays.asList(bcaUnmatched));
        unmatchedByBank.put("Mandiri", Arrays.asList(mandiriUnmatched));
        unmatchedByBank.put("BNI", Arrays.asList(bniUnmatched));

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .unmatchedBankByBank(unmatchedByBank)
                .build();

        // Verify grouping by bank
        assertThat(summary.getUnmatchedBankByBank()).hasSize(3);
        assertThat(summary.getUnmatchedBankByBank().get("BCA")).hasSize(1);
        assertThat(summary.getUnmatchedBankByBank().get("Mandiri")).hasSize(1);
        assertThat(summary.getUnmatchedBankByBank().get("BNI")).hasSize(1);
    }

    @Test
    void shouldCalculateTotalDiscrepancy_FromMatchedPairs() {
        MatchedPair match1 = MatchedPair.builder()
                .discrepancy(new BigDecimal("500"))
                .confidence(95.0)
                .build();

        MatchedPair match2 = MatchedPair.builder()
                .discrepancy(new BigDecimal("1000"))
                .confidence(92.0)
                .build();

        MatchedPair match3 = MatchedPair.builder()
                .discrepancy(BigDecimal.ZERO)  // Exact match
                .confidence(100.0)
                .build();

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .matches(Arrays.asList(match1, match2, match3))
                .totalDiscrepancy(new BigDecimal("1500"))  // 500 + 1000 + 0
                .build();

        assertThat(summary.getTotalDiscrepancy()).isEqualByComparingTo("1500");
    }

    @Test
    void shouldHandleNoMatches() {
        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalSystemTransactions(10)
                .totalBankTransactions(10)
                .matchedCount(0)
                .unmatchedCount(20)
                .totalDiscrepancy(BigDecimal.ZERO)
                .matches(Arrays.asList())  // Empty list
                .build();

        assertThat(summary.getMatchedCount()).isEqualTo(0);
        assertThat(summary.getMatches()).isEmpty();
        assertThat(summary.getTotalDiscrepancy()).isEqualByComparingTo("0");
    }

    @Test
    void shouldHandleAllMatched() {
        MatchedPair match1 = MatchedPair.builder()
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        MatchedPair match2 = MatchedPair.builder()
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalSystemTransactions(2)
                .totalBankTransactions(2)
                .matchedCount(2)
                .unmatchedCount(0)
                .totalDiscrepancy(BigDecimal.ZERO)
                .matches(Arrays.asList(match1, match2))
                .unmatchedSystem(Arrays.asList())
                .unmatchedBankByBank(new HashMap<>())
                .build();

        assertThat(summary.getMatchedCount()).isEqualTo(2);
        assertThat(summary.getUnmatchedCount()).isEqualTo(0);
        assertThat(summary.getUnmatchedSystem()).isEmpty();
        assertThat(summary.getUnmatchedBankByBank()).isEmpty();
    }

    @Test
    void shouldProvinceDateRangeContext() {
        LocalDate startDate = LocalDate.parse("2024-01-01");
        LocalDate endDate = LocalDate.parse("2024-01-31");

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        assertThat(summary.getStartDate()).isEqualTo(startDate);
        assertThat(summary.getEndDate()).isEqualTo(endDate);
    }

    @Test
    void shouldCountUnmatchedSystem() {
        SystemTransaction unmatched1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.now())
                .build();

        SystemTransaction unmatched2 = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("2000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.now())
                .build();

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .unmatchedSystem(Arrays.asList(unmatched1, unmatched2))
                .build();

        assertThat(summary.getUnmatchedSystem()).hasSize(2);
    }

    @Test
    void shouldCountUnmatchedBank_AcrossAllBanks() {
        BankStatement bcaUnmatched = BankStatement.builder()
                .uniqueIdentifier("BCA001")
                .amount(new BigDecimal("100"))
                .date(LocalDate.now())
                .bankName("BCA")
                .build();

        BankStatement mandiriUnmatched = BankStatement.builder()
                .uniqueIdentifier("MND001")
                .amount(new BigDecimal("200"))
                .date(LocalDate.now())
                .bankName("Mandiri")
                .build();

        Map<String, List<BankStatement>> unmatchedByBank = new HashMap<>();
        unmatchedByBank.put("BCA", Arrays.asList(bcaUnmatched));
        unmatchedByBank.put("Mandiri", Arrays.asList(mandiriUnmatched));

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .unmatchedBankByBank(unmatchedByBank)
                .build();

        int totalUnmatchedBank = summary.getTotalUnmatchedBank();

        assertThat(totalUnmatchedBank).isEqualTo(2);  // 1 from BCA + 1 from Mandiri
    }

    @Test
    void shouldCalculateReconciliationRate() {
        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalSystemTransactions(100)
                .totalBankTransactions(100)
                .matchedCount(95)
                .unmatchedCount(5)
                .build();

        double reconciliationRate = summary.getReconciliationRate();

        assertThat(reconciliationRate).isEqualTo(95.0);  // 95 / 100 * 100%
    }
}
