package com.andy.reconcile.matcher;

import com.andy.reconcile.domain.BankStatement;
import com.andy.reconcile.domain.MatchedPair;
import com.andy.reconcile.domain.SystemTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact Matcher - Core reconciliation algorithm using HashMap for O(n+m) performance.
 *
 * Algorithm:
 * 1. Build HashMap<MatchingKey, SystemTransaction> from system transactions
 *    - Key format: "{normalizedAmount}_{date}" (e.g., "-5000000_2024-01-10")
 * 2. For each bank statement:
 *    - Build matching key from amount + date
 *    - Look up in HashMap (O(1))
 *    - If found: Create MatchedPair, remove from map (prevent duplicate matching)
 *    - If not found: Add to unmatched bank list
 * 3. Remaining items in HashMap = unmatched system transactions
 *
 * Time Complexity: O(n + m) where n = system transactions, m = bank statements
 * Space Complexity: O(n) for the HashMap
 *
 * Why this works:
 * - Transaction IDs don't match across systems (different formats)
 * - So we match by business key: amount + date
 * - System amounts are normalized (DEBIT=negative, CREDIT=positive)
 * - Bank amounts are already signed
 * - Same amount + same date = same transaction!
 */
@Component
public class ExactMatcher {

    /**
     * Performs exact matching between system transactions and bank statements.
     *
     * Matches are made when:
     * - Normalized system amount == bank statement amount (exactly)
     * - Transaction date == bank statement date (exactly)
     *
     * @param systemTransactions List of system transactions to match
     * @param bankStatements List of bank statements to match
     * @return MatchResult containing matches, unmatched system, and unmatched bank
     */
    public MatchResult match(
            List<SystemTransaction> systemTransactions,
            List<BankStatement> bankStatements
    ) {
        // Validate inputs (handle nulls gracefully)
        if (systemTransactions == null) {
            systemTransactions = new ArrayList<>();
        }
        if (bankStatements == null) {
            bankStatements = new ArrayList<>();
        }

        // Build hash map from system transactions
        // Key: "{normalizedAmount}_{date}", Value: SystemTransaction
        Map<String, SystemTransaction> systemMap = buildSystemMap(systemTransactions);

        // Track which system transactions got matched (by trxID)
        Map<String, Boolean> matchedSystemIds = new HashMap<>();

        // Lists to collect results
        List<MatchedPair> matches = new ArrayList<>();
        List<BankStatement> unmatchedBank = new ArrayList<>();

        // Try to match each bank statement
        for (BankStatement bankStmt : bankStatements) {
            String key = buildMatchingKey(bankStmt);

            if (systemMap.containsKey(key)) {
                // Match found!
                SystemTransaction matchedSys = systemMap.remove(key);  // Remove to prevent duplicate matching
                matchedSystemIds.put(matchedSys.getTrxID(), true);     // Mark as matched

                MatchedPair pair = MatchedPair.builder()
                        .systemTransaction(matchedSys)
                        .bankStatement(bankStmt)
                        .discrepancy(BigDecimal.ZERO)  // Exact match = zero discrepancy
                        .confidence(100.0)              // Exact match = 100% confidence
                        .build();

                matches.add(pair);
            } else {
                // No match found
                unmatchedBank.add(bankStmt);
            }
        }

        // Collect unmatched system transactions
        // Any transaction that wasn't matched (either in map or not added due to duplicates)
        List<SystemTransaction> unmatchedSystem = new ArrayList<>();
        for (SystemTransaction trx : systemTransactions) {
            if (!matchedSystemIds.containsKey(trx.getTrxID())) {
                unmatchedSystem.add(trx);
            }
        }

        return MatchResult.builder()
                .matches(matches)
                .unmatchedSystem(unmatchedSystem)
                .unmatchedBank(unmatchedBank)
                .build();
    }

    /**
     * Builds a HashMap from system transactions for fast lookup.
     *
     * Key format: "{normalizedAmount}_{date}"
     * Example: "-5000000_2024-01-10"
     *
     * Note: If duplicate keys exist (same amount + date), the FIRST one is kept.
     * Subsequent duplicates are ignored for matching (they become unmatched).
     * This handles edge cases where multiple transactions have identical amount + date.
     *
     * @param systemTransactions List of system transactions
     * @return HashMap for O(1) lookup
     */
    private Map<String, SystemTransaction> buildSystemMap(List<SystemTransaction> systemTransactions) {
        Map<String, SystemTransaction> map = new HashMap<>();

        for (SystemTransaction trx : systemTransactions) {
            String key = buildMatchingKey(trx);
            // putIfAbsent: only adds if key doesn't exist (keeps first, ignores duplicates)
            map.putIfAbsent(key, trx);
        }

        return map;
    }

    /**
     * Builds a matching key from a system transaction.
     *
     * Format: "{normalizedAmount}_{date}"
     * - normalizedAmount: DEBIT → negative, CREDIT → positive
     * - date: YYYY-MM-DD format (stripped time component)
     *
     * @param systemTransaction The system transaction
     * @return Matching key string
     */
    private String buildMatchingKey(SystemTransaction systemTransaction) {
        return systemTransaction.buildMatchingKey();
    }

    /**
     * Builds a matching key from a bank statement.
     *
     * Format: "{amount}_{date}"
     * - amount: Already signed (negative=debit, positive=credit)
     * - date: YYYY-MM-DD format
     *
     * @param bankStatement The bank statement
     * @return Matching key string
     */
    private String buildMatchingKey(BankStatement bankStatement) {
        return bankStatement.buildMatchingKey();
    }
}
