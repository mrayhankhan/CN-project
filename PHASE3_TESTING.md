# Phase 3 Testing - Quick Reference

## Overview

Phase 3 implements essential testing and verification to ensure the server is production-ready with proper concurrency handling, WebSocket reliability, and cross-platform compatibility.

---

## Automated Tests

### 1. Concurrency Smoke Test
**File**: `test_concurrency.sh`

**Purpose**: Verify per-ID locking prevents data corruption

**What it does**:
- Creates a test paste
- Launches 10 concurrent PUT requests to the same paste ID
- Verifies the final JSON is valid (not corrupted)
- Checks data file integrity
- Ensures no temporary files are left behind

**Run**:
```bash
chmod +x test_concurrency.sh
./test_concurrency.sh
```

**Expected Result**: ✅ All tests pass, no corruption

---

### 2. WebSocket Reconnect Test
**File**: `test_websocket.sh`

**Purpose**: Verify clients can reconnect after network interruption

**What it does**:
- Creates a test paste
- Connects WebSocket client
- Disconnects (simulating network issue)
- Updates paste content
- Reconnects WebSocket client
- Verifies client receives updated content

**Requirements**: Node.js installed

**Run**:
```bash
chmod +x test_websocket.sh
./test_websocket.sh
```

**Expected Result**: ✅ Client reconnects and receives current content

**Fallback**: If Node.js not available, provides manual testing instructions

---

### 3. Run All Tests
**File**: `run_phase3_tests.sh`

**Purpose**: Execute all automated tests in sequence

**Run**:
```bash
chmod +x run_phase3_tests.sh
./run_phase3_tests.sh
```

**Output**: Summary of all test results

---

## Manual Testing

### Cross-Browser Verification
**File**: `CROSS_BROWSER_TESTING.md`

**Purpose**: Verify real-time collaboration across browsers and devices

**Key Tests**:
1. **Desktop Cross-Browser**: Edit in Chrome, verify in Firefox
2. **Mobile/Desktop**: Edit on mobile, verify on desktop
3. **Network Interruption**: Disconnect/reconnect, verify sync
4. **WebSocket Connection**: Verify in browser DevTools
5. **Multiple Users**: Test with 3+ simultaneous clients

**How to Use**:
```bash
# View the guide
cat CROSS_BROWSER_TESTING.md

# Or open in editor
code CROSS_BROWSER_TESTING.md
```

---

## Test Checklist

Before deploying to production:

- [ ] ✅ Concurrency test passes
- [ ] ✅ WebSocket reconnect test passes
- [ ] ✅ Cross-browser test (2+ browsers)
- [ ] ✅ Mobile device test (or emulator)
- [ ] ✅ Network interruption recovery verified
- [ ] ✅ WebSocket connection verified in DevTools
- [ ] ✅ Multiple simultaneous users (3+) tested

---

## Quick Test Commands

```bash
# Full test suite
./run_phase3_tests.sh

# Individual tests
./test_concurrency.sh      # Concurrency
./test_websocket.sh        # WebSocket reconnect

# Basic functionality
./smoke_test.sh            # HTTP endpoints
```

---

## Troubleshooting

### Concurrency Test Fails
**Symptoms**: JSON corruption, temporary files remain

**Check**:
- Per-ID locking implementation in `Storage.java`
- Atomic write implementation
- Server logs: `cat server.log`

**Fix**: Verify `getIdLock()` is called before all write operations

---

### WebSocket Test Fails
**Symptoms**: Client doesn't reconnect, no content received

**Check**:
- Node.js installed: `node --version`
- WebSocket server running
- Server logs: `cat server.log`

**Manual Test**:
1. Open paste in browser
2. F12 → Network → WS filter
3. Verify WebSocket connection
4. Disable network, re-enable
5. Verify reconnection

---

### Cross-Browser Issues
**Symptoms**: Updates don't appear in other browser

**Check**:
- Both browsers on same paste URL
- WebSocket connection in both browsers (F12 → Network)
- No JavaScript errors in console (F12 → Console)
- Server logs: `cat server.log`

**Fix**: Refresh page, check WebSocket connection status

---

## Success Criteria

### Automated Tests
✅ **Concurrency**: No corruption after 10 concurrent writes
✅ **WebSocket**: Client receives content after reconnect

### Manual Tests  
✅ **Cross-Browser**: Updates appear in 2+ browsers
✅ **Mobile**: Works on mobile device/emulator
✅ **Recovery**: Reconnects after network interruption
✅ **Multi-User**: 3+ users can collaborate simultaneously

---

## Next Steps

After all tests pass:

1. **Review logs**: `cat server.log`
2. **Performance test**: Load testing with more concurrent users
3. **Security review**: Verify all Phase 2 hardening features
4. **Documentation**: Update deployment docs
5. **Deploy**: Move to production environment

---

## Test Results Template

```
Date: ___________
Tester: ___________

Automated Tests:
[ ] Concurrency Test: PASS / FAIL
[ ] WebSocket Test: PASS / FAIL

Manual Tests:
[ ] Chrome → Firefox: PASS / FAIL
[ ] Desktop → Mobile: PASS / FAIL
[ ] Network Recovery: PASS / FAIL
[ ] 3+ Users: PASS / FAIL

Notes:
_____________________
_____________________
_____________________

Overall: PASS / FAIL
```

---

## Quick Mobile Test

Find your IP and access from mobile:

```bash
# Get your local IP
hostname -I | awk '{print $1}'

# Access from mobile on same network
# http://[YOUR_IP]:8080
```

Or generate QR code (if `qrencode` installed):
```bash
IP=$(hostname -I | awk '{print $1}')
echo "http://$IP:8080" | qrencode -t UTF8
```

Scan with mobile device to quickly access the server.
