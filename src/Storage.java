import java.io.*;
import java.nio.file.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage - Handles paste persistence using JSON files
 * Implements synchronized access to prevent race conditions
 */
public class Storage {
    private static final String DATA_DIR = "../data";
    private static final String COUNTER_FILE = DATA_DIR + "/counter.txt";
    public static final int MAX_PASTE_SIZE = 10 * 1024 * 1024; // 10 MB limit
    private static final ReentrantLock counterLock = new ReentrantLock();
    private static final ConcurrentHashMap<String, ReentrantLock> idLocks = new ConcurrentHashMap<>();
    
    public static void initialize() {
        try {
            // Create data directory if not exists
            Files.createDirectories(Paths.get(DATA_DIR));
            
            // Create counter file if not exists
            File counterFile = new File(COUNTER_FILE);
            if (!counterFile.exists()) {
                Files.write(Paths.get(COUNTER_FILE), "0".getBytes());
            }
            
            System.out.println("Storage initialized");
        } catch (IOException e) {
            System.err.println("Failed to initialize storage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static String createPaste(String text) {
        counterLock.lock();
        try {
            // Validate paste size
            if (text == null || text.length() > MAX_PASTE_SIZE) {
                ServerLogger.log("Rejected paste: size exceeds limit");
                return null;
            }
            
            // Read current counter
            String counterStr = new String(Files.readAllBytes(Paths.get(COUNTER_FILE))).trim();
            int counter = Integer.parseInt(counterStr);
            counter++;
            
            // Generate ID - always 5 digits
            String id = String.format("%05d", counter);
            
            // Validate ID format (paranoid check)
            if (!isValidId(id)) {
                throw new IllegalStateException("Generated invalid ID: " + id);
            }
            
            // Update counter
            Files.write(Paths.get(COUNTER_FILE), String.valueOf(counter).getBytes());
            
            // Save paste with per-ID locking
            savePaste(id, text);
            
            ServerLogger.log("Created paste: " + id);
            return id;
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to create paste", e);
            return null;
        } finally {
            counterLock.unlock();
        }
    }
    
    public static String getPaste(String id) {
        try {
            // Validate ID format - prevent path traversal
            if (!isValidId(id)) {
                ServerLogger.log("Rejected invalid ID format: " + id);
                return null;
            }
            
            // Safe file naming - always data/{id}.json
            String filePath = getSafeFilePath(id);
            File file = new File(filePath);
            
            if (!file.exists()) {
                return null;
            }
            
            // Read file with per-ID lock
            ReentrantLock lock = getIdLock(id);
            lock.lock();
            try {
                String json = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
                return extractTextFromJson(json);
            } finally {
                lock.unlock();
            }
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to get paste: " + id, e);
            return null;
        }
    }
    
    public static boolean updatePaste(String id, String text) {
        try {
            // Validate ID format - prevent path traversal
            if (!isValidId(id)) {
                ServerLogger.log("Rejected invalid ID format for update: " + id);
                return false;
            }
            
            // Validate paste size
            if (text == null || text.length() > MAX_PASTE_SIZE) {
                ServerLogger.log("Rejected update for " + id + ": size exceeds limit");
                return false;
            }
            
            String filePath = getSafeFilePath(id);
            File file = new File(filePath);
            
            if (!file.exists()) {
                return false;
            }
            
            // Update paste with per-ID locking
            savePaste(id, text);
            
            ServerLogger.log("Updated paste: " + id);
            return true;
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to update paste: " + id, e);
            return false;
        }
    }
    
    private static void savePaste(String id, String text) throws IOException {
        // Get per-ID lock to serialize writes for this paste
        ReentrantLock lock = getIdLock(id);
        lock.lock();
        try {
            String filePath = getSafeFilePath(id);
            
            // Create JSON object
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"id\": \"").append(id).append("\",\n");
            json.append("  \"text\": ").append(Utils.toJsonString(text)).append(",\n");
            json.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
            json.append("  \"version\": 1\n");
            json.append("}");
            
            // Atomic write: write to temp file then rename
            String tempPath = filePath + ".tmp";
            Files.write(Paths.get(tempPath), json.toString().getBytes("UTF-8"));
            
            // Atomic rename - prevents partial/corrupted files
            Files.move(Paths.get(tempPath), Paths.get(filePath), 
                      StandardCopyOption.REPLACE_EXISTING, 
                      StandardCopyOption.ATOMIC_MOVE);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Validate ID format - must be exactly 5 digits
     * Prevents path traversal attacks
     */
    private static boolean isValidId(String id) {
        return id != null && id.matches("^\\d{5}$");
    }
    
    /**
     * Get safe file path - always data/{id}.json
     * Never accepts user-supplied paths
     */
    private static String getSafeFilePath(String id) {
        // Paranoid: double-check ID is valid
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid ID: " + id);
        }
        return DATA_DIR + "/" + id + ".json";
    }
    
    /**
     * Get or create a lock for a specific paste ID
     * Enables per-ID write serialization
     */
    private static ReentrantLock getIdLock(String id) {
        return idLocks.computeIfAbsent(id, k -> new ReentrantLock());
    }
    
    private static String extractTextFromJson(String json) {
        // Simple JSON parsing - find "text" field value
        int textIndex = json.indexOf("\"text\"");
        if (textIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", textIndex);
        if (colonIndex == -1) return "";
        
        // Skip whitespace and quote
        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return "";
        
        // Find end quote (handle escaped quotes)
        int endQuote = startQuote + 1;
        while (endQuote < json.length()) {
            if (json.charAt(endQuote) == '"' && json.charAt(endQuote - 1) != '\\') {
                break;
            }
            endQuote++;
        }
        
        String text = json.substring(startQuote + 1, endQuote);
        
        // Unescape JSON string
        return Utils.unescapeJson(text);
    }
}
