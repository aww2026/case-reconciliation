package com.andy.reconcile.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MatchedPair domain model.
 *
 * MatchedPair represents a successfully matched transaction between
 * the internal system and a bank statement.
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
class MatchedPairTest {

    @Test
    void shouldCreateMatchedPair_WithAllFields() {
        // ARRANGE
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-5000000"))
                .date(LocalDate.parse("2024-01-10"))
                .bankName("BCA")
                .build();

        // ACT
        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .bankStatement(bankStmt)
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        // ASSERT
        assertThat(pair.getSystemTransaction()).isEqualTo(systemTrx);
        assertThat(pair.getBankStatement()).isEqualTo(bankStmt);
        assertThat(pair.getDiscrepancy()).isEqualByComparingTo("0");
        assertThat(pair.getConfidence()).isEqualTo(100.0);
    }

    @Test
    void shouldCreateMatchedPair_WithExactMatch() {
        // Exact match: same amount, same date, no discrepancy, 100% confidence
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("110000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK002")
                .amount(new BigDecimal("110000"))  // Same amount
                .date(LocalDate.parse("2024-01-10"))
                .build();

        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .bankStatement(bankStmt)
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        assertThat(pair.getDiscrepancy()).isEqualByComparingTo("0");
        assertThat(pair.getConfidence()).isEqualTo(100.0);
    }

    @Test
    void shouldCreateMatchedPair_WithTolerance() {
        // Tolerance match: slight amount difference, lower confidence
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX003")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK003")
                .amount(new BigDecimal("-4999500"))  // Rp 500 difference (bank fee)
                .date(LocalDate.parse("2024-01-10"))
                .build();

        BigDecimal discrepancy = new BigDecimal("500");  // Absolute difference
        Double confidence = 95.5;  // High but not 100%

        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .bankStatement(bankStmt)
                .discrepancy(discrepancy)
                .confidence(confidence)
                .build();

        assertThat(pair.getDiscrepancy()).isEqualByComparingTo("500");
        assertThat(pair.getConfidence()).isEqualTo(95.5);
    }

    @Test
    void shouldHandleZeroDiscrepancy() {
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX004")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.now())
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK004")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.now())
                .build();

        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .bankStatement(bankStmt)
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        assertThat(pair.getDiscrepancy()).isEqualByComparingTo("0");
        assertThat(pair.isExactMatch()).isTrue();
    }

    @Test
    void shouldIdentifyExactMatch_WhenDiscrepancyIsZero() {
        MatchedPair exactMatch = MatchedPair.builder()
                .discrepancy(BigDecimal.ZERO)
                .confidence(100.0)
                .build();

        assertThat(exactMatch.isExactMatch()).isTrue();
    }

    @Test
    void shouldIdentifyToleranceMatch_WhenDiscrepancyExists() {
        MatchedPair toleranceMatch = MatchedPair.builder()
                .discrepancy(new BigDecimal("500"))
                .confidence(95.0)
                .build();

        assertThat(toleranceMatch.isExactMatch()).isFalse();
    }

    @Test
    void shouldHandleLargeDiscrepancy() {
        MatchedPair pair = MatchedPair.builder()
                .discrepancy(new BigDecimal("100000"))  // Rp 100,000 difference
                .confidence(75.0)
                .build();

        assertThat(pair.getDiscrepancy()).isEqualByComparingTo("100000");
        assertThat(pair.getConfidence()).isEqualTo(75.0);
        assertThat(pair.isExactMatch()).isFalse();
    }

    @Test
    void shouldCalculateDiscrepancy_FromTransactions() {
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX005")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK005")
                .amount(new BigDecimal("-4999000"))  // Rp 1,000 difference
                .date(LocalDate.parse("2024-01-10"))
                .build();

        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .bankStatement(bankStmt)
                .discrepancy(new BigDecimal("1000"))
                .confidence(92.0)
                .build();

        BigDecimal calculatedDiscrepancy = pair.calculateDiscrepancyFromTransactions();

        assertThat(calculatedDiscrepancy).isEqualByComparingTo("1000");
        assertThat(calculatedDiscrepancy).isEqualByComparingTo(pair.getDiscrepancy());
    }

    @Test
    void shouldGetSystemTrxID() {
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX999")
                .amount(new BigDecimal("1000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.now())
                .build();

        MatchedPair pair = MatchedPair.builder()
                .systemTransaction(systemTrx)
                .build();

        assertThat(pair.getSystemTrxID()).isEqualTo("TRX999");
    }

    @Test
    void shouldGetBankIdentifier() {
        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BCA2024011001")
                .amount(new BigDecimal("1000"))
                .date(LocalDate.now())
                .build();

        MatchedPair pair = MatchedPair.builder()
                .bankStatement(bankStmt)
                .build();

        assertThat(pair.getBankIdentifier()).isEqualTo("BCA2024011001");
    }
}
