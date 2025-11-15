# Deployment Quick Reference

## Essential Commands for Deployment Verification

### 1. Test Backend Endpoints

Replace `YOUR_BACKEND_HOST` with your actual backend URL (e.g., `https://your-app.railway.app`)

#### Health Check
```bash
curl -i https://YOUR_BACKEND_HOST/health
```
**Expected:** HTTP 200 with `{"status":"ok"}`

#### History API
```bash
curl -i https://YOUR_BACKEND_HOST/api/history
```
**Expected:** HTTP 200 with JSON array `[]`

#### Create Paste
```bash
curl -i -X POST https://YOUR_BACKEND_HOST/create \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=Test+paste"
```
**Expected:** HTTP 303 with Location header

#### Get Paste
```bash
curl -i https://YOUR_BACKEND_HOST/api/00001
```
**Expected:** HTTP 200 with paste JSON

### 2. Test WebSocket

#### Using wscat (install: `npm install -g wscat`)
```bash
wscat -c wss://YOUR_BACKEND_HOST/00001
```
**Expected:** Connection opens, initial message received

#### Using Browser Console
```javascript
const ws = new WebSocket('wss://YOUR_BACKEND_HOST/00001');
ws.onopen = () => console.log('✓ Connected!');
ws.onmessage = (e) => console.log('Message:', e.data);
```

### 3. Update Frontend Configuration

#### Edit docs/app.js (line 5)
```javascript
const API_BASE = 'https://YOUR_BACKEND_HOST';
```

#### Edit docs/history.js (line 4)
```javascript
const API_BASE = 'https://YOUR_BACKEND_HOST';
```

#### Commit and Push
```bash
git add docs/app.js docs/history.js
git commit -m "Configure production backend"
git push origin main
```

### 4. Run Automated Tests

#### Local Smoke Test
```bash
./scripts/smoke_test.sh
```

#### Deployment Verification
```bash
chmod +x scripts/verify_deployment.sh
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST
```

#### All Tests
```bash
./scripts/run_all_tests.sh
```

### 5. Set Environment Variables

#### Railway
```bash
railway variables set ALLOWED_ORIGIN=https://mrayhankhan.github.io
```

#### Render
Set in Dashboard → Environment tab:
- `ALLOWED_ORIGIN` = `https://mrayhankhan.github.io`

#### Heroku
```bash
heroku config:set ALLOWED_ORIGIN=https://mrayhankhan.github.io
```

## Troubleshooting Quick Fixes

### CORS Error
```bash
# Check CORS headers
curl -i https://YOUR_BACKEND_HOST/health | grep access-control

# Fix: Verify ALLOWED_ORIGIN environment variable
```

### WebSocket Failed
```bash
# Test with curl first
curl -i https://YOUR_BACKEND_HOST/health

# Ensure using wss:// for HTTPS backends
```

### 404 on Health Endpoint
```bash
# Redeploy with latest code
git push origin main
```

## Verification Checklist

- [ ] Backend health endpoint responds
- [ ] History API returns JSON array
- [ ] Create paste returns 303 redirect
- [ ] CORS headers present
- [ ] WebSocket connection successful
- [ ] Frontend updated with backend URL
- [ ] Changes pushed to GitHub
- [ ] GitHub Pages deployed
- [ ] Can create paste from Pages
- [ ] Real-time collaboration works

## Useful Links

- **Complete Guide:** [DEPLOYMENT_VERIFICATION.md](DEPLOYMENT_VERIFICATION.md)
- **GitHub Pages Setup:** [DEPLOYMENT.md](DEPLOYMENT.md)
- **Project Docs:** [README.md](README.md)
- **GitHub Pages:** `https://mrayhankhan.github.io/CN-project/`

## One-Line Test Script

```bash
# Test all endpoints at once
./scripts/verify_deployment.sh https://YOUR_BACKEND_HOST && echo "✓ All tests passed!"
```
