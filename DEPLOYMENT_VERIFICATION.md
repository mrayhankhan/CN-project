# Deployment Verification Guide

This guide walks you through verifying your deployment of the Minimal Collaborative Paste Service.

## Prerequisites

- Backend deployed to a hosting service (Railway, Render, Heroku, etc.)
- Frontend deployed to GitHub Pages
- `curl` installed (for API testing)
- `wscat` installed (optional, for WebSocket testing): `npm install -g wscat`

## Step 1: Verify Runtime and Core Endpoints

### 1.1 Get Your Backend URL

Your backend should be deployed and accessible via HTTPS. Example URLs:
- Railway: `https://your-app.up.railway.app`
- Render: `https://your-app.onrender.com`
- Heroku: `https://your-app.herokuapp.com`

Replace `YOUR_BACKEND_HOST` with your actual URL in the commands below.

### 1.2 Test Health Endpoint

The health endpoint confirms your server is running and reachable:

```bash
curl -i https://YOUR_BACKEND_HOST/health
```

**Expected Response:**
```
HTTP/2 200
content-type: application/json
access-control-allow-origin: https://mrayhankhan.github.io

{"status":"ok","service":"paste-service"}
```

✅ **Success criteria:** HTTP 200 status and JSON response with "status":"ok"

### 1.3 Test History API Endpoint

```bash
curl -i https://YOUR_BACKEND_HOST/api/history
```

**Expected Response:**
```
HTTP/2 200
content-type: application/json
access-control-allow-origin: https://mrayhankhan.github.io

[]
```

✅ **Success criteria:** HTTP 200 status and JSON array (empty `[]` if no pastes exist)

### 1.4 Test Create Endpoint

```bash
curl -i -X POST https://YOUR_BACKEND_HOST/create \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=Deployment+test+paste"
```

**Expected Response:**
```
HTTP/2 303
location: /00001
access-control-allow-origin: https://mrayhankhan.github.io
```

✅ **Success criteria:** HTTP 303 redirect with Location header containing paste ID

### 1.5 Test Get Paste Endpoint

Using the paste ID from the previous step (e.g., `00001`):

```bash
curl -i https://YOUR_BACKEND_HOST/api/00001
```

**Expected Response:**
```
HTTP/2 200
content-type: application/json

{"id":"00001","text":"Deployment test paste","timestamp":"2025-11-15T..."}
```

✅ **Success criteria:** HTTP 200 and JSON with paste data

## Step 2: Verify CORS Headers

CORS headers are essential for GitHub Pages to communicate with your backend.

```bash
curl -i https://YOUR_BACKEND_HOST/api/history | grep -i "access-control"
```

**Expected Output:**
```
access-control-allow-origin: https://mrayhankhan.github.io
access-control-allow-methods: GET, POST, PUT, DELETE, OPTIONS
access-control-allow-headers: Content-Type, Authorization
access-control-max-age: 86400
```

✅ **Success criteria:** All CORS headers present with correct GitHub Pages origin

### Test OPTIONS Preflight

```bash
curl -i -X OPTIONS https://YOUR_BACKEND_HOST/api/history \
  -H "Origin: https://mrayhankhan.github.io" \
  -H "Access-Control-Request-Method: GET"
```

✅ **Success criteria:** HTTP 200 with CORS headers

## Step 3: Verify WebSocket Handshake

### Option A: Using wscat (recommended)

```bash
wscat -c wss://YOUR_BACKEND_HOST/00001
```

**Expected Output:**
```
Connected (press CTRL+C to quit)
< {"type":"init","text":"Deployment test paste","users":1}
>
```

Type a message and press Enter. You should see it echoed back.

✅ **Success criteria:** Connection opens, initial message received

### Option B: Using Browser Console

1. Open browser console (F12)
2. Run:
```javascript
const ws = new WebSocket('wss://YOUR_BACKEND_HOST/00001');
ws.onopen = () => console.log('✓ Connected!');
ws.onmessage = (e) => console.log('Message:', e.data);
ws.onerror = (e) => console.error('Error:', e);
```

✅ **Success criteria:** "✓ Connected!" message appears

## Step 4: Update Frontend Configuration

### 4.1 Update docs/app.js

Edit `docs/app.js` line 5:

```javascript
const API_BASE = 'https://YOUR_BACKEND_HOST'; // Replace with actual backend URL
```

**Example:**
```javascript
const API_BASE = 'https://cn-project-production.up.railway.app';
```

### 4.2 Update docs/history.js

Edit `docs/history.js` line 4:

```javascript
const API_BASE = 'https://YOUR_BACKEND_HOST'; // Replace with actual backend URL
```

### 4.3 Commit and Push Changes

```bash
git add docs/app.js docs/history.js
git commit -m "Configure frontend with production backend URL"
git push origin main
```

GitHub Pages will automatically redeploy (takes 1-2 minutes).

## Step 5: Test GitHub Pages Integration

### 5.1 Open Your GitHub Pages Site

Visit: `https://mrayhankhan.github.io/CN-project/`

### 5.2 Create a Test Paste

1. Enter some text in the textarea
2. Click "Create Paste" or press `Ctrl+S`
3. You should be redirected to `/00001` (or next ID)

✅ **Success criteria:** Paste created successfully, URL changes

### 5.3 Test Real-time Collaboration

1. Copy the paste URL
2. Open it in a second browser window (or incognito mode)
3. Edit the text in one window
4. The other window should update in real-time

✅ **Success criteria:** Changes sync across windows

### 5.4 Test History Page

Visit: `https://mrayhankhan.github.io/CN-project/history.html`

✅ **Success criteria:** Your test paste appears in the history table

### 5.5 Check Browser Console for Errors

Open Developer Tools (F12) → Console tab

❌ **Common issues:**
- `CORS error`: Backend CORS headers not configured correctly
- `WebSocket failed`: Origin validation blocking connection
- `Failed to fetch`: Backend URL incorrect or server not responding

## Step 6: Run Automated Verification Scripts

### 6.1 Run Deployment Verification Script

```bash
chmod +x scripts/verify_deployment.sh
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST
```

**Expected Output:**
```
==========================================
Deployment Verification Script
==========================================
Backend URL: https://YOUR_BACKEND_HOST

1. Testing Health Endpoint
------------------------------------------
Testing: Health check... ✓ PASS

2. Testing API Endpoints
------------------------------------------
Testing: History API... ✓ PASS

...

==========================================
Summary
==========================================
Tests Passed: 5
Tests Failed: 0
==========================================
All tests passed!
```

### 6.2 Run Smoke Tests

```bash
./scripts/smoke_test.sh
```

### 6.3 Run All Tests

```bash
./scripts/run_all_tests.sh
```

## Step 7: Environment Variables Check

Ensure these environment variables are set in your hosting platform:

| Variable | Value | Required |
|----------|-------|----------|
| `PORT` | Auto-assigned or 8080 | Yes |
| `ALLOWED_ORIGIN` | `https://mrayhankhan.github.io` | Yes |

### Railway Example:
```bash
railway variables set ALLOWED_ORIGIN=https://mrayhankhan.github.io
```

### Render Example:
Add in dashboard → Environment tab

## Troubleshooting

### Issue: CORS Errors in Browser Console

**Solution:** Check ALLOWED_ORIGIN environment variable matches your GitHub Pages URL exactly (no trailing slash).

```bash
# Verify environment variable
curl -i https://YOUR_BACKEND_HOST/health | grep access-control-allow-origin
```

### Issue: WebSocket Connection Fails

**Symptoms:** "WebSocket connection failed" in browser console

**Solutions:**
1. Verify WebSocket URL uses `wss://` for HTTPS backends
2. Check origin validation in `WebSocketServer.java`
3. Test with curl to confirm backend is reachable

### Issue: 404 on /health Endpoint

**Solution:** Redeploy backend with updated code that includes health endpoint.

### Issue: GitHub Pages Not Updating

**Solutions:**
1. Check GitHub Actions tab for deployment status
2. Hard refresh browser: `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)
3. Clear browser cache

## Final Verification Checklist

- [ ] Health endpoint returns 200
- [ ] History API returns 200 and JSON array
- [ ] Create endpoint returns 303 with Location header
- [ ] CORS headers present on all responses
- [ ] WebSocket connection successful
- [ ] Frontend configured with correct backend URL
- [ ] GitHub Pages site loads
- [ ] Can create paste from Pages site
- [ ] Real-time collaboration works
- [ ] History page displays pastes
- [ ] No console errors in browser
- [ ] All automated tests pass

## Next Steps

1. **Test from Multiple Devices:** Verify on mobile and different browsers
2. **Load Testing:** Run `./scripts/test_concurrency.sh` if not already done
3. **Documentation:** Update README.md with deployment URLs
4. **Demo Video:** Record a 2-minute demo showing all features
5. **Backup Strategy:** Set up automated data backups if needed

## Quick Reference Commands

```bash
# Test health
curl -i https://YOUR_BACKEND_HOST/health

# Test history API
curl -i https://YOUR_BACKEND_HOST/api/history

# Test WebSocket
wscat -c wss://YOUR_BACKEND_HOST/00001

# Run verification script
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST

# Update frontend and deploy
git add docs/app.js docs/history.js
git commit -m "Configure production backend"
git push origin main
```

## Support

If you encounter issues:
1. Check server logs in your hosting platform dashboard
2. Review browser console for client-side errors
3. Verify environment variables are set correctly
4. Test each endpoint individually using curl commands above
