package com.andy.reconcile.matcher;

import com.andy.reconcile.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ExactMatcher - THE CORE MATCHING ALGORITHM.
 *
 * ExactMatcher implements the hash map based O(n+m) exact matching algorithm.
 * This is critical for meeting the basic requirements:
 * - Matches transactions by amount + date (since IDs don't match across systems)
 * - Produces MatchedPair objects for matched transactions
 * - Identifies unmatched system transactions
 * - Identifies unmatched bank statements
 *
 * Algorithm:
 * 1. Build HashMap<MatchingKey, SystemTransaction> from system transactions
 * 2. For each bank statement, look up matching key in hash map
 * 3. If found: create MatchedPair, remove from map
 * 4. If not found: add to unmatched bank list
 * 5. Remaining items in map = unmatched system transactions
 *
 * TDD Approach:
 * 🔴 RED: Write failing tests first
 * 🟢 GREEN: Implement minimal code to pass
 * 🔵 REFACTOR: Improve code quality
 */
class ExactMatcherTest {

    private ExactMatcher exactMatcher;

    @BeforeEach
    void setUp() {
        exactMatcher = new ExactMatcher();
    }

    @Test
    void shouldMatchExactly_WhenAmountAndDateMatch() {
        // ARRANGE
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-5000000"))  // Matches normalized amount
                .date(LocalDate.parse("2024-01-10"))
                .bankName("BCA")
                .build();

        List<SystemTransaction> systemTransactions = Arrays.asList(systemTrx);
        List<BankStatement> bankStatements = Arrays.asList(bankStmt);

        // ACT
        MatchResult result = exactMatcher.match(systemTransactions, bankStatements);

        // ASSERT
        assertThat(result.getMatches()).hasSize(1);
        assertThat(result.getUnmatchedSystem()).isEmpty();
        assertThat(result.getUnmatchedBank()).isEmpty();

        MatchedPair match = result.getMatches().get(0);
        assertThat(match.getSystemTransaction().getTrxID()).isEqualTo("TRX001");
        assertThat(match.getBankStatement().getUniqueIdentifier()).isEqualTo("BANK001");
        assertThat(match.getDiscrepancy()).isEqualByComparingTo("0");
        assertThat(match.getConfidence()).isEqualTo(100.0);
    }

    @Test
    void shouldNotMatch_WhenAmountDiffers() {
        // ARRANGE
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK002")
                .amount(new BigDecimal("-4999000"))  // Different amount!
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(systemTrx),
                Arrays.asList(bankStmt)
        );

        // ASSERT - No exact match
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getUnmatchedSystem()).hasSize(1);
        assertThat(result.getUnmatchedBank()).hasSize(1);
    }

    @Test
    void shouldNotMatch_WhenDateDiffers() {
        // ARRANGE
        SystemTransaction systemTrx = SystemTransaction.builder()
                .trxID("TRX003")
                .amount(new BigDecimal("5000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:30:00"))
                .build();

        BankStatement bankStmt = BankStatement.builder()
                .uniqueIdentifier("BANK003")
                .amount(new BigDecimal("-5000000"))
                .date(LocalDate.parse("2024-01-11"))  // Different date!
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(systemTrx),
                Arrays.asList(bankStmt)
        );

        // ASSERT - No exact match
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getUnmatchedSystem()).hasSize(1);
        assertThat(result.getUnmatchedBank()).hasSize(1);
    }

    @Test
    void shouldMatchMultiple_WhenAllExactlyMatch() {
        // ARRANGE
        SystemTransaction sys1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        SystemTransaction sys2 = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("110000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T10:00:00"))
                .build();

        BankStatement bank1 = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        BankStatement bank2 = BankStatement.builder()
                .uniqueIdentifier("BANK002")
                .amount(new BigDecimal("110000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(sys1, sys2),
                Arrays.asList(bank1, bank2)
        );

        // ASSERT - Both should match
        assertThat(result.getMatches()).hasSize(2);
        assertThat(result.getUnmatchedSystem()).isEmpty();
        assertThat(result.getUnmatchedBank()).isEmpty();
    }

    @Test
    void shouldHandleUnmatchedSystemTransactions() {
        // ARRANGE
        SystemTransaction matched = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        SystemTransaction unmatched = SystemTransaction.builder()
                .trxID("TRX002")
                .amount(new BigDecimal("500000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.parse("2024-01-11T09:00:00"))
                .build();

        BankStatement bank1 = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(matched, unmatched),
                Arrays.asList(bank1)
        );

        // ASSERT
        assertThat(result.getMatches()).hasSize(1);
        assertThat(result.getUnmatchedSystem()).hasSize(1);
        assertThat(result.getUnmatchedSystem().get(0).getTrxID()).isEqualTo("TRX002");
    }

    @Test
    void shouldHandleUnmatchedBankStatements() {
        // ARRANGE
        SystemTransaction sys1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        BankStatement matched = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.parse("2024-01-10"))
                .bankName("BCA")
                .build();

        BankStatement unmatched = BankStatement.builder()
                .uniqueIdentifier("BANK002")
                .amount(new BigDecimal("250000"))
                .date(LocalDate.parse("2024-01-11"))
                .bankName("Mandiri")
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(sys1),
                Arrays.asList(matched, unmatched)
        );

        // ASSERT
        assertThat(result.getMatches()).hasSize(1);
        assertThat(result.getUnmatchedBank()).hasSize(1);
        assertThat(result.getUnmatchedBank().get(0).getUniqueIdentifier()).isEqualTo("BANK002");
    }

    @Test
    void shouldHandleEmptySystemTransactions() {
        // ARRANGE
        BankStatement bank1 = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("100000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(),  // Empty system transactions
                Arrays.asList(bank1)
        );

        // ASSERT
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getUnmatchedSystem()).isEmpty();
        assertThat(result.getUnmatchedBank()).hasSize(1);
    }

    @Test
    void shouldHandleEmptyBankStatements() {
        // ARRANGE
        SystemTransaction sys1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("100000"))
                .type(TransactionType.CREDIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(sys1),
                Arrays.asList()  // Empty bank statements
        );

        // ASSERT
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getUnmatchedSystem()).hasSize(1);
        assertThat(result.getUnmatchedBank()).isEmpty();
    }

    @Test
    void shouldHandleDuplicateMatchingKeys_MatchFirstOnly() {
        // ARRANGE - Two system transactions with same normalized amount + date
        SystemTransaction sys1 = SystemTransaction.builder()
                .trxID("TRX001")
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        SystemTransaction sys2 = SystemTransaction.builder()
                .trxID("TRX002")  // Different ID
                .amount(new BigDecimal("1000000"))
                .type(TransactionType.DEBIT)
                .transactionTime(LocalDateTime.parse("2024-01-10T14:00:00"))  // Different time, same date
                .build();

        BankStatement bank1 = BankStatement.builder()
                .uniqueIdentifier("BANK001")
                .amount(new BigDecimal("-1000000"))
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(sys1, sys2),
                Arrays.asList(bank1)
        );

        // ASSERT - Only one should match (first one encountered)
        assertThat(result.getMatches()).hasSize(1);
        assertThat(result.getUnmatchedSystem()).hasSize(1);
    }

    @Test
    void shouldMatchCreditTransactions() {
        // ARRANGE
        SystemTransaction credit = SystemTransaction.builder()
                .trxID("TRX005")
                .amount(new BigDecimal("110000"))
                .type(TransactionType.CREDIT)  // Money IN
                .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                .build();

        BankStatement bankCredit = BankStatement.builder()
                .uniqueIdentifier("BANK005")
                .amount(new BigDecimal("110000"))  // Positive (money IN)
                .date(LocalDate.parse("2024-01-10"))
                .build();

        // ACT
        MatchResult result = exactMatcher.match(
                Arrays.asList(credit),
                Arrays.asList(bankCredit)
        );

        // ASSERT
        assertThat(result.getMatches()).hasSize(1);
        assertThat(result.getMatches().get(0).getSystemTransaction().getType())
                .isEqualTo(TransactionType.CREDIT);
    }

    @Test
    void shouldHandleLargeDataset_Performance() {
        // ARRANGE - Create 1000 matching transactions
        List<SystemTransaction> systemTrxList = new java.util.ArrayList<>();
        List<BankStatement> bankStmtList = new java.util.ArrayList<>();

        for (int i = 1; i <= 1000; i++) {
            SystemTransaction sys = SystemTransaction.builder()
                    .trxID("TRX" + String.format("%04d", i))
                    .amount(new BigDecimal(i * 1000))
                    .type(TransactionType.DEBIT)
                    .transactionTime(LocalDateTime.parse("2024-01-10T09:00:00"))
                    .build();

            BankStatement bank = BankStatement.builder()
                    .uniqueIdentifier("BANK" + String.format("%04d", i))
                    .amount(new BigDecimal(i * 1000).negate())
                    .date(LocalDate.parse("2024-01-10"))
                    .build();

            systemTrxList.add(sys);
            bankStmtList.add(bank);
        }

        // ACT
        long startTime = System.currentTimeMillis();
        MatchResult result = exactMatcher.match(systemTrxList, bankStmtList);
        long endTime = System.currentTimeMillis();

        // ASSERT
        assertThat(result.getMatches()).hasSize(1000);
        assertThat(result.getUnmatchedSystem()).isEmpty();
        assertThat(result.getUnmatchedBank()).isEmpty();

        // Performance check: Should be fast (< 500ms for 1000 transactions)
        long executionTime = endTime - startTime;
        assertThat(executionTime).isLessThan(500);  // O(n+m) should be fast!
    }
}
