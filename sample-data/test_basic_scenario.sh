#!/bin/bash

# Test script for basic reconciliation scenario
echo "🧪 Testing Basic Reconciliation Scenario"
echo ""

# Check if server is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "❌ Server is not running on port 8080"
    echo "   Start it with: ./gradlew bootRun"
    exit 1
fi

echo "✅ Server is running"
echo ""

# Create reconciliation job
echo "📤 Creating reconciliation job..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/reconciliations \
  -F "systemFile=@sample-data/basic/system_transactions.csv" \
  -F "bankFiles=@sample-data/basic/bank_statement_bca.csv" \
  -F "bankFiles=@sample-data/basic/bank_statement_mandiri.csv" \
  -F "startDate=2024-01-01" \
  -F "endDate=2024-01-31")

echo "$RESPONSE" | jq '.'

# Extract job ID
JOB_ID=$(echo "$RESPONSE" | jq -r '.jobId')

if [ "$JOB_ID" = "null" ] || [ -z "$JOB_ID" ]; then
    echo "❌ Failed to create reconciliation job"
    exit 1
fi

echo ""
echo "✅ Job created with ID: $JOB_ID"
echo ""

# Wait for job to complete
echo "⏳ Waiting for job to complete..."
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    sleep 1
    ATTEMPT=$((ATTEMPT + 1))

    STATUS=$(curl -s http://localhost:8080/api/reconciliations/$JOB_ID | jq -r '.job.status')

    if [ "$STATUS" = "COMPLETED" ]; then
        echo "✅ Job completed!"
        break
    elif [ "$STATUS" = "FAILED" ]; then
        echo "❌ Job failed!"
        curl -s http://localhost:8080/api/reconciliations/$JOB_ID | jq '.job.errorMessage'
        exit 1
    fi

    echo "   Status: $STATUS (attempt $ATTEMPT/$MAX_ATTEMPTS)"
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "❌ Job did not complete within timeout"
    exit 1
fi

echo ""
echo "📊 Reconciliation Results:"
echo ""

# Get detailed results
curl -s http://localhost:8080/api/reconciliations/$JOB_ID | jq '{
  job: {
    id: .job.id,
    status: .job.status,
    matchedCount: .job.matchedCount,
    unmatchedCount: .job.unmatchedCount,
    totalSystemTransactions: .job.totalSystemTransactions,
    totalBankTransactions: .job.totalBankTransactions
  },
  summary: {
    matchedCount: .summary.matchedCount,
    unmatchedSystemCount: (.summary.unmatchedSystem | length),
    unmatchedBankCount: (.summary.unmatchedBankByBank | to_entries | map(.value | length) | add),
    totalDiscrepancy: .summary.totalDiscrepancy,
    reconciliationRate: .summary.reconciliationRate
  }
}'

echo ""
echo "✅ Test completed successfully!"
