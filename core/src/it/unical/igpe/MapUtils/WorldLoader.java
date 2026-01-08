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
		FileHandle fileHandle = Gdx.files.internal(path);
		BufferedReader br = new BufferedReader(fileHandle.reader());
		for (int i = 0; i < map[0].length; i++) {
			String line = null;
			line = br.readLine();
			String[] tokens = line.split(" ");
			for (int j = 0; j < tokens.length; j++) {
				this.map[j][i] = Integer.parseInt(tokens[j]);
			}
		}
		br.close();
	}
}
