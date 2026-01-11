package com.andy.reconcile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Abstract base class for all reconciliation items.
 * Uses Joined Table Inheritance strategy for normalized storage.
 *
 * Database Design:
 * - Base table: reconciliation_items (id, job_id, created_at)
 * - Child tables: matched_transactions, unmatched_system_transactions, unmatched_bank_statements
 *
 * Benefits:
 * - Normalized (no NULL waste)
 * - Polymorphic queries supported
 * - Type safe at database level
 * - Scalable (can partition child tables)
 */
@Entity
@Table(name = "reconciliation_items")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ReconciliationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ReconciliationJob job;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
