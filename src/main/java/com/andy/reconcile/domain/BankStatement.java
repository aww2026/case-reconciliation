package com.andy.reconcile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a transaction from a bank statement.
 *
 * Key characteristics:
 * - amount is SIGNED (negative=debit/money out, positive=credit/money in)
 * - date is date-only (no time component, unlike SystemTransaction)
 * - uniqueIdentifier varies by bank (different formats across BCA, Mandiri, BNI, etc.)
 * - bankName is optional (helps group unmatched transactions)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatement {

    /**
     * Bank's unique identifier for the transaction.
     * Format varies by bank:
     * - BCA: "BCA2024011001200456" (19 chars)
     * - Mandiri: "MND1234567820240110789" (23 chars)
     * - BNI: "BNI202401100930451234AB" (23-27 chars)
     * - BRI: "110120240110000987" (18 chars)
     *
     * NOTE: This ID will NOT match the system's trxID!
     */
    private String uniqueIdentifier;

    /**
     * Transaction amount (SIGNED)
     * - Negative = DEBIT (money out)
     * - Positive = CREDIT (money in)
     *
     * This is the bank's perspective, which matches our normalized amounts.
     */
    private BigDecimal amount;

    /**
     * Transaction date (date-only, no time component)
     * Banks typically only provide dates, not full datetimes.
     */
    private LocalDate date;

    /**
     * Name of the bank (optional)
     * Used for grouping unmatched transactions by bank.
     * Examples: "BCA", "Mandiri", "BNI", "BRI"
     */
    private String bankName;

    /**
     * Builds a matching key for reconciliation.
     *
     * Combines amount + date to create a key that can be matched
     * against SystemTransaction's normalized amounts.
     *
     * Format: "{amount}_{date}"
     * Example: "-5000000_2024-01-10"
     *
     * @return Matching key string
     */
    public String buildMatchingKey() {
        return amount + "_" + date.toString();
    }

    /**
     * Returns the date as a string in ISO format (YYYY-MM-DD).
     *
     * @return Date string in YYYY-MM-DD format
     */
    public String getDateAsString() {
        return date.toString();
    }
}
