import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerLogger - Centralized logging facility
 * Logs server events and errors to a file for debugging
 */
public class ServerLogger {
    private static final String LOG_FILE = "../server.log";
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Log an informational message
     */
    public static void log(String message) {
        writeLog("INFO", message, null);
        System.out.println("[INFO] " + message);
    }
    
    /**
     * Log an error with exception details
     */
    public static void logError(String message, Exception e) {
        writeLog("ERROR", message, e);
        System.err.println("[ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
    
    /**
     * Write log entry to file
     */
    private static synchronized void writeLog(String level, String message, Exception e) {
        try {
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("[").append(LocalDateTime.now().format(formatter)).append("] ");
            logEntry.append("[").append(level).append("] ");
            logEntry.append(message);
            
            if (e != null) {
                logEntry.append("\n  Exception: ").append(e.getClass().getName());
                logEntry.append(": ").append(e.getMessage());
                
                // Include stack trace
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();
                
                // Indent stack trace
                for (String line : stackTrace.split("\n")) {
                    logEntry.append("\n  ").append(line);
                }
            }
            
            logEntry.append("\n");
            
            // Append to log file
            Files.write(Paths.get(LOG_FILE), 
                       logEntry.toString().getBytes("UTF-8"),
                       StandardOpenOption.CREATE,
                       StandardOpenOption.APPEND);
                       
        } catch (IOException ex) {
            // Fallback to console only if file logging fails
            System.err.println("Failed to write to log file: " + ex.getMessage());
        }
    }
}
