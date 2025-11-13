#!/bin/bash

# Minimal Collaborative Paste Service - Build and Run Script

echo "============================================"
echo "Minimal Collaborative Paste Service"
echo "============================================"
echo ""

# Navigate to script directory
cd "$(dirname "$0")"

# Clean up old class files
echo "Cleaning old build files..."
rm -f src/*.class 2>/dev/null

# Compile Java source files
echo "Compiling Java source files..."
javac src/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo ""

# Initialize data directories and counter files
echo "Initializing data structures..."
mkdir -p data
mkdir -p src/data

# Initialize counter file in root data/ if it doesn't exist
if [ ! -f data/counter.txt ]; then
    echo "0" > data/counter.txt
    echo "✓ Created data/counter.txt with initial value 0"
fi

# Initialize counter file in src/data/ if it doesn't exist
if [ ! -f src/data/counter.txt ]; then
    echo "0" > src/data/counter.txt
    echo "✓ Created src/data/counter.txt with initial value 0"
fi

# Initialize history log file if it doesn't exist
if [ ! -f data/history.log ]; then
    touch data/history.log
    echo "✓ Created data/history.log"
fi

echo ""

# Run the server
echo "============================================"
echo ""

cd src
java MainServer
