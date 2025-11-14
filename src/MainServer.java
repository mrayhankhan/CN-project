import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * MainServer - Entry point for the paste sharing application
 * Starts both HTTP and WebSocket servers on the configured port
 */
public class MainServer {
    private static final int PORT = getPort();
    
    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable: " + portEnv + ", using default 8080");
            }
        }
        return 8080;
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Minimal Collaborative Paste Service...");
        
        // Initialize storage
        Storage.initialize();
        
        // Create executor for handling concurrent connections
        ExecutorService executor = Executors.newCachedThreadPool();
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("============================================");
            System.out.println("Server ready on port " + PORT);
            System.out.println("============================================");
            System.out.println("Access at http://localhost:" + PORT);
            System.out.println("Press Ctrl+C to stop");
            System.out.println("");
            
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Handle each connection in a separate thread
                    executor.submit(() -> handleConnection(clientSocket));
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
    
    private static void handleConnection(Socket socket) {
        try {
            // Read first line to determine if HTTP or WebSocket
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            
            String firstLine = reader.readLine();
            if (firstLine == null) {
                socket.close();
                return;
            }
            
            // Check if this is a WebSocket upgrade request
            if (firstLine.startsWith("GET") && firstLine.contains("ws/")) {
                WebSocketServer.handleWebSocket(socket, firstLine, reader);
            } else {
                // Handle as HTTP request
                HttpServer.handleHttpRequest(socket, firstLine, reader);
            }
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
