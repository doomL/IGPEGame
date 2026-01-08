package it.unical.igpe.utils;

public interface FilePicker {
    /**
     * Opens a file picker dialog to select a file
     * @param callback The callback to invoke when a file is selected (or null if cancelled)
     */
    void pickFile(FilePickerCallback callback);

    /**
     * Callback interface for file picker results
     */
    interface FilePickerCallback {
        void onFileSelected(String filePath);
        void onCancelled();
    }
}
