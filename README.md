# Minimal Collaborative Paste Service

A lightweight paste sharing application built entirely in pure Java using only standard libraries—no frameworks, no external dependencies.

## 🌐 Live Demo

- **Frontend (GitHub Pages):** https://mrayhankhan.github.io/CN-project/
- **Backend (Render):** https://cn-project-6y4k.onrender.com

## Features

- **Simple Interface**: Full-screen textarea with instant paste creation
- **Sequential IDs**: Pastes numbered sequentially (00001, 00002, etc.)
- **Real-time Collaboration**: Multiple users can edit simultaneously via WebSockets
- **Persistent Storage**: JSON file-based storage
- **Dark Theme**: Clean, minimal UI
- **Health Monitoring**: Built-in health check endpoint for deployment verification

## Quick Start

### Local Development

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

Access the application at `http://localhost:8080`

### Production Deployment

**Quick steps:**
1. Deploy backend to Render using the included Dockerfile
2. Update `docs/app.js` and `docs/history.js` with your Render URL
3. Push to GitHub to deploy frontend via GitHub Pages

For detailed instructions, see the Deployment section below.

### Testing

```bash
# Basic smoke test
./scripts/smoke_test.sh

# Run all automated tests
./scripts/run_all_tests.sh

# Verify deployment
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST
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
const ws = new WebSocket('ws://localhost:8080/00001');
ws.onmessage = (event) => {
  console.log('Paste updated:', event.data);
  // Update UI with new content
};
```

## 🚀 Deployment

### Render Deployment (Recommended)

This project uses Docker for deployment on Render:

1. **Create Render Web Service:**
   - Go to [render.com](https://render.com) and create a new Web Service
   - Connect your GitHub repository
   - Select branch: `main`
   - Environment: **Docker**
   - Leave Start Command blank (Dockerfile handles it)

2. **Configure Health Check:**
   - Health Check Path: `/api/health` or `/health`

3. **Deploy:** Click "Create Web Service" and wait for deployment

4. **Update Frontend:**
   - Copy your Render URL (e.g., `https://yourservice.onrender.com`)
   - Update `docs/app.js`: Set `API_BASE` to your Render URL
   - Update `docs/history.js`: Set `API_BASE` to your Render URL
   - Commit and push to deploy GitHub Pages

### Deployment Files

- `Dockerfile` - Docker container configuration for Render
- `start.sh` - Local development startup script

### Environment Variables

Render automatically provides the `PORT` variable. No additional environment variables are required.

## Documentation

### Project Report
📄 **[PROJECT_REPORT.md](PROJECT_REPORT.md)** - Complete project documentation including:
- Architecture diagrams (HTTP flow, WebSocket flow, Storage)
- Detailed networking concepts (Sockets, TCP, Thread pools, WebSocket handshake, File persistence)
- Implementation details and code structure
- Concurrency design and locking mechanisms
- Complete LOC statistics (1,476 lines of Java)
- File structure and technology stack

### Deployment & Testing Guides

- **[DEPLOYMENT_VERIFICATION.md](DEPLOYMENT_VERIFICATION.md)** - Complete deployment verification guide
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - GitHub Pages setup instructions

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

