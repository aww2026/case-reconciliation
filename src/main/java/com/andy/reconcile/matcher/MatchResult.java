package com.andy.reconcile.matcher;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.domain.MatchedPair;
import com.andy.reconcile.domain.SystemTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result object returned by matching algorithms.
 *
 * Contains:
 * - List of matched transaction pairs
 * - List of unmatched system transactions
 * - List of unmatched bank statements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    /**
     * Successfully matched transaction pairs.
     */
    private List<MatchedPair> matches;

    /**
     * System transactions that couldn't be matched.
     */
    private List<SystemTransaction> unmatchedSystem;

    /**
     * Bank statements that couldn't be matched.
     */
    private List<BankStatement> unmatchedBank;
}
