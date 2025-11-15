# GitHub Pages Deployment Configuration

## Step 1: Enable GitHub Pages

1. Go to your repository settings on GitHub
2. Navigate to **Pages** section (under Code and automation)
3. Under **Source**, select **Deploy from a branch**
4. Under **Branch**, select **main** and **/docs** folder
5. Click **Save**
6. Your site will be published at: `https://mrayhankhan.github.io/CN-project/`

## Step 2: Update Frontend Configuration

After deploying your backend, update the `BASE_URL` in these files:

### docs/app.js
```javascript
const BASE_URL = 'https://YOUR_BACKEND_HOST';
```

### docs/history.js
```javascript
const BASE_URL = 'https://YOUR_BACKEND_HOST';
```

Replace `https://YOUR_BACKEND_HOST` with your actual backend deployment URL, for example:
- Render: `https://your-app-name.onrender.com`
- Railway: `https://your-app-name.up.railway.app`
- Heroku: `https://your-app-name.herokuapp.com`

## Step 3: Configure Backend CORS

The backend has been configured to accept requests from your GitHub Pages origin.

### Update the allowed origin:

1. **Option A: Environment Variable (Recommended)**
   Set the `ALLOWED_ORIGIN` environment variable in your deployment platform:
   ```
   ALLOWED_ORIGIN=https://mrayhankhan.github.io
   ```

2. **Option B: Update Java Code**
   Edit `src/HttpServer.java` and `src/WebSocketServer.java`:
   ```java
   allowedOrigin = "https://mrayhankhan.github.io";
   ```

### CORS Headers Included:
- `Access-Control-Allow-Origin`: Your GitHub Pages URL
- `Access-Control-Allow-Methods`: GET, POST, PUT, DELETE, OPTIONS
- `Access-Control-Allow-Headers`: Content-Type, Authorization
- `Access-Control-Max-Age`: 86400 (24 hours)

## Step 4: Deploy Backend

### Required Environment Variables:
```bash
PORT=8080                                    # Will be set automatically by most platforms
ALLOWED_ORIGIN=https://mrayhankhan.github.io # Your GitHub Pages URL
```

### Example: Deploying to Render
1. Create a new Web Service
2. Connect your GitHub repository
3. Set Build Command: `cd src && javac *.java`
4. Set Start Command: `cd src && java MainServer`
5. Add environment variable: `ALLOWED_ORIGIN=https://mrayhankhan.github.io`
6. Deploy

### Example: Deploying to Railway
1. Create new project from GitHub repo
2. Add environment variable: `ALLOWED_ORIGIN=https://mrayhankhan.github.io`
3. Railway will auto-detect and deploy

## Step 5: Test the Deployment

1. Visit your GitHub Pages URL: `https://mrayhankhan.github.io/CN-project/`
2. Create a new paste
3. Verify it saves to your backend
4. Open the paste in multiple tabs to test WebSocket collaboration
5. Check browser console for any CORS errors

## Troubleshooting

### CORS Errors
- Verify `ALLOWED_ORIGIN` matches your GitHub Pages URL exactly
- Check browser console for specific error messages
- Ensure backend is deployed and running

### WebSocket Connection Failed
- Verify backend URL is HTTPS (required for WSS)
- Check that WebSocket origin validation allows your GitHub Pages URL
- Ensure PORT environment variable is set correctly

### 404 Errors on GitHub Pages
- Verify `/docs` folder exists and contains all HTML/CSS/JS files
- Check that GitHub Pages is enabled in repository settings
- Wait a few minutes for GitHub Pages to build and deploy

## Files Modified

### Frontend (docs/ folder):
- `docs/index.html` - Updated links to relative paths
- `docs/view.html` - Updated Create New link
- `docs/history.html` - Updated Create New link
- `docs/about.html` - Updated Create New link
- `docs/app.js` - Added BASE_URL configuration with placeholder
- `docs/history.js` - Added BASE_URL configuration with placeholder
- `docs/style.css` - Copied from web/style.css

### Backend (src/ folder):
- `src/MainServer.java` - Reads PORT from environment variable
- `src/HttpServer.java` - Added CORS headers to all responses
- `src/RequestHandler.java` - Added OPTIONS handler for CORS preflight
- `src/WebSocketServer.java` - Added origin validation for WebSocket connections

## Security Notes

1. **CORS**: Backend only accepts requests from specified origin
2. **WebSocket Origin**: WebSocket connections validate origin header
3. **Environment Variables**: Use environment variables for configuration instead of hardcoding
4. **HTTPS**: Use HTTPS for production deployment to enable secure WebSocket (WSS)
