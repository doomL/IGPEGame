package it.unical.igpe.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import it.unical.igpe.utils.FilePicker;

public class AndroidFilePicker implements FilePicker {
    private static final String TAG = "AndroidFilePicker";
    private final AndroidLauncher activity;
    private FilePickerCallback currentCallback;

    public AndroidFilePicker(AndroidLauncher activity) {
        this.activity = activity;
    }

    @Override
    public void pickFile(FilePickerCallback callback) {
        this.currentCallback = callback;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select Map File"),
                AndroidLauncher.FILE_PICKER_REQUEST_CODE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "No file manager found", ex);
            if (callback != null) {
                callback.onCancelled();
            }
        }
    }

    public void handleActivityResult(int resultCode, Intent data) {
        if (currentCallback == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // Copy file to app's cache directory so we can read it
                    String filePath = copyUriToCache(uri);
                    currentCallback.onFileSelected(filePath);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading file", e);
                    currentCallback.onCancelled();
                }
            } else {
                currentCallback.onCancelled();
            }
        } else {
            currentCallback.onCancelled();
        }

        currentCallback = null;
    }

    private String copyUriToCache(Uri uri) throws Exception {
        InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new Exception("Could not open input stream");
        }

        // Create temp file in cache directory
        File cacheDir = activity.getCacheDir();
        File tempFile = new File(cacheDir, "selected_map.map");

        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();

        return tempFile.getAbsolutePath();
    }
}
