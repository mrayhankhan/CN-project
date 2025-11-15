# Minimal Collaborative Paste Service

A lightweight paste sharing application built entirely in pure Java using only standard libraries—no frameworks, no external dependencies.

## Features

- **Simple Interface**: Full-screen textarea with instant paste creation
- **Sequential IDs**: Pastes numbered sequentially (00001, 00002, etc.)
- **Real-time Collaboration**: Multiple users can edit simultaneously via WebSockets
- **Persistent Storage**: JSON file-based storage
- **Dark Theme**: Clean, minimal UI

## Quick Start

### Running the Server

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

Access the application at `http://localhost:8080`

### Testing

```bash
# Basic smoke test
./scripts/smoke_test.sh

# Run all automated tests
./scripts/run_all_tests.sh
```

## API Examples

### HTTP API (curl commands)

**Create a new paste:**
```bash
curl -X POST http://localhost:8080/create \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=Hello World"
# Returns: 303 redirect with Location header containing paste ID (e.g., /00001)
```

**Read a paste (JSON API):**
```bash
curl http://localhost:8080/api/00001
# Returns: {"id":"00001","text":"Hello World","timestamp":"2025-11-14T..."}
```

**Update a paste:**
```bash
curl -X PUT http://localhost:8080/00001 \
  -H "Content-Type: text/plain" \
  -d "Updated content"
# Returns: 200 OK
```

**Get paste history:**
```bash
curl http://localhost:8080/api/history
# Returns: [{"id":"00001","timestamp":"...","preview":"Hello World"},...]
```

### WebSocket Connection Example

```bash
# Connect to WebSocket for real-time updates (using websocat or similar)
websocat ws://localhost:8080/ws/00001

# Or using Node.js:
node -e "
const WebSocket = require('ws');
const ws = new WebSocket('ws://localhost:8080/ws/00001');
ws.on('open', () => console.log('Connected'));
ws.on('message', (data) => console.log('Update:', data));
"
```

**Browser JavaScript example:**
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/00001');
ws.onmessage = (event) => {
  console.log('Paste updated:', event.data);
  // Update UI with new content
};
```

## Documentation

### Project Report
📄 **[PROJECT_REPORT.md](PROJECT_REPORT.md)** - Complete project documentation including:
- Architecture diagrams (HTTP flow, WebSocket flow, Storage)
- Detailed networking concepts (Sockets, TCP, Thread pools, WebSocket handshake, File persistence)
- Implementation details and code structure
- Concurrency design and locking mechanisms
- Complete LOC statistics (1,476 lines of Java)
- File structure and technology stack

### Testing Guides
Detailed testing documentation is available in the `documentation/` folder:

- **[Testing Guide](documentation/testing-guide.md)** - Comprehensive testing overview and checklist
- **[WebSocket Testing](documentation/websocket-testing.md)** - Real-time collaboration tests
- **[Concurrency Testing](documentation/concurrency-testing.md)** - Multi-user concurrent access tests
- **[Cross-Browser Testing](documentation/cross-browser-testing.md)** - Browser compatibility verification
- **[History Feature Testing](documentation/history-testing.md)** - Paste history functionality tests

## Architecture

Built with:
- HTTP server using `java.net.ServerSocket`
- WebSocket server for real-time updates
- File-based JSON storage
- Per-paste locking for concurrency control

## License

MIT

