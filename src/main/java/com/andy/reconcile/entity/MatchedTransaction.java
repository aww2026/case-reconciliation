package com.andy.reconcile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for matched transactions.
 * Represents successful matches between system transactions and bank statements.
 *
 * Extends ReconciliationItem using Joined Table Inheritance.
 * Database: matched_transactions table (child of reconciliation_items)
 */
@Entity
@Table(name = "matched_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedTransaction extends ReconciliationItem {

    // System Transaction Data
    @Column(name = "system_trx_id", length = 100)
    private String systemTrxId;

    @Column(name = "system_amount", precision = 19, scale = 2)
    private BigDecimal systemAmount;

    @Column(name = "system_type", length = 10)
    private String systemType;

    @Column(name = "system_transaction_time")
    private LocalDateTime systemTransactionTime;

    // Bank Statement Data
    @Column(name = "bank_unique_identifier", length = 100)
    private String bankUniqueIdentifier;

    @Column(name = "bank_amount", precision = 19, scale = 2)
    private BigDecimal bankAmount;

    @Column(name = "bank_date")
    private LocalDate bankDate;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    // Match Metadata
    @Column(name = "discrepancy", precision = 19, scale = 2)
    private BigDecimal discrepancy;

    @Column(name = "confidence")
    private Double confidence;
}
