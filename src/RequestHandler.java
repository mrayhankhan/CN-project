import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * RequestHandler - Routes and handles HTTP requests
 * Implements the paste service endpoints
 */
public class RequestHandler {
    
    public static void handleRequest(Socket socket, String method, String path, String body) throws IOException {
        // Serve static files
        if (path.equals("/") || path.equals("/index.html")) {
            serveFile(socket, "../web/index.html", "text/html");
        } else if (path.equals("/style.css")) {
            serveFile(socket, "../web/style.css", "text/css");
        } else if (path.equals("/app.js")) {
            serveFile(socket, "../web/app.js", "application/javascript");
        } else if (path.equals("/view.html")) {
            serveFile(socket, "../web/view.html", "text/html");
        }
        // Create paste endpoint
        else if (path.equals("/create") && method.equals("POST")) {
            handleCreate(socket, body);
        }
        // API endpoint for getting paste data as JSON
        else if (path.matches("^/api/\\d{5}$")) {
            String id = path.substring(5); // Remove "/api/"
            if (method.equals("GET")) {
                handleViewJson(socket, id);
            } else {
                HttpServer.sendResponse(socket, 400, "text/plain", "Method not allowed");
            }
        }
        // View/Edit paste endpoint - serve HTML page
        else if (path.matches("^/\\d{5}$")) {
            String id = path.substring(1);
            if (method.equals("GET")) {
                // Check if paste exists
                String paste = Storage.getPaste(id);
                if (paste == null) {
                    HttpServer.sendResponse(socket, 404, "text/plain", "Paste not found");
                } else {
                    // Serve the view.html page
                    serveFile(socket, "../web/view.html", "text/html");
                }
            } else if (method.equals("PUT")) {
                handleUpdate(socket, id, body);
            } else {
                HttpServer.sendResponse(socket, 400, "text/plain", "Method not allowed");
            }
        }
        // 404 for unknown paths
        else {
            HttpServer.sendResponse(socket, 404, "text/plain", "Not Found");
        }
        
        socket.close();
    }
    
    private static void serveFile(Socket socket, String filePath, String contentType) throws IOException {
        try {
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path), "UTF-8");
            HttpServer.sendResponse(socket, 200, contentType, content);
        } catch (IOException e) {
            HttpServer.sendResponse(socket, 404, "text/plain", "File not found");
        }
    }
    
    private static void handleCreate(Socket socket, String body) throws IOException {
        // Parse form data to extract text parameter
        String text = Utils.parseFormData(body);
        
        // Validate input
        if (text == null || text.trim().isEmpty()) {
            HttpServer.sendResponse(socket, 400, "text/plain", "Empty paste content");
            return;
        }
        
        // Enforce paste size limit with HTTP 413
        if (text.length() > Storage.MAX_PASTE_SIZE) {
            HttpServer.sendResponse(socket, 413, "text/plain", 
                "Paste too large. Maximum size: " + (Storage.MAX_PASTE_SIZE / (1024 * 1024)) + " MB");
            return;
        }
        
        // Create paste
        String id = Storage.createPaste(text);
        
        if (id == null) {
            HttpServer.sendResponse(socket, 500, "text/plain", "Failed to create paste");
            return;
        }
        
        // Redirect to view page
        HttpServer.sendRedirect(socket, "/" + id);
    }
    
    private static void handleViewJson(Socket socket, String id) throws IOException {
        String paste = Storage.getPaste(id);
        
        if (paste == null) {
            HttpServer.sendResponse(socket, 404, "text/plain", "Paste not found");
            return;
        }
        
        // Return JSON with paste data
        String json = "{\"id\":\"" + id + "\",\"text\":" + Utils.toJsonString(paste) + "}";
        HttpServer.sendResponse(socket, 200, "application/json", json);
    }
    
    private static void handleUpdate(Socket socket, String id, String body) throws IOException {
        // Validate input
        if (body == null || body.trim().isEmpty()) {
            HttpServer.sendResponse(socket, 400, "text/plain", "Empty paste content");
            return;
        }
        
        // Enforce paste size limit with HTTP 413
        if (body.length() > Storage.MAX_PASTE_SIZE) {
            HttpServer.sendResponse(socket, 413, "text/plain", 
                "Paste too large. Maximum size: " + (Storage.MAX_PASTE_SIZE / (1024 * 1024)) + " MB");
            return;
        }
        
        // Update paste
        boolean success = Storage.updatePaste(id, body);
        
        if (success) {
            // Broadcast update to all WebSocket clients
            WebSocketServer.broadcast(id, body);
            HttpServer.sendResponse(socket, 200, "text/plain", "Updated");
        } else {
            HttpServer.sendResponse(socket, 404, "text/plain", "Paste not found");
        }
    }
}
