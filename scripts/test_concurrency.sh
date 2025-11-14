#!/bin/bash

# Concurrency Smoke Test - Tests per-ID locking
# Issues multiple concurrent PUT requests to the same paste ID
# Verifies that the final result is valid JSON and not corrupted

echo "============================================"
echo "Concurrency Smoke Test - Per-ID Locking"
echo "============================================"
echo ""

SERVER_URL="http://localhost:8080"
TEST_PASSED=0
TEST_FAILED=0

# Step 1: Create a test paste
echo "Step 1: Creating initial test paste..."
INITIAL_TEXT="Initial content for concurrency test"
RESPONSE=$(curl -s -i -X POST "$SERVER_URL/create" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=$INITIAL_TEXT")

PASTE_ID=$(echo "$RESPONSE" | grep -i "^Location:" | sed 's/.*\/\([0-9]*\).*/\1/' | tr -d '\r\n ')

if [ -z "$PASTE_ID" ]; then
    echo "✗ FAILED: Could not create test paste"
    exit 1
fi

echo "✓ Created test paste with ID: $PASTE_ID"
echo ""

# Step 2: Launch concurrent PUT requests
echo "Step 2: Launching 10 concurrent PUT requests..."
NUM_REQUESTS=10
PIDS=()

for i in $(seq 1 $NUM_REQUESTS); do
    TEXT="Update $i from concurrent request at $(date +%s%N)"
    (curl -s -X PUT "$SERVER_URL/$PASTE_ID" \
      -H "Content-Type: text/plain" \
      -d "$TEXT" > /dev/null 2>&1) &
    PIDS+=($!)
done

# Wait for all requests to complete
echo "Waiting for all requests to complete..."
for pid in "${PIDS[@]}"; do
    wait $pid
done

echo "✓ All $NUM_REQUESTS concurrent requests completed"
echo ""

# Step 3: Verify the paste is still valid
echo "Step 3: Verifying paste integrity..."
sleep 1  # Brief pause to ensure writes are flushed

# Fetch the paste via API
FINAL_RESPONSE=$(curl -s "$SERVER_URL/api/$PASTE_ID")

if [ -z "$FINAL_RESPONSE" ]; then
    echo "✗ FAILED: Empty response after concurrent writes"
    TEST_FAILED=$((TEST_FAILED + 1))
else
    # Check if response is valid JSON
    echo "$FINAL_RESPONSE" | python3 -m json.tool > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✓ PASSED: Response is valid JSON"
        TEST_PASSED=$((TEST_PASSED + 1))
        
        # Display the final content
        echo ""
        echo "Final paste content:"
        echo "$FINAL_RESPONSE" | python3 -m json.tool
    else
        echo "✗ FAILED: Response is not valid JSON (possible corruption)"
        echo "Response: $FINAL_RESPONSE"
        TEST_FAILED=$((TEST_FAILED + 1))
    fi
fi

echo ""

# Step 4: Check that the data file is not corrupted
echo "Step 4: Checking data file integrity..."
DATA_FILE="data/$PASTE_ID.json"

if [ -f "$DATA_FILE" ]; then
    cat "$DATA_FILE" | python3 -m json.tool > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✓ PASSED: Data file is valid JSON"
        TEST_PASSED=$((TEST_PASSED + 1))
    else
        echo "✗ FAILED: Data file is corrupted"
        echo "File contents:"
        cat "$DATA_FILE"
        TEST_FAILED=$((TEST_FAILED + 1))
    fi
else
    echo "✗ FAILED: Data file not found at $DATA_FILE"
    TEST_FAILED=$((TEST_FAILED + 1))
fi

echo ""

# Step 5: Verify no .tmp files left behind
echo "Step 5: Checking for leftover temp files..."
TEMP_FILES=$(find data -name "*.tmp" 2>/dev/null | wc -l)
if [ $TEMP_FILES -eq 0 ]; then
    echo "✓ PASSED: No temporary files left behind"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ WARNING: Found $TEMP_FILES temporary file(s)"
    find data -name "*.tmp"
    # Don't fail the test for this, just warn
fi

echo ""
echo "============================================"
echo "Concurrency Test Summary"
echo "============================================"
echo "Tests Passed: $TEST_PASSED"
echo "Tests Failed: $TEST_FAILED"
echo ""

if [ $TEST_FAILED -eq 0 ]; then
    echo "✓ Concurrency test PASSED"
    echo "Per-ID locking is working correctly!"
    echo "============================================"
    exit 0
else
    echo "✗ Concurrency test FAILED"
    echo "Check server logs for details"
    echo "============================================"
    exit 1
fi
