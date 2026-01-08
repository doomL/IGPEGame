package it.unical.igpe.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class DebugUtils {
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
        }
        
        // Log to console
        Gdx.app.error("IGPEGame", fullMessage, exception);
        
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
        
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            try {
                Class<?> androidDebugClass = Class.forName("it.unical.igpe.android.AndroidDebug");
                androidDebugClass.getMethod("showMessage", String.class).invoke(null, message);
            } catch (Exception e) {
                // Fallback: just log
            }
        }
    }
}
