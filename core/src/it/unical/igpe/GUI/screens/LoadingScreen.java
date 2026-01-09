package it.unical.igpe.GUI.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import it.unical.igpe.GUI.Assets;
import it.unical.igpe.GUI.SoundManager;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.utils.GameConfig;

public class LoadingScreen implements Screen {
	public static boolean isMP = false;
	private SpriteBatch batch;
	public Stage stage;
	private Table table;
	private ProgressBar loadingBar;
	private Label loading;
	
	public LoadingScreen() {
		batch = new SpriteBatch();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, GameConfig.BACKGROUNDWIDTH, GameConfig.BACKGROUNDHEIGHT);

		stage = new Stage();
		
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);

		loading = new Label("LOADING...", IGPEGame.skinsoldier);

		loadingBar = new ProgressBar(0.0f, 1.0f, 0.1f, false, IGPEGame.skinComic);
		loadingBar.setValue(0);
		table.add(loading);
		table.row();
		table.add(loadingBar);
	}

	@Override
	public void show() {
		Assets.load();
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		batch.draw(IGPEGame.background, 0, 0);
		batch.end();

		loadingBar.setValue(Assets.manager.getProgress());

		if (Assets.manager.update() && SoundManager.manager.update()) {
			it.unical.igpe.utils.DebugUtils.showMessage("=== LoadingScreen: Assets finished loading ===");
			Assets.manager.finishLoading();
			it.unical.igpe.utils.DebugUtils.showMessage("Assets.manager.finishLoading() called");
			
			if (!isMP) {
				it.unical.igpe.utils.DebugUtils.showMessage("Single-player mode, checking GameScreen...");
				if (ScreenManager.GS != null) {
					it.unical.igpe.utils.DebugUtils.showMessage("GameScreen exists, transitioning...");
					IGPEGame.game.setScreen(ScreenManager.GS);
				} else {
					it.unical.igpe.utils.DebugUtils.showError("GameScreen is null, cannot transition", null);
					IGPEGame.game.setScreen(ScreenManager.LCS);
				}
			} else {
				it.unical.igpe.utils.DebugUtils.showMessage("=== Multiplayer mode, checking MultiplayerGameScreen ===");
				isMP = false;
				if (ScreenManager.MGS != null) {
					it.unical.igpe.utils.DebugUtils.showMessage("MGS is not null, about to post runnable for screen transition");
					// Defer screen transition to OpenGL thread (like single-player)
					final com.badlogic.gdx.Screen targetScreen = ScreenManager.MGS;
					it.unical.igpe.utils.DebugUtils.showMessage("Posted runnable to OpenGL thread for setScreen(MGS)");
					com.badlogic.gdx.Gdx.app.postRunnable(new Runnable() {
						@Override
						public void run() {
							try {
								it.unical.igpe.utils.DebugUtils.showMessage("=== Inside postRunnable: setScreen(MGS) called on OpenGL thread ===");
								it.unical.igpe.utils.DebugUtils.showMessage("targetScreen: " + (targetScreen != null ? targetScreen.getClass().getSimpleName() : "NULL"));
								IGPEGame.game.setScreen(targetScreen);
								it.unical.igpe.utils.DebugUtils.showMessage("setScreen(MGS) completed on OpenGL thread");
							} catch (Exception e) {
								it.unical.igpe.utils.DebugUtils.showError("CRITICAL: Exception during setScreen(MGS) on OpenGL thread", e);
								e.printStackTrace();
								com.badlogic.gdx.Gdx.app.postRunnable(new Runnable() {
									@Override
									public void run() {
										IGPEGame.game.setScreen(ScreenManager.MS);
									}
								});
							}
						}
					});
					it.unical.igpe.utils.DebugUtils.showMessage("postRunnable completed, returning from render()");
				} else {
					it.unical.igpe.utils.DebugUtils.showError("MultiplayerGameScreen is null, cannot transition", null);
					IGPEGame.game.setScreen(ScreenManager.MS);
				}
			}
		}

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}
	
	@Override
	public void dispose() {
		batch.dispose();
		stage.dispose();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}

}
