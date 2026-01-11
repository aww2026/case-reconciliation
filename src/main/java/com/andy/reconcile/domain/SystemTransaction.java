package com.andy.reconcile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a transaction from Amartha's internal system.
 *
 * Key characteristics:
 * - amount is ALWAYS positive
 * - type indicates direction (DEBIT=out, CREDIT=in)
 * - transactionTime includes full datetime (with time component)
 * - trxID is Amartha's internal identifier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemTransaction {

    /**
     * Amartha's internal transaction ID
     * Format: TRX + Date + Sequence (e.g., "TRX20240110001")
     */
    private String trxID;

    /**
     * Transaction amount (ALWAYS positive)
     * Direction indicated by 'type' field
     */
    private BigDecimal amount;

    /**
     * Transaction type indicating money flow direction
     * DEBIT = money OUT, CREDIT = money IN
     */
    private TransactionType type;

    /**
     * Full datetime when transaction was created
     * Includes time component (HH:MM:SS)
     */
    private LocalDateTime transactionTime;

    /**
     * Converts transaction to signed amount for reconciliation matching.
     *
     * DEBIT → negative (money out)
     * CREDIT → positive (money in)
     *
     * This normalization allows comparison with bank statement amounts
     * which use signed numbers (negative=debit, positive=credit)
     *
     * @return Normalized signed amount
     */
    public BigDecimal getNormalizedAmount() {
        if (type == TransactionType.DEBIT) {
            return amount.negate();  // Make negative for DEBIT
        }
        return amount;  // Keep positive for CREDIT
    }

    /**
     * Extracts date-only string from transactionTime.
     *
     * Bank statements only have dates (no time component),
     * so we need to strip the time for matching.
     *
     * @return Date in YYYY-MM-DD format
     */
    public String getDateOnly() {
        return transactionTime.toLocalDate().toString();
    }

    /**
     * Builds a matching key for reconciliation.
     *
     * Combines normalized amount + date to create a unique key
     * that can be matched against bank statements.
     *
     * Format: "{normalizedAmount}_{date}"
     * Example: "-5000000_2024-01-10"
     *
     * @return Matching key string
     */
    public String buildMatchingKey() {
        return getNormalizedAmount() + "_" + getDateOnly();
    }
}
