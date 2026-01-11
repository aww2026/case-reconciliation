package com.andy.reconcile.integration;

import com.andy.reconcile.domain.ReconciliationSummary;
import com.andy.reconcile.service.ReconciliationService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test - End-to-End Reconciliation Flow.
 *
 * This test proves that ALL 5 BASIC REQUIREMENTS work correctly with real CSV files:
 *
 * ✅ Requirement 1: Total number of transactions processed
 * ✅ Requirement 2: Total number of matched transactions
 * ✅ Requirement 3: Total number of unmatched transactions
 * ✅ Requirement 4: Details of unmatched transactions
 *    - System transaction details if missing in bank statement(s)
 *    - Bank statement details if missing in system (grouped by bank!)
 * ✅ Requirement 5: Total discrepancies (sum of absolute differences)
 *
 * Test Data:
 * - system_transactions.csv: 8 transactions
 * - bca_statements.csv: 6 statements (5 matched + 1 unmatched)
 * - mandiri_statements.csv: 4 statements (3 matched + 1 unmatched)
 *
 * Expected Results:
 * - Total system: 8
 * - Total bank: 10 (6 BCA + 4 Mandiri)
 * - Matched: 8
 * - Unmatched: 2 (0 system + 2 bank, grouped by bank name)
 * - Discrepancy: 0 (all exact matches)
 */
class ReconciliationIntegrationTest {

    @Test
    void shouldReconcile_EndToEnd_WithAllBasicRequirements() throws IOException {
        // ARRANGE - Real CSV files in src/test/resources/integration/
        String basePath = "src/test/resources/integration/";
        String systemFile = basePath + "system_transactions.csv";
        String bcaFile = basePath + "bca_statements.csv";
        String mandiriFile = basePath + "mandiri_statements.csv";

        LocalDate startDate = LocalDate.parse("2024-01-01");
        LocalDate endDate = LocalDate.parse("2024-01-31");

        ReconciliationService service = new ReconciliationService();

        // ACT - Perform full reconciliation
        ReconciliationSummary summary = service.reconcile(
                systemFile,
                Arrays.asList(bcaFile, mandiriFile),
                startDate,
                endDate
        );

        // ASSERT - ✅ REQUIREMENT 1: Total transactions processed
        assertThat(summary.getTotalSystemTransactions())
                .as("Total system transactions should be 8")
                .isEqualTo(8);

        assertThat(summary.getTotalBankTransactions())
                .as("Total bank transactions should be 10 (6 BCA + 4 Mandiri)")
                .isEqualTo(10);

        assertThat(summary.getTotalProcessed())
                .as("Total processed should be 18 (8 + 10)")
                .isEqualTo(18);

        // ASSERT - ✅ REQUIREMENT 2: Total matched transactions
        assertThat(summary.getMatchedCount())
                .as("All 8 system transactions should match with bank statements")
                .isEqualTo(8);

        assertThat(summary.getMatches())
                .as("Should have 8 matched pairs")
                .hasSize(8);

        // ASSERT - ✅ REQUIREMENT 3: Total unmatched transactions
        assertThat(summary.getUnmatchedCount())
                .as("Should have 2 unmatched (0 system + 2 bank)")
                .isEqualTo(2);

        // ASSERT - ✅ REQUIREMENT 4a: Unmatched system details
        assertThat(summary.getUnmatchedSystem())
                .as("All system transactions matched, so no unmatched system")
                .isEmpty();

        // ASSERT - ✅ REQUIREMENT 4b: Unmatched bank details (GROUPED BY BANK!)
        assertThat(summary.getUnmatchedBankByBank())
                .as("Should have unmatched statements from 2 banks")
                .hasSize(2);

        assertThat(summary.getUnmatchedBankByBank().get("BCA"))
                .as("BCA should have 1 unmatched statement (Jan 15, 500K)")
                .hasSize(1);

        assertThat(summary.getUnmatchedBankByBank().get("BCA").get(0).getAmount())
                .as("BCA unmatched amount should be 500,000")
                .isEqualByComparingTo("500000");

        assertThat(summary.getUnmatchedBankByBank().get("Mandiri"))
                .as("Mandiri should have 1 unmatched statement (Jan 15, 250K)")
                .hasSize(1);

        assertThat(summary.getUnmatchedBankByBank().get("Mandiri").get(0).getAmount())
                .as("Mandiri unmatched amount should be 250,000")
                .isEqualByComparingTo("250000");

        // ASSERT - ✅ REQUIREMENT 5: Total discrepancies
        assertThat(summary.getTotalDiscrepancy())
                .as("All matches are exact, so total discrepancy should be 0")
                .isEqualByComparingTo("0");

        // ASSERT - Date range context
        assertThat(summary.getStartDate()).isEqualTo(startDate);
        assertThat(summary.getEndDate()).isEqualTo(endDate);

        // ASSERT - Reconciliation rate
        assertThat(summary.getReconciliationRate())
                .as("Reconciliation rate should be 100% (8/8 matched)")
                .isEqualTo(100.0);

        // Print summary for visual verification
        printSummary(summary);
    }

    /**
     * Helper method to print reconciliation summary (for manual verification).
     */
    private void printSummary(ReconciliationSummary summary) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RECONCILIATION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Period: " + summary.getStartDate() + " to " + summary.getEndDate());
        System.out.println();

        System.out.println("✅ REQUIREMENT 1: Total Transactions Processed");
        System.out.println("  - System transactions: " + summary.getTotalSystemTransactions());
        System.out.println("  - Bank transactions: " + summary.getTotalBankTransactions());
        System.out.println("  - Total processed: " + summary.getTotalProcessed());
        System.out.println();

        System.out.println("✅ REQUIREMENT 2: Matched Transactions");
        System.out.println("  - Matched count: " + summary.getMatchedCount());
        System.out.println("  - Reconciliation rate: " + String.format("%.1f%%", summary.getReconciliationRate()));
        System.out.println();

        System.out.println("✅ REQUIREMENT 3: Unmatched Transactions");
        System.out.println("  - Total unmatched: " + summary.getUnmatchedCount());
        System.out.println("  - Unmatched system: " + summary.getUnmatchedSystem().size());
        System.out.println("  - Unmatched bank: " + summary.getTotalUnmatchedBank());
        System.out.println();

        System.out.println("✅ REQUIREMENT 4: Unmatched Details");
        System.out.println("  a) System transactions missing in bank:");
        if (summary.getUnmatchedSystem().isEmpty()) {
            System.out.println("     (none)");
        } else {
            summary.getUnmatchedSystem().forEach(sys ->
                    System.out.println("     - " + sys.getTrxID() + ": " + sys.getAmount() + " " + sys.getType())
            );
        }
        System.out.println();

        System.out.println("  b) Bank statements missing in system (GROUPED BY BANK):");
        summary.getUnmatchedBankByBank().forEach((bankName, statements) -> {
            System.out.println("     " + bankName + ": " + statements.size() + " unmatched");
            statements.forEach(stmt ->
                    System.out.println("       - " + stmt.getUniqueIdentifier() + ": " +
                            stmt.getAmount() + " on " + stmt.getDate())
            );
        });
        System.out.println();

        System.out.println("✅ REQUIREMENT 5: Total Discrepancies");
        System.out.println("  - Total discrepancy amount: Rp " +
                String.format("%,d", summary.getTotalDiscrepancy().longValue()));
        System.out.println("=".repeat(60) + "\n");
    }
}
