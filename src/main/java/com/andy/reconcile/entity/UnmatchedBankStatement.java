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

/**
 * Entity for unmatched bank statements.
 * Represents bank statements that were not found in system transactions.
 *
 * Extends ReconciliationItem using Joined Table Inheritance.
 * Database: unmatched_bank_statements table (child of reconciliation_items)
 */
@Entity
@Table(name = "unmatched_bank_statements")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UnmatchedBankStatement extends ReconciliationItem {

    @Column(name = "unique_identifier", length = 100)
    private String uniqueIdentifier;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "bank_name", length = 50)
    private String bankName;
}
