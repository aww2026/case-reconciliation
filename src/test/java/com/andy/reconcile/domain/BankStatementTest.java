package com.andy.reconcile.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BankStatement domain model.
 *
 * Bank statements use SIGNED amounts:
 * - Negative amounts = money OUT (debits from bank's perspective)
 * - Positive amounts = money IN (credits from bank's perspective)
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
class BankStatementTest {

    @Test
    void shouldCreateBankStatement_WithAllFields() {
        // ARRANGE & ACT
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BCA2024011001200456")
                .amount(new BigDecimal("-5000000"))  // Negative = debit
                .date(LocalDate.parse("2024-01-10"))
                .bankName("BCA")
                .build();

        // ASSERT
        assertThat(statement.getUniqueIdentifier()).isEqualTo("BCA2024011001200456");
        assertThat(statement.getAmount()).isEqualByComparingTo("-5000000");
        assertThat(statement.getDate()).isEqualTo(LocalDate.parse("2024-01-10"));
        assertThat(statement.getBankName()).isEqualTo("BCA");
    }

    @Test
    void shouldCreateBankStatement_WithoutBankName() {
        // Bank name is optional
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK123")
                .amount(new BigDecimal("110000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        assertThat(statement.getBankName()).isNull();
    }

    @Test
    void shouldHandlePositiveAmount_ForCredit() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK456")
                .amount(new BigDecimal("110000"))  // Positive = credit
                .date(LocalDate.parse("2024-01-10"))
                .build();

        assertThat(statement.getAmount()).isPositive();
        assertThat(statement.getAmount()).isEqualByComparingTo("110000");
    }

    @Test
    void shouldHandleNegativeAmount_ForDebit() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK789")
                .amount(new BigDecimal("-5000000"))  // Negative = debit
                .date(LocalDate.parse("2024-01-10"))
                .build();

        assertThat(statement.getAmount()).isNegative();
        assertThat(statement.getAmount()).isEqualByComparingTo("-5000000");
    }

    @Test
    void shouldBuildMatchingKey_CombiningAmountAndDate() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK999")
                .amount(new BigDecimal("-5000000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        String matchingKey = statement.buildMatchingKey();

        assertThat(matchingKey).isEqualTo("-5000000_2024-01-10");
    }

    @Test
    void shouldHandleLargeAmount() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK111")
                .amount(new BigDecimal("999999999.99"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        assertThat(statement.getAmount()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void shouldHandleZeroAmount() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK000")
                .amount(BigDecimal.ZERO)
                .date(LocalDate.parse("2024-01-10"))
                .build();

        assertThat(statement.getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void shouldFormatDate_AsString() {
        BankStatement statement = BankStatement.builder()
                .uniqueIdentifier("BANK222")
                .amount(new BigDecimal("100000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        String dateString = statement.getDateAsString();

        assertThat(dateString).isEqualTo("2024-01-10");
    }
}
