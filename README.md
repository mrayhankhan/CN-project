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

## Documentation

Detailed documentation is available in the `documentation/` folder:

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
