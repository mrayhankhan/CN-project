# Pre-Deployment Checklist

Use this checklist to ensure everything is ready before deploying.

## ✅ Code Readiness

- [x] Health endpoint implemented in `src/RequestHandler.java`
- [x] CORS headers configured in `src/HttpServer.java`
- [x] WebSocket origin validation in `src/WebSocketServer.java`
- [x] OPTIONS preflight handler working
- [x] Frontend API_BASE configurable in `docs/app.js`
- [x] Frontend API_BASE configurable in `docs/history.js`
- [x] WebSocket URL correctly formatted (no `/ws/` prefix)
- [x] No compilation errors

## 📝 Documentation Checklist

- [x] DEPLOYMENT_VERIFICATION.md created
- [x] DEPLOYMENT_QUICKREF.md created
- [x] VERIFICATION_SUMMARY.md created
- [x] README.md updated with deployment info
- [x] Verification scripts created

## 🧪 Local Testing Checklist

Before deploying, test locally:

### Start Server
```bash
./scripts/run.sh
```

### Test Health Endpoint
```bash
curl -i http://localhost:8080/health
```
Expected: HTTP 200 with `{"status":"ok","service":"paste-service"}`

### Test History API
```bash
curl -i http://localhost:8080/api/history
```
Expected: HTTP 200 with JSON array

### Test Create Paste
```bash
curl -i -X POST http://localhost:8080/create \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=Local+test"
```
Expected: HTTP 303 with Location header

### Test CORS Headers
```bash
curl -i http://localhost:8080/api/history | grep -i access-control
```
Expected: CORS headers present

### Run Smoke Test
```bash
./scripts/smoke_test.sh
```
Expected: All tests pass

## 🚀 Deployment Steps

### 1. Backend Deployment

Choose your platform:
- [ ] Railway (recommended)
- [ ] Render
- [ ] Heroku
- [ ] Other

#### Steps:
1. [ ] Create account on chosen platform
2. [ ] Connect GitHub repository
3. [ ] Configure build settings:
   - Build command: Auto-detected from `railway.toml` or `nixpacks.toml`
   - Start command: `java -cp src MainServer`
4. [ ] Set environment variables:
   ```
   ALLOWED_ORIGIN=https://mrayhankhan.github.io
   ```
5. [ ] Deploy and wait for build to complete
6. [ ] Note the deployed URL (e.g., `https://your-app.railway.app`)

### 2. Backend Verification

Replace `YOUR_BACKEND_HOST` with your actual backend URL:

```bash
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST
```

Expected: All tests pass (5/5)

### 3. Frontend Configuration

#### Update docs/app.js
```bash
# Line 5
const API_BASE = 'https://YOUR_BACKEND_HOST';
```

#### Update docs/history.js
```bash
# Line 4
const API_BASE = 'https://YOUR_BACKEND_HOST';
```

### 4. Deploy Frontend

```bash
git add docs/app.js docs/history.js
git commit -m "Configure production backend URL"
git push origin main
```

GitHub Pages will auto-deploy (1-2 minutes).

### 5. GitHub Pages Setup

1. [ ] Go to repository Settings
2. [ ] Navigate to Pages section
3. [ ] Source: Deploy from branch
4. [ ] Branch: main, folder: /docs
5. [ ] Save
6. [ ] Wait for deployment
7. [ ] Note the Pages URL: `https://mrayhankhan.github.io/CN-project/`

### 6. End-to-End Testing

Visit: `https://mrayhankhan.github.io/CN-project/`

- [ ] Page loads without errors
- [ ] Can create a paste
- [ ] Paste URL redirects correctly
- [ ] Can view paste content
- [ ] Can edit paste
- [ ] Real-time sync works (test in 2 browsers)
- [ ] History page loads
- [ ] History shows created pastes
- [ ] No CORS errors in console

### 7. WebSocket Testing

#### Browser Console Method:
```javascript
const ws = new WebSocket('wss://YOUR_BACKEND_HOST/00001');
ws.onopen = () => console.log('✓ Connected!');
ws.onmessage = (e) => console.log('Message:', e.data);
ws.onerror = (e) => console.error('Error:', e);
```

- [ ] Connection opens successfully
- [ ] Initial message received
- [ ] Can send/receive messages

#### wscat Method (if installed):
```bash
wscat -c wss://YOUR_BACKEND_HOST/00001
```

- [ ] Connection opens
- [ ] Can type and send messages

## 🔍 Final Verification

### Backend Health Check
```bash
curl -i https://YOUR_BACKEND_HOST/health
```
- [ ] Returns HTTP 200
- [ ] JSON response with "status":"ok"

### API Endpoints
```bash
curl -i https://YOUR_BACKEND_HOST/api/history
```
- [ ] Returns HTTP 200
- [ ] Valid JSON array response

### CORS Headers
```bash
curl -i https://YOUR_BACKEND_HOST/health | grep access-control
```
- [ ] Contains `access-control-allow-origin: https://mrayhankhan.github.io`
- [ ] Contains `access-control-allow-methods`
- [ ] Contains `access-control-allow-headers`

### WebSocket Connection
```bash
wscat -c wss://YOUR_BACKEND_HOST/00001
```
- [ ] Connection established
- [ ] Initial message received

### Frontend Integration
- [ ] Pages site loads
- [ ] Can create paste from Pages
- [ ] Real-time collaboration works
- [ ] History page functional
- [ ] No console errors

## 📊 Performance Check

- [ ] Page load time < 3 seconds
- [ ] API response time < 500ms
- [ ] WebSocket connection < 2 seconds
- [ ] No memory leaks (monitor hosting dashboard)

## 🐛 Common Issues Checklist

### CORS Errors
- [ ] `ALLOWED_ORIGIN` environment variable set correctly
- [ ] No trailing slash in `ALLOWED_ORIGIN`
- [ ] CORS headers present in response

### WebSocket Connection Failed
- [ ] Using `wss://` for HTTPS backend
- [ ] Origin validation allows GitHub Pages URL
- [ ] No firewall blocking WebSocket connections

### 404 Errors
- [ ] Backend deployed successfully
- [ ] Build completed without errors
- [ ] Health endpoint accessible

### Paste Creation Fails
- [ ] Backend reachable
- [ ] CORS headers correct
- [ ] `docs/app.js` has correct API_BASE

## 📝 Documentation Updates

After successful deployment:

### Update README.md
Replace placeholders with actual URLs:
```markdown
## 🌐 Live Demo

- **Frontend (GitHub Pages):** https://mrayhankhan.github.io/CN-project/
- **Backend:** https://your-actual-backend.app
```

### Commit Changes
```bash
git add README.md
git commit -m "Update README with production URLs"
git push origin main
```

## ✨ Final Deliverables

- [ ] Backend deployed and accessible
- [ ] Frontend deployed to GitHub Pages
- [ ] All endpoints tested and working
- [ ] WebSocket connections functional
- [ ] Documentation complete
- [ ] Test scripts pass
- [ ] README updated with URLs
- [ ] No errors in browser console
- [ ] Demo video recorded (if required)

## 🎉 Deployment Complete!

Once all items are checked:
1. Capture test output: `./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST > deployment-test-results.txt`
2. Test in multiple browsers
3. Share the Pages URL for demo
4. Monitor backend logs for any issues

---

**Need Help?**
- Review: [DEPLOYMENT_VERIFICATION.md](DEPLOYMENT_VERIFICATION.md)
- Quick Commands: [DEPLOYMENT_QUICKREF.md](DEPLOYMENT_QUICKREF.md)
- Summary: [VERIFICATION_SUMMARY.md](VERIFICATION_SUMMARY.md)
