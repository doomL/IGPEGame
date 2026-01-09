package it.unical.igpe.net.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.math.Rectangle;

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
import it.unical.igpe.GUI.screens.ScreenManager;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.Player;
import it.unical.igpe.net.MultiplayerWorld;
import it.unical.igpe.net.MultiplayerWorldRenderer;
import it.unical.igpe.net.packet.Packet02Move;
import it.unical.igpe.utils.GameConfig;
import it.unical.igpe.utils.TileType;

public class MultiplayerGameScreen implements Screen {
	MultiplayerWorld world;
	HUD hud;
	MultiplayerWorldRenderer renderer;
	TouchController touchController;
	boolean isAndroid;
	private boolean renderStarted = false;

	public MultiplayerGameScreen() {
		it.unical.igpe.utils.DebugUtils.showMessage("=== MultiplayerGameScreen constructor START ===");
		
		// Check if world already exists (created by server or previous attempt)
		if (IGPEGame.game.worldMP != null) {
			it.unical.igpe.utils.DebugUtils.showMessage("Using existing MultiplayerWorld (from server or previous creation)");
			this.world = IGPEGame.game.worldMP;
		} else {
			// Use server map name if available, otherwise use default
			String mapToLoad = MultiplayerWorld.serverMapName != null ? MultiplayerWorld.serverMapName : "arena.map";
			String mapContent = MultiplayerWorld.serverMapContent;
			it.unical.igpe.utils.DebugUtils.showMessage("Creating new MultiplayerWorld with map: " + mapToLoad + (mapContent != null ? " (from content, length: " + mapContent.length() + ")" : ""));
			try {
				IGPEGame.game.worldMP = new MultiplayerWorld(mapToLoad, mapContent, false);
				this.world = IGPEGame.game.worldMP;
				it.unical.igpe.utils.DebugUtils.showMessage("MultiplayerWorld created successfully");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create MultiplayerWorld", e);
				throw new RuntimeException("Failed to create MultiplayerWorld", e);
			}
		}
		
		it.unical.igpe.utils.DebugUtils.showMessage("World assigned: " + (this.world != null ? "OK" : "NULL"));
		it.unical.igpe.utils.DebugUtils.showMessage("World.player: " + (this.world != null && this.world.getPlayer() != null ? this.world.getPlayer().getUsername() : "NULL"));
		
		// Initialize OpenGL objects to null - will be created in first render()
		this.hud = null;
		this.renderer = null;
		this.isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
		this.touchController = null;
		this.renderStarted = false;
		it.unical.igpe.utils.DebugUtils.showMessage("=== MultiplayerGameScreen constructor END ===");
	}

	@Override
	public void show() {
		it.unical.igpe.utils.DebugUtils.showMessage("=== MultiplayerGameScreen.show() called ===");
		Gdx.input.setInputProcessor(null);
		SoundManager.manager.get(SoundManager.MenuMusic, Music.class).pause();
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setVolume(GameConfig.MUSIC_VOLUME);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setLooping(true);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).play();
		
		// Assets should already be loaded by LoadingScreen, but verify
		try {
			it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
			it.unical.igpe.utils.DebugUtils.showMessage("Assets verified as loaded");
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Assets not loaded, this should not happen!", e);
		}
		
		it.unical.igpe.utils.DebugUtils.showMessage("=== MultiplayerGameScreen.show() completed ===");
	}

	@Override
	public void render(float delta) {
		// First render call - log it immediately
		if (!renderStarted) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("=== MultiplayerGameScreen.render() called for first time ===");
				it.unical.igpe.utils.DebugUtils.showMessage("World: " + (world != null ? "OK" : "NULL"));
				it.unical.igpe.utils.DebugUtils.showMessage("World.player: " + (world != null && world.getPlayer() != null ? world.getPlayer().getUsername() : "NULL"));
				renderStarted = true;
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("CRITICAL: Exception logging first render", e);
				e.printStackTrace();
			}
		}
		
		try {
			// Ensure OpenGL objects are created (lazy initialization)
			ensureOpenGLObjectsReady();
			
			if (world == null || world.getPlayer() == null) {
				it.unical.igpe.utils.DebugUtils.showError("World or Player is null in multiplayer render!", null);
				return;
			}
			
			if (renderer == null) {
				// Renderer not ready yet, skip this frame
				return;
			}
			
			// Ensure assets are fully loaded before rendering
			if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
				it.unical.igpe.GUI.Assets.manager.update();
				if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
					return; // Skip this frame, assets still loading
				}
				it.unical.igpe.GUI.Assets.manager.finishLoading();
			}
			
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			world.update(delta);
			if (isAndroid && touchController != null) {
				touchController.update();
			}
			handleInput(delta);
			renderer.render(delta);
			if (hud != null) {
				hud.render(world.player);
			}
			
			// Render touch controls on top of everything (Android only)
			if (isAndroid && touchController != null && renderer != null) {
				touchController.render(renderer.getBatch());
			}

			if (world.isGameOver())
				IGPEGame.game.setScreen(ScreenManager.MOS);
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Exception in MultiplayerGameScreen.render(): " + e.getMessage(), e);
			e.printStackTrace();
			com.badlogic.gdx.Gdx.app.error("MultiplayerGameScreen", "Render exception", e);
		}
	}
	
	private void ensureOpenGLObjectsReady() {
		// Verify assets are actually loaded
		boolean assetsReady = false;
		try {
			it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
			assetsReady = true;
		} catch (Exception e) {
			if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
				it.unical.igpe.GUI.Assets.manager.update();
				if (it.unical.igpe.GUI.Assets.manager.getProgress() < 1.0f) {
					return;
				}
				it.unical.igpe.GUI.Assets.manager.finishLoading();
			}
			try {
				it.unical.igpe.GUI.Assets.manager.get(it.unical.igpe.GUI.Assets.Ground, com.badlogic.gdx.graphics.Texture.class);
				assetsReady = true;
			} catch (Exception e2) {
				return;
			}
		}
		
		if (!assetsReady) {
			return;
		}
		
		// Create WorldRenderer and HUD on first render call when OpenGL is definitely ready
		if (this.renderer == null) {
			try {
				// Verify player exists before creating renderer
				if (world.getPlayer() == null) {
					it.unical.igpe.utils.DebugUtils.showMessage("Player not ready yet, waiting...");
					return;
				}
				it.unical.igpe.utils.DebugUtils.showMessage("Creating MultiplayerWorldRenderer now (in render, OpenGL context is active)");
				it.unical.igpe.utils.DebugUtils.showMessage("Player exists: " + world.getPlayer().getUsername());
				this.renderer = new MultiplayerWorldRenderer(world);
				it.unical.igpe.utils.DebugUtils.showMessage("MultiplayerWorldRenderer created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create MultiplayerWorldRenderer in render()", e);
				e.printStackTrace();
				return;
			}
		}
		
		if (this.hud == null) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating HUD for multiplayer now (in render, OpenGL context is active)");
				this.hud = new HUD(true);
				it.unical.igpe.utils.DebugUtils.showMessage("HUD created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create HUD in render()", e);
				return;
			}
		}
		
		// Recreate touch controller now that renderer is ready
		if (isAndroid && this.touchController == null && this.renderer != null) {
			try {
				it.unical.igpe.utils.DebugUtils.showMessage("Creating TouchController for multiplayer now");
				this.touchController = new TouchController(renderer.getCamera());
				it.unical.igpe.utils.DebugUtils.showMessage("TouchController created successfully in render()");
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to create TouchController in render()", e);
			}
		}
	}

	@Override
	public void resize(int width, int height) {
		if (renderer != null) {
			renderer.viewport.update(width, height, true);
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

	private void handleInput(float delta) {
		// Handle aiming
		if (isAndroid && touchController != null) {
			// Use aiming joystick on right side
			Vector2 aimDir = touchController.getAimDirection();
			if (aimDir != null) {
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
			
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			
			box = new Rectangle(world.player.getBoundingBox().x + moveX, world.player.getBoundingBox().y + moveY,
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += moveX;
				world.player.getBoundingBox().y += moveY;
			}
		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y -= GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x -= GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL) {
				world.player.getBoundingBox().x += GameConfig.DIAGONALSPEED * delta;
				world.player.getBoundingBox().y += GameConfig.DIAGONALSPEED * delta;
			}

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.W)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x,
					world.player.getBoundingBox().y - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL)
				world.player.getBoundingBox().y -= GameConfig.MOVESPEED * delta;

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.A)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x - (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y, world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL)
				world.player.getBoundingBox().x -= GameConfig.MOVESPEED * delta;

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.S)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x,
					world.player.getBoundingBox().y + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().width, world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL)
				world.player.getBoundingBox().y += GameConfig.MOVESPEED * delta;

		} else if (!isAndroid && Gdx.input.isKeyPressed(Input.Keys.D)) {
			if (!world.player.isReloading())
				world.player.state = Player.STATE_RUNNING;
			box = new Rectangle((int) world.player.getBoundingBox().x + (int) (GameConfig.MOVESPEED * delta),
					world.player.getBoundingBox().y, world.player.getBoundingBox().width,
					world.player.getBoundingBox().height);
			TileType tmp = MultiplayerWorld.getNextTile(box);
			if (tmp != TileType.WALL)
				world.player.getBoundingBox().x += GameConfig.MOVESPEED * delta;

		}

		// Fire and Reloading action of the player
		boolean shouldFire = false;
		if (isAndroid && touchController != null) {
			shouldFire = touchController.isShootPressed() && world.player.canShoot();
		} else {
			shouldFire = Gdx.input.isButtonPressed(Buttons.LEFT) && world.player.canShoot();
		}
		
		if (shouldFire) {
			world.player.fire();
			if (world.player.checkAmmo()) {
				if (world.player.getActWeapon() == "pistol")
					SoundManager.manager.get(SoundManager.PistolReload, Sound.class).play(GameConfig.SOUND_VOLUME);
				else if (world.player.getActWeapon() == "shotgun")
					SoundManager.manager.get(SoundManager.ShotgunReload, Sound.class).play(GameConfig.SOUND_VOLUME);
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

		// Pause/Escape
		boolean shouldPause = false;
		if (isAndroid && touchController != null) {
			shouldPause = touchController.isPausePressed();
		} else {
			shouldPause = Gdx.input.isKeyJustPressed(Keys.ESCAPE);
		}
		
		if (shouldPause) {
			// Create pause screen lazily if it doesn't exist
			if (ScreenManager.MPS == null) {
				try {
					it.unical.igpe.utils.DebugUtils.showMessage("Creating MultiplayerPauseScreen lazily (on OpenGL thread)");
					ScreenManager.MPS = new it.unical.igpe.net.screens.MultiplayerPauseScreen();
					it.unical.igpe.utils.DebugUtils.showMessage("MultiplayerPauseScreen created successfully");
				} catch (Exception e) {
					it.unical.igpe.utils.DebugUtils.showError("Failed to create MultiplayerPauseScreen", e);
					return; // Don't pause if we can't create the screen
				}
			}
			IGPEGame.game.setScreen(ScreenManager.MPS);
		}

		Packet02Move packetMove;
		if (world.player.activeWeapon.ID == "pistol")
			packetMove = new Packet02Move(world.player.getUsername(), (int) world.player.getBoundingBox().x,
					(int) world.player.getBoundingBox().y, world.player.angle, world.player.state, 0);
		else if (world.player.activeWeapon.ID == "shotgun")
			packetMove = new Packet02Move(world.player.getUsername(), (int) world.player.getBoundingBox().x,
					(int) world.player.getBoundingBox().y, world.player.angle, world.player.state, 1);
		else
			packetMove = new Packet02Move(world.player.getUsername(), (int) world.player.getBoundingBox().x,
					(int) world.player.getBoundingBox().y, world.player.angle, world.player.state, 2);
		packetMove.writeData(IGPEGame.game.socketClient);
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
