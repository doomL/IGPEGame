package it.unical.igpe.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;

public class AndroidDebug {
    private static Activity activity;
    private static Handler handler = new Handler(Looper.getMainLooper());
    
    public static void setActivity(Activity activity) {
        AndroidDebug.activity = activity;
    }
    
    public static void showError(final String message) {
        if (activity == null) {
            Gdx.app.error("AndroidDebug", "Activity not set, cannot show error: " + message);
            return;
        }
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Show Toast message
                Toast.makeText(activity, "ERROR: " + message, Toast.LENGTH_LONG).show();
                
                // Also log it
                Gdx.app.error("AndroidDebug", message);
            }
        });
    }
    
    public static void showMessage(final String message) {
        if (activity == null) {
            Gdx.app.log("AndroidDebug", message);
            return;
        }
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                Gdx.app.log("AndroidDebug", message);
            }
        });
    }
}
