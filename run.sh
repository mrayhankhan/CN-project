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

# Create data directory if it doesn't exist
mkdir -p data

# Initialize counter file if it doesn't exist
if [ ! -f data/counter.txt ]; then
    echo "0" > data/counter.txt
fi

# Run the server
echo "Starting server on port 8080..."
echo "Access the application at: http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop the server"
echo "============================================"
echo ""

cd src
java MainServer
