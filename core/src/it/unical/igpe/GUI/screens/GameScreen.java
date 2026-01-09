package it.unical.igpe.GUI.screens;

import com.badlogic.gdx.math.Rectangle;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;

import it.unical.igpe.GUI.SoundManager;
import it.unical.igpe.GUI.TouchController;
import it.unical.igpe.GUI.HUD.HUD;
import it.unical.igpe.MapUtils.WorldRenderer;
import it.unical.igpe.MapUtils.World;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.Player;
import it.unical.igpe.utils.GameConfig;
import it.unical.igpe.utils.TileType;

public class GameScreen implements Screen {
	World world;
	HUD hud;
	WorldRenderer renderer;
	TouchController touchController;
	boolean isAndroid;
	private boolean renderStarted = false;

	public GameScreen(String path) {
		this(path, null);
	}
	
	public GameScreen(String path, String mapContent) {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Creating GameScreen with map: " + path + (mapContent != null ? " (from content, length: " + mapContent.length() + ")" : ""));
			if (mapContent != null) {
				int lineCount = mapContent.split("\n").length;
				it.unical.igpe.utils.DebugUtils.showMessage("Map content has " + lineCount + " lines, preview (first 200 chars): " + mapContent.substring(0, Math.min(200, mapContent.length())));
			}
			this.world = new World(path, mapContent);
			it.unical.igpe.utils.DebugUtils.showMessage("World created successfully");
			// Don't create HUD or WorldRenderer here - OpenGL context might not be ready
			// Create them in show() method instead
			this.hud = null;
			this.renderer = null;
			it.unical.igpe.utils.DebugUtils.showMessage("HUD and WorldRenderer creation deferred to show() method");
			this.isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
			// TouchController will be created in show() after renderer is ready
			this.touchController = null;
			it.unical.igpe.utils.DebugUtils.showMessage("GameScreen created successfully - all components initialized");
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Failed to create GameScreen: " + path + " - " + e.getMessage(), e);
			e.printStackTrace();
			// Also log to Gdx.app.error for Android logcat
			com.badlogic.gdx.Gdx.app.error("GameScreen", "Failed to create GameScreen: " + e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void show() {
		it.unical.igpe.utils.DebugUtils.showMessage("=== GameScreen.show() called ===");
		Gdx.input.setInputProcessor(null);
		SoundManager.manager.get(SoundManager.MenuMusic, Music.class).pause();
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setVolume(GameConfig.MUSIC_VOLUME);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setLooping(true);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).play();
		
		// Always check if assets are actually loaded by trying to get one
		// The progress check alone isn't reliable
		boolean assetsReallyLoaded = false;
		try {
			it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
			assetsReallyLoaded = true;
			it.unical.igpe.utils.DebugUtils.showMessage("Assets verified as loaded (ground.png exists)");
		} catch (Exception e) {
			assetsReallyLoaded = false;
			it.unical.igpe.utils.DebugUtils.showMessage("Assets NOT loaded, loading now...");
		}
		
		if (!assetsReallyLoaded) {
			it.unical.igpe.GUI.Assets.load();
			// Force finish loading immediately (synchronous)
			it.unical.igpe.utils.DebugUtils.showMessage("Waiting for assets to finish loading...");
			while (!it.unical.igpe.GUI.Assets.manager.update()) {
				// Wait for assets to load
			}
			it.unical.igpe.GUI.Assets.manager.finishLoading();
			it.unical.igpe.utils.DebugUtils.showMessage("Assets finished loading synchronously");
			
			// Verify again
			try {
				it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
				it.unical.igpe.utils.DebugUtils.showMessage("Assets verified after loading");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Assets failed to load even after finishLoading()!", e);
			}
		}
		
		// Don't create OpenGL objects here - defer to first render() call
		// The OpenGL context might not be fully ready even in show()
		it.unical.igpe.utils.DebugUtils.showMessage("OpenGL objects will be created in first render() call");
		
		it.unical.igpe.utils.DebugUtils.showMessage("GameScreen.show() completed - music started, input processor set");
	}

	@Override
	public void render(float delta) {
		// First render call - log it
		if (!renderStarted) {
			it.unical.igpe.utils.DebugUtils.showMessage("=== GameScreen.render() called for first time ===");
			it.unical.igpe.utils.DebugUtils.showMessage("World: " + (world != null ? "OK" : "NULL"));
			it.unical.igpe.utils.DebugUtils.showMessage("World.player: " + (world != null && world.getPlayer() != null ? "OK" : "NULL"));
			it.unical.igpe.utils.DebugUtils.showMessage("Renderer: " + (renderer != null ? "OK" : "NULL"));
			it.unical.igpe.utils.DebugUtils.showMessage("HUD: " + (hud != null ? "OK" : "NULL"));
			renderStarted = true;
		}
		
		try {
			// Ensure OpenGL objects are created (lazy initialization)
			ensureOpenGLObjectsReady();
			
			if (world == null || world.getPlayer() == null) {
				it.unical.igpe.utils.DebugUtils.showError("World or Player is null in render!", null);
				return;
			}
			
			if (renderer == null) {
				// Renderer not ready yet (assets still loading or OpenGL not ready), skip this frame
				return;
			}
			
			// Assets should already be loaded from ensureOpenGLObjectsReady(), but double-check
			if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
				it.unical.igpe.utils.DebugUtils.showMessage("Assets not ready in render, waiting... progress: " + it.unical.igpe.GUI.Assets.manager.getProgress());
				it.unical.igpe.GUI.Assets.manager.update();
				if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
					return; // Skip this frame, assets still loading
				}
				it.unical.igpe.GUI.Assets.manager.finishLoading();
			}
			
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			if (World.player.isSlowMo(delta))
				delta *= 0.5f;

			world.update(delta);
			if (isAndroid && touchController != null) {
				touchController.update();
			}
			handleInput(delta);
			renderer.render(delta);
			if (hud != null) {
				hud.render(world.getPlayer());
			}
			
			// Render touch controls on top of everything (Android only)
			if (isAndroid && touchController != null && renderer != null) {
				touchController.render(renderer.getBatch());
			}
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Exception in GameScreen.render(): " + e.getMessage(), e);
			e.printStackTrace();
			com.badlogic.gdx.Gdx.app.error("GameScreen", "Render exception", e);
		}
	}
	
	private void ensureOpenGLObjectsReady() {
		// Verify assets are actually loaded by trying to get one
		boolean assetsReady = false;
		try {
			it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
			assetsReady = true;
		} catch (Exception e) {
			// Assets not loaded, try to load them
			if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
				it.unical.igpe.GUI.Assets.manager.update();
				if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
					it.unical.igpe.utils.DebugUtils.showMessage("Assets still loading... progress: " + it.unical.igpe.GUI.Assets.manager.getProgress());
					return; // Wait for assets to load
				}
				it.unical.igpe.GUI.Assets.manager.finishLoading();
			}
			// Try again
			try {
				it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
				assetsReady = true;
			} catch (Exception e2) {
				it.unical.igpe.utils.DebugUtils.showMessage("Assets still not ready, skipping this frame");
				return;
			}
		}
		
		if (!assetsReady) {
			return;
		}
		
		// Create WorldRenderer and HUD on first render call when OpenGL is definitely ready
		if (this.renderer == null) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating WorldRenderer now (in render, OpenGL context is active)");
				this.renderer = new WorldRenderer(world);
				it.unical.igpe.utils.DebugUtils.showMessage("WorldRenderer created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create WorldRenderer in render()", e);
				return; // Don't throw, just skip rendering this frame
			}
		}
		
		if (this.hud == null) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating HUD now (in render, OpenGL context is active)");
				this.hud = new HUD(false);
				it.unical.igpe.utils.DebugUtils.showMessage("HUD created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create HUD in render()", e);
				return; // Don't throw, just skip rendering this frame
			}
		}
		
		// Recreate touch controller now that renderer is ready
		if (isAndroid && this.touchController == null && this.renderer != null) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating TouchController now");
				this.touchController = new TouchController(renderer.camera);
				it.unical.igpe.utils.DebugUtils.showMessage("TouchController created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create TouchController in render()", e);
			}
		}

		if (world.isLevelFinished()) {
			ScreenManager.LCompletedS.kills = World.player.kills;
			IGPEGame.game.setScreen(ScreenManager.LCompletedS);
		} else if (world.isGameOver()) {
			ScreenManager.LCompletedS.kills = World.player.kills;
			ScreenManager.LCompletedS.gameOver = true;
			IGPEGame.game.setScreen(ScreenManager.LCompletedS);
		}
	}

	@Override
	public void resize(int width, int height) {
		if (renderer != null) {
			renderer.viewport.update(width, height, false);
		}
	}

	@Override
	public void dispose() {
		if (hud != null) {
			hud.dispose();
		}
		if (renderer != null) {
			renderer.dispose();
		}
		if (isAndroid && touchController != null) {
			touchController.dispose();
		}
	}

	/**
	 * Handle Inputs from the user
	 * 
	 * @param delta
	 *            The time in seconds since the last update
	 */
	@SuppressWarnings("static-access")
	private void handleInput(float delta) {
		if (world.player.slowActive)
			delta *= 2f;

		// Handle aiming (mouse/touch)
		if (isAndroid && touchController != null) {
			// On Android, use aiming joystick on right side
			Vector2 aimDir = touchController.getAimDirection();
			if (aimDir != null) {
				// Convert joystick direction to angle
				world.player.angle = aimDir.angle();
			}
		} else {
			// Desktop: use mouse
			float midX = Gdx.graphics.getWidth() / 2;
			float midY = Gdx.graphics.getHeight() / 2;
			float mouseX = Gdx.input.getX();
			float mouseY = Gdx.input.getY();
			Vector2 dir = new Vector2(mouseX - midX, mouseY - midY);
			dir.rotate90(-1);
			world.player.angle = dir.angle();
		}

		Rectangle box = new Rectangle();
		
		// Handle movement - Android uses touch controller, Desktop uses keyboard
		Vector2 moveDir = null;
		if (isAndroid && touchController != null) {
			moveDir = touchController.getMovementDirection();
		}
		
		// Movements and Collisions of the player
		if (isAndroid && touchController != null && moveDir != null && moveDir.len() > 0.1f) {
			// Android touch movement
			float moveX = moveDir.x * GameConfig.MOVESPEED * delta;
			float moveY = moveDir.y * GameConfig.MOVESPEED * delta;
			
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			
			box = new Rectangle(world.player.getX() + moveX, world.player.getY() + moveY,
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x += moveX;
				world.player.getBoundingBox().y += moveY;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += moveX;
				world.player.getBoundingBox().y += moveY;
			}
		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() - (int) (GameConfig.MOVESPEED * delta),
					world.player.getY() - (int) (GameConfig.MOVESPEED * delta), world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() + (int) (GameConfig.MOVESPEED * delta),
					world.player.getY() - (int) (GameConfig.MOVESPEED * delta), world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() - (int) (GameConfig.MOVESPEED * delta),
					world.player.getY() + (int) (GameConfig.MOVESPEED * delta), world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() + (int) (GameConfig.MOVESPEED * delta),
					world.player.getY() + (int) (GameConfig.MOVESPEED * delta), world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX(), world.player.getY() - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().y -= GameConfig.MOVESPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().y -= GameConfig.MOVESPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() - (int) (GameConfig.MOVESPEED * delta), world.player.getY(),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x -= GameConfig.MOVESPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x -= GameConfig.MOVESPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX(), world.player.getY() + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().y += GameConfig.MOVESPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().y += GameConfig.MOVESPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading() && !world.player.isShooting())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle(world.player.getX() + (int) (GameConfig.MOVESPEED * delta), world.player.getY(),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = World.getNextTile(box);
			if (tmp == TileType.ENDLEVEL && World.isDoorUnlocked()) {
				World.finished = true;
				world.player.getBoundingBox().x += GameConfig.MOVESPEED * delta;
			} else if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += GameConfig.MOVESPEED * delta;
			}

		}

		// Fire and Reloading action of the player
		boolean shouldFire = false;
		if (isAndroid && touchController != null) {
			shouldFire = touchController.isShootPressed() && world.player.canShoot();
		} else {
			shouldFire = Gdx.input.isButtonPressed(Buttons.LEFT) && world.player.canShoot();
		}
		
		if (shouldFire) {
			if (world.player.getActWeapon() == "pistol") {
				SoundManager.manager.get(SoundManager.PistolFire, Sound.class).play(GameConfig.SOUND_VOLUME);
				world.player.fire();
			} else if (world.player.getActWeapon() == "shotgun") {
				SoundManager.manager.get(SoundManager.ShotgunFire, Sound.class).play(GameConfig.SOUND_VOLUME);
				world.player.fire();
			} else if (world.player.getActWeapon() == "rifle") {
				SoundManager.manager.get(SoundManager.RifleFire, Sound.class).play(GameConfig.SOUND_VOLUME);
				world.player.fire();
			}
		}

		// Reload
		boolean shouldReload = false;
		if (isAndroid && touchController != null) {
			shouldReload = touchController.isReloadPressed() && world.player.canReload();
		} else {
			shouldReload = Gdx.input.isKeyJustPressed(Input.Keys.R) && world.player.canReload();
		}
		
		if (shouldReload || world.player.checkAmmo()) {
			world.player.reload();
			if (world.player.getActWeapon() == "pistol") {
				SoundManager.manager.get(SoundManager.PistolReload, Sound.class).play(GameConfig.SOUND_VOLUME);
			} else if (world.player.getActWeapon() == "shotgun") {
				SoundManager.manager.get(SoundManager.ShotgunReload, Sound.class).play(GameConfig.SOUND_VOLUME);
			}
		}
		
		// Weapon switching
		if (isAndroid && touchController != null) {
			if (touchController.isPistolButtonPressed()) {
				world.player.setActWeapon("pistol");
			} else if (touchController.isShotgunButtonPressed()) {
				world.player.setActWeapon("shotgun");
			} else if (touchController.isRifleButtonPressed()) {
				world.player.setActWeapon("rifle");
			}
		} else {
			if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
				world.player.setActWeapon("pistol");
			} else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
				world.player.setActWeapon("shotgun");
			} else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
				world.player.setActWeapon("rifle");
			}
		}

		// Slow-Motion
		boolean shouldToggleSlowMo = false;
		if (isAndroid && touchController != null) {
			shouldToggleSlowMo = touchController.isSlowMotionPressed();
		} else {
			shouldToggleSlowMo = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
		}
		
		if (shouldToggleSlowMo) {
			if (world.player.slowActive)
				world.player.slowActive = false;
			else
				world.player.slowActive = true;
		}

		// Pause/Escape
		boolean shouldPause = false;
		if (isAndroid && touchController != null) {
			shouldPause = touchController.isPausePressed();
		} else {
			shouldPause = Gdx.input.isKeyJustPressed(Keys.ESCAPE);
		}
		
		if (shouldPause)
			IGPEGame.game.setScreen(ScreenManager.PS);
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
