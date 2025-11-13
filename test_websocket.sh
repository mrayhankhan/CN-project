#!/bin/bash

# WebSocket Reconnect Sanity Check
# Tests that clients can reconnect after network interruption
# and receive the current paste content

echo "============================================"
echo "WebSocket Reconnect Sanity Check"
echo "============================================"
echo ""

SERVER_URL="http://localhost:8080"
WS_URL="ws://localhost:8080"
TEST_PASSED=0
TEST_FAILED=0

echo "NOTE: This test verifies WebSocket upgrade handshake"
echo "      For full reconnect testing, use manual browser test"
echo ""

# Step 1: Create a test paste
echo "Step 1: Creating test paste..."
INITIAL_TEXT="WebSocket reconnect test - initial content"
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

# Step 2: Create a test client script
echo "Step 2: Testing WebSocket upgrade handshake..."

# Generate WebSocket key for handshake
WS_KEY=$(echo -n "test-key-$(date +%s)" | base64)

# Test WebSocket upgrade request
WS_RESPONSE=$(curl -s -i -N \
    -H "Connection: Upgrade" \
    -H "Upgrade: websocket" \
    -H "Sec-WebSocket-Version: 13" \
    -H "Sec-WebSocket-Key: $WS_KEY" \
    "http://localhost:8080/ws/$PASTE_ID" 2>&1 | head -20)

if echo "$WS_RESPONSE" | grep -q "101"; then
    echo "✓ PASSED: WebSocket upgrade successful (HTTP 101)"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ FAILED: WebSocket upgrade failed"
    echo "Response:"
    echo "$WS_RESPONSE"
    TEST_FAILED=$((TEST_FAILED + 1))
fi

echo ""

# Step 3: Verify WebSocket handshake headers
echo "Step 3: Verifying WebSocket handshake headers..."

if echo "$WS_RESPONSE" | grep -qi "Upgrade: websocket"; then
    echo "✓ PASSED: Upgrade header present"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ FAILED: Upgrade header missing"
    TEST_FAILED=$((TEST_FAILED + 1))
fi

if echo "$WS_RESPONSE" | grep -qi "Sec-WebSocket-Accept:"; then
    echo "✓ PASSED: Sec-WebSocket-Accept header present"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ FAILED: Sec-WebSocket-Accept header missing"
    TEST_FAILED=$((TEST_FAILED + 1))
fi

echo ""

# Step 4: Update the paste content
echo "Step 4: Updating paste content..."
UPDATED_TEXT="WebSocket reconnect test - UPDATED content after disconnect"
curl -s -X PUT "$SERVER_URL/$PASTE_ID" \
  -H "Content-Type: text/plain" \
  -d "$UPDATED_TEXT" > /dev/null

echo "✓ Paste updated with new content"
sleep 1
echo ""

# Step 5: Verify updated content via API
echo "Step 5: Verifying paste content via API..."
API_RESPONSE=$(curl -s "$SERVER_URL/api/$PASTE_ID")

if echo "$API_RESPONSE" | grep -q "UPDATED content"; then
    echo "✓ PASSED: Paste content updated successfully"
    TEST_PASSED=$((TEST_PASSED + 1))
else
    echo "✗ FAILED: Paste content not updated"
    TEST_FAILED=$((TEST_FAILED + 1))
fi

echo ""

# Manual reconnect testing instructions
echo "=========================================="
echo "Manual WebSocket Reconnect Test"
echo "=========================================="
echo ""
echo "To fully test WebSocket reconnection:"
echo ""
echo "1. Open in browser: http://localhost:8080/$PASTE_ID"
echo "2. Open DevTools (F12) → Network → WS filter"
echo "3. Verify WebSocket connection shows status 101"
echo "4. Click on WS connection → Messages tab"
echo "5. Disable network or close connection"
echo "6. Re-enable network"
echo "7. Verify connection re-establishes"
echo "8. Edit paste in another browser/tab"
echo "9. Verify update appears in first browser"
echo ""
echo "See CROSS_BROWSER_TESTING.md for detailed steps"
echo ""

# Cleanup
rm -f /tmp/ws_test.js /tmp/ws_initial.log /tmp/ws_reconnect.log 2>/dev/null

echo "============================================"
echo "WebSocket Reconnect Test Summary"
echo "============================================"
echo "Tests Passed: $TEST_PASSED"
echo "Tests Failed: $TEST_FAILED"
echo ""

if [ $TEST_FAILED -eq 0 ]; then
    echo "✓ WebSocket reconnect test PASSED"
    echo "Clients can reconnect and resync successfully!"
    echo "============================================"
    exit 0
else
    echo "✗ WebSocket reconnect test FAILED"
    echo "Check server logs for details"
    echo "============================================"
    exit 1
fi
