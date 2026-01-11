package com.andy.reconcile.repository;

import com.andy.reconcile.entity.UnmatchedBankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for UnmatchedBankStatement.
 * Provides database operations for unmatched bank statements.
 */
@Repository
public interface UnmatchedBankStatementRepository extends JpaRepository<UnmatchedBankStatement, Long> {

    /**
     * Find all unmatched bank statements for a specific job.
     *
     * @param jobId The reconciliation job ID
     * @return List of unmatched bank statements
     */
    List<UnmatchedBankStatement> findByJobId(Long jobId);
}
