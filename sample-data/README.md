# Sample Data Sets

Comprehensive sample datasets for testing the reconciliation service.

---

## 📁 Dataset Categories

### 1. **Basic Scenarios** (Small datasets for quick testing)
- `basic/system_transactions.csv` - 10 system transactions
- `basic/bank_statement_bca.csv` - 8 BCA statements (6 matched, 2 unmatched)
- `basic/bank_statement_mandiri.csv` - 5 Mandiri statements (4 matched, 1 unmatched)

**Use case:** Quick functional testing, API demos

---

### 2. **Edge Cases** (Special scenarios)
- `edge-cases/system_with_duplicates.csv` - Duplicate amounts on same date
- `edge-cases/bank_with_discrepancies.csv` - Small amount differences (fees)
- `edge-cases/mixed_dates.csv` - Transactions spanning multiple months
- `edge-cases/zero_amounts.csv` - Zero amount transactions

**Use case:** Testing matching logic edge cases

---

### 3. **Performance Testing** (Large datasets)
- `performance/system_10k.csv` - 10,000 system transactions
- `performance/bank_10k_bca.csv` - 10,000 BCA statements
- `performance/system_100k.csv` - 100,000 system transactions (parallel test)
- `performance/bank_100k_split_*.csv` - 3 files with 33k each (streaming test)

**Use case:** Performance benchmarks, load testing

---

### 4. **Real-World Scenarios** (Realistic data)
- `realistic/january_system.csv` - 1 month of realistic transactions
- `realistic/january_bca.csv` - BCA statements for January
- `realistic/january_mandiri.csv` - Mandiri statements for January
- `realistic/january_bni.csv` - BNI statements for January

**Use case:** Demo to stakeholders, UAT

---

### 5. **Multi-Bank Scenarios**
- `multi-bank/system_q1_2024.csv` - Q1 2024 system data
- `multi-bank/bca_q1.csv` - BCA Q1
- `multi-bank/mandiri_q1.csv` - Mandiri Q1
- `multi-bank/bni_q1.csv` - BNI Q1
- `multi-bank/bri_q1.csv` - BRI Q1

**Use case:** Testing parallel processing with multiple banks

---

## 🎯 Quick Start Examples

### Example 1: Basic Test
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/basic/system_transactions.csv" \
  -F "bankFiles=@sample-data/basic/bank_statement_bca.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-01-31"
```

**Expected result:** ~60% match rate (8 matched, 5 unmatched)

---

### Example 2: Multi-Bank Test (Parallel Processing)
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/multi-bank/system_q1_2024.csv" \
  -F "bankFiles=@sample-data/multi-bank/bca_q1.csv" \
  -F "bankFiles=@sample-data/multi-bank/mandiri_q1.csv" \
  -F "bankFiles=@sample-data/multi-bank/bni_q1.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-03-31"
```

**Expected result:** ~300 transactions, 3x faster with parallel processing

---

### Example 3: Large File Test (Streaming)
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/performance/system_100k.csv" \
  -F "bankFiles=@sample-data/performance/bank_100k_split_1.csv" \
  -F "bankFiles=@sample-data/performance/bank_100k_split_2.csv" \
  -F "bankFiles=@sample-data/performance/bank_100k_split_3.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-12-31"
```

**Expected result:** Automatic streaming mode, handles 100k+ transactions

---

## 📊 Dataset Details

### System Transactions Format
```csv
trxID,amount,type,transactionTime
TRX001,5000000,DEBIT,2024-01-10T10:30:00
TRX002,3000000,CREDIT,2024-01-11T14:45:00
```

**Fields:**
- `trxID`: Unique transaction ID (format: TRX + 6 digits)
- `amount`: Transaction amount in Rupiah (positive number)
- `type`: DEBIT (money out) or CREDIT (money in)
- `transactionTime`: ISO 8601 datetime

---

### Bank Statement Format
```csv
uniqueIdentifier,amount,date,bankName
BCA20240110001,−5000000,2024-01-10,BCA
MND20240111001,3000000,2024-01-11,Mandiri
```

**Fields:**
- `uniqueIdentifier`: Bank's transaction ID (varies by bank)
- `amount`: Signed amount (negative=debit, positive=credit)
- `date`: Transaction date (YYYY-MM-DD)
- `bankName`: Bank name (BCA, Mandiri, BNI, BRI)

---

## 🔍 Dataset Characteristics

### Basic Dataset
| Metric | Value |
|--------|-------|
| System transactions | 10 |
| BCA statements | 8 |
| Mandiri statements | 5 |
| Expected matches | 10 |
| Expected unmatched | 3 |
| File size | <10KB |

### Performance Dataset (10k)
| Metric | Value |
|--------|-------|
| System transactions | 10,000 |
| Bank statements | 10,000 |
| Expected matches | ~9,500 |
| Expected unmatched | ~500 |
| File size | ~1MB |
| Processing time | <1 second |

### Performance Dataset (100k)
| Metric | Value |
|--------|-------|
| System transactions | 100,000 |
| Bank statements | 100,000 (split into 3 files) |
| Expected matches | ~95,000 |
| Expected unmatched | ~5,000 |
| File size | ~30MB total |
| Processing time | <10 seconds |

---

## 🎲 Data Generation Scripts

### Generate Custom Dataset
```bash
# Generate 1000 transactions
./scripts/generate_sample_data.sh 1000 sample-data/custom/
```

### Generate Specific Scenario
```bash
# Generate high match rate scenario (95%)
./scripts/generate_sample_data.sh 500 sample-data/high-match/ --match-rate=0.95

# Generate low match rate scenario (50%)
./scripts/generate_sample_data.sh 500 sample-data/low-match/ --match-rate=0.50
```

---

## 📝 Test Scenarios

### Scenario 1: All Matched
**Files:** `basic/system_transactions.csv`, `basic/bank_statement_bca.csv`
**Filter dates:** 2024-01-01 to 2024-01-15
**Expected:** 100% match rate

### Scenario 2: Partial Match
**Files:** `basic/system_transactions.csv`, `basic/bank_statement_bca.csv` + `basic/bank_statement_mandiri.csv`
**Filter dates:** 2024-01-01 to 2024-01-31
**Expected:** 80% match rate, some unmatched in both system and bank

### Scenario 3: Multiple Banks (Parallel Processing)
**Files:** All multi-bank files
**Expected:** Parallel processing kicks in, 3x faster

### Scenario 4: Large File (Streaming)
**Files:** performance/system_100k.csv
**Expected:** Streaming mode activates, low memory usage

---

## 🚀 Using with Tests

### Unit Tests
```java
@Test
void testBasicReconciliation() throws IOException {
    String systemFile = "sample-data/basic/system_transactions.csv";
    String bankFile = "sample-data/basic/bank_statement_bca.csv";

    ReconciliationSummary summary = service.reconcile(
        systemFile, List.of(bankFile),
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2024-01-31")
    );

    assertThat(summary.getMatchedCount()).isGreaterThan(5);
}
```

### Integration Tests
```bash
# Run full integration test suite with sample data
./gradlew integrationTest -Dsample.data.path=sample-data/
```

---

## 📈 Expected Results by Dataset

### Basic Dataset
```json
{
  "totalSystemTransactions": 10,
  "totalBankTransactions": 13,
  "matchedCount": 10,
  "unmatchedCount": 3,
  "reconciliationRate": 100.0
}
```

### Multi-Bank Dataset
```json
{
  "totalSystemTransactions": 300,
  "totalBankTransactions": 320,
  "matchedCount": 285,
  "unmatchedCount": 35,
  "reconciliationRate": 95.0,
  "processingTimeMs": 500
}
```

### Performance Dataset (100k)
```json
{
  "totalSystemTransactions": 100000,
  "totalBankTransactions": 100000,
  "matchedCount": 95000,
  "unmatchedCount": 5000,
  "reconciliationRate": 95.0,
  "processingTimeMs": 8500
}
```

---

## 🔧 Troubleshooting

### Issue: File not found
```bash
# Make sure you're in the right directory
pwd
# Should show: /path/to/reconcile-service

# Check files exist
ls -lh sample-data/basic/
```

### Issue: Wrong date range
```bash
# Check transaction dates in CSV
head -5 sample-data/basic/system_transactions.csv

# Adjust date range accordingly
-F "startDate=2024-01-01" \
-F "endDate=2024-01-31"
```

---

**Last Updated:** 2026-01-11
**Total Datasets:** 20+ files
**Total Transactions:** 200,000+
