package com.andy.reconcile.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for SystemTransaction domain model
 *
 * Following RED-GREEN-REFACTOR cycle:
 * 1. RED: Write test (it will fail because class doesn't exist)
 * 2. GREEN: Write minimal code to pass
 * 3. REFACTOR: Improve code quality
 */
class SystemTransactionTest {

    @Test
    void shouldCreateSystemTransaction_WithAllFields() {
        // Arrange & Act
        SystemTransaction transaction = SystemTransaction.builder()
                .trxID("TRX20240110001")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        // Assert
        assertThat(transaction.getTrxID()).isEqualTo("TRX20240110001");
        assertThat(transaction.getAmount()).isEqualByComparingTo("5000000");
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.getTransactionTime()).isEqualTo("2024-01-10T09:00:00");
    }

    @Test
    void shouldNormalizeAmount_ForDebitTransaction() {
        // Arrange
        SystemTransaction debit = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.now())
                .build();

        // Act
        BigDecimal normalized = debit.getNormalizedAmount();

        // Assert
        assertThat(normalized).isEqualByComparingTo("-5000000");
    }

    @Test
    void shouldNormalizeAmount_ForCreditTransaction() {
        // Arrange
        SystemTransaction credit = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("110000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.now())
                .build();

        // Act
        BigDecimal normalized = credit.getNormalizedAmount();

        // Assert
        assertThat(normalized).isEqualByComparingTo("110000");
    }

    @Test
    void shouldExtractDateOnly_FromTransactionTime() {
        // Arrange
        SystemTransaction transaction = SystemTransaction.builder()
                .trxID("TRX003")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:45"))
                .build();

        // Act
        String dateOnly = transaction.getDateOnly();

        // Assert
        assertThat(dateOnly).isEqualTo("2024-01-10");
    }

    @Test
    void shouldBuildMatchingKey_CombiningNormalizedAmountAndDate() {
        // Arrange
        SystemTransaction transaction = SystemTransaction.builder()
                .trxID("TRX004")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        // Act
        String matchingKey = transaction.buildMatchingKey();

        // Assert
        assertThat(matchingKey).isEqualTo("-5000000_2024-01-10");
    }

    @Test
    void shouldHandleZeroAmount() {
        // Arrange
        SystemTransaction transaction = SystemTransaction.builder()
                .trxID("TRX005")
                .amount(BigDecimal.ZERO)
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.now())
                .build();

        // Act
        BigDecimal normalized = transaction.getNormalizedAmount();

        // Assert
        assertThat(normalized).isEqualByComparingTo("0");
    }

    @Test
    void shouldHandleLargeAmount() {
        // Arrange
        BigDecimal largeAmount = new BigDecimal("999999999999.99");
        SystemTransaction transaction = SystemTransaction.builder()
                .trxID("TRX006")
                .amount(largeAmount)
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.now())
                .build();

        // Act
        BigDecimal normalized = transaction.getNormalizedAmount();

        // Assert
        assertThat(normalized).isEqualByComparingTo("-999999999999.99");
    }
}
