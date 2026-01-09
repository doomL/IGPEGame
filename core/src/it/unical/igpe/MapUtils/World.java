package it.unical.igpe.MapUtils;

import com.badlogic.gdx.math.Rectangle;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;

import it.unical.igpe.GUI.SoundManager;
import it.unical.igpe.GUI.screens.ScreenManager;
import it.unical.igpe.ai.EnemyManager;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.Bullet;
import it.unical.igpe.logic.Enemy;
import it.unical.igpe.logic.Lootable;
import it.unical.igpe.logic.Player;
import it.unical.igpe.logic.Tile;
import it.unical.igpe.utils.GameConfig;
import it.unical.igpe.utils.LootableType;
import it.unical.igpe.utils.TileType;
import it.unical.igpe.utils.Updatable;

public class World implements Updatable {
	public static boolean finished;
	public static int keyCollected;

	public static Player player;
	private LinkedList<Bullet> bls;
	private static LinkedList<Tile> tiles;
	private static LinkedList<Lootable> lootables;
	public LinkedList<Enemy> ens;
	public EnemyManager EM;
	public Vector2 dir;
	private WorldLoader manager;

	public World(String path) {
		player = new Player(new Vector2(), this, null);
		tiles = new LinkedList<Tile>();
		lootables = new LinkedList<Lootable>();
		ens = new LinkedList<Enemy>();
		bls = new LinkedList<Bullet>();
		
		finished = false;
		keyCollected = 0;

		manager = new WorldLoader(GameConfig.TILEDIM, GameConfig.TILEDIM);
		try {
			manager.LoadMap(path);
		} catch (IOException e) {
			it.unical.igpe.utils.DebugUtils.showError("Failed to load map: " + path, e);
			System.out.println("Map not found: " + path);
			IGPEGame.game.setScreen(ScreenManager.LCS);
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Unexpected error loading map: " + path, e);
			IGPEGame.game.setScreen(ScreenManager.LCS);
		}
	}
	
	public World(String path, String mapContent) {
		player = new Player(new Vector2(), this, null);
		tiles = new LinkedList<Tile>();
		lootables = new LinkedList<Lootable>();
		ens = new LinkedList<Enemy>();
		bls = new LinkedList<Bullet>();
		
		finished = false;
		keyCollected = 0;

		manager = new WorldLoader(GameConfig.TILEDIM, GameConfig.TILEDIM);
		try {
			if (mapContent != null && !mapContent.isEmpty()) {
				it.unical.igpe.utils.DebugUtils.showMessage("Loading map from content, length: " + mapContent.length());
				it.unical.igpe.utils.DebugUtils.showMessage("Content preview (first 200 chars): " + mapContent.substring(0, Math.min(200, mapContent.length())));
				manager.LoadMapFromContent(mapContent);
			} else {
				it.unical.igpe.utils.DebugUtils.showMessage("Loading map from path: " + path);
				manager.LoadMap(path);
			}
		} catch (IOException e) {
			it.unical.igpe.utils.DebugUtils.showError("Failed to load map: " + path, e);
			e.printStackTrace();
			throw new RuntimeException("Failed to load map: " + path + " - " + e.getMessage(), e);
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Unexpected error loading map: " + path, e);
			e.printStackTrace();
			throw new RuntimeException("Unexpected error loading map: " + path + " - " + e.getMessage(), e);
		}
		
		// Verify map was loaded
		if (manager.map == null || manager.map.length == 0) {
			throw new RuntimeException("Map array is null or empty after loading: " + path);
		}
		
		it.unical.igpe.utils.DebugUtils.showMessage("Map verified, starting tile/enemy population. Map size: " + manager.map.length + "x" + (manager.map.length > 0 ? manager.map[0].length : 0));

		// Map is int[height][width] = int[row][column]
		// x is row, y is column
		try {
			int tileCount = 0;
			for (int x = 0; x < manager.map.length; x++) {
				for (int y = 0; y < (manager.map.length > 0 ? manager.map[0].length : 0); y++) {
					if (manager.map[x][y] == 0) // Ground
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				else if (manager.map[x][y] == 1) // Wall
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.WALL));
				else if (manager.map[x][y] == 2) // EndLevel
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.ENDLEVEL));
				else if (manager.map[x][y] == 3) { // AmmoPack
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.AMMOPACK));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 4) { // HealthPack
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.HEALTPACK));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 5) { // Trap
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.TRAP));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 6) { // Yellow Key
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.KEYY));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 7) { // Red Key
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.KEYR));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 8) { // Blue Key
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.KEYB));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 9) { // Green Key
					lootables.add(new Lootable(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM),
							LootableType.KEYG));
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
				} else if (manager.map[x][y] == 10) { // Player
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
					player.setPos(new Vector2(x, y));
				} else if (manager.map[x][y] == 11) { // Enemy
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.GROUND));
					Enemy e = new Enemy(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM));
					ens.add(e);
				} else if (manager.map[x][y] == 12) // Box
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.BOX));
				else if (manager.map[x][y] == 13) { // Barrel
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.BARREL));
				} else if (manager.map[x][y] == 14) { // Cactus
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.CACTUS));
				} else if (manager.map[x][y] == 15) { // Plant
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.PLANT));
				} else if (manager.map[x][y] == 16) { // Logs
					tiles.add(new Tile(new Vector2(x * GameConfig.TILEDIM, y * GameConfig.TILEDIM), TileType.LOGS));
				}
					tileCount++;
				}
			}
			it.unical.igpe.utils.DebugUtils.showMessage("Tile/enemy population completed. Created " + tileCount + " cells, " + tiles.size() + " total tiles, " + ens.size() + " enemies, " + lootables.size() + " lootables");
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Error during tile/enemy population", e);
			e.printStackTrace();
			throw new RuntimeException("Failed to populate world from map: " + e.getMessage(), e);
		}
		dir = new Vector2();
		EM = new EnemyManager(this);
		it.unical.igpe.utils.DebugUtils.showMessage("World constructor completed successfully");
	}

	@SuppressWarnings("static-access")
	public void update(float delta) {
		player.state = Player.STATE_IDLE;

		if (player.isReloading(delta) && player.hasAmmo())
			player.state = Player.STATE_RELOADING;

		if (player.isShooting(delta))
			player.state = Player.STATE_SHOOTING;

		// Enemies
		EM.update(delta);
		if (!bls.isEmpty()) {
			boolean removed = false;
			ListIterator<Bullet> it = bls.listIterator();
			while (it.hasNext()) {
				ListIterator<Enemy> iter = EM.getList().listIterator();
				Bullet b = it.next();
				b.update(delta);
				while (iter.hasNext()) {
					Enemy e = iter.next();
					if (b.getBoundingBox().overlaps(e.getBoundingBox()) && e.Alive() && b.getID() == "player") {
						it.remove();
						e.hit(b.getHP());
						removed = true;
						if (e.getHP() <= 0) {
							e.setAlive(false);
							player.kills++;
						}
					}
				}
				if (removed)
					continue;
				if (b.getBoundingBox().overlaps(player.getBoundingBox()) && b.getID() == "enemy") {
					it.remove();
					player.hit(b.getHP());
					continue;
				}
				TileType tmp = getNextTile(b.getBoundingBox());
				if (tmp == TileType.WALL)
					it.remove();
			}
		}

		// Checking lootable items
		Iterator<Lootable> itl = lootables.iterator();
		while (itl.hasNext()) {
			Lootable l = itl.next();
			if (l.getBoundingBox().overlaps(player.getBoundingBox())) {
				if (l.getType() == LootableType.HEALTPACK && player.getHP() < 100) {
					player.setHP(player.getHP() + 25);
					SoundManager.manager.get(SoundManager.HealthRestored, Sound.class).play(GameConfig.SOUND_VOLUME);
					itl.remove();
				} else if (l.getType() == LootableType.AMMOPACK) {
					if (player.pistol.canAdd() || player.shotgun.canAdd() || player.rifle.canAdd()) {
						player.pistol.addAmmo(15);
						player.shotgun.addAmmo(6);
						player.rifle.addAmmo(5);
						itl.remove();
					}
				} else if (l.getType() == LootableType.TRAP && l.closed == false) {
					player.setHP(player.getHP() - 50);
					SoundManager.manager.get(SoundManager.TrapClosing, Sound.class).play(GameConfig.SOUND_VOLUME);
					l.closed = true;
				} else if (l.getType() == LootableType.KEYY || l.getType() == LootableType.KEYR
						|| l.getType() == LootableType.KEYG || l.getType() == LootableType.KEYB) {
					this.keyCollected++;
					itl.remove();
				}
			}
		}

	}

	public static TileType getNextTile(Rectangle _box) {
		for (Tile tile : tiles) {
			if (Math.sqrt(Math.pow((_box.x - tile.getBoundingBox().x), 2)
					+ Math.pow(_box.y - tile.getBoundingBox().y, 2)) < 128) {
				if (tile.getType() != TileType.GROUND && tile.getType() != TileType.ENDLEVEL && _box.overlaps(tile.getBoundingBox()))
					return TileType.WALL;
				else if (tile.getType() == TileType.ENDLEVEL && _box.overlaps(tile.getBoundingBox()))
					return TileType.ENDLEVEL;
			}
		}
		return TileType.GROUND;
	}

	public static boolean isDoorUnlocked() {
		if (keyCollected == 4) {
			return true;
		}
		return false;
	}

	public Player getPlayer() {
		return player;
	}

	public LinkedList<Bullet> getBls() {
		return bls;
	}

	public void addBullet(Bullet _bullet) {
		bls.add(_bullet);
	}

	public LinkedList<Tile> getTiles() {
		return tiles;
	}

	public LinkedList<Lootable> getLootables() {
		return lootables;
	}

	public boolean isLevelFinished() {
		return finished;
	}

	public boolean isGameOver() {
		if (player.getHP() <= 0)
			return true;
		return false;
	}
}