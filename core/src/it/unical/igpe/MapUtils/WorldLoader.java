package it.unical.igpe.MapUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class WorldLoader {
	public int[][] map;

	public WorldLoader(int width, int height) {
		map = new int[height][width];
	}

	/**
	 * Load map from content string (used for Android file picker)
	 */
	public void LoadMapFromContent(String content) throws IOException {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Loading map from content (length: " + content.length() + ")");
			BufferedReader br = new BufferedReader(new StringReader(content));
			
			// Count total lines first to validate format
			String[] allLines = content.split("\n");
			it.unical.igpe.utils.DebugUtils.showMessage("Map has " + allLines.length + " lines");
			
			// First, detect the map size by reading the first line
			String firstLine = allLines.length > 0 ? allLines[0] : null;
			if (firstLine == null || firstLine.trim().isEmpty()) {
				br.close();
				throw new IOException("Map content is empty");
			}
			
			String[] firstTokens = firstLine.trim().split("\\s+");
			int detectedSize = firstTokens.length;
			
			it.unical.igpe.utils.DebugUtils.showMessage("First line has " + detectedSize + " tokens: " + firstLine.substring(0, Math.min(50, firstLine.length())));
			
			// Validate that we have enough lines
			if (allLines.length < detectedSize) {
				br.close();
				throw new IOException("Map content is incomplete: expected " + detectedSize + " lines, but only found " + allLines.length + " lines");
			}
			
			// Resize the map array if needed
			if (map == null || map.length != detectedSize || (map.length > 0 && map[0].length != detectedSize)) {
				map = new int[detectedSize][detectedSize];
				it.unical.igpe.utils.DebugUtils.showMessage("Detected map size: " + detectedSize + "x" + detectedSize);
			}
			
			// Now read the entire map from the array
			for (int i = 0; i < detectedSize; i++) {
				String line = allLines[i].trim();
				if (line.isEmpty()) {
					throw new IOException("Map content has empty line at row " + i);
				}
				String[] tokens = line.split("\\s+");
				if (tokens.length != detectedSize) {
					throw new IOException("Map content row " + i + " has " + tokens.length + " tokens, expected " + detectedSize);
				}
				for (int j = 0; j < tokens.length && j < detectedSize; j++) {
					try {
						this.map[i][j] = Integer.parseInt(tokens[j]);
					} catch (NumberFormatException e) {
						throw new IOException("Map content contains invalid number at row " + i + ", column " + j + ": " + tokens[j], e);
					}
				}
			}
			br.close();
			it.unical.igpe.utils.DebugUtils.showMessage("Map loaded successfully from content (size: " + detectedSize + "x" + detectedSize + ", total cells: " + (detectedSize * detectedSize) + ")");
			// Verify map was populated
			int nonZeroCount = 0;
			for (int i = 0; i < detectedSize; i++) {
				for (int j = 0; j < detectedSize; j++) {
					if (this.map[i][j] != 0) nonZeroCount++;
				}
			}
			it.unical.igpe.utils.DebugUtils.showMessage("Map verification: " + nonZeroCount + " non-zero cells out of " + (detectedSize * detectedSize) + " total");
		} catch (NumberFormatException e) {
			throw new IOException("Map content contains invalid data", e);
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			}
			throw new IOException("Error reading map content: " + e.getMessage(), e);
		}
	}

	public void LoadMap(String path) throws IOException {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Loading map: " + path);
			FileHandle fileHandle = null;
			
			// Check if path is absolute (starts with /) or is a URI (starts with content:// or file://)
			// This handles Android file picker results
			if (path.startsWith("/") || path.startsWith("content://") || path.startsWith("file://")) {
				// Absolute path or URI
				if (path.startsWith("/")) {
					// Absolute path - try absolute first, then extract filename for local()
					fileHandle = Gdx.files.absolute(path);
					if (!fileHandle.exists()) {
						// Extract filename and try local() (for Android internal files)
						String fileName = path.substring(path.lastIndexOf("/") + 1);
						fileHandle = Gdx.files.local(fileName);
					}
				} else if (path.startsWith("file://")) {
					// Remove file:// prefix
					String cleanPath = path.substring(7);
					fileHandle = Gdx.files.absolute(cleanPath);
					if (!fileHandle.exists()) {
						String fileName = cleanPath.substring(cleanPath.lastIndexOf("/") + 1);
						fileHandle = Gdx.files.local(fileName);
					}
				} else {
					// content:// URI - Android file picker should have copied it
					throw new IOException("Cannot load from content URI directly. File should be copied first.");
				}
			} else {
				// Relative path - could be from assets or from Android internal files
				// Try local() first (for Android file picker results that return just filename)
				fileHandle = Gdx.files.local(path);
				
				// If not found, try internal (assets)
				if (!fileHandle.exists()) {
					fileHandle = Gdx.files.internal(path);
				}
			}
			
			// If still not found, try external storage
			if (!fileHandle.exists()) {
				fileHandle = Gdx.files.external(path);
			}
			
			// Last resort: if path was absolute, try extracting filename
			if (!fileHandle.exists() && (path.startsWith("/") || path.contains("/"))) {
				String fileName = path.substring(path.lastIndexOf("/") + 1);
				if (!fileName.equals(path)) { // Only if we extracted something different
					fileHandle = Gdx.files.local(fileName);
					if (!fileHandle.exists()) {
						fileHandle = Gdx.files.external(fileName);
					}
				}
			}
			
			if (!fileHandle.exists()) {
				throw new IOException("Map file does not exist: " + path + " (tried local, internal, external, absolute)");
			}
			
			BufferedReader br = new BufferedReader(fileHandle.reader());
			
			// First, detect the map size by reading the first line
			String firstLine = br.readLine();
			if (firstLine == null) {
				br.close();
				throw new IOException("Map file is empty: " + path);
			}
			
			String[] firstTokens = firstLine.trim().split("\\s+");
			int detectedSize = firstTokens.length;
			
			// Resize the map array if needed
			if (map == null || map.length != detectedSize || (map.length > 0 && map[0].length != detectedSize)) {
				map = new int[detectedSize][detectedSize];
				it.unical.igpe.utils.DebugUtils.showMessage("Detected map size: " + detectedSize + "x" + detectedSize);
			}
			
			// Reset reader to start from beginning
			br.close();
			br = new BufferedReader(fileHandle.reader());
			
			// Now read the entire map
			// Map is stored as int[height][width] = int[row][column]
			// World.java uses map[x][y] where x is row and y is column
			// So we need: map[row][column] = map[i][j] where i=row, j=column
			for (int i = 0; i < detectedSize; i++) {
				String line = br.readLine();
				if (line == null) {
					br.close();
					throw new IOException("Map file is incomplete or corrupted: " + path + " (expected " + detectedSize + " lines, got " + i + ")");
				}
				String[] tokens = line.trim().split("\\s+");
				// i is row (y coordinate in map file), j is column (x coordinate in map file)
				// World uses map[x][y] where x=row, y=column, so map[i][j] is correct
				for (int j = 0; j < tokens.length && j < detectedSize; j++) {
					this.map[i][j] = Integer.parseInt(tokens[j]);
				}
			}
			br.close();
			it.unical.igpe.utils.DebugUtils.showMessage("Map loaded successfully: " + path + " (size: " + detectedSize + "x" + detectedSize + ")");
		} catch (NumberFormatException e) {
			throw new IOException("Map file contains invalid data: " + path, e);
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			}
			throw new IOException("Error reading map file: " + path, e);
		}
	}
}
