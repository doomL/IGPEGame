package it.unical.igpe.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import it.unical.igpe.GUI.Assets;
import it.unical.igpe.GUI.SoundManager;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.AbstractDynamicObject;
import it.unical.igpe.logic.Bullet;
import it.unical.igpe.logic.Player;
import it.unical.igpe.logic.Tile;
import it.unical.igpe.utils.GameConfig;
import it.unical.igpe.utils.TileType;

public class MultiplayerWorldRenderer {
	public static boolean pistolShot;
	public static boolean shotgunShot;
	public static boolean rifleShot;
	public static Vector2 shotPos;
	
	private OrthographicCamera camera;
	public OrthographicCamera getCamera() { return camera; }
	public Viewport viewport;
	private SpriteBatch batch;
	public SpriteBatch getBatch() { return batch; }
	private ShapeRenderer sr;
	private float stateTime;
	// CRITICAL: Don't store world reference - it can be replaced when map is reloaded!
	// Always get it fresh from IGPEGame.game.worldMP to avoid stale reference bug

	public MultiplayerWorldRenderer(MultiplayerWorld world) {
		// Don't store world reference! It can be replaced when map is reloaded
		this.camera = new OrthographicCamera();
		// Use same camera settings as single-player to avoid stretching
		// Note: setToOrtho with yDown=true means Y increases downward
		this.camera.setToOrtho(true, 800, 800);
		// Only set camera position if player exists (client side)
		// Use getPlayer() which is safer
		PlayerMP player = world.getPlayer();
		if (player != null) {
			this.camera.position.x = player.getX();
			this.camera.position.y = player.getY();
		} else {
			// Server side or player not created yet - use default position
			this.camera.position.x = 400;
			this.camera.position.y = 400;
		}
		this.camera.update();
		// Use ExtendViewport to fill screen while maintaining aspect ratio (no stretching)
		// ExtendViewport extends the world to fill screen, maintaining aspect ratio
		this.viewport = new ExtendViewport(800, 800, camera);
		this.batch = new SpriteBatch();
		batch.setColor(1f, 1f, 1f, 0.7f);
		this.sr = new ShapeRenderer();
		this.sr.setColor(Color.BLACK);
	}

	public void render(float deltaTime) {
		stateTime += deltaTime;

		// Get fresh world reference (it can be replaced when map is reloaded!)
		MultiplayerWorld world = IGPEGame.game.worldMP;
		if (world == null) {
			return; // World not ready yet
		}

		// Match single-player EXACT order: set projection matrices first, then update camera
		// (Single-player sets them before camera.update(), so we do the same)
		batch.setProjectionMatrix(camera.combined);
		sr.setProjectionMatrix(camera.combined);

		// Update camera position and update (match single-player)
		if (world.getPlayer() != null) {
			camera.position.x = world.getPlayer().getX();
			camera.position.y = world.getPlayer().getY();
		}
		camera.update();

		// Begin batch (handle state errors)
		try {
			batch.begin();
		} catch (IllegalStateException e) {
			// Batch was already active, end it first
			try {
				batch.end();
			} catch (Exception e2) {
				// Ignore
			}
			batch.begin();
		}

		// Drawing tiles
		LinkedList<Tile> tiles = world.getTiles();
		if (tiles == null || tiles.isEmpty()) {
			if (tiles == null) {
				it.unical.igpe.utils.DebugUtils.showError("Tiles list is null in renderer!", null);
			} else {
				it.unical.igpe.utils.DebugUtils.showError("Tiles list is EMPTY in renderer! Map not loaded yet.", null);
			}
			batch.end();
			return;
		}
		for (Tile tile : tiles) {
			if (tile == null) continue;
			batch.draw(Assets.manager.get(Assets.Ground, Texture.class), tile.getBoundingBox().x,
						tile.getBoundingBox().y);
			
			if (tile.getType() == TileType.WALL)
				batch.draw(Assets.manager.get(Assets.Wall, Texture.class), tile.getBoundingBox().x,
						tile.getBoundingBox().y);
			else if (tile.getType() == TileType.BOX)
				batch.draw(Assets.manager.get(Assets.Box, Texture.class), tile.getX(), tile.getY());
			else if (tile.getType() == TileType.BARREL) 
				batch.draw(Assets.manager.get(Assets.Barrel, Texture.class), tile.getX(), tile.getY());
			else if (tile.getType() == TileType.CACTUS) 
				batch.draw(Assets.manager.get(Assets.Cactus, Texture.class), tile.getX(), tile.getY());
			else if (tile.getType() == TileType.PLANT) 
				batch.draw(Assets.manager.get(Assets.Plant, Texture.class), tile.getX(), tile.getY());
			else if (tile.getType() == TileType.LOGS)
				batch.draw(Assets.manager.get(Assets.Logs, Texture.class), tile.getX(), tile.getY());
		}
		
		// Drawing players
		batch.setColor(1f, 1f, 1f, 1f);
		List<AbstractDynamicObject> entitiesCopy;
		try {
			synchronized (world.getEntities()) {
				entitiesCopy = new ArrayList<>(world.getEntities());
			}
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Error getting entities in renderer", e);
			entitiesCopy = new ArrayList<>();
		}
		for (AbstractDynamicObject obj : entitiesCopy) {
			if (obj == null || !(obj instanceof PlayerMP)) continue;
			PlayerMP e = (PlayerMP) obj;
			if (e == null) continue;
			if (e.state == Player.STATE_RUNNING) {
				e.timeToNextStep -= deltaTime;
				if (e.timeToNextStep < 0) {
					float boundary = camera.viewportWidth / 2;
					float xDistance = e.getX() - camera.position.x;
					float distance = camera.position.dst(e.getX(), e.getY(), 0) * Math.signum(xDistance);
					distance = Math.min(boundary, Math.max(distance, -boundary));
					SoundManager.manager.get(SoundManager.Step, Sound.class).play(
							GameConfig.SOUND_VOLUME * (1 - Math.abs(distance) / boundary), 1.0f, xDistance / boundary);
					while (e.timeToNextStep < 0)
						e.timeToNextStep += 0.35f;
				}
			} else {
				e.timeToNextStep = 0;
			}
			if (e.getUsername() != null && MultiplayerWorld.username != null && e.getUsername().equalsIgnoreCase(MultiplayerWorld.username)) {
				if (e.getActWeapon() == "pistol") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.idlePistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.reloadingPistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.runningPistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				} else if (e.getActWeapon() == "shotgun") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.idleShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.reloadingShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.runningShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				} else if (e.getActWeapon() == "rifle") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.idleRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.reloadingRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.runningRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				}
			} else {
				if (e.getActWeapon() == "pistol") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.eIdlePistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.eReloadingPistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.eRunningPistolAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				} else if (e.getActWeapon() == "shotgun") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.eIdleShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.eReloadingShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.eRunningShotgunAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				} else if (e.getActWeapon() == "rifle") {
					if (e.state == Player.STATE_IDLE)
						batch.draw(Assets.eIdleRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RELOADING)
						batch.draw(Assets.eReloadingRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
					else if (e.state == Player.STATE_RUNNING)
						batch.draw(Assets.eRunningRifleAnimation.getKeyFrame(stateTime, true), e.getBoundingBox().x,
								e.getBoundingBox().y, 32, 32, 64, 64, 1f, 1f, e.angle);
				}
			}
		}
		batch.setColor(1f, 1f, 1f, 0.7f);
		batch.end();

		// Draw Bullets with proper synchronization
		// CRITICAL FIX: Set projection matrix AFTER camera.update() and BEFORE sr.begin()
		// This ensures ShapeRenderer uses the updated camera matrix
		sr.setProjectionMatrix(camera.combined);

		LinkedList<Bullet> bullets = world.getBls();
		if (bullets != null && bullets.size() > 0) {
			sr.begin(ShapeType.Filled);
			try {
				for (Bullet bullet : bullets) {
					if (bullet != null) {
						sr.circle(bullet.getX(), bullet.getY(), 4);
					}
				}
			} catch (Exception e) {
				// Concurrent modification, skip this frame
			}
			sr.end();
		}
		
		if(pistolShot)
			this.firePistol();
		else if(shotgunShot)
			this.fireShotgun();
		else if(rifleShot)
			this.fireRifle();

	}

	public void dispose() {
		batch.dispose();
	}
	
	public void firePistol() {
		pistolShot = false;
		float boundary = camera.viewportWidth / 2;
		float xDistance = shotPos.x - camera.position.x;
		float distance = camera.position.dst(shotPos.x, shotPos.y, 0) * Math.signum(xDistance);
		distance = Math.min(boundary, Math.max(distance, -boundary));
		SoundManager.manager.get(SoundManager.PistolFire, Sound.class).play(
				GameConfig.SOUND_VOLUME * (1 - Math.abs(distance) / boundary), 1.0f, xDistance / boundary);
	}
	
	public void fireShotgun() {
		shotgunShot = false;
		float boundary = camera.viewportWidth / 2;
		float xDistance = shotPos.x - camera.position.x;
		float distance = camera.position.dst(shotPos.x, shotPos.y, 0) * Math.signum(xDistance);
		distance = Math.min(boundary, Math.max(distance, -boundary));
		SoundManager.manager.get(SoundManager.ShotgunFire, Sound.class).play(
				GameConfig.SOUND_VOLUME * (1 - Math.abs(distance) / boundary), 1.0f, xDistance / boundary);
	}

	public void fireRifle() {
		rifleShot = false;
		float boundary = camera.viewportWidth / 2;
		float xDistance = shotPos.x - camera.position.x;
		float distance = camera.position.dst(shotPos.x, shotPos.y, 0) * Math.signum(xDistance);
		distance = Math.min(boundary, Math.max(distance, -boundary));
		SoundManager.manager.get(SoundManager.RifleFire, Sound.class).play(
				GameConfig.SOUND_VOLUME * (1 - Math.abs(distance) / boundary), 1.0f, xDistance / boundary);
	}

}
