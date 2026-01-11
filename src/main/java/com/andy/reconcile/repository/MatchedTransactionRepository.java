package com.andy.reconcile.repository;

import com.andy.reconcile.entity.MatchedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MatchedTransaction.
 * Provides database operations for matched transactions.
 */
@Repository
public interface MatchedTransactionRepository extends JpaRepository<MatchedTransaction, Long> {

    /**
     * Find all matched transactions for a specific job.
     *
     * @param jobId The reconciliation job ID
     * @return List of matched transactions
     */
    List<MatchedTransaction> findByJobId(Long jobId);
}
