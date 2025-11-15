import java.io.*;
import java.net.*;

/**
 * HttpServer - Handles HTTP requests
 * Implements basic HTTP/1.1 protocol manually
 */
public class HttpServer {
    
    public static void handleHttpRequest(Socket socket, String firstLine, BufferedReader reader) {
        try {
            // Parse request line
            String[] parts = firstLine.split(" ");
            if (parts.length < 3) {
                sendResponse(socket, 400, "text/plain", "Bad Request");
                socket.close();
                return;
            }
            
            String method = parts[0];
            String path = parts[1];
            
            // Read headers
            StringBuilder headersBuilder = new StringBuilder();
            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headersBuilder.append(line).append("\r\n");
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }
            
            // Read body if present
            String body = "";
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int read = reader.read(bodyChars, 0, contentLength);
                body = new String(bodyChars, 0, read);
            }
            
            // Route request
            RequestHandler.handleRequest(socket, method, path, body);
            
        } catch (Exception e) {
            ServerLogger.logError("Error handling HTTP request", e);
            try {
                sendResponse(socket, 500, "text/plain", "Internal Server Error");
                socket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
    
    public static void sendResponse(Socket socket, int statusCode, String contentType, String body) throws IOException {
        OutputStream out = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(out, false);
        
        // Status line
        String statusText = getStatusText(statusCode);
        writer.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        
        // Headers
        writer.print("Content-Type: " + contentType + "; charset=UTF-8\r\n");
        writer.print("Content-Length: " + body.getBytes("UTF-8").length + "\r\n");
        writer.print("Connection: close\r\n");
        
        // CORS headers - Allow requests from any origin for API access
        writer.print("Access-Control-Allow-Origin: *\r\n");
        writer.print("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        writer.print("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        writer.print("Access-Control-Max-Age: 86400\r\n");
        
        writer.print("\r\n");
        writer.flush();
        
        // Body
        out.write(body.getBytes("UTF-8"));
        out.flush();
    }
    
    public static void sendRedirect(Socket socket, String location) throws IOException {
        OutputStream out = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(out, false);
        
        writer.print("HTTP/1.1 303 See Other\r\n");
        writer.print("Location: " + location + "\r\n");
        writer.print("Content-Length: 0\r\n");
        writer.print("Connection: close\r\n");
        
        // CORS headers
        writer.print("Access-Control-Allow-Origin: *\r\n");
        writer.print("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        writer.print("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        
        writer.print("\r\n");
        writer.flush();
    }
    
    private static String getStatusText(int code) {
        switch (code) {
            case 200: return "OK";
            case 303: return "See Other";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 410: return "Gone";
            case 413: return "Payload Too Large";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }
}
