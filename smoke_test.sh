#!/bin/bash

# Smoke Test Script for Minimal Collaborative Paste Service
# Tests basic functionality: POST to /create and GET to view paste

echo "============================================"
echo "Smoke Test - Basic Functionality"
echo "============================================"
echo ""

SERVER_URL="http://localhost:8080"
TEST_PASSED=0
TEST_FAILED=0

# Test 1: POST to /create
echo "Test 1: POST to /create"
echo "--------------------------------------------"
TEST_TEXT="Hello from smoke test!"
echo "Sending POST request with text: '$TEST_TEXT'"

# POST returns a redirect (303) with Location header containing the paste ID
# Use -i to get headers, -L would follow redirect but we just need the ID
RESPONSE=$(curl -s -i -X POST "$SERVER_URL/create" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=$TEST_TEXT")

echo "Response headers received"

# Extract paste ID from Location header (format: Location: /00001)
PASTE_ID=$(echo "$RESPONSE" | grep -i "^Location:" | sed 's/.*\/\([0-9]*\).*/\1/' | tr -d '\r\n ')

if [ -z "$PASTE_ID" ]; then
    echo "✗ FAILED: Could not extract paste ID from Location header"
    echo "Full response:"
    echo "$RESPONSE"
    TEST_FAILED=$((TEST_FAILED + 1))
    echo ""
    echo "============================================"
    echo "Smoke Test Result: FAILED"
    echo "============================================"
    exit 1
else
    echo "✓ PASSED: Created paste with ID: $PASTE_ID"
    TEST_PASSED=$((TEST_PASSED + 1))
fi

echo ""

# Test 2: GET to view paste
echo "Test 2: GET to view paste (JSON API)"
echo "--------------------------------------------"
echo "Fetching paste ID: $PASTE_ID via /api/$PASTE_ID"

GET_RESPONSE=$(curl -s "$SERVER_URL/api/$PASTE_ID")

if [ -z "$GET_RESPONSE" ]; then
    echo "✗ FAILED: Empty response from GET request"
    TEST_FAILED=$((TEST_FAILED + 1))
elif echo "$GET_RESPONSE" | grep -q "$PASTE_ID"; then
    echo "Response: $GET_RESPONSE"
    echo "✓ PASSED: Successfully retrieved paste with ID $PASTE_ID"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ FAILED: Response does not contain expected paste ID"
    echo "Expected ID: '$PASTE_ID'"
    echo "Got: $(echo "$GET_RESPONSE" | head -c 100)..."
    TEST_FAILED=$((TEST_FAILED + 1))
fi

echo ""
echo "============================================"
echo "Smoke Test Summary"
echo "============================================"
echo "Tests Passed: $TEST_PASSED"
echo "Tests Failed: $TEST_FAILED"
echo ""

if [ $TEST_FAILED -eq 0 ]; then
    echo "✓ All smoke tests PASSED"
    echo "Server is working correctly!"
    echo "============================================"
    exit 0
else
    echo "✗ Some tests FAILED"
    echo "Please check the server logs"
    echo "============================================"
    exit 1
fi
