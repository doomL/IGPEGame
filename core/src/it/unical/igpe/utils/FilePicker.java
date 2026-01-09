package it.unical.igpe.utils;

public interface FilePicker {
    /**
     * Opens a file picker dialog to select a file
     * @param callback The callback to invoke when a file is selected (or null if cancelled)
     */
    void pickFile(FilePickerCallback callback);

    /**
     * Opens a file picker dialog to save a file
     * @param suggestedFileName Suggested file name (can be null)
     * @param callback The callback to invoke when a file is selected (or null if cancelled)
     */
    void saveFile(String suggestedFileName, FilePickerCallback callback);

    /**
     * Callback interface for file picker results
     */
    interface FilePickerCallback {
        void onFileSelected(String filePath);
        
        /**
         * Called when a file is selected with its content (used on Android)
         * Default implementation calls onFileSelected for backwards compatibility
         */
        default void onFileSelectedWithContent(String filePath, String fileContent) {
            onFileSelected(filePath);
        }
        
        void onCancelled();
    }
}
