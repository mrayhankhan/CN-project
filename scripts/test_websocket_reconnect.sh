#!/bin/bash

# WebSocket Reconnection Test
# Tests that WebSocket clients can reconnect after network interruption

echo "=========================================="
echo "WebSocket Reconnection Test"
echo "=========================================="
echo ""

# Check if server is running
if ! curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✗ ERROR: Server is not running on port 8080"
    echo ""
    echo "Please start the server first:"
    echo "  ./run.sh"
    echo ""
    exit 1
fi

echo "✓ Server is running"
echo ""

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "⚠️  Node.js not found - using manual test instructions"
    echo ""
    echo "Manual WebSocket Reconnection Test:"
    echo "===================================="
    echo ""
    echo "1. Create a paste at http://localhost:8080"
    echo "2. Open browser DevTools (F12) → Network tab → WS filter"
    echo "3. Note the WebSocket connection status (101 Switching Protocols)"
    echo "4. Toggle DevTools 'Offline' mode"
    echo "5. Wait 2-3 seconds"
    echo "6. Toggle 'Offline' mode back to online"
    echo "7. Edit the paste in another browser tab"
    echo "8. Verify the reconnected tab receives the update"
    echo ""
    echo "Result: Manual verification required"
    exit 0
fi

echo "✓ Node.js is available"
echo ""

# Create test paste
echo "Creating test paste..."
RESPONSE=$(curl -s -X POST http://localhost:8080/create -d "WebSocket reconnect test - initial content" -D -)
PASTE_ID=$(echo "$RESPONSE" | grep -i "Location:" | sed 's/.*\///' | tr -d '\r\n ')

if [ -z "$PASTE_ID" ]; then
    echo "✗ Failed to create test paste"
    exit 1
fi

echo "✓ Created test paste: $PASTE_ID"
echo ""

# Create WebSocket test client
WS_TEST_FILE="/tmp/ws_test_$$.js"
cat > "$WS_TEST_FILE" << 'EOF'
const WebSocket = require('ws');

const pasteId = process.argv[2];
const wsUrl = `ws://localhost:8080/ws/${pasteId}`;

console.log('Connecting to WebSocket...');
let ws = new WebSocket(wsUrl);

ws.on('open', () => {
    console.log('✓ WebSocket connected');
    
    setTimeout(() => {
        console.log('Disconnecting...');
        ws.close();
        
        setTimeout(() => {
            console.log('Reconnecting...');
            ws = new WebSocket(wsUrl);
            
            ws.on('open', () => {
                console.log('✓ Reconnected successfully');
                process.exit(0);
            });
            
            ws.on('error', (err) => {
                console.log('✗ Reconnection failed:', err.message);
                process.exit(1);
            });
        }, 1000);
    }, 1000);
});

ws.on('error', (err) => {
    console.log('✗ Connection failed:', err.message);
    process.exit(1);
});

setTimeout(() => {
    console.log('✗ Test timeout');
    process.exit(1);
}, 10000);
EOF

# Run WebSocket test
echo "Testing WebSocket reconnection..."
if node "$WS_TEST_FILE" "$PASTE_ID" 2>&1; then
    echo ""
    echo "✓ WebSocket reconnection test PASSED"
    rm -f "$WS_TEST_FILE"
    exit 0
else
    echo ""
    echo "✗ WebSocket reconnection test FAILED"
    rm -f "$WS_TEST_FILE"
    exit 1
fi
