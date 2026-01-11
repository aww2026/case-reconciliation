#!/bin/bash

# Sample Data Generator for Reconciliation Service
# Usage: ./generate_sample_data.sh <row_count> <output_dir> [--match-rate=0.95]

set -e

# Default values
ROW_COUNT=${1:-1000}
OUTPUT_DIR=${2:-"sample-data/generated"}
MATCH_RATE=0.95

# Parse optional arguments
for arg in "$@"; do
    case $arg in
        --match-rate=*)
        MATCH_RATE="${arg#*=}"
        shift
        ;;
    esac
done

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "🎲 Generating sample data..."
echo "  Rows: $ROW_COUNT"
echo "  Output: $OUTPUT_DIR"
echo "  Match rate: $MATCH_RATE"

# Generate system transactions
SYSTEM_FILE="$OUTPUT_DIR/system_transactions.csv"
echo "trxID,amount,type,transactionTime" > "$SYSTEM_FILE"

echo "📝 Generating system transactions..."
for i in $(seq 1 $ROW_COUNT); do
    TRX_ID=$(printf "TRX%06d" $i)

    # Random amount between 100k and 50M
    AMOUNT=$((RANDOM % 49900000 + 100000))

    # Random type (50% DEBIT, 50% CREDIT)
    if [ $((RANDOM % 2)) -eq 0 ]; then
        TYPE="DEBIT"
    else
        TYPE="CREDIT"
    fi

    # Random date in January 2024
    DAY=$((RANDOM % 31 + 1))
    HOUR=$((RANDOM % 24))
    MINUTE=$((RANDOM % 60))
    DATE=$(printf "2024-01-%02d" $DAY)
    TIME=$(printf "%02d:%02d:00" $HOUR $MINUTE)

    echo "$TRX_ID,$AMOUNT,$TYPE,${DATE}T${TIME}" >> "$SYSTEM_FILE"
done

# Generate bank statements (with controlled match rate)
BANK_FILE="$OUTPUT_DIR/bank_statements.csv"
echo "uniqueIdentifier,amount,date,bankName" > "$BANK_FILE"

echo "🏦 Generating bank statements..."

# Matched transactions
MATCHED_COUNT=$(echo "$ROW_COUNT * $MATCH_RATE" | bc | cut -d. -f1)

for i in $(seq 1 $MATCHED_COUNT); do
    # Read corresponding system transaction
    LINE=$((i + 1))  # +1 for header
    SYS_LINE=$(sed "${LINE}q;d" "$SYSTEM_FILE")

    TRX_ID=$(echo "$SYS_LINE" | cut -d, -f1)
    AMOUNT=$(echo "$SYS_LINE" | cut -d, -f2)
    TYPE=$(echo "$SYS_LINE" | cut -d, -f3)
    DATETIME=$(echo "$SYS_LINE" | cut -d, -f4)
    DATE=$(echo "$DATETIME" | cut -dT -f1)

    # Convert to signed amount (DEBIT=negative, CREDIT=positive)
    if [ "$TYPE" = "DEBIT" ]; then
        SIGNED_AMOUNT="-$AMOUNT"
    else
        SIGNED_AMOUNT="$AMOUNT"
    fi

    # Random bank
    BANKS=("BCA" "Mandiri" "BNI" "BRI")
    BANK=${BANKS[$((RANDOM % 4))]}

    # Bank unique identifier
    BANK_ID="${BANK}$(date +%Y%m%d -d "$DATE" | tr -d '-')$(printf '%03d' $i)"

    echo "$BANK_ID,$SIGNED_AMOUNT,$DATE,$BANK" >> "$BANK_FILE"
done

# Unmatched bank statements
UNMATCHED_COUNT=$((ROW_COUNT - MATCHED_COUNT))

for i in $(seq 1 $UNMATCHED_COUNT); do
    # Random unique ID
    BANK_ID="UNM$(printf '%08d' $i)"

    # Random amount
    AMOUNT=$((RANDOM % 10000000 + 100000))
    SIGNED_AMOUNT=$((RANDOM % 2 == 0 ? AMOUNT : -AMOUNT))

    # Random date
    DAY=$((RANDOM % 31 + 1))
    DATE=$(printf "2024-01-%02d" $DAY)

    # Random bank
    BANKS=("BCA" "Mandiri" "BNI" "BRI")
    BANK=${BANKS[$((RANDOM % 4))]}

    echo "$BANK_ID,$SIGNED_AMOUNT,$DATE,$BANK" >> "$BANK_FILE"
done

# Statistics
SYSTEM_SIZE=$(wc -l < "$SYSTEM_FILE" | tr -d ' ')
BANK_SIZE=$(wc -l < "$BANK_FILE" | tr -d ' ')
SYSTEM_SIZE=$((SYSTEM_SIZE - 1))  # Exclude header
BANK_SIZE=$((BANK_SIZE - 1))

echo ""
echo "✅ Sample data generated successfully!"
echo ""
echo "📊 Statistics:"
echo "  System transactions: $SYSTEM_SIZE"
echo "  Bank statements: $BANK_SIZE"
echo "  Expected matches: ~$MATCHED_COUNT"
echo "  Expected unmatched: ~$UNMATCHED_COUNT"
echo ""
echo "📁 Files created:"
echo "  $SYSTEM_FILE"
echo "  $BANK_FILE"
echo ""
echo "🚀 Test with:"
echo "  curl -X POST http://localhost:8080/api/reconciliations \\"
echo "    -F \"systemFile=@$SYSTEM_FILE\" \\"
echo "    -F \"bankFiles=@$BANK_FILE\" \\"
echo "    -F \"startDate=2024-01-01\" \\"
echo "    -F \"endDate=2024-01-31\""
