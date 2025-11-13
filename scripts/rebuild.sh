#!/bin/bash
set -e

echo "Clean rebuild of Market Data Stream Normalizer..."

# Remove and recreate build directory
cd /workspaces/CN-project
rm -rf build
bash scripts/build.sh

echo ""
echo "Running tests..."
cd build
make test
ctest --output-on-failure
