import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebSocketServer - Handles WebSocket connections for real-time collaboration
 * Implements minimal WebSocket protocol (RFC 6455)
 */
public class WebSocketServer {
    // Store active WebSocket connections per paste ID
    private static final ConcurrentHashMap<String, Set<Socket>> connections = new ConcurrentHashMap<>();
    
    public static void handleWebSocket(Socket socket, String firstLine, BufferedReader reader) {
        try {
            // Parse path to get paste ID
            String[] parts = firstLine.split(" ");
            String path = parts[1];
            String id = path.substring(path.lastIndexOf("/") + 1);
            
            // Read headers
            String secWebSocketKey = null;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                    secWebSocketKey = line.substring(18).trim();
                }
            }
            
            if (secWebSocketKey == null) {
                socket.close();
                return;
            }
            
            // Perform WebSocket handshake
            String acceptKey = generateAcceptKey(secWebSocketKey);
            
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, false);
            writer.print("HTTP/1.1 101 Switching Protocols\r\n");
            writer.print("Upgrade: websocket\r\n");
            writer.print("Connection: Upgrade\r\n");
            writer.print("Sec-WebSocket-Accept: " + acceptKey + "\r\n");
            writer.print("\r\n");
            writer.flush();
            
            // Add to connections
            connections.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(socket);
            System.out.println("WebSocket connected for paste: " + id);
            
            // Send initial content
            String content = Storage.getPaste(id);
            if (content != null) {
                sendWebSocketMessage(socket, "{\"type\":\"init\",\"text\":" + Utils.toJsonString(content) + "}");
            }
            
            // Broadcast updated user count to all clients
            broadcastUserCount(id);
            
            // Handle incoming messages
            handleWebSocketMessages(socket, id);
            
        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
    
    private static void handleWebSocketMessages(Socket socket, String id) {
        try {
            InputStream in = socket.getInputStream();
            
            while (!socket.isClosed()) {
                // Read frame header
                int b1 = in.read();
                if (b1 == -1) break;
                
                boolean fin = (b1 & 0x80) != 0;
                int opcode = b1 & 0x0F;
                
                // Opcode 8 = close
                if (opcode == 8) break;
                
                int b2 = in.read();
                if (b2 == -1) break;
                
                boolean masked = (b2 & 0x80) != 0;
                long payloadLength = b2 & 0x7F;
                
                // Extended payload length
                if (payloadLength == 126) {
                    payloadLength = (in.read() << 8) | in.read();
                } else if (payloadLength == 127) {
                    payloadLength = 0;
                    for (int i = 0; i < 8; i++) {
                        payloadLength = (payloadLength << 8) | in.read();
                    }
                }
                
                // Read masking key
                byte[] maskingKey = new byte[4];
                if (masked) {
                    in.read(maskingKey);
                }
                
                // Read payload
                byte[] payload = new byte[(int) payloadLength];
                int totalRead = 0;
                while (totalRead < payloadLength) {
                    int read = in.read(payload, totalRead, (int) payloadLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                
                // Unmask payload
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                    }
                }
                
                // Only handle text frames (opcode 1)
                if (opcode == 1) {
                    String message = new String(payload, "UTF-8");
                    
                    // Get client IP
                    String clientIp = socket.getInetAddress().getHostAddress();
                    
                    // Update storage
                    Storage.updatePaste(id, message, clientIp);
                    
                    // Broadcast to all clients except sender
                    broadcastExcept(id, message, socket);
                }
            }
        } catch (IOException e) {
            // Connection closed
        } finally {
            // Remove from connections
            Set<Socket> sockets = connections.get(id);
            if (sockets != null) {
                sockets.remove(socket);
                System.out.println("WebSocket disconnected for paste: " + id);
                
                // Broadcast updated user count to remaining clients
                broadcastUserCount(id);
            }
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    public static void broadcast(String id, String message) {
        Set<Socket> sockets = connections.get(id);
        if (sockets != null) {
            String jsonMessage = "{\"type\":\"update\",\"text\":" + Utils.toJsonString(message) + "}";
            for (Socket socket : sockets) {
                try {
                    sendWebSocketMessage(socket, jsonMessage);
                } catch (IOException e) {
                    // Remove dead connection
                    sockets.remove(socket);
                }
            }
        }
    }
    
    private static void broadcastExcept(String id, String message, Socket except) {
        Set<Socket> sockets = connections.get(id);
        if (sockets != null) {
            String jsonMessage = "{\"type\":\"update\",\"text\":" + Utils.toJsonString(message) + "}";
            for (Socket socket : sockets) {
                if (socket != except) {
                    try {
                        sendWebSocketMessage(socket, jsonMessage);
                    } catch (IOException e) {
                        // Remove dead connection
                        sockets.remove(socket);
                    }
                }
            }
        }
    }
    
    /**
     * Broadcast user count to all connected clients for a paste
     */
    private static void broadcastUserCount(String id) {
        Set<Socket> sockets = connections.get(id);
        if (sockets != null) {
            int count = sockets.size();
            String countMessage = "{\"type\":\"userCount\",\"count\":" + count + "}";
            for (Socket socket : sockets) {
                try {
                    sendWebSocketMessage(socket, countMessage);
                } catch (IOException e) {
                    // Remove dead connection
                    sockets.remove(socket);
                }
            }
        }
    }
    
    private static void sendWebSocketMessage(Socket socket, String message) throws IOException {
        byte[] payload = message.getBytes("UTF-8");
        OutputStream out = socket.getOutputStream();
        
        // Frame header: FIN + text opcode
        out.write(0x81);
        
        // Payload length
        if (payload.length <= 125) {
            out.write(payload.length);
        } else if (payload.length <= 65535) {
            out.write(126);
            out.write((payload.length >> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((payload.length >> (i * 8)) & 0xFF);
            }
        }
        
        // Payload
        out.write(payload);
        out.flush();
    }
    
    private static String generateAcceptKey(String key) throws Exception {
        String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest((key + magic).getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }
}
