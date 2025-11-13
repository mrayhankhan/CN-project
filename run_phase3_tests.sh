#!/bin/bash

# Phase 3 Test Suite - Run all essential tests
# Tests concurrency, WebSocket reconnect, and provides manual testing guide

echo "=========================================="
echo "Phase 3 Essential Testing Suite"
echo "=========================================="
echo ""
echo "This suite includes:"
echo "  1. Concurrency smoke test (per-ID locking)"
echo "  2. WebSocket reconnect sanity check"
echo "  3. Cross-browser manual verification guide"
echo ""

# Check if server is running
echo "Checking if server is running..."
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✓ Server is running on port 8080"
else
    echo "✗ ERROR: Server is not running!"
    echo ""
    echo "Please start the server first:"
    echo "  cd /workspaces/CN-project"
    echo "  ./run.sh"
    echo ""
    exit 1
fi

echo ""
echo "=========================================="
echo "Test 1: Concurrency Smoke Test"
echo "=========================================="
echo ""

chmod +x test_concurrency.sh
./test_concurrency.sh
CONCURRENCY_RESULT=$?

echo ""
echo "=========================================="
echo "Test 2: WebSocket Reconnect Test"
echo "=========================================="
echo ""

chmod +x test_websocket.sh
./test_websocket.sh
WEBSOCKET_RESULT=$?

echo ""
echo "=========================================="
echo "Test 3: Cross-Browser Manual Verification"
echo "=========================================="
echo ""
echo "For cross-browser testing, please refer to:"
echo "  CROSS_BROWSER_TESTING.md"
echo ""
echo "Quick summary:"
echo "  1. Open paste in 2+ different browsers"
echo "  2. Edit in one browser"
echo "  3. Verify update appears in other browser(s)"
echo "  4. Test on mobile device or emulator if possible"
echo ""
echo "✓ Manual testing guide available"
echo ""

# Summary
echo "=========================================="
echo "Phase 3 Test Suite Summary"
echo "=========================================="
echo ""

TOTAL_TESTS=3
PASSED_TESTS=0

if [ $CONCURRENCY_RESULT -eq 0 ]; then
    echo "✓ Concurrency Test: PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "✗ Concurrency Test: FAILED"
fi

if [ $WEBSOCKET_RESULT -eq 0 ]; then
    echo "✓ WebSocket Reconnect Test: PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "✗ WebSocket Reconnect Test: FAILED"
fi

echo "○ Cross-Browser Test: MANUAL (see CROSS_BROWSER_TESTING.md)"
echo ""
echo "Automated tests passed: $PASSED_TESTS out of 2"
echo ""

if [ $PASSED_TESTS -eq 2 ]; then
    echo "=========================================="
    echo "✓ All automated tests PASSED!"
    echo "=========================================="
    echo ""
    echo "Next steps:"
    echo "  1. Review CROSS_BROWSER_TESTING.md"
    echo "  2. Perform manual cross-browser verification"
    echo "  3. Test on mobile device if possible"
    echo ""
    exit 0
else
    echo "=========================================="
    echo "✗ Some automated tests FAILED"
    echo "=========================================="
    echo ""
    echo "Please check:"
    echo "  - Server logs: cat server.log"
    echo "  - Individual test output above"
    echo ""
    exit 1
fi
