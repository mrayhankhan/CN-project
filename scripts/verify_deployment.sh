#!/bin/bash

# Deployment Verification Script
# Tests core endpoints for the paste service

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the backend URL from argument or use default
BACKEND_URL="${1:-http://localhost:8080}"

echo "=========================================="
echo "Deployment Verification Script"
echo "=========================================="
echo "Backend URL: $BACKEND_URL"
echo ""

# Counter for passed and failed tests
PASSED=0
FAILED=0

# Test function
test_endpoint() {
    local description="$1"
    local url="$2"
    local expected_status="$3"
    local extra_check="$4"
    
    echo -n "Testing: $description... "
    
    # Make request and capture response
    response=$(curl -i -s "$url" 2>&1)
    status_code=$(echo "$response" | grep "HTTP/" | head -1 | awk '{print $2}')
    
    if [ "$status_code" = "$expected_status" ]; then
        # Check extra validation if provided
        if [ -n "$extra_check" ]; then
            if echo "$response" | grep -q "$extra_check"; then
                echo -e "${GREEN}✓ PASS${NC}"
                ((PASSED++))
            else
                echo -e "${RED}✗ FAIL${NC} (Status OK but content validation failed)"
                ((FAILED++))
            fi
        else
            echo -e "${GREEN}✓ PASS${NC}"
            ((PASSED++))
        fi
    else
        echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status_code)"
        ((FAILED++))
    fi
}

echo "1. Testing Health Endpoint"
echo "------------------------------------------"
test_endpoint "Health check" "$BACKEND_URL/health" "200" "status"

echo ""
echo "2. Testing API Endpoints"
echo "------------------------------------------"
test_endpoint "History API" "$BACKEND_URL/api/history" "200" "\\["

echo ""
echo "3. Testing CORS Headers"
echo "------------------------------------------"
echo -n "Checking CORS headers... "
cors_headers=$(curl -i -s "$BACKEND_URL/api/history" | grep -i "access-control-allow-origin")
if [ -n "$cors_headers" ]; then
    echo -e "${GREEN}✓ PASS${NC}"
    echo "  $cors_headers"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC} (CORS headers not found)"
    ((FAILED++))
fi

echo ""
echo "4. Testing Create Endpoint"
echo "------------------------------------------"
echo -n "Creating test paste... "
create_response=$(curl -i -s -X POST "$BACKEND_URL/create" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "text=Deployment+test+paste" 2>&1)
    
create_status=$(echo "$create_response" | grep "HTTP/" | head -1 | awk '{print $2}')
location_header=$(echo "$create_response" | grep -i "^Location:" | head -1)

if [ "$create_status" = "303" ] && [ -n "$location_header" ]; then
    echo -e "${GREEN}✓ PASS${NC}"
    echo "  $location_header"
    ((PASSED++))
    
    # Extract paste ID from Location header
    paste_id=$(echo "$location_header" | sed 's/.*\/\([0-9]\{5\}\).*/\1/')
    
    if [ -n "$paste_id" ]; then
        echo ""
        echo "5. Testing Paste Retrieval"
        echo "------------------------------------------"
        test_endpoint "Get paste JSON" "$BACKEND_URL/api/$paste_id" "200" "\"id\""
    fi
else
    echo -e "${RED}✗ FAIL${NC} (Expected: 303 with Location header, Got: $create_status)"
    ((FAILED++))
fi

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo -e "Tests Passed: ${GREEN}$PASSED${NC}"
echo -e "Tests Failed: ${RED}$FAILED${NC}"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
