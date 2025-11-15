import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * StorageHistory - Manages paste history in newline-delimited JSON format
 * Tracks creation, updates, and deletions with IP addresses and timestamps
 */
public class StorageHistory {
    private static final String HISTORY_FILE = "../data/history.log";
    private static final int MAX_HISTORY_ENTRIES = 500;
    private static final ReentrantLock historyLock = new ReentrantLock();
    
    /**
     * Append a history entry to the log
     */
    public static void append(String id, String action, int version, String creatorIp, String note) {
        historyLock.lock();
        try {
            // Ensure history file exists
            File file = new File(HISTORY_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            // Build JSON entry
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"id\":\"").append(id).append("\",");
            json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");
            json.append("\"creator_ip\":\"").append(creatorIp != null ? creatorIp : "unknown").append("\",");
            json.append("\"version\":").append(version).append(",");
            json.append("\"action\":\"").append(action).append("\",");
            json.append("\"deleted\":").append("delete".equals(action) ? "true" : "false");
            if (note != null && !note.isEmpty()) {
                json.append(",\"note\":").append(Utils.toJsonString(note));
            }
            json.append("}");
            
            // Append to file
            try (FileOutputStream fos = new FileOutputStream(HISTORY_FILE, true)) {
                fos.write(json.toString().getBytes("UTF-8"));
                fos.write('\n');
            }
            
            ServerLogger.log("History: " + action + " for paste " + id + " by " + creatorIp);
            
            // Cap history size if needed
            capHistory();
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to append history for " + id, e);
        } finally {
            historyLock.unlock();
        }
    }
    
    /**
     * Read all history entries (up to MAX_HISTORY_ENTRIES, newest first)
     */
    public static List<Map<String, Object>> readAll() {
        historyLock.lock();
        try {
            File file = new File(HISTORY_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            // Read all lines
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            
            // Parse JSON lines and compute deleted status per ID
            Map<String, Boolean> deletedStatusMap = new HashMap<>();
            List<Map<String, Object>> allEntries = new ArrayList<>();
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                Map<String, Object> entry = parseJsonLine(line);
                if (entry != null) {
                    allEntries.add(entry);
                    String id = (String) entry.get("id");
                    // Track if this ID has been deleted (last action wins)
                    Boolean deleted = (Boolean) entry.get("deleted");
                    deletedStatusMap.put(id, deleted);
                }
            }
            
            // Build result with computed deleted status
            List<Map<String, Object>> result = new ArrayList<>();
            
            // Get unique IDs by their first appearance (create action)
            Map<String, Map<String, Object>> firstAppearance = new LinkedHashMap<>();
            for (Map<String, Object> entry : allEntries) {
                String id = (String) entry.get("id");
                if (!firstAppearance.containsKey(id)) {
                    firstAppearance.put(id, entry);
                }
            }
            
            // Process newest to oldest unique IDs
            List<String> ids = new ArrayList<>(firstAppearance.keySet());
            for (int i = ids.size() - 1; i >= 0 && result.size() < MAX_HISTORY_ENTRIES; i--) {
                String id = ids.get(i);
                Map<String, Object> entry = new HashMap<>(firstAppearance.get(id));
                
                // Apply current deleted status
                entry.put("deleted", deletedStatusMap.get(id));
                
                result.add(entry);
            }
            
            return result;
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to read history", e);
            return new ArrayList<>();
        } finally {
            historyLock.unlock();
        }
    }
    
    /**
     * Read all history entries for a specific ID
     */
    public static List<Map<String, Object>> readById(String id) {
        historyLock.lock();
        try {
            File file = new File(HISTORY_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                Map<String, Object> entry = parseJsonLine(line);
                if (entry != null && id.equals(entry.get("id"))) {
                    result.add(entry);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to read history for " + id, e);
            return new ArrayList<>();
        } finally {
            historyLock.unlock();
        }
    }
    
    /**
     * Check if a paste is deleted (last action is delete)
     */
    public static boolean isDeleted(String id) {
        historyLock.lock();
        try {
            List<Map<String, Object>> history = readById(id);
            if (history.isEmpty()) {
                System.out.println("DEBUG isDeleted(" + id + "): No history found, returning false");
                return false;
            }
            
            // Check the last action for this ID
            Map<String, Object> lastEntry = history.get(history.size() - 1);
            Boolean deleted = (Boolean) lastEntry.get("deleted");
            boolean result = deleted != null && deleted;
            System.out.println("DEBUG isDeleted(" + id + "): history size=" + history.size() + 
                             ", last entry deleted field=" + deleted + 
                             ", returning " + result);
            return result;
            
        } finally {
            historyLock.unlock();
        }
    }
    
    /**
     * Mark a paste as deleted by appending a delete action
     */
    public static void markDelete(String id, String deleterIp) {
        // Get current version
        int version = 1;
        List<Map<String, Object>> history = readById(id);
        if (!history.isEmpty()) {
            Map<String, Object> latest = history.get(history.size() - 1);
            version = ((Number) latest.get("version")).intValue();
        }
        
        append(id, "delete", version, deleterIp, null);
    }
    
    /**
     * Cap history to MAX_HISTORY_ENTRIES by rewriting file
     */
    private static void capHistory() {
        try {
            File file = new File(HISTORY_FILE);
            if (!file.exists()) return;
            
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            
            if (lines.size() > MAX_HISTORY_ENTRIES) {
                // Keep newest MAX_HISTORY_ENTRIES
                List<String> keep = lines.subList(lines.size() - MAX_HISTORY_ENTRIES, lines.size());
                
                // Write to temp file
                String tempPath = HISTORY_FILE + ".tmp";
                try (FileWriter writer = new FileWriter(tempPath)) {
                    for (String line : keep) {
                        writer.write(line);
                        writer.write('\n');
                    }
                }
                
                // Atomic rename
                Files.move(Paths.get(tempPath), Paths.get(HISTORY_FILE),
                          StandardCopyOption.REPLACE_EXISTING,
                          StandardCopyOption.ATOMIC_MOVE);
                
                ServerLogger.log("History capped to " + MAX_HISTORY_ENTRIES + " entries");
            }
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to cap history", e);
        }
    }
    
    /**
     * Parse a JSON line into a Map
     */
    private static Map<String, Object> parseJsonLine(String line) {
        try {
            Map<String, Object> map = new HashMap<>();
            
            // Simple JSON parser for our specific format
            line = line.trim();
            if (!line.startsWith("{") || !line.endsWith("}")) return null;
            
            line = line.substring(1, line.length() - 1); // Remove { }
            
            String[] pairs = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split on comma not in quotes
            
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length != 2) continue;
                
                String key = kv[0].trim().replaceAll("\"", "");
                String value = kv[1].trim();
                
                if (value.equals("true") || value.equals("false")) {
                    map.put(key, Boolean.parseBoolean(value));
                } else if (value.startsWith("\"")) {
                    map.put(key, value.replaceAll("\"", ""));
                } else {
                    try {
                        map.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        map.put(key, value);
                    }
                }
            }
            
            return map;
            
        } catch (Exception e) {
            ServerLogger.logError("Failed to parse JSON line: " + line, e);
            return null;
        }
    }
}
