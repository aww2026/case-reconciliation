package com.andy.reconcile.service;

import com.andy.reconcile.domain.*;
import com.andy.reconcile.matcher.ExactMatcher;
import com.andy.reconcile.matcher.MatchResult;
import com.andy.reconcile.parser.CSVParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reconciliation Service - THE CRITICAL SERVICE LAYER.
 *
 * This service orchestrates the complete reconciliation flow and implements
 *
 * Input Parameters (from basic requirements):
 * - System transaction CSV file path
 * - Bank statement CSV file paths (multiple files from different banks)
 * - Start date for reconciliation timeframe
 * - End date for reconciliation timeframe
 *
 * Output (ReconciliationSummary with ALL 5 requirements):
 *  1. Total number of transactions processed
 *  2. Total number of matched transactions
 *  3. Total number of unmatched transactions
 *  4. Details of unmatched transactions:
 *    - System transaction details if missing in bank statement(s)
 *    - Bank statement details if missing in system (grouped by bank)
 *  5. Total discrepancies (sum of absolute differences)
 *
 * Workflow:
 * 1. Parse system transactions from CSV (with date filtering)
 * 2. Parse bank statements from multiple CSV files (with date filtering)
 * 3. Match transactions using ExactMatcher
 * 4. Build comprehensive ReconciliationSummary
 * 5. Group unmatched bank statements by bank name
 * 6. Calculate total discrepancy
 */
@Service
public class ReconciliationService {

    private final CSVParser csvParser;
    private final ExactMatcher exactMatcher;
    private final ParallelCSVParser parallelCSVParser;

    /**
     * Constructor with dependency injection.
     *
     * @param csvParser Parser for CSV files
     * @param exactMatcher Matcher for transaction matching
     * @param parallelCSVParser Parallel parser for multiple bank files (optional)
     */
    public ReconciliationService(CSVParser csvParser, ExactMatcher exactMatcher, ParallelCSVParser parallelCSVParser) {
        this.csvParser = csvParser;
        this.exactMatcher = exactMatcher;
        this.parallelCSVParser = parallelCSVParser;
    }

    /**
     * Constructor for testing (without parallel parser).
     */
    public ReconciliationService(CSVParser csvParser, ExactMatcher exactMatcher) {
        this(csvParser, exactMatcher, null);
    }

    /**
     * Default constructor (for production use without mocking).
     */
    public ReconciliationService() {
        this.csvParser = new CSVParser();
        this.exactMatcher = new ExactMatcher();
        this.parallelCSVParser = null;
    }

    /**
     * Performs reconciliation between system transactions and bank statements.
     *
     * This is the main method that implements ALL 5 basic requirements!
     *
     * @param systemTransactionFile Path to system transaction CSV file
     * @param bankStatementFiles Paths to bank statement CSV files (can be multiple)
     * @param startDate Start date for reconciliation timeframe (inclusive)
     * @param endDate End date for reconciliation timeframe (inclusive)
     * @return ReconciliationSummary containing all required information
     * @throws IOException if file reading fails
     */
    public ReconciliationSummary reconcile(
            String systemTransactionFile,
            List<String> bankStatementFiles,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        // Step 1: Parse system transactions (with date filtering)
        List<SystemTransaction> systemTransactions = csvParser.parseSystemTransactions(
                systemTransactionFile,
                startDate,
                endDate
        );

        // Step 2: Parse bank statements from multiple files (with date filtering)
        // Use parallel processing if available and multiple files exist
        List<BankStatement> bankStatements;
        if (parallelCSVParser != null && bankStatementFiles.size() > 1) {
            // Parallel processing for better performance
            bankStatements = parallelCSVParser.parseBankStatementsParallel(
                    bankStatementFiles,
                    startDate,
                    endDate
            );
        } else {
            // Sequential processing (fallback or single file)
            bankStatements = new ArrayList<>();
            for (String bankFile : bankStatementFiles) {
                List<BankStatement> statements = csvParser.parseBankStatements(
                        bankFile,
                        startDate,
                        endDate
                );
                bankStatements.addAll(statements);
            }
        }

        // Step 3: Match transactions using ExactMatcher
        MatchResult matchResult = exactMatcher.match(systemTransactions, bankStatements);

        // Step 4: Build ReconciliationSummary
        return buildSummary(
                systemTransactions,
                bankStatements,
                matchResult,
                startDate,
                endDate
        );
    }

    /**
     * Builds a comprehensive ReconciliationSummary with ALL 5 basic requirements.
     */
    private ReconciliationSummary buildSummary(
            List<SystemTransaction> systemTransactions,
            List<BankStatement> bankStatements,
            MatchResult matchResult,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // Total transactions processed
        int totalSystemTransactions = systemTransactions.size();
        int totalBankTransactions = bankStatements.size();

        // Total matched
        int matchedCount = matchResult.getMatches().size();

        // Total unmatched
        int unmatchedSystemCount = matchResult.getUnmatchedSystem().size();
        int unmatchedBankCount = matchResult.getUnmatchedBank().size();
        int totalUnmatchedCount = unmatchedSystemCount + unmatchedBankCount;

        // Group unmatched bank statements by bank name
        Map<String, List<BankStatement>> unmatchedBankByBank = groupByBank(
                matchResult.getUnmatchedBank()
        );

        // Calculate total discrepancy
        BigDecimal totalDiscrepancy = calculateTotalDiscrepancy(
                matchResult.getMatches()
        );

        return ReconciliationSummary.builder()
                .totalSystemTransactions(totalSystemTransactions)
                .totalBankTransactions(totalBankTransactions)
                .matchedCount(matchedCount)
                .unmatchedCount(totalUnmatchedCount)
                .matches(matchResult.getMatches())
                .unmatchedSystem(matchResult.getUnmatchedSystem())
                .unmatchedBankByBank(unmatchedBankByBank)
                .totalDiscrepancy(totalDiscrepancy)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Groups unmatched bank statements by bank name.
     *
     *  Group unmatched bank statements by bank.
     *
     * @param unmatchedBankStatements List of unmatched bank statements
     * @return Map of bank name → list of unmatched statements
     */
    private Map<String, List<BankStatement>> groupByBank(List<BankStatement> unmatchedBankStatements) {
        return unmatchedBankStatements.stream()
                .collect(Collectors.groupingBy(
                        stmt -> stmt.getBankName() != null ? stmt.getBankName() : "Unknown",
                        HashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Calculates the total discrepancy across all matched pairs.
     *
     *  Sum of absolute differences.
     *
     * @param matches List of matched transaction pairs
     * @return Total discrepancy amount
     */
    private BigDecimal calculateTotalDiscrepancy(List<MatchedPair> matches) {
        return matches.stream()
                .map(MatchedPair::getDiscrepancy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
