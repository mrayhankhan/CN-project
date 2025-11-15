import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * RequestHandler - Routes and handles HTTP requests
 * Implements the paste service endpoints
 */
public class RequestHandler {
    
    public static void handleRequest(Socket socket, String method, String path, String body) throws IOException {
        // Handle CORS preflight requests
        if (method.equals("OPTIONS")) {
            handleOptions(socket);
            return;
        }
        
        // Serve static files
        if (path.equals("/") || path.equals("/index.html")) {
            serveFile(socket, "web/index.html", "text/html");
        } else if (path.equals("/style.css")) {
            serveFile(socket, "web/style.css", "text/css");
        } else if (path.equals("/app.js")) {
            serveFile(socket, "web/app.js", "application/javascript");
        } else if (path.equals("/view.html")) {
            serveFile(socket, "web/view.html", "text/html");
        } else if (path.equals("/history.html")) {
            serveFile(socket, "web/history.html", "text/html");
        } else if (path.equals("/history.js")) {
            serveFile(socket, "web/history.js", "application/javascript");
        } else if (path.equals("/about") || path.equals("/about.html")) {
            serveFile(socket, "web/about.html", "text/html");
        } else if (path.equals("/debug.html")) {
            serveFile(socket, "web/debug.html", "text/html");
        }
        // Create paste endpoint
        else if (path.equals("/create") && method.equals("POST")) {
            handleCreate(socket, body);
        }
        // History API endpoints
        else if (path.equals("/api/history") && method.equals("GET")) {
            handleGetHistory(socket);
        }
        else if (path.matches("^/api/history/\\d{5}$")) {
            String id = path.substring(13); // Remove "/api/history/"
            if (method.equals("GET")) {
                handleGetHistoryById(socket, id);
            } else {
                HttpServer.sendResponse(socket, 400, "text/plain", "Method not allowed");
            }
        }
        else if (path.matches("^/api/history/\\d{5}/delete$") && method.equals("POST")) {
            String id = path.substring(13, 18); // Extract ID from "/api/history/00001/delete"
            handleDeletePaste(socket, id);
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
                    // Serve the view.html page with deleted status
                    serveViewWithStatus(socket, id);
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
    
    private static void serveViewWithStatus(Socket socket, String id) throws IOException {
        try {
            Path path = Paths.get("web/view.html");
            String content = new String(Files.readAllBytes(path), "UTF-8");
            
            // Check if paste is deleted
            boolean deleted = StorageHistory.isDeleted(id);
            
            // Inject paste status before </head>
            String statusScript = "<script>window.pasteStatus = {deleted: " + deleted + "};</script>";
            content = content.replace("</head>", statusScript + "\n</head>");
            
            HttpServer.sendResponse(socket, 200, "text/html", content);
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
        
        // Get client IP
        String clientIp = socket.getInetAddress().getHostAddress();
        
        // Create paste
        String id = Storage.createPaste(text, clientIp);
        
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
        
        // Check if paste is deleted
        boolean deleted = StorageHistory.isDeleted(id);
        
        // Return JSON with paste data and deleted status
        String json = "{\"id\":\"" + id + "\",\"text\":" + Utils.toJsonString(paste) + ",\"deleted\":" + deleted + "}";
        HttpServer.sendResponse(socket, 200, "application/json", json);
    }
    
    private static void handleUpdate(Socket socket, String id, String body) throws IOException {
        // Check if paste is deleted
        if (StorageHistory.isDeleted(id)) {
            HttpServer.sendResponse(socket, 410, "text/plain", "This paste has been deleted and cannot be edited");
            return;
        }
        
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
        
        // Get client IP
        String clientIp = socket.getInetAddress().getHostAddress();
        
        // Update paste
        boolean success = Storage.updatePaste(id, body, clientIp);
        
        if (success) {
            // Broadcast update to all WebSocket clients
            WebSocketServer.broadcast(id, body);
            HttpServer.sendResponse(socket, 200, "text/plain", "Updated");
        } else {
            HttpServer.sendResponse(socket, 404, "text/plain", "Paste not found");
        }
    }
    
    private static void handleGetHistory(Socket socket) throws IOException {
        String clientIp = socket.getInetAddress().getHostAddress();
        ServerLogger.log("Incoming request for /api/history from " + clientIp);
        
        try {
            List<Map<String, Object>> history = StorageHistory.readAll();
            ServerLogger.log("Retrieved " + history.size() + " history entries");
            String json = historyListToJson(history);
            HttpServer.sendResponse(socket, 200, "application/json", json);
        } catch (Exception e) {
            ServerLogger.logError("Failed to retrieve history", e);
            HttpServer.sendResponse(socket, 500, "text/plain", "Internal server error");
        }
    }
    
    private static void handleGetHistoryById(Socket socket, String id) throws IOException {
        // Validate ID format
        if (!id.matches("\\d{5}")) {
            HttpServer.sendResponse(socket, 400, "text/plain", "Invalid ID format");
            return;
        }
        
        List<Map<String, Object>> history = StorageHistory.readById(id);
        if (history.isEmpty()) {
            HttpServer.sendResponse(socket, 404, "text/plain", "No history found for ID");
            return;
        }
        
        String json = historyListToJson(history);
        HttpServer.sendResponse(socket, 200, "application/json", json);
    }
    
    private static void handleDeletePaste(Socket socket, String id) throws IOException {
        // Validate ID format
        if (!id.matches("\\d{5}")) {
            HttpServer.sendResponse(socket, 400, "text/plain", "Invalid ID format");
            return;
        }
        
        // Get client IP
        String clientIp = socket.getInetAddress().getHostAddress();
        
        // Mark as deleted in history
        StorageHistory.markDelete(id, clientIp);
        
        ServerLogger.log("Paste " + id + " marked as deleted by " + clientIp);
        HttpServer.sendResponse(socket, 200, "text/plain", "Deleted");
    }
    
    private static String historyListToJson(List<Map<String, Object>> history) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) json.append(",");
            json.append(historyEntryToJson(history.get(i)));
        }
        
        json.append("]");
        return json.toString();
    }
    
    private static String historyEntryToJson(Map<String, Object> entry) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":\"").append(entry.get("id")).append("\",");
        json.append("\"timestamp\":\"").append(entry.get("timestamp")).append("\",");
        json.append("\"creator_ip\":\"").append(entry.get("creator_ip")).append("\",");
        json.append("\"version\":").append(entry.get("version")).append(",");
        json.append("\"action\":\"").append(entry.get("action")).append("\",");
        json.append("\"deleted\":").append(entry.get("deleted"));
        if (entry.containsKey("note")) {
            json.append(",\"note\":").append(Utils.toJsonString((String) entry.get("note")));
        }
        json.append("}");
        return json.toString();
    }
    
    // Handle CORS preflight OPTIONS requests
    private static void handleOptions(Socket socket) throws IOException {
        HttpServer.sendResponse(socket, 200, "text/plain", "");
        socket.close();
    }
}
