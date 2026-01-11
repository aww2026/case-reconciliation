package com.andy.reconcile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for detailed reconciliation results.
 * Contains complete job information including all matches and unmatched transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationDetailDto {

    private ReconciliationJobDto job;
    private ReconciliationSummaryDto summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationSummaryDto {
        private int totalSystemTransactions;
        private int totalBankTransactions;
        private int matchedCount;
        private int unmatchedCount;
        private BigDecimal totalDiscrepancy;
        private Double reconciliationRate;
        private LocalDate startDate;
        private LocalDate endDate;

        private List<MatchedPairDto> matches;
        private List<UnmatchedSystemTransactionDto> unmatchedSystem;
        private Map<String, List<UnmatchedBankStatementDto>> unmatchedBankByBank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedPairDto {
        private String systemTrxId;
        private BigDecimal systemAmount;
        private String systemType;
        private LocalDateTime systemTransactionTime;

        private String bankUniqueIdentifier;
        private BigDecimal bankAmount;
        private LocalDate bankDate;
        private String bankName;

        private BigDecimal discrepancy;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedSystemTransactionDto {
        private String trxId;
        private BigDecimal amount;
        private String type;
        private LocalDateTime transactionTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedBankStatementDto {
        private String uniqueIdentifier;
        private BigDecimal amount;
        private LocalDate date;
        private String bankName;
    }
}
