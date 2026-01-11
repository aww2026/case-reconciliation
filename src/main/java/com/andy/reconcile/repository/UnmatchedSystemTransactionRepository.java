package com.andy.reconcile.repository;

import com.andy.reconcile.entity.UnmatchedSystemTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for UnmatchedSystemTransaction.
 * Provides database operations for unmatched system transactions.
 */
@Repository
public interface UnmatchedSystemTransactionRepository extends JpaRepository<UnmatchedSystemTransaction, Long> {

    /**
     * Find all unmatched system transactions for a specific job.
     *
     * @param jobId The reconciliation job ID
     * @return List of unmatched system transactions
     */
    List<UnmatchedSystemTransaction> findByJobId(Long jobId);
}
