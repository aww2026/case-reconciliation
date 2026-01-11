package com.andy.reconcile.repository;

import com.andy.reconcile.entity.ReconciliationJob;
import com.andy.reconcile.entity.ReconciliationJob.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ReconciliationJob.
 * Provides database operations for reconciliation jobs.
 */
@Repository
public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {

    /**
     * Find all jobs with a specific status.
     *
     * @param status The job status to filter by
     * @return List of jobs with the specified status
     */
    List<ReconciliationJob> findByStatus(JobStatus status);

    /**
     * Find all jobs ordered by creation date descending (newest first).
     *
     * @return List of all jobs ordered by creation date
     */
    List<ReconciliationJob> findAllByOrderByCreatedAtDesc();
}
