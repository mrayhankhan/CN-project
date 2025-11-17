# Testing Guide

## Overview

This document provides a comprehensive guide to all available tests for the Minimal Collaborative Paste Service.

---

## Quick Start

### Run All Automated Tests

```bash
chmod +x run_all_tests.sh
./run_all_tests.sh
```

This will execute:
- Concurrency tests
- WebSocket reconnection tests

---

## Individual Test Scripts

### 1. Smoke Test

**Purpose**: Basic functionality verification

**Script**: `smoke_test.sh`

**What it tests**:
- Server is responsive
- Can create a paste
- Can retrieve paste via API

**Run**:
```bash
chmod +x smoke_test.sh
./smoke_test.sh
```

---

### 2. Concurrency Test

**Purpose**: Verify thread-safe concurrent operations

**Script**: `test_concurrency.sh`

**What it tests**:
- Multiple simultaneous writes to same paste
- Per-ID locking mechanism
- Data integrity under load
- No file corruption

**Run**:
```bash
chmod +x test_concurrency.sh
./test_concurrency.sh
```

**See**: `documentation/concurrency-testing.md` for detailed guide

---

### 3. WebSocket Reconnection Test

**Purpose**: Verify WebSocket client reconnection

**Script**: `test_websocket_reconnect.sh`

**What it tests**:
- WebSocket connection establishment
- Client disconnection handling
- Client reconnection capability
- Content synchronization after reconnect

**Run**:
```bash
chmod +x test_websocket_reconnect.sh
./test_websocket_reconnect.sh
```

**See**: `documentation/websocket-testing.md` for detailed guide

---

## Manual Testing Guides

### Cross-Browser Testing

**Documentation**: `documentation/cross-browser-testing.md`

**What to test**:
- Real-time updates across browsers (Chrome, Firefox, etc.)
- Mobile/Desktop cross-platform collaboration
- Network interruption recovery
- WebSocket connection verification
- Multiple simultaneous users (3+)

**When to use**: Before deploying to production or after significant changes

---

### WebSocket Testing

**Documentation**: `documentation/websocket-testing.md`

**What to test**:
- Basic real-time updates
- Multiple simultaneous users
- Connection monitoring in DevTools
- Network interruption recovery
- WebSocket frame inspection

**When to use**: When working on real-time collaboration features

---

### Concurrency Testing

**Documentation**: `documentation/concurrency-testing.md`

**What to test**:
- Simultaneous multi-user edits
- Rapid sequential updates
- Different pastes concurrent access
- Lock contention scenarios
- WebSocket + HTTP concurrent updates

**When to use**: Before deploying or after modifying storage/locking code

---

### History Feature Testing

**Documentation**: `documentation/history-testing.md`

**What to test**:
- History entry creation
- History persistence
- Backfill functionality
- Concurrent history writes
- History file integrity

**When to use**: When working on history tracking features

---

## Maintenance Scripts

### History Backfill

**Script**: `backfill_history.sh`

**Purpose**: Create history entries for existing pastes

**Run**:
```bash
chmod +x backfill_history.sh
./backfill_history.sh
```

**When to use**: After upgrading from version without history tracking

---

### Cleanup Old Files

**Script**: `cleanup_old_files.sh`

**Purpose**: Remove deprecated phase-based documentation

**Run**:
```bash
chmod +x cleanup_old_files.sh
./cleanup_old_files.sh
```

**When to use**: After upgrading to new documentation structure

---

## Test Checklist

Use this checklist before releasing:

### Automated Tests
- [ ] Smoke test passes
- [ ] Concurrency test passes
- [ ] WebSocket reconnection test passes

### Manual Verification
- [ ] Cross-browser updates work (Chrome ↔ Firefox)
- [ ] Mobile/Desktop collaboration works
- [ ] Network interruption recovery works
- [ ] WebSocket connections visible in DevTools
- [ ] Multiple users (3+) can collaborate

### Load Testing
- [ ] Server handles 100+ concurrent requests
- [ ] No data corruption under load
- [ ] Reasonable response times

### History Feature
- [ ] History entries created for new pastes
- [ ] History file is valid JSON
- [ ] Backfill works for existing pastes

---

## Troubleshooting

### Server Not Running

```bash
# Check if server is running
curl http://localhost:8080

# If not, start it
./run.sh
```

### Tests Failing

1. **Check server logs**:
```bash
tail -f server.log
```

2. **Verify data directory**:
```bash
ls -la data/
```

3. **Clean and restart**:
```bash
# Stop server (Ctrl+C)
# Remove test data if needed
rm -f data/test_*.json
# Restart
./run.sh
```

### Test Scripts Not Executable

```bash
# Make all scripts executable
chmod +x *.sh
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Start Server
        run: |
          ./run.sh &
          sleep 5
      
      - name: Run Tests
        run: ./run_all_tests.sh
      
      - name: Check Logs
        if: failure()
        run: cat server.log
```

---

## Performance Benchmarking

### Using Apache Bench

```bash
# Install apache bench
sudo apt-get install apache2-utils

# Create test paste
PASTE_ID=$(curl -X POST http://localhost:8080/create \
  -d "Benchmark test" -s -D - | \
  grep Location | sed 's/.*\///' | tr -d '\r')

# Run benchmark - 1000 requests, 10 concurrent
ab -n 1000 -c 10 -p payload.txt -T 'text/plain' \
  http://localhost:8080/$PASTE_ID

# Check results
echo "Paste ID: $PASTE_ID"
curl http://localhost:8080/$PASTE_ID
```

---

## Documentation Index

All documentation files are in the `documentation/` folder:

1. **websocket-testing.md** - WebSocket and real-time collaboration tests
2. **concurrency-testing.md** - Multi-user and concurrent access tests
3. **cross-browser-testing.md** - Browser compatibility verification
4. **history-testing.md** - Paste history feature tests
5. **testing-guide.md** - This file (comprehensive testing overview)

---

## Getting Help

- Check the main README.md for project overview
- Review specific documentation for detailed guides
- Check server.log for error messages
- Verify all dependencies are installed (Java, curl, optional: Node.js)
