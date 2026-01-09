package it.unical.igpe.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugUtils {
    private static final String LOG_FILE = "Download/igpe_debug.log";
    private static FileHandle logFile = null;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    private static void initLogFile() {
        if (logFile == null) {
            try {
                // Try Downloads folder first
                logFile = Gdx.files.external(LOG_FILE);
                
                // If that doesn't work, try alternative paths
                if (!logFile.parent().exists()) {
                    // Try with capital D
                    logFile = Gdx.files.external("download/igpe_debug.log");
                    if (!logFile.parent().exists()) {
                        // Fallback to root
                        logFile = Gdx.files.external("igpe_debug.log");
                    }
                }
                
                // Clear old log on first write
                if (logFile.exists()) {
                    logFile.writeString("", false); // Clear file
                }
                writeToFile("=== Debug log started ===");
                writeToFile("Log file path: " + logFile.path());
            } catch (Exception e) {
                Gdx.app.error("DebugUtils", "Failed to initialize log file", e);
            }
        }
    }
    
    private static void writeToFile(String message) {
        try {
            initLogFile();
            if (logFile != null) {
                String timestamp = dateFormat.format(new Date());
                String logEntry = "[" + timestamp + "] " + message + "\n";
                logFile.writeString(logEntry, true); // Append
            }
        } catch (Exception e) {
            // Silently fail - don't break the app if logging fails
            Gdx.app.error("DebugUtils", "Failed to write to log file", e);
        }
    }
    
    public static void showError(String message) {
        showError(message, null);
    }
    
    public static void showError(String message, Throwable exception) {
        String fullMessage = message;
        if (exception != null) {
            fullMessage += "\n" + exception.getClass().getSimpleName();
            if (exception.getMessage() != null) {
                fullMessage += ": " + exception.getMessage();
            }
            // Add stack trace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            fullMessage += "\n" + sw.toString();
        }
        
        // Log to console
        Gdx.app.error("IGPEGame", fullMessage, exception);
        
        // Write to file
        writeToFile("ERROR: " + fullMessage);
        
        // On Android, show a popup
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // Use reflection to call Android-specific code
            try {
                Class<?> androidDebugClass = Class.forName("it.unical.igpe.android.AndroidDebug");
                androidDebugClass.getMethod("showError", String.class).invoke(null, fullMessage);
            } catch (Exception e) {
                // Fallback: just log if Android debug class not available
                Gdx.app.error("DebugUtils", "Could not show Android popup", e);
            }
        }
    }
    
    public static void showMessage(String message) {
        Gdx.app.log("IGPEGame", message);
        
        // Write to file
        writeToFile("INFO: " + message);
        
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            try {
                Class<?> androidDebugClass = Class.forName("it.unical.igpe.android.AndroidDebug");
                androidDebugClass.getMethod("showMessage", String.class).invoke(null, message);
            } catch (Exception e) {
                // Fallback: just log
            }
        }
    }
    
    public static String getLogFilePath() {
        try {
            initLogFile();
            if (logFile != null) {
                return logFile.path();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Log file not available";
    }
}
