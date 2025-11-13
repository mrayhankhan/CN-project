#!/bin/bash
set -e

echo "Building Market Data Stream Normalizer..."

# Ensure we're in the project root
cd "$(dirname "$0")/.."

# Create build directory
mkdir -p build
cd build

# Configure with CMake
cmake .. \
  -DCMAKE_BUILD_TYPE=Debug \
  -DCMAKE_CXX_COMPILER=/usr/bin/clang++ \
  -DCMAKE_C_COMPILER=/usr/bin/clang \
  -DCMAKE_EXPORT_COMPILE_COMMANDS=TRUE

# Build all targets
make -j$(nproc)

echo ""
echo "Build complete!"
echo ""
echo "Executables:"
echo "  ./build/src/ingest_server"
echo "  ./build/src/replay_server"
echo "  ./build/src/feed_generator"
echo "  ./build/src/subscriber_client"
echo "  ./build/src/websocket_bridge"
echo ""
echo "Tests:"
echo "  cd build && make test"
echo "  or: cd build && ctest --output-on-failure"
