package com.andy.reconcile.domain;

/**
 * Transaction type enum representing money flow direction.
 *
 * DEBIT = Money OUT (e.g., loan disbursement)
 * CREDIT = Money IN (e.g., loan repayment)
 */
public enum TransactionType {
    /**
     * DEBIT: Money leaving the account (outflow)
     * Example: Loan disbursement, investor payout, fees paid
     */
    DEBIT,

    /**
     * CREDIT: Money entering the account (inflow)
     * Example: Loan repayment, investor funding, interest earned
     */
    CREDIT
}
