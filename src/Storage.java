import java.io.*;
import java.nio.file.*;
import java.util.concurrent.locks.*;

/**
 * Storage - Handles paste persistence using JSON files
 * Implements synchronized access to prevent race conditions
 */
public class Storage {
    private static final String DATA_DIR = "../data";
    private static final String COUNTER_FILE = DATA_DIR + "/counter.txt";
    private static final ReentrantLock counterLock = new ReentrantLock();
    private static final ReentrantLock fileLock = new ReentrantLock();
    
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
            // Read current counter
            String counterStr = new String(Files.readAllBytes(Paths.get(COUNTER_FILE))).trim();
            int counter = Integer.parseInt(counterStr);
            counter++;
            
            // Generate ID
            String id = String.format("%05d", counter);
            
            // Update counter
            Files.write(Paths.get(COUNTER_FILE), String.valueOf(counter).getBytes());
            
            // Save paste
            savePaste(id, text);
            
            System.out.println("Created paste: " + id);
            return id;
            
        } catch (IOException e) {
            System.err.println("Failed to create paste: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            counterLock.unlock();
        }
    }
    
    public static String getPaste(String id) {
        fileLock.lock();
        try {
            // Validate ID format
            if (!id.matches("\\d{5}")) {
                return null;
            }
            
            String filePath = DATA_DIR + "/" + id + ".json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return null;
            }
            
            // Read file
            String json = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
            
            // Parse JSON (simple extraction)
            return extractTextFromJson(json);
            
        } catch (IOException e) {
            System.err.println("Failed to get paste: " + e.getMessage());
            return null;
        } finally {
            fileLock.unlock();
        }
    }
    
    public static boolean updatePaste(String id, String text) {
        fileLock.lock();
        try {
            // Validate ID format
            if (!id.matches("\\d{5}")) {
                return false;
            }
            
            String filePath = DATA_DIR + "/" + id + ".json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return false;
            }
            
            // Update paste
            savePaste(id, text);
            
            System.out.println("Updated paste: " + id);
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to update paste: " + e.getMessage());
            return false;
        } finally {
            fileLock.unlock();
        }
    }
    
    private static void savePaste(String id, String text) throws IOException {
        String filePath = DATA_DIR + "/" + id + ".json";
        
        // Create JSON object
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(id).append("\",\n");
        json.append("  \"text\": ").append(Utils.toJsonString(text)).append(",\n");
        json.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
        json.append("  \"version\": 1\n");
        json.append("}");
        
        // Write to file
        Files.write(Paths.get(filePath), json.toString().getBytes("UTF-8"));
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
