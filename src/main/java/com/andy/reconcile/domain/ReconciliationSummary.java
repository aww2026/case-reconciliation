package com.andy.reconcile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reconciliation Summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummary {

    // ===== BASIC REQUIREMENT 1: Total transactions processed =====

    /**
     * Total number of system transactions processed (filtered by date range).
     */
    private int totalSystemTransactions;

    /**
     * Total number of bank transactions processed (filtered by date range).
     */
    private int totalBankTransactions;

    // ===== BASIC REQUIREMENT 2: Total matched =====

    /**
     * Total number of matched transactions (exact + tolerance matches).
     */
    private int matchedCount;

    /**
     * List of all matched transaction pairs with details.
     */
    private List<MatchedPair> matches;

    // ===== BASIC REQUIREMENT 3: Total unmatched =====

    /**
     * Total number of unmatched transactions (system + bank).
     */
    private int unmatchedCount;

    // ===== BASIC REQUIREMENT 4: Unmatched details =====

    /**
     * System transactions that have no match in any bank statement.
     * These represent transactions in our system but missing from banks.
     */
    private List<SystemTransaction> unmatchedSystem;

    /**
     * Bank statements that have no match in the system, GROUPED BY BANK.
     * These represent transactions in bank statements but missing from our system.
     *
     * Map<BankName, List<BankStatement>>
     * Example:
     * {
     *   "BCA": [stmt1, stmt2],
     *   "Mandiri": [stmt3]
     * }
     */
    private Map<String, List<BankStatement>> unmatchedBankByBank;

    // ===== BASIC REQUIREMENT 5: Total discrepancies =====

    /**
     * Sum of absolute differences in amount between matched transactions.
     * This represents the total amount variance due to:
     * - Bank fees
     * - Rounding errors
     * - Currency conversion differences
     * - Processing fees
     */
    private BigDecimal totalDiscrepancy;

    // ===== Additional context fields =====

    /**
     * Start date of the reconciliation timeframe.
     */
    private LocalDate startDate;

    /**
     * End date of the reconciliation timeframe.
     */
    private LocalDate endDate;

    // ===== Calculated fields (helper methods) =====

    /**
     * Returns the total number of transactions processed (system + bank).
     *
     * @return totalSystemTransactions + totalBankTransactions
     */
    public int getTotalProcessed() {
        return totalSystemTransactions + totalBankTransactions;
    }

    /**
     * Returns the total number of unmatched bank transactions across all banks.
     *
     * @return Count of all unmatched bank statements
     */
    public int getTotalUnmatchedBank() {
        if (unmatchedBankByBank == null) {
            return 0;
        }
        return unmatchedBankByBank.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Returns the reconciliation rate (percentage of matched transactions).
     *
     * Formula: (matchedCount / totalSystemTransactions) * 100
     *
     * @return Reconciliation rate as a percentage (0-100)
     */
    public double getReconciliationRate() {
        if (totalSystemTransactions == 0) {
            return 0.0;
        }
        return ((double) matchedCount / totalSystemTransactions) * 100.0;
    }
}
