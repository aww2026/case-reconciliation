package com.andy.reconcile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a successfully matched pair of transactions between
 * the internal system and a bank statement.
 *
 * This is the output of the matching algorithm, containing:
 * - The system transaction
 * - The corresponding bank statement entry
 * - The discrepancy (if any) between the amounts
 * - The confidence level of the match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedPair {

    /**
     * The transaction from the internal system.
     */
    private SystemTransaction systemTransaction;

    /**
     * The corresponding transaction from the bank statement.
     */
    private BankStatement bankStatement;

    /**
     * The absolute difference between the normalized system amount
     * and the bank statement amount.
     *
     * Zero (0) indicates an exact match.
     * Non-zero indicates a tolerance match with some discrepancy
     * (e.g., bank fees, rounding errors).
     */
    private BigDecimal discrepancy;

    /**
     * The confidence level of this match (0-100%).
     *
     * 100% = Exact match (same amount + same date)
     * 70-99% = Tolerance match (slight difference in amount/date)
     * < 70% = Should not be matched (below threshold)
     *
     * Calculated based on:
     * - Amount proximity (70% weight)
     * - Date proximity (30% weight)
     */
    private Double confidence;

    /**
     * Checks if this is an exact match (no discrepancy).
     *
     * @return true if discrepancy is zero, false otherwise
     */
    public boolean isExactMatch() {
        return discrepancy.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calculates the discrepancy directly from the transaction objects.
     *
     * This is useful for verification purposes to ensure the stored
     * discrepancy value is correct.
     *
     * @return The absolute difference between normalized system amount and bank amount
     */
    public BigDecimal calculateDiscrepancyFromTransactions() {
        BigDecimal normalizedSystemAmount = systemTransaction.getNormalizedAmount();
        BigDecimal bankAmount = bankStatement.getAmount();
        return normalizedSystemAmount.subtract(bankAmount).abs();
    }

    /**
     * Returns the system transaction ID for quick reference.
     *
     * @return The trxID from the system transaction
     */
    public String getSystemTrxID() {
        return systemTransaction.getTrxID();
    }

    /**
     * Returns the bank unique identifier for quick reference.
     *
     * @return The uniqueIdentifier from the bank statement
     */
    public String getBankIdentifier() {
        return bankStatement.getUniqueIdentifier();
    }
}
