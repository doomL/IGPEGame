package it.unical.igpe.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import it.unical.igpe.utils.FilePicker;

public class AndroidFilePicker implements FilePicker {
    private static final String TAG = "AndroidFilePicker";
    private static final int FILE_PICKER_REQUEST_CODE = 1001;
    private static final int FILE_SAVER_REQUEST_CODE = 1002;
    private final AndroidLauncher activity;
    private FilePickerCallback currentCallback;
    private boolean isSaveOperation = false;
    private String saveContent = null; // Store content to save

    public AndroidFilePicker(AndroidLauncher activity) {
        this.activity = activity;
    }

    @Override
    public void pickFile(FilePickerCallback callback) {
        this.currentCallback = callback;
        this.isSaveOperation = false;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select Map File"),
                FILE_PICKER_REQUEST_CODE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "No file manager found", ex);
            if (callback != null) {
                callback.onCancelled();
            }
        }
    }

    @Override
    public void saveFile(String suggestedFileName, FilePickerCallback callback) {
        this.currentCallback = callback;
        this.isSaveOperation = true;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        
        // Set suggested file name
        if (suggestedFileName != null) {
            if (!suggestedFileName.endsWith(".map")) {
                suggestedFileName += ".map";
            }
            intent.putExtra(Intent.EXTRA_TITLE, suggestedFileName);
        } else {
            intent.putExtra(Intent.EXTRA_TITLE, "map.map");
        }

        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, "Save Map File"),
                FILE_SAVER_REQUEST_CODE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "No file manager found", ex);
            if (callback != null) {
                callback.onCancelled();
            }
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentCallback == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    if (isSaveOperation) {
                        // For save operation, return the URI path
                        // The actual saving will be done by the caller
                        String filePath = uri.toString();
                        currentCallback.onFileSelected(filePath);
                    } else {
                        // For load operation, read file content directly
                        String fileContent = readUriContent(uri);
                        if (fileContent != null && !fileContent.isEmpty()) {
                            // Extract filename from URI for display purposes
                            String fileName = getFileNameFromUri(uri);
                            currentCallback.onFileSelectedWithContent(fileName, fileContent);
                        } else {
                            currentCallback.onCancelled();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling file", e);
                    currentCallback.onCancelled();
                }
            } else {
                currentCallback.onCancelled();
            }
        } else {
            currentCallback.onCancelled();
        }

        currentCallback = null;
        isSaveOperation = false;
    }

    private String readUriContent(Uri uri) throws Exception {
        InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new Exception("Could not open input stream for URI: " + uri);
        }

        // Read entire file content into a string
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream));
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(line);
            lineCount++;
        }
        reader.close();
        inputStream.close();

        String result = content.toString();
        Log.d(TAG, "Read " + lineCount + " lines from URI, total length: " + result.length());
        if (result.isEmpty()) {
            throw new Exception("File content is empty");
        }
        return result;
    }
    
    private String getFileNameFromUri(Uri uri) {
        String fileName = "map.map";
        String path = uri.getPath();
        if (path != null && path.contains("/")) {
            fileName = path.substring(path.lastIndexOf("/") + 1);
            if (!fileName.endsWith(".map")) {
                fileName += ".map";
            }
        }
        return fileName;
    }
}
