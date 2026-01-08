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

	public MultiplayerGameScreen() {
		IGPEGame.game.worldMP = new MultiplayerWorld("arena.map", false);
		this.world = IGPEGame.game.worldMP;
		this.hud = new HUD(true);
		this.renderer = new MultiplayerWorldRenderer(world);
		this.isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
		if (isAndroid) {
			this.touchController = new TouchController(renderer.getCamera());
		}
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
		SoundManager.manager.get(SoundManager.MenuMusic, Music.class).pause();
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setVolume(GameConfig.MUSIC_VOLUME);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).setLooping(true);
		SoundManager.manager.get(SoundManager.GameMusic, Music.class).play();
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		world.update(delta);
		if (isAndroid && touchController != null) {
			touchController.update();
		}
		handleInput(delta);
		renderer.render(delta);
		hud.render(world.player);
		
		// Render touch controls on top of everything (Android only)
		if (isAndroid && touchController != null) {
			touchController.render(renderer.getBatch());
		}

		if (world.isGameOver())
			IGPEGame.game.setScreen(ScreenManager.MOS);
	}

	@Override
	public void resize(int width, int height) {
		renderer.viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		hud.dispose();
		renderer.dispose();
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
		
		if (shouldPause)
			IGPEGame.game.setScreen(ScreenManager.MPS);

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
