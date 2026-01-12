package it.unical.igpe.net;

import com.badlogic.gdx.math.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;

import it.unical.igpe.MapUtils.WorldLoader;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.AbstractDynamicObject;
import it.unical.igpe.logic.Bullet;
import it.unical.igpe.logic.Lootable;
import it.unical.igpe.logic.Player;
import it.unical.igpe.logic.Tile;
import it.unical.igpe.net.packet.Packet00Login;
import it.unical.igpe.net.packet.Packet04Death;
import it.unical.igpe.net.screens.MultiplayerOverScreen;
import it.unical.igpe.utils.DebugUtils;
import it.unical.igpe.utils.GameConfig;
import it.unical.igpe.utils.TileType;
import it.unical.igpe.utils.Updatable;

public class MultiplayerWorld implements Updatable {
	public static String username;
	public static String serverMapName = "arena.map"; // Default, will be set by server
	public static String serverMapContent = null; // Map content received from server
	public boolean gameOver = false;
	public static int keyCollected;
	public PlayerMP player;
	public List<AbstractDynamicObject> entities;

	private LinkedList<Bullet> bls;
	private static LinkedList<Tile> tiles;
	private static LinkedList<Lootable> lootables;
	private static LinkedList<Vector2> spawnPoints;
	public Vector2 dir;
	private WorldLoader manager;
	public boolean isServer = false;

	public MultiplayerWorld(String path, boolean isServer) {
		this(path, null, isServer);
	}
	
	public MultiplayerWorld(String path, String mapContent, boolean isServer) {
		this.isServer = isServer;
		tiles = new LinkedList<Tile>();
		lootables = new LinkedList<Lootable>();
		bls = new LinkedList<Bullet>();
		entities = new ArrayList<AbstractDynamicObject>();
		spawnPoints = new LinkedList<Vector2>();
		keyCollected = 0;

		manager = new WorldLoader(32, 32);
		try {
			if (mapContent != null && !mapContent.isEmpty()) {
				it.unical.igpe.utils.DebugUtils.showMessage("Loading multiplayer map from content (length: " + mapContent.length() + ")");
				manager.LoadMapFromContent(mapContent);
			} else {
				it.unical.igpe.utils.DebugUtils.showMessage("Loading multiplayer map from path: " + path);
				manager.LoadMap(path);
			}
			it.unical.igpe.utils.DebugUtils.showMessage("Multiplayer map loaded successfully");
		} catch (IOException e) {
			it.unical.igpe.utils.DebugUtils.showError("Map not found: " + path, e);
			System.out.println("Map not found: " + path);
			throw new RuntimeException("Failed to load map: " + path, e);
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Error loading multiplayer map: " + path, e);
			throw new RuntimeException("Failed to load map: " + path, e);
		}

		for (int x = 0; x < manager.map.length; x++)
			for (int y = 0; y < manager.map.length; y++) {
				if (manager.map[x][y] == 0)
					tiles.add(new Tile(new Vector2(x * 64, y * 64), TileType.GROUND));
				else if (manager.map[x][y] == 1)
					tiles.add(new Tile(new Vector2(x * 64, y * 64), TileType.WALL));
				else if (manager.map[x][y] == 17) {
					tiles.add(new Tile(new Vector2(x * 64, y * 64), TileType.GROUND));
					spawnPoints.add(new Vector2(x, y));
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
			}

		if (!isServer) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating multiplayer client player: " + username);
				player = new PlayerMP(randomSpawn(), this, username, null, -1);
				this.addEntity(player);
				Packet00Login loginPacket = new Packet00Login(player.getUsername(), (int) player.getBoundingBox().x,
						(int) player.getBoundingBox().y);
				// If the client has started a server, add it as a connection
				if (IGPEGame.game.socketServer != null) {
					it.unical.igpe.utils.DebugUtils.showMessage("Adding connection to local server");
					IGPEGame.game.socketServer.addConnection((PlayerMP) player, loginPacket);
				}
				if (IGPEGame.game.socketClient != null) {
					loginPacket.writeData(IGPEGame.game.socketClient);
					it.unical.igpe.utils.DebugUtils.showMessage("Login packet sent to server");
				} else {
					it.unical.igpe.utils.DebugUtils.showError("socketClient is null, cannot send login packet");
				}
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Error creating multiplayer client player", e);
				throw e;
			}
		}
	}

	public static Vector2 randomSpawn() {
		return spawnPoints.get(new Random().nextInt(spawnPoints.size()));
	}

	public void update(float delta) {

		player.state = Player.STATE_IDLE;

		if (player.isReloading(delta))
			player.state = Player.STATE_RELOADING;

		player.activeWeapon.lastFired += delta;

		String Killer = null;
		// Bullet collisions
		synchronized (bls) {
			if (!bls.isEmpty()) {
				boolean removed = false;
				ListIterator<Bullet> it = bls.listIterator();
				while (it.hasNext()) {
					ListIterator<AbstractDynamicObject> iter = entities.listIterator();
					Bullet b = it.next();
					b.update(delta);

					// Skip collision for first 0.1 seconds
					if (b.getLifetime() < 0.1f) {
						continue;
					}

					while (iter.hasNext()) {
						PlayerMP a = (PlayerMP) iter.next();
						if (!b.getID().equalsIgnoreCase(((PlayerMP) a).getUsername())
								&& b.getBoundingBox().overlaps(a.getBoundingBox()) && a.Alive()) {
							it.remove();
							removed = true;
							if (a.getUsername() == this.player.getUsername()) {
								this.player.hit(b.getHP());
								Killer = b.getID();
							}
						}
					}
					if (removed)
						continue;
					TileType tmp = getNextTile(b.getBoundingBox());
					if (tmp == TileType.WALL)
						it.remove();
				}
			}
		}

		if (this.player.getHP() <= 0) {
			Packet04Death packet = new Packet04Death(Killer, this.player.getUsername());
			packet.writeData(IGPEGame.game.socketClient);

			// For host player (port -1), handle death locally immediately
			// since they won't receive the death packet back from the server
			if (this.player.port == -1 && this.player.ipAddress == null) {
				this.handleDeath(Killer, this.player.getUsername());
			}
		}
	}

	public static TileType getNextTile(Rectangle _box) {
		for (Tile tile : tiles) {
			if (Math.sqrt(Math.pow((_box.x - tile.getBoundingBox().x), 2)
					+ Math.pow(_box.y - tile.getBoundingBox().y, 2)) < 128) {
				if (tile.getType() != TileType.GROUND && tile.getType() != TileType.ENDLEVEL
						&& _box.overlaps(tile.getBoundingBox()))
					return TileType.WALL;
				else if (tile.getType() == TileType.ENDLEVEL && _box.overlaps(tile.getBoundingBox()))
					return TileType.ENDLEVEL;
			}
		}
		return TileType.GROUND;
	}

	public synchronized void removePlayerMP(String username) {
		int index = 0;
		boolean found = false;
		for (AbstractDynamicObject e : entities) {
			if (e instanceof PlayerMP && ((PlayerMP) e).getUsername().equals(username)) {
				found = true;
				break;
			}
			index++;
		}
		if (found && index < this.getEntities().size()) {
			this.getEntities().remove(index);
		}
	}

	public int getPlayerMPIndex(String username) {
		int index = 0;
		for (AbstractDynamicObject e : this.getEntities()) {
			if (e instanceof PlayerMP && ((PlayerMP) e).getUsername().equals(username)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	public synchronized void movePlayer(String username, int x, int y, float angle, int state, int weapon) {
		int index = getPlayerMPIndex(username);
		int localPlayerIndex = getPlayerMPIndex(player.username);
		if (index >= 0 && index < this.getEntities().size() && index != localPlayerIndex) {
			AbstractDynamicObject obj = this.getEntities().get(index);
			if (obj instanceof PlayerMP) {
				PlayerMP p = (PlayerMP) obj;
				p.getBoundingBox().x = x;
				p.getBoundingBox().y = y;
				p.angle = angle;
				p.state = state;
				if (weapon == 0)
					p.activeWeapon = p.pistol;
				else if (weapon == 1)
					p.activeWeapon = p.shotgun;
				else if (weapon == 2)
					p.activeWeapon = p.rifle;
			}
		}
	}

	public void fireBullet(String username, int x, int y, float angle, int weapon) {
		synchronized (bls) {
			float x2 = (float) (16 * Math.cos(Math.toRadians(angle)) - 16 * Math.sin(Math.toRadians(angle)));
			float y2 = (float) (16 * Math.sin(Math.toRadians(angle)) + 16 * Math.cos(Math.toRadians(angle)));
			Vector2 shotPos = new Vector2(x + 32 + x2, y + 32 + y2);
			Bullet newBullet = null;
			if (weapon == 1) {
				newBullet = new Bullet(shotPos, (float) Math.toRadians(angle + 90f), username, 15);
				this.bls.add(newBullet);
				MultiplayerWorldRenderer.pistolShot = true;
				MultiplayerWorldRenderer.shotPos = shotPos;
			} else if (weapon == 2) {
				this.bls.add(new Bullet(shotPos, (float) Math.toRadians(angle + 90f), username, 34));
				this.bls.add(new Bullet(shotPos, (float) Math.toRadians(angle + 100f), username, 34));
				this.bls.add(new Bullet(shotPos, (float) Math.toRadians(angle + 80f), username, 34));
				MultiplayerWorldRenderer.shotgunShot = true;
				MultiplayerWorldRenderer.shotPos = shotPos;
			} else {
				newBullet = new Bullet(shotPos, (float) Math.toRadians(angle + 90f), username, 50);
				this.bls.add(newBullet);
				MultiplayerWorldRenderer.rifleShot = true;
				MultiplayerWorldRenderer.shotPos = shotPos;
			}
		}
	}

	public void handleDeath(String usernameKiller, String usernameKilled) {
		if (usernameKiller.equalsIgnoreCase(this.player.username))
			this.player.kills++;
		else if (usernameKilled.equalsIgnoreCase(this.player.username)) {
			this.player.deaths++;
			this.player.setHP(100);
			this.player.setPos(randomSpawn());
		}
	}

	public void handleGameOver(String usernameWinner, int killsWinner) {
		MultiplayerOverScreen.winner = usernameWinner;
		MultiplayerOverScreen.kills = killsWinner;
		this.gameOver = true;
	}

	public PlayerMP getPlayer() {
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

	public synchronized void addEntity(AbstractDynamicObject player) {
		this.getEntities().add(player);
	}

	public synchronized List<AbstractDynamicObject> getEntities() {
		return this.entities;
	}

	public boolean isGameOver() {
		return gameOver;
	}

}
