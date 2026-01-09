package it.unical.igpe.GUI.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.utils.FilePicker;
import it.unical.igpe.utils.GameConfig;

public class LevelChooseScreen implements Screen {
	private SpriteBatch batch;
	private Stage stage;
	private Table table;
	private Label title;
	private TextButton defaultLevel;
	private TextButton chooseLevel;
	private TextButton returnButton;
	public String world;
	
	public LevelChooseScreen() {
		batch = new SpriteBatch();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, GameConfig.BACKGROUNDWIDTH, GameConfig.BACKGROUNDHEIGHT);

		stage = new Stage();
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);

		title = new Label("CHOOSE LEVEL", IGPEGame.skinsoldier);

		defaultLevel = new TextButton("Default Level", IGPEGame.skinsoldier);
		defaultLevel.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ScreenManager.GS = new GameScreen("Default.map");
				IGPEGame.game.setScreen(ScreenManager.LS);
			}
		});

		chooseLevel = new TextButton("Choose Level", IGPEGame.skinsoldier);
		chooseLevel.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (IGPEGame.filePicker == null) {
					// File picker not available (shouldn't happen, but handle gracefully)
					return;
				}

				// Desktop-specific fullscreen handling
				if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
					if(GameConfig.isFullscreen)
						Gdx.graphics.setWindowedMode(GameConfig.WIDTH, GameConfig.HEIGHT);
				}
				
				IGPEGame.filePicker.pickFile(new FilePicker.FilePickerCallback() {
					private boolean processing = false;
					
					@Override
					public void onFileSelected(String filePath) {
						// Desktop-specific fullscreen handling
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
						ScreenManager.GS = new GameScreen(filePath);
						IGPEGame.game.setScreen(ScreenManager.LS);
					}

					@Override
					public void onFileSelectedWithContent(String filePath, String fileContent) {
						// Prevent multiple calls
						if (processing) {
							it.unical.igpe.utils.DebugUtils.showMessage("Already processing file selection, ignoring duplicate call");
							return;
						}
						processing = true;
						
						// Desktop-specific fullscreen handling
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
						
						it.unical.igpe.utils.DebugUtils.showMessage("=== Starting GameScreen creation ===");
						it.unical.igpe.utils.DebugUtils.showMessage("Debug log file location: " + it.unical.igpe.utils.DebugUtils.getLogFilePath());
						GameScreen newGameScreen = null;
						try {
							it.unical.igpe.utils.DebugUtils.showMessage("Creating GameScreen with map content (length: " + fileContent.length() + ")");
							newGameScreen = new GameScreen(filePath, fileContent);
							it.unical.igpe.utils.DebugUtils.showMessage("GameScreen object created successfully");
							
							// Only assign and switch if creation was successful
							if (newGameScreen != null && newGameScreen.world != null) {
								ScreenManager.GS = newGameScreen;
								it.unical.igpe.utils.DebugUtils.showMessage("GameScreen assigned to ScreenManager");
								// Use LoadingScreen for custom maps (it's safe and shows nice loading progress)
								it.unical.igpe.utils.DebugUtils.showMessage("Switching to LoadingScreen (will transition to GameScreen when assets ready)");
								com.badlogic.gdx.Gdx.app.postRunnable(new Runnable() {
									@Override
									public void run() {
										IGPEGame.game.setScreen(ScreenManager.LS);
									}
								});
							} else {
								throw new RuntimeException("GameScreen or World is null after creation");
							}
						} catch (Exception e) {
							it.unical.igpe.utils.DebugUtils.showError("Failed to create GameScreen with content: " + e.getMessage(), e);
							e.printStackTrace();
							com.badlogic.gdx.Gdx.app.error("MapLoad", "Failed to load map: " + e.getMessage(), e);
							// Clear any partial GameScreen
							if (ScreenManager.GS == newGameScreen) {
								ScreenManager.GS = null;
							}
							// Stay on LevelChooseScreen if creation failed
							if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
								if(GameConfig.isFullscreen)
									Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
							}
						} finally {
							processing = false;
						}
					}

					@Override
					public void onCancelled() {
						// Desktop-specific fullscreen handling
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
					}
				});
			}
		});

		returnButton = new TextButton("Return", IGPEGame.skinsoldier);
		returnButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				IGPEGame.game.setScreen(ScreenManager.MMS);
			}
		});
		table.add(title);
		table.row();
		table.add(defaultLevel);
		table.row();
		table.add(chooseLevel);
		table.row();
		table.add(returnButton);
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		batch.draw(IGPEGame.background, 0, 0);
		batch.end();

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}


	@Override
	public void dispose() {
		stage.dispose();
		batch.dispose();
	}
	
	@Override
	public void hide() {}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
