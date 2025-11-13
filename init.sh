#!/bin/bash

# Initialization script for Minimal Collaborative Paste Service
# Ensures data directory and counter file exist with correct initial values

echo "============================================"
echo "Initializing Paste Service"
echo "============================================"
echo ""

# Navigate to script directory
cd "$(dirname "$0")"

# Create data directory if it doesn't exist
if [ ! -d "data" ]; then
    echo "Creating data/ directory..."
    mkdir -p data
    echo "✓ data/ directory created"
else
    echo "✓ data/ directory already exists"
fi

# Create counter file with initial value if it doesn't exist
if [ ! -f "data/counter.txt" ]; then
    echo "Creating counter.txt with initial value 0..."
    echo "0" > data/counter.txt
    echo "✓ counter.txt initialized with value 0 (next paste will be 00001)"
else
    CURRENT_COUNT=$(cat data/counter.txt 2>/dev/null | tr -d '\n' | tr -d '\r')
    echo "✓ counter.txt already exists (current value: $CURRENT_COUNT)"
fi

# Create src/data directory if it doesn't exist (for when running from src/)
if [ ! -d "src/data" ]; then
    echo "Creating src/data/ directory..."
    mkdir -p src/data
    echo "✓ src/data/ directory created"
else
    echo "✓ src/data/ directory already exists"
fi

# Create counter file in src/data if it doesn't exist
if [ ! -f "src/data/counter.txt" ]; then
    echo "Creating src/data/counter.txt with initial value 0..."
    echo "0" > src/data/counter.txt
    echo "✓ src/data/counter.txt initialized"
else
    echo "✓ src/data/counter.txt already exists"
fi

echo ""
echo "============================================"
echo "Initialization complete!"
echo "============================================"
echo ""
echo "You can now run the server with: ./run.sh"
