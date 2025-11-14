#!/bin/bash

# Run All Automated Tests
# Executes all automated test suites

echo "=========================================="
echo "Automated Test Suite"
echo "=========================================="
echo ""
echo "Running all automated tests..."
echo ""

# Check if server is running
echo "Checking if server is running..."
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✓ Server is running on port 8080"
else
    echo "✗ ERROR: Server is not running!"
    echo ""
    echo "Please start the server first:"
    echo "  ./run.sh"
    echo ""
    exit 1
fi

echo ""

# Track test results
TOTAL_TESTS=0
PASSED_TESTS=0

# Run concurrency test
echo "=========================================="
echo "Test 1: Concurrency"
echo "=========================================="
echo ""

chmod +x test_concurrency.sh
if ./test_concurrency.sh; then
    echo "✓ Concurrency test PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "✗ Concurrency test FAILED"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""

# Run WebSocket reconnection test
echo "=========================================="
echo "Test 2: WebSocket Reconnection"
echo "=========================================="
echo ""

chmod +x test_websocket_reconnect.sh
if ./test_websocket_reconnect.sh; then
    echo "✓ WebSocket reconnection test PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "✗ WebSocket reconnection test FAILED"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""

# Summary
echo "=========================================="
echo "Test Suite Summary"
echo "=========================================="
echo ""
echo "Automated tests: $PASSED_TESTS out of $TOTAL_TESTS passed"
echo ""

# Manual tests reminder
echo "Manual Tests Available:"
echo "----------------------"
echo ""
echo "For additional testing, please review:"
echo "  • documentation/cross-browser-testing.md"
echo "  • documentation/websocket-testing.md"
echo "  • documentation/concurrency-testing.md"
echo "  • documentation/history-testing.md"
echo ""

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo "=========================================="
    echo "✓ All automated tests PASSED!"
    echo "=========================================="
    echo ""
    exit 0
else
    echo "=========================================="
    echo "✗ Some tests FAILED"
    echo "=========================================="
    echo ""
    echo "Check server logs: tail -f server.log"
    echo ""
    exit 1
fi
