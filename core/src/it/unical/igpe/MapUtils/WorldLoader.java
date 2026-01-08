package it.unical.igpe.MapUtils;

import java.io.BufferedReader;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class WorldLoader {
	public int[][] map;

	public WorldLoader(int width, int height) {
		map = new int[height][width];
	}

	public void LoadMap(String path) throws IOException {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Loading map: " + path);
			FileHandle fileHandle = Gdx.files.internal(path);
			
			if (!fileHandle.exists()) {
				throw new IOException("Map file does not exist: " + path);
			}
			
			BufferedReader br = new BufferedReader(fileHandle.reader());
			for (int i = 0; i < map[0].length; i++) {
				String line = null;
				line = br.readLine();
				if (line == null) {
					br.close();
					throw new IOException("Map file is incomplete or corrupted: " + path);
				}
				String[] tokens = line.split(" ");
				for (int j = 0; j < tokens.length; j++) {
					this.map[j][i] = Integer.parseInt(tokens[j]);
				}
			}
			br.close();
			it.unical.igpe.utils.DebugUtils.showMessage("Map loaded successfully: " + path);
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
