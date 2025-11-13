# Minimal Collaborative Paste Service

A lightweight, fully functional paste sharing application built entirely in pure Java using only `java.net.ServerSocket`, `java.net.Socket`, and standard Java libraries. No frameworks, no external dependencies—just plain Java sockets, threads, and file I/O.

## Features

- **Simple Landing Page**: Full-screen textarea with instant paste creation
- **Sequential IDs**: Pastes are numbered sequentially (00001, 00002, etc.)
- **View and Edit**: Each paste has a unique URL for viewing and editing
- **Real-time Collaboration**: Multiple users can edit the same paste simultaneously using WebSockets
- **Persistent Storage**: Pastes stored as JSON files
- **Dark Theme**: Clean, minimal dark-themed UI
- **Security**: HTML escaping to prevent XSS attacks

## Quick Start

### Running the Server

```bash
chmod +x run.sh
./run.sh
```

The server will start on port 8080. Access it at `http://localhost:8080`

### Manual Compilation and Execution

```bash
javac src/*.java
cd src
java MainServer
```

## Architecture Overview

### Components

1. **MainServer.java**: Entry point that accepts socket connections and routes to HTTP or WebSocket handlers
2. **HttpServer.java**: Implements basic HTTP/1.1 protocol manually—parses requests, sends responses
3. **RequestHandler.java**: Routes HTTP requests to appropriate handlers (create, view, update)
4. **WebSocketServer.java**: Implements WebSocket protocol (RFC 6455) for real-time collaboration
5. **Storage.java**: Manages paste persistence using JSON files with thread-safe access
6. **Utils.java**: Utility functions for HTML escaping and JSON encoding

### Frontend

- **index.html**: Landing page with textarea for creating new pastes
- **view.html**: Paste viewer with edit capability
- **app.js**: Client-side JavaScript for WebSocket communication and editing
- **style.css**: Dark-themed responsive styling

### Data Storage

- **data/counter.txt**: Maintains the current paste counter
- **data/XXXXX.json**: Individual paste files with metadata

## Network Concepts Used

### 1. **TCP Socket Programming**
- Server listens on port 8080 using `ServerSocket`
- Accepts incoming connections and creates `Socket` instances
- Bidirectional communication over TCP streams

### 2. **HTTP Protocol Implementation**
- Manual parsing of HTTP request lines, headers, and body
- Proper HTTP response formatting with status codes
- Support for GET, POST, and PUT methods
- HTTP redirect responses (303 See Other)

### 3. **WebSocket Protocol**
- Upgrade handshake from HTTP to WebSocket
- Frame parsing (FIN bit, opcode, payload length, masking)
- Bidirectional message passing
- Connection management and broadcasting

### 4. **Concurrency**
- Thread pool (`ExecutorService`) for handling multiple connections
- Synchronized access to shared resources (counter, files)
- `ReentrantLock` for protecting critical sections
- `ConcurrentHashMap` for managing WebSocket connections

### 5. **Client-Server Architecture**
- Stateless HTTP for paste operations
- Stateful WebSocket connections for collaboration
- Client-side JavaScript for dynamic UI updates

## API Endpoints

### HTTP Endpoints

#### GET /
Returns the landing page for creating new pastes.

#### POST /create
Creates a new paste.
- **Body**: Raw text content
- **Response**: 303 redirect to `/XXXXX`

Example:
```bash
curl -X POST http://localhost:8080/create -d "Hello, World!"
```

#### GET /XXXXX
Retrieves paste data as JSON.
- **Response**: `{"id": "XXXXX", "text": "content"}`

Example:
```bash
curl http://localhost:8080/00001
```

#### PUT /XXXXX
Updates an existing paste.
- **Body**: New text content
- **Response**: 200 OK

Example:
```bash
curl -X PUT http://localhost:8080/00001 -d "Updated content"
```

### WebSocket Endpoint

#### ws://localhost:8080/ws/XXXXX
Real-time collaboration endpoint.
- Clients connect and receive updates when paste is modified
- Sending a message updates the paste and broadcasts to all connected clients

## How WebSocket Collaboration Works

1. **Connection**: Client opens WebSocket connection to `ws://host/ws/XXXXX`
2. **Handshake**: Server performs WebSocket upgrade using Sec-WebSocket-Key
3. **Broadcasting**: When any client sends an update:
   - Server receives the WebSocket frame
   - Unmasks the payload
   - Updates the paste in storage
   - Broadcasts the update to all other connected clients
4. **Consistency**: Last-write-wins model—no conflict resolution

### Frame Structure

The WebSocket implementation handles:
- **Text frames** (opcode 1): UTF-8 encoded messages
- **Close frames** (opcode 8): Connection termination
- **Masking**: Client-to-server messages are masked per RFC 6455
- **Variable payload length**: Supports 7-bit, 16-bit, and 64-bit length encoding

## Security Features

1. **HTML Escaping**: All user content is escaped to prevent XSS
2. **Input Validation**: Paste size limited to 1MB
3. **Path Validation**: ID format strictly validated (5 digits only)
4. **No Path Traversal**: File paths are constructed safely

## File Structure

```
/
├── src/
│   ├── MainServer.java       # Entry point and connection router
│   ├── HttpServer.java        # HTTP protocol handler
│   ├── RequestHandler.java   # Request routing and handlers
│   ├── WebSocketServer.java  # WebSocket protocol and collaboration
│   ├── Storage.java           # Paste persistence
│   └── Utils.java             # Utility functions
├── web/
│   ├── index.html             # Landing page
│   ├── view.html              # Paste viewer/editor
│   ├── app.js                 # Client-side logic
│   └── style.css              # Styling
├── data/
│   ├── counter.txt            # Paste counter
│   └── *.json                 # Paste files (created at runtime)
├── README.md                  # This file
└── run.sh                     # Build and run script
```

## Development Notes

- **Pure Java**: No external libraries or frameworks
- **No Build Tools**: No Maven, Gradle, or similar
- **Minimal Dependencies**: Only standard Java libraries
- **Thread-Safe**: Proper synchronization for concurrent access
- **Simple Protocol**: Basic but functional HTTP and WebSocket implementation

## Testing

1. **Create a paste**: Visit `http://localhost:8080`, enter text, click "Go"
2. **View paste**: Automatically redirected to `/00001` (or next ID)
3. **Edit paste**: Click "Edit" button, modify text, click "Save"
4. **Collaborate**: Open the same paste URL in multiple browser tabs/windows
5. **Real-time updates**: Edit in one window, see changes appear in others

## Limitations

- Basic WebSocket implementation (text frames only)
- No authentication or paste ownership
- No paste deletion or expiration
- Simple last-write-wins conflict resolution
- No TLS/SSL support (use reverse proxy in production)
