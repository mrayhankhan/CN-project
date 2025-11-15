#!/bin/bash

# WebSocket Connection Test Script
# Tests WebSocket handshake and basic connectivity

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get parameters
BACKEND_HOST="${1:-localhost:8080}"
PASTE_ID="${2:-00001}"

# Determine protocol (ws or wss)
if [[ "$BACKEND_HOST" == https://* ]]; then
    PROTOCOL="wss"
    HOST="${BACKEND_HOST#https://}"
elif [[ "$BACKEND_HOST" == http://* ]]; then
    PROTOCOL="ws"
    HOST="${BACKEND_HOST#http://}"
else
    # Assume plain host without protocol
    HOST="$BACKEND_HOST"
    if [[ "$HOST" == "localhost"* ]] || [[ "$HOST" == "127.0.0.1"* ]]; then
        PROTOCOL="ws"
    else
        PROTOCOL="wss"
    fi
fi

WS_URL="$PROTOCOL://$HOST/$PASTE_ID"

echo "=========================================="
echo "WebSocket Connection Test"
echo "=========================================="
echo "WebSocket URL: $WS_URL"
echo ""

# Check if wscat is available
if ! command -v wscat &> /dev/null; then
    echo -e "${YELLOW}Note: wscat is not installed${NC}"
    echo "To install wscat, run: npm install -g wscat"
    echo ""
    echo "Alternative manual test:"
    echo "1. Open browser console"
    echo "2. Run: const ws = new WebSocket('$WS_URL')"
    echo "3. Check for connection: ws.onopen = () => console.log('Connected!')"
    echo "4. Send message: ws.send('test')"
    echo ""
    exit 0
fi

echo "Testing WebSocket handshake..."
echo ""

# Test connection with timeout
timeout 5 wscat -c "$WS_URL" --no-color <<EOF &
hello from test
EOF

WS_PID=$!
sleep 2

if ps -p $WS_PID > /dev/null 2>&1; then
    echo -e "${GREEN}✓ WebSocket connection successful${NC}"
    kill $WS_PID 2>/dev/null || true
    exit 0
else
    echo -e "${RED}✗ WebSocket connection failed${NC}"
    exit 1
fi
