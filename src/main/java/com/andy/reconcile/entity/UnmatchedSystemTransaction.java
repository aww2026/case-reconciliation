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
import java.time.LocalDateTime;

/**
 * Entity for unmatched system transactions.
 * Represents system transactions that were not found in bank statements.
 *
 * Extends ReconciliationItem using Joined Table Inheritance.
 * Database: unmatched_system_transactions table (child of reconciliation_items)
 */
@Entity
@Table(name = "unmatched_system_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UnmatchedSystemTransaction extends ReconciliationItem {

    @Column(name = "trx_id", length = 100)
    private String trxId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", length = 10)
    private String type;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;
}
