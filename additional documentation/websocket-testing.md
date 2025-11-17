# WebSocket Real-Time Collaboration Testing

## Overview

This guide covers testing the WebSocket functionality that enables real-time collaborative editing between multiple users.

---

## Automated WebSocket Reconnection Test

**Script**: `test_websocket_reconnect.sh`

### Purpose
Verify that WebSocket clients can successfully reconnect after network interruption and receive the current paste content.

### What It Tests
- WebSocket connection establishment
- Client disconnection handling
- Paste updates while client is disconnected
- Client reconnection capability
- Synchronization of content after reconnect

### Requirements
- Node.js installed (for test client)
- Server running on port 8080

### Running the Test

```bash
chmod +x test_websocket_reconnect.sh
./test_websocket_reconnect.sh
```

### Test Procedure
1. Creates a test paste
2. Connects WebSocket client
3. Simulates disconnection
4. Updates paste content via HTTP PUT
5. Reconnects WebSocket client
6. Verifies client receives updated content

### Expected Result
✅ Client successfully reconnects and receives current paste content

### Fallback
If Node.js is not available, the script provides manual testing instructions using browser DevTools.

---

## Manual WebSocket Testing

### Test 1: Basic Real-Time Updates

#### Setup
1. Start the server: `./run.sh`
2. Create a new paste at `http://localhost:8080`
3. Copy the paste URL (e.g., `http://localhost:8080/00001`)

#### Procedure
1. Open the paste URL in two browser tabs
2. Edit content in Tab 1
3. **Verify**: Changes appear instantly in Tab 2
4. Edit content in Tab 2
5. **Verify**: Changes appear instantly in Tab 1

**Result**: ✅ PASS / ❌ FAIL

---

### Test 2: Multiple Simultaneous Users

#### Setup
1. Open the same paste in 3+ browser tabs/windows
2. Position windows side-by-side

#### Procedure
1. Type simultaneously in multiple windows
2. **Verify**: All changes propagate to all windows
3. **Note**: Last-write-wins behavior is expected

**Result**: ✅ PASS / ❌ FAIL

---

### Test 3: Connection Monitoring

#### Using Browser DevTools

**Chrome/Edge:**
1. Open DevTools (F12)
2. Go to Network tab
3. Filter by "WS" (WebSocket)
4. Open a paste in view mode
5. **Verify**: WebSocket connection established to `ws://localhost:8080/ws/{id}`

**Firefox:**
1. Open DevTools (F12)
2. Go to Network tab
3. Click "WS" filter
4. Open a paste in view mode
5. **Verify**: WebSocket connection shows "101 Switching Protocols"

#### Procedure
1. Observe WebSocket connection in DevTools
2. Edit the paste
3. **Verify**: Messages flow through the WebSocket connection
4. Check "Messages" tab to see actual data

**Result**: ✅ PASS / ❌ FAIL

---

### Test 4: Network Interruption Recovery

#### Procedure
1. Open a paste in browser
2. Open DevTools Network tab, filter for WS
3. Edit content (verify real-time updates work)
4. Disable network in DevTools (Offline mode)
5. Edit content (changes won't sync)
6. Re-enable network
7. Reload the page
8. **Verify**: Page loads with the last saved content

**Result**: ✅ PASS / ❌ FAIL

---

### Test 5: WebSocket Frame Inspection

#### Advanced Testing with DevTools

1. Open paste in browser
2. DevTools → Network → WS → Select connection
3. Click "Messages" tab
4. Edit the paste content
5. **Observe**: 
   - Outgoing frames (client → server): masked data
   - Incoming frames (server → clients): broadcast updates

**Expected Behavior**:
- Text frames (opcode 0x1)
- Messages contain paste updates
- All connected clients receive broadcasts

**Result**: ✅ PASS / ❌ FAIL

---

## Troubleshooting

### WebSocket Connection Failed

**Symptoms**: Console error "WebSocket connection failed"

**Checks**:
1. Verify server is running: `curl http://localhost:8080`
2. Check server logs: `tail -f server.log`
3. Verify paste ID is valid (5 digits)
4. Try different browser

---

### Updates Not Appearing

**Symptoms**: Changes in one tab don't appear in another

**Checks**:
1. Check DevTools console for errors
2. Verify WebSocket connection is active (Network tab)
3. Check if browser has blocked WebSocket
4. Restart server and try again

---

### Connection Drops Frequently

**Symptoms**: WebSocket disconnects repeatedly

**Checks**:
1. Check server logs for errors
2. Monitor server resource usage
3. Check network stability
4. Verify no proxy/firewall blocking WebSocket

---

## Testing Checklist

- [ ] WebSocket connection establishes successfully
- [ ] Real-time updates work between 2+ clients
- [ ] Simultaneous edits handled (last-write-wins)
- [ ] Connection visible in browser DevTools
- [ ] Messages flow correctly in DevTools
- [ ] Clients can reconnect after disconnect
- [ ] Content syncs after reconnection
- [ ] Multiple concurrent users supported
- [ ] No console errors during normal operation

---

## Technical Details

### WebSocket Protocol Implementation
- Implements RFC 6455 WebSocket protocol
- Handles text frames (opcode 0x1)
- Supports close frames (opcode 0x8)
- Client-to-server masking per spec
- Variable payload length encoding (7-bit, 16-bit, 64-bit)

### Broadcasting Mechanism
1. Client sends message through WebSocket
2. Server unmasks payload
3. Server updates paste in storage
4. Server broadcasts to all connected clients for that paste ID
5. Clients receive update and refresh UI

### Connection Management
- `ConcurrentHashMap` tracks connections per paste ID
- Automatic cleanup on connection close
- Thread-safe broadcast operations
