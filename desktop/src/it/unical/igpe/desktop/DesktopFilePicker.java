package it.unical.igpe.desktop;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import it.unical.igpe.utils.FilePicker;

public class DesktopFilePicker implements FilePicker {

    @Override
    public void pickFile(final FilePickerCallback callback) {
        // Run on Swing thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setAcceptAllFileFilterUsed(false);

                int result = fileChooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (file != null && callback != null) {
                        // Desktop can access files directly, so just use onFileSelected
                        callback.onFileSelected(file.getPath());
                    } else if (callback != null) {
                        callback.onCancelled();
                    }
                } else if (callback != null) {
                    callback.onCancelled();
                }
            }
        });
    }

    @Override
    public void saveFile(final String suggestedFileName, final FilePickerCallback callback) {
        // Run on Swing thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setDialogTitle("Save Map");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Map files (*.map)", "map"));
                
                if (suggestedFileName != null) {
                    fileChooser.setSelectedFile(new File(suggestedFileName));
                }

                int result = fileChooser.showSaveDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (file != null) {
                        String filePath = file.getPath();
                        if (!filePath.endsWith(".map")) {
                            filePath += ".map";
                        }
                        if (callback != null) {
                            callback.onFileSelected(filePath);
                        }
                    } else if (callback != null) {
                        callback.onCancelled();
                    }
                } else if (callback != null) {
                    callback.onCancelled();
                }
            }
        });
    }
}
