# Concurrency and Multi-User Testing

## Overview

This guide covers testing the server's ability to handle concurrent requests and multiple simultaneous users safely without data corruption.

---

## Automated Concurrency Test

**Script**: `test_concurrency.sh`

### Purpose
Verify that the per-ID locking mechanism prevents data corruption when multiple requests modify the same paste simultaneously.

### What It Tests
- Concurrent write operations to the same paste
- Per-ID locking mechanism (ReentrantLock)
- Data integrity after concurrent modifications
- JSON file validity
- Temporary file cleanup

### Running the Test

```bash
chmod +x test_concurrency.sh
./test_concurrency.sh
```

### Test Procedure
1. Creates a test paste with initial content
2. Launches 10 concurrent PUT requests to the same paste ID
3. Each request sends different content
4. Verifies the final JSON file is valid (not corrupted)
5. Checks no `.tmp` files are left behind
6. Validates data file integrity

### Expected Result
✅ All tests pass - JSON is valid, no corruption, no temporary files

### What It Verifies
- **Atomicity**: Each write completes fully or not at all
- **Isolation**: Concurrent writes don't interfere with each other
- **Consistency**: Final data is valid JSON
- **Durability**: Changes are persisted to disk correctly

---

## Manual Concurrency Testing

### Test 1: Simultaneous Multi-User Edits

#### Setup
1. Start the server: `./run.sh`
2. Create a new paste
3. Open the paste URL in 3+ browser tabs

#### Procedure
1. In all tabs simultaneously, start typing different content
2. Save changes rapidly from multiple tabs
3. Reload all tabs
4. **Verify**: No data corruption or partial writes
5. **Note**: Last-write-wins behavior is expected

**Result**: ✅ PASS / ❌ FAIL

---

### Test 2: Rapid Sequential Updates

#### Procedure
1. Open a paste in one browser tab
2. Using curl, send rapid updates:
```bash
for i in {1..20}; do
  curl -X PUT http://localhost:8080/00001 -d "Update $i" &
done
wait
```
3. Reload the paste in browser
4. **Verify**: Content is valid (one of the updates)
5. Check data file: `cat data/00001.json`
6. **Verify**: Valid JSON structure

**Result**: ✅ PASS / ❌ FAIL

---

### Test 3: Different Pastes Concurrent Access

#### Purpose
Verify that updates to different pastes can happen in parallel without blocking each other.

#### Procedure
1. Create 3 pastes (00001, 00002, 00003)
2. Open each in a separate browser tab
3. Edit all three simultaneously
4. **Verify**: All updates succeed without delays
5. **Verify**: No paste blocks another

**Result**: ✅ PASS / ❌ FAIL

---

### Test 4: Lock Contention Test

#### Setup
Create a script to hammer a single paste:

```bash
#!/bin/bash
PASTE_ID="00001"
for i in {1..50}; do
  curl -X PUT "http://localhost:8080/$PASTE_ID" \
       -d "Concurrent write $i - $(date +%s%N)" &
done
wait
echo "All requests completed"
```

#### Procedure
1. Create test paste
2. Run the script above
3. Wait for completion
4. Check the paste: `curl http://localhost:8080/00001`
5. **Verify**: Valid JSON response
6. Check file: `cat data/00001.json`
7. **Verify**: Valid JSON, no corruption

**Result**: ✅ PASS / ❌ FAIL

---

### Test 5: WebSocket + HTTP Concurrent Updates

#### Purpose
Test interaction between WebSocket and HTTP updates happening simultaneously.

#### Procedure
1. Open paste in browser (WebSocket connected)
2. Start editing in browser
3. While editing, send HTTP PUT requests:
```bash
while true; do
  curl -X PUT http://localhost:8080/00001 -d "HTTP update $(date +%s)"
  sleep 0.5
done
```
4. Continue editing in browser
5. Stop curl after 10-20 iterations
6. **Verify**: No errors in browser console
7. **Verify**: Paste content is valid
8. **Verify**: No data corruption

**Result**: ✅ PASS / ❌ FAIL

---

## Data Integrity Verification

### Checking for Corruption

#### JSON Validation
```bash
# Validate all paste files
for file in data/*.json; do
  if ! jq empty "$file" 2>/dev/null; then
    echo "CORRUPTED: $file"
  fi
done
```

#### Temporary File Cleanup
```bash
# Check for leftover temporary files
ls -la data/*.tmp 2>/dev/null
# Should return "No such file or directory"
```

#### File Size Sanity Check
```bash
# Check for unusually large or empty files
find data -name "*.json" -size 0
find data -name "*.json" -size +10M
```

---

## Load Testing

### Stress Test with Apache Bench

```bash
# Install apache bench (if needed)
# sudo apt-get install apache2-utils

# Create a test paste first
PASTE_ID=$(curl -X POST http://localhost:8080/create -d "Load test paste" -s -D - | grep Location | sed 's/.*\///' | tr -d '\r')

# Run load test - 1000 requests, 10 concurrent
ab -n 1000 -c 10 -p payload.txt -T 'text/plain' http://localhost:8080/$PASTE_ID

# Check results
cat data/$PASTE_ID.json
```

Create `payload.txt`:
```
Load test content - timestamp: current
```

**Verify**:
- All requests complete successfully
- Response times are reasonable
- No errors in server.log
- Final paste content is valid

---

## Troubleshooting

### Data Corruption Detected

**Symptoms**: Invalid JSON in data files

**Investigation**:
1. Check server.log for stack traces
2. Verify atomic write implementation
3. Check disk space: `df -h`
4. Verify file permissions: `ls -la data/`

**Fix**:
- Restore from backup if available
- Delete corrupted file to reset

---

### Temporary Files Not Cleaned Up

**Symptoms**: `.tmp` files remain in data directory

**Investigation**:
1. Check server.log for write failures
2. Verify atomic rename operation
3. Check for server crashes during writes

**Fix**:
```bash
# Clean up manually
rm data/*.tmp
```

---

### Lock Contention Issues

**Symptoms**: Slow response times under concurrent load

**Investigation**:
1. Check server logs for timeouts
2. Monitor CPU usage during tests
3. Verify per-ID locking is working

**Optimization**:
- Already implemented: Different pastes don't block each other
- Expected: Same paste updates are serialized (correct behavior)

---

## Testing Checklist

- [ ] Automated concurrency test passes
- [ ] Multiple simultaneous edits work correctly
- [ ] Rapid sequential updates succeed
- [ ] Different pastes update in parallel
- [ ] High lock contention handled safely
- [ ] WebSocket + HTTP concurrent updates work
- [ ] No JSON corruption after stress test
- [ ] No temporary files left behind
- [ ] Load testing shows acceptable performance
- [ ] Server logs show no errors

---

## Technical Details

### Concurrency Mechanism

**Per-ID Locking**:
```java
private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

public static void save(String id, String text) {
    ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
    lock.lock();
    try {
        // Write to temporary file
        // Atomic rename
    } finally {
        lock.unlock();
    }
}
```

**Benefits**:
- Different pastes can be modified in parallel
- Same paste modifications are serialized
- No global lock - maximizes concurrency
- Thread-safe lock management

### Atomic Write Process

1. Write content to `{id}.json.tmp`
2. Flush all buffers
3. Atomically rename `.tmp` to `.json`
4. Atomic rename guarantees:
   - Either old content or new content (no partial writes)
   - Crash-safe operation
   - No corruption even if server crashes mid-write
