package it.unical.igpe.editor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Handles map data and save/load operations for the level editor
 */
public class MapEditor {
	public int[][] map;
	public int mapDimension;
	public int tileSize;
	public boolean isMultiplayer;
	
	// Tile type constants (matching the original editor)
	public static final int STAIR_POS = 2;
	public static final int KEY_POS = 6;
	public static final int RED_KEY_POS = 7;
	public static final int BLUE_KEY_POS = 8;
	public static final int GREEN_KEY_POS = 9;
	public static final int PLAYER_POS = 10;
	public static final int ENEMY1_POS = 11;
	public static final int ENEMY2_POS = 12;
	public static final int BOX_POS = 12;
	public static final int BARREL_POS = 13;
	public static final int CACTUS_POS = 14;
	public static final int PLANT_POS = 15;
	public static final int LOGS_POS = 16;
	public static final int WALL_POS = 1;
	public static final int GROUND_POS = 0;
	
	public MapEditor(boolean multiplayer) {
		this.isMultiplayer = multiplayer;
		if (multiplayer) {
			this.mapDimension = 32;
			this.tileSize = 64;
		} else {
			this.mapDimension = 64;
			this.tileSize = 32;
		}
		initializeMap();
	}
	
	private void initializeMap() {
		map = new int[mapDimension][mapDimension];
		// Initialize borders as walls
		for (int i = 0; i < mapDimension; i++) {
			for (int j = 0; j < mapDimension; j++) {
				if (i == 0 || i == mapDimension - 1 || j == 0 || j == mapDimension - 1) {
					map[i][j] = WALL_POS;
				} else {
					map[i][j] = GROUND_POS;
				}
			}
		}
	}
	
	public void clear() {
		initializeMap();
	}
	
	public void setTile(int x, int y, int tileId) {
		if (x >= 0 && x < mapDimension && y >= 0 && y < mapDimension) {
			// Don't allow placing tiles on borders
			if (x != 0 && x != mapDimension - 1 && y != 0 && y != mapDimension - 1) {
				map[y][x] = tileId;
			}
		}
	}
	
	public int getTile(int x, int y) {
		if (x >= 0 && x < mapDimension && y >= 0 && y < mapDimension) {
			return map[y][x];
		}
		return WALL_POS;
	}
	
	public void loadMap(String path) throws IOException {
		try {
			FileHandle fileHandle = null;
			
			// Check if path is absolute (starts with /) or is a URI
			// This handles Android file picker results which return absolute paths
			if (path.startsWith("/") || path.startsWith("content://") || path.startsWith("file://")) {
				// Absolute path or URI - try as absolute file first
				if (path.startsWith("/")) {
					fileHandle = Gdx.files.absolute(path);
				} else if (path.startsWith("file://")) {
					// Remove file:// prefix
					String cleanPath = path.substring(7);
					fileHandle = Gdx.files.absolute(cleanPath);
				} else {
					// content:// URI - should have been copied to cache by AndroidFilePicker
					throw new IOException("Cannot load from content URI directly. File should be copied to cache first.");
				}
			} else {
				// Relative path - try internal (assets) first
				fileHandle = Gdx.files.internal(path);
			}
			
			// If internal file doesn't exist, try external storage
			if (!fileHandle.exists() && !path.startsWith("/") && !path.startsWith("content://") && !path.startsWith("file://")) {
				fileHandle = Gdx.files.external(path);
			}
			
			if (!fileHandle.exists()) {
				throw new IOException("Map file does not exist: " + path);
			}
			
			BufferedReader br = new BufferedReader(fileHandle.reader());
			
			// Determine map size from first line
			String firstLine = br.readLine();
			if (firstLine == null) {
				br.close();
				throw new IOException("Map file is empty: " + path);
			}
			
			String[] tokens = firstLine.trim().split("\\s+");
			int detectedSize = tokens.length;
			
			// Reset to detected size
			this.mapDimension = detectedSize;
			this.tileSize = detectedSize == 32 ? 64 : 32;
			this.isMultiplayer = (detectedSize == 32);
			map = new int[mapDimension][mapDimension];
			
			// Reset file reader
			br.close();
			br = new BufferedReader(fileHandle.reader());
			
			for (int i = 0; i < mapDimension; i++) {
				String line = br.readLine();
				if (line == null) {
					br.close();
					throw new IOException("Map file is incomplete: " + path);
				}
				tokens = line.trim().split("\\s+");
				for (int j = 0; j < tokens.length && j < mapDimension; j++) {
					map[j][i] = Integer.parseInt(tokens[j]);
				}
			}
			br.close();
		} catch (NumberFormatException e) {
			throw new IOException("Map file contains invalid data: " + path, e);
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			}
			throw new IOException("Error reading map file: " + path, e);
		}
	}
	
	public String saveMapToString() {
		StringWriter sw = new StringWriter();
		try {
			writeMap(sw);
		} catch (IOException e) {
			// Should not happen with StringWriter
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	public void saveMap(String path) throws IOException {
		it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Saving to path: " + path);
		FileHandle fileHandle;

		// Check if path is absolute (starts with / or C:\ etc.)
		boolean isAbsolute = path.startsWith("/") || path.contains(":\\");

		if (isAbsolute) {
			// Path is absolute, use it directly
			fileHandle = Gdx.files.absolute(path);
			it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Using absolute path: " + fileHandle.file().getAbsolutePath());
		} else if (Gdx.files.isExternalStorageAvailable()) {
			// Path is relative, use external storage (home directory)
			fileHandle = Gdx.files.external(path);
			it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Using external storage: " + fileHandle.file().getAbsolutePath());
		} else {
			// Fallback to local storage
			fileHandle = Gdx.files.local(path);
			it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Using local storage: " + fileHandle.file().getAbsolutePath());
		}

		it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Writing map data...");
		writeMap(fileHandle.writer(false));
		it.unical.igpe.utils.DebugUtils.showMessage("MapEditor.saveMap: Map saved successfully to: " + fileHandle.file().getAbsolutePath());
	}
	
	private void writeMap(Writer writer) throws IOException {
		// Write map[i][j] not map[j][i] to match the row-column order expected by WorldLoader
		// i = row (y coordinate), j = column (x coordinate)
		for (int i = 0; i < mapDimension; i++) {
			for (int j = 0; j < mapDimension; j++) {
				writer.write(String.valueOf(map[i][j]));
				if (j < mapDimension - 1) {
					writer.write(" ");
				}
			}
			writer.write("\n");
		}
		writer.flush();
		writer.close();
	}
	
	public void switchToMultiplayer() {
		this.isMultiplayer = true;
		this.mapDimension = 32;
		this.tileSize = 64;
		initializeMap();
	}
	
	public void switchToSinglePlayer() {
		this.isMultiplayer = false;
		this.mapDimension = 64;
		this.tileSize = 32;
		initializeMap();
	}
}
