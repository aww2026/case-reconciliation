# Sample Datasets Summary

Quick reference for all available sample datasets.

---

## 📊 Available Datasets

### 1. Basic Scenario (✅ Complete)

**Location:** `sample-data/basic/`

| File | Transactions | Description |
|------|-------------|-------------|
| `system_transactions.csv` | 10 | Mixed DEBIT/CREDIT transactions |
| `bank_statement_bca.csv` | 8 | BCA statements (6 matched + 2 unmatched) |
| `bank_statement_mandiri.csv` | 5 | Mandiri statements (4 matched + 1 unmatched) |

**Match Rate:** ~77% (10 matched / 13 total bank)

**Test Command:**
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/basic/system_transactions.csv" \
  -F "bankFiles=@sample-data/basic/bank_statement_bca.csv" \
  -F "bankFiles=@sample-data/basic/bank_statement_mandiri.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-01-31"
```

---

### 2. Edge Cases (✅ Complete)

**Location:** `sample-data/edge-cases/`

| File | Transactions | Description |
|------|-------------|-------------|
| `system_with_duplicates.csv` | 10 | Same amount on same date (tests duplicate handling) |
| `bank_with_discrepancies.csv` | 6 | Small differences due to bank fees |

**Use Case:** Test matching logic with edge cases

**Test Scenario:**
- Duplicate amounts on same date
- Bank fees causing small discrepancies (500-2000 difference)

---

### 3. Multi-Bank Scenario (✅ Complete)

**Location:** `sample-data/multi-bank/`

| File | Transactions | Description |
|------|-------------|-------------|
| `system_q1_2024.csv` | 22 | Q1 2024 system transactions |
| `bca_q1.csv` | 8 | BCA statements Q1 |
| `mandiri_q1.csv` | 8 | Mandiri statements Q1 |
| `bni_q1.csv` | 6 | BNI statements Q1 |

**Match Rate:** 100% (all 22 system transactions matched)

**Test Command (Parallel Processing):**
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/multi-bank/system_q1_2024.csv" \
  -F "bankFiles=@sample-data/multi-bank/bca_q1.csv" \
  -F "bankFiles=@sample-data/multi-bank/mandiri_q1.csv" \
  -F "bankFiles=@sample-data/multi-bank/bni_q1.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-03-31"
```

**Expected:** Parallel processing with 3 bank files (3x faster)

---

### 4. Performance Testing (✅ Complete)

**Location:** `sample-data/performance/`

| File | Transactions | Size | Description |
|------|-------------|------|-------------|
| `system_transactions.csv` | 10,000 | ~1MB | Large system dataset |
| `bank_statements.csv` | ~1,600 | ~160KB | Bank statements (95% match rate) |

**Use Case:**
- Stress testing
- Benchmark parallel processing
- Memory usage testing

**Test Command:**
```bash
curl -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/performance/system_transactions.csv" \
  -F "bankFiles=@sample-data/performance/bank_statements.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-01-31"
```

**Expected Performance:**
- Processing time: <2 seconds
- Memory usage: <300MB
- Match rate: ~95%

---

## 🎯 Quick Test Scripts

### Test Basic Scenario
```bash
./sample-data/test_basic_scenario.sh
```

**What it does:**
1. Checks if server is running
2. Creates reconciliation job with basic dataset
3. Waits for completion
4. Displays results

---

### Generate Custom Dataset
```bash
./scripts/generate_sample_data.sh 1000 sample-data/custom --match-rate=0.90
```

**Parameters:**
- `1000` - Number of transactions
- `sample-data/custom` - Output directory
- `--match-rate=0.90` - 90% match rate (optional, default 0.95)

---

## 📈 Test Scenarios

### Scenario 1: Happy Path (100% Match)
```bash
# Use: multi-bank/system_q1_2024.csv + all bank files
# Expected: All 22 transactions matched
# Match rate: 100%
```

### Scenario 2: Partial Match
```bash
# Use: basic/* files
# Expected: 10 matched, 3 unmatched
# Match rate: 77%
```

### Scenario 3: Duplicates
```bash
# Use: edge-cases/system_with_duplicates.csv
# Expected: First occurrence matched, rest unmatched
# Tests: Duplicate handling logic
```

### Scenario 4: Discrepancies
```bash
# Use: edge-cases/bank_with_discrepancies.csv
# Expected: Exact match fails (need tolerance matching)
# Tests: Amount tolerance feature (when implemented)
```

### Scenario 5: Large Files
```bash
# Use: performance/* files (10k transactions)
# Expected: Completes in <2 seconds
# Tests: Performance, memory usage
```

### Scenario 6: Parallel Processing
```bash
# Use: multi-bank/* files (3+ bank files)
# Expected: 3x faster than sequential
# Tests: Parallel processing feature
```

---

## 🔍 Dataset Characteristics

### Distribution by Type

| Dataset | Total Trx | DEBIT | CREDIT | Banks | Match Rate |
|---------|-----------|-------|--------|-------|------------|
| Basic | 10 | 5 | 5 | 2 (BCA, Mandiri) | 77% |
| Edge Cases | 10 | 5 | 5 | 1 (BCA) | Varies |
| Multi-Bank Q1 | 22 | 11 | 11 | 3 (BCA, Mandiri, BNI) | 100% |
| Performance | 10,000 | ~5,000 | ~5,000 | 4 (Random) | 95% |

### File Sizes

```
basic/                  ~1.1 KB (tiny)
edge-cases/             ~700 B (tiny)
multi-bank/             ~2.2 KB (small)
performance/            ~1.2 MB (medium)
```

---

## 💡 Usage Tips

### 1. Start with Basic
For first-time testing, use basic dataset:
```bash
./sample-data/test_basic_scenario.sh
```

### 2. Test Parallel Processing
Use multi-bank dataset with 3 files:
```bash
# You should see parallel processing logs
# Processing time should be ~3x faster
```

### 3. Stress Test
Use performance dataset:
```bash
# Monitor memory usage:
# Should stay under 500MB
```

### 4. Generate Custom Data
Need specific scenario? Generate it:
```bash
# High match rate (99%)
./scripts/generate_sample_data.sh 5000 sample-data/high-match --match-rate=0.99

# Low match rate (50%)
./scripts/generate_sample_data.sh 5000 sample-data/low-match --match-rate=0.50
```

---

## 📁 Complete File List

```
sample-data/
├── README.md                                    # Main documentation
├── DATASETS_SUMMARY.md                          # This file
├── test_basic_scenario.sh                       # Quick test script
│
├── basic/
│   ├── system_transactions.csv                 # 10 system transactions
│   ├── bank_statement_bca.csv                  # 8 BCA statements
│   └── bank_statement_mandiri.csv              # 5 Mandiri statements
│
├── edge-cases/
│   ├── system_with_duplicates.csv              # Duplicate amounts/dates
│   └── bank_with_discrepancies.csv             # Bank fees (small diffs)
│
├── multi-bank/
│   ├── system_q1_2024.csv                      # 22 Q1 transactions
│   ├── bca_q1.csv                              # 8 BCA Q1
│   ├── mandiri_q1.csv                          # 8 Mandiri Q1
│   └── bni_q1.csv                              # 6 BNI Q1
│
└── performance/
    ├── system_transactions.csv                 # 10,000 transactions
    └── bank_statements.csv                     # ~1,600 statements
```

---

## 🚀 Next Steps

1. **Run Basic Test:**
   ```bash
   ./sample-data/test_basic_scenario.sh
   ```

2. **Try Multi-Bank:**
   ```bash
   # Copy the curl command from multi-bank section above
   ```

3. **Generate Large Dataset:**
   ```bash
   ./scripts/generate_sample_data.sh 100000 sample-data/stress-test
   ```

4. **Monitor Performance:**
   ```bash
   # Watch logs for parallel processing messages
   # Check memory usage during reconciliation
   ```

---

## 📊 Expected Results

### Basic Dataset
```json
{
  "totalSystemTransactions": 10,
  "totalBankTransactions": 13,
  "matchedCount": 10,
  "unmatchedCount": 3,
  "reconciliationRate": 76.9
}
```

### Multi-Bank Dataset
```json
{
  "totalSystemTransactions": 22,
  "totalBankTransactions": 22,
  "matchedCount": 22,
  "unmatchedCount": 0,
  "reconciliationRate": 100.0,
  "processingTimeMs": 150
}
```

### Performance Dataset
```json
{
  "totalSystemTransactions": 10000,
  "totalBankTransactions": 1600,
  "matchedCount": ~1520,
  "unmatchedCount": ~80,
  "reconciliationRate": 95.0,
  "processingTimeMs": <2000
}
```

---

**Total Datasets:** 13 CSV files
**Total Transactions:** 10,000+
**Ready to use:** ✅

