package it.unical.igpe.editor;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import it.unical.igpe.GUI.screens.ScreenManager;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.utils.GameConfig;

/**
 * LibGDX-based level editor screen
 */
public class EditorScreen implements Screen {
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private Stage stage;
	private MapEditor mapEditor;
	
	// Textures
	private Array<Texture> tileTextures;
	private Texture backgroundTexture;
	private Texture gridTexture;
	
	// UI
	private Table mainTable;
	private Table toolbarTable;
	private Table mapTable;
	private ScrollPane mapScrollPane;
	private ScrollPane tileScrollPane;
	private Table tileButtonTable;
	private TextButton saveButton;
	private TextButton loadButton;
	private TextButton newButton;
	private TextButton backButton;
	private TextButton spButton;
	private TextButton mpButton;
	private Label statusLabel;
	
	// Editor state
	private int selectedTileId = 0;
	private int cameraOffsetX = 0;
	private int cameraOffsetY = 0;
	private static final int TILE_BUTTON_SIZE = 24;
	private static final int TOOLBAR_WIDTH = 110;
	private boolean uiCreated = false;
	private float mobileScaleFactor = 1.0f; // Scale factor for mobile devices
	
	public EditorScreen() {
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		stage = new Stage(new ScreenViewport());
		
		// Calculate scale factor for mobile devices
		if (Gdx.app.getType() == Application.ApplicationType.Android) {
			// Scale tiles larger on mobile for better visibility and touch interaction
			// Use a scale factor based on screen density
			float density = Gdx.graphics.getDensity();
			// Minimum scale of 3x, up to 5x for very high density screens
			// This makes buttons and tiles much more visible and touchable
			mobileScaleFactor = Math.max(3.0f, Math.min(5.0f, density * 2.0f));
		}
		
		mapEditor = new MapEditor(false); // Start with single player
		
		loadTextures();
		createUI();
	}
	
	private void loadTextures() {
		tileTextures = new Array<Texture>();
		
		// Load tile textures (map_0.png to map_17.png - 18 tiles total, but map_14.png is missing)
		for (int i = 0; i < 18; i++) {
			try {
				Texture tex = new Texture(Gdx.files.internal("editor/map_" + i + ".png"));
				tileTextures.add(tex);
			} catch (Exception e) {
				// If texture doesn't exist, add null (will be handled in rendering)
				tileTextures.add(null);
				Gdx.app.error("EditorScreen", "Failed to load tile texture: map_" + i + ".png");
			}
		}
		
		// Load background
		try {
			backgroundTexture = new Texture(Gdx.files.internal("editor/background.png"));
		} catch (Exception e) {
			Gdx.app.error("EditorScreen", "Failed to load background texture");
			backgroundTexture = null;
		}
		
		// Create grid texture (simple white line)
		gridTexture = new Texture(Gdx.files.internal("ground.png"));
	}
	
	private void createUI() {
		// Prevent creating UI multiple times
		if (uiCreated) {
			return;
		}
		
		// Clear stage first to avoid duplicates
		stage.clear();
		
		mainTable = new Table();
		mainTable.setFillParent(true);
		stage.addActor(mainTable);
		
		// Top toolbar - create new table to avoid duplicates
		toolbarTable = new Table();
		// Clear any existing children
		toolbarTable.clearChildren();
		// Don't set background - the skin might not have the drawable we need
		
		// Scale font sizes on mobile - make them much larger
		float buttonFontScale = Gdx.app.getType() == Application.ApplicationType.Android ? 
			Math.max(1.2f, 0.5f * mobileScaleFactor) : 0.5f;
		float labelFontScale = Gdx.app.getType() == Application.ApplicationType.Android ? 
			Math.max(1.0f, 0.4f * mobileScaleFactor) : 0.4f;
		
		// Mode buttons - create with smaller font
		spButton = new TextButton("SP", IGPEGame.skinsoldier);
		spButton.getLabel().setFontScale(buttonFontScale);
		spButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				mapEditor.switchToSinglePlayer();
				updateMapDisplay();
				updateTileButtons(); // Update tile buttons to show SP-only tiles
				statusLabel.setText("SP 64x64");
			}
		});
		
		mpButton = new TextButton("MP", IGPEGame.skinsoldier);
		mpButton.getLabel().setFontScale(buttonFontScale);
		mpButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				mapEditor.switchToMultiplayer();
				updateMapDisplay();
				updateTileButtons(); // Update tile buttons to show MP-only tiles
				statusLabel.setText("MP 32x32");
			}
		});
		
		// File operations
		newButton = new TextButton("New", IGPEGame.skinsoldier);
		newButton.getLabel().setFontScale(buttonFontScale);
		newButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				mapEditor.clear();
				updateMapDisplay();
				statusLabel.setText("New map");
			}
		});
		
		saveButton = new TextButton("Save", IGPEGame.skinsoldier);
		saveButton.getLabel().setFontScale(buttonFontScale);
		saveButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveMap();
			}
		});
		
		loadButton = new TextButton("Load", IGPEGame.skinsoldier);
		loadButton.getLabel().setFontScale(buttonFontScale);
		loadButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				loadMap();
			}
		});
		
		backButton = new TextButton("Back", IGPEGame.skinsoldier);
		backButton.getLabel().setFontScale(buttonFontScale);
		backButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				IGPEGame.game.setScreen(ScreenManager.MMS);
			}
		});
		
		statusLabel = new Label("Editor", IGPEGame.skinsoldier);
		statusLabel.setFontScale(labelFontScale);
		
		// Scale button heights on mobile - make them much larger
		float buttonHeight = Gdx.app.getType() == Application.ApplicationType.Android ? 
			Math.max(60f, 30 * mobileScaleFactor) : 22;
		float buttonPad = Gdx.app.getType() == Application.ApplicationType.Android ? 
			Math.max(5f, 3 * mobileScaleFactor) : 2;
		
		// Add buttons to toolbar - make them very compact with more spacing
		toolbarTable.add(spButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(mpButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(newButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(saveButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(loadButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(backButton).pad(buttonPad).fillX().height(buttonHeight);
		toolbarTable.row();
		toolbarTable.add(statusLabel).pad(2).fillX();
		statusLabel.setWrap(true);
		
		// Tile selection buttons
		toolbarTable.row();
		Label tileLabel = new Label("Tiles", IGPEGame.skinsoldier);
		tileLabel.setFontScale(buttonFontScale);
		float labelPad = Gdx.app.getType() == Application.ApplicationType.Android ? 
			3 * mobileScaleFactor : 3;
		toolbarTable.add(tileLabel).padTop(labelPad).padBottom(labelPad);
		toolbarTable.row();
		
		tileButtonTable = new Table();
		updateTileButtons(); // Populate tile buttons based on current mode
		
		tileScrollPane = new ScrollPane(tileButtonTable, IGPEGame.skinsoldier);
		tileScrollPane.setScrollingDisabled(true, false); // Disable horizontal scrolling
		toolbarTable.add(tileScrollPane).fill().expand();
		
		// Wrap toolbar in scroll pane to make it scrollable
		ScrollPane toolbarScrollPane = new ScrollPane(toolbarTable, IGPEGame.skinsoldier);
		toolbarScrollPane.setFadeScrollBars(false);
		toolbarScrollPane.setScrollingDisabled(true, false); // Disable horizontal scrolling
		
		// Map display area
		mapTable = new Table();
		updateMapDisplay();
		
		mapScrollPane = new ScrollPane(mapTable, IGPEGame.skinsoldier);
		mapScrollPane.setScrollingDisabled(false, false);
		mapScrollPane.setFadeScrollBars(false);
		
		// Main layout - scale toolbar width on mobile - make it wider
		float toolbarWidth = Gdx.app.getType() == Application.ApplicationType.Android ? 
			Math.max(200f, TOOLBAR_WIDTH * mobileScaleFactor) : TOOLBAR_WIDTH;
		mainTable.add(toolbarScrollPane).width(toolbarWidth).fillY();
		mainTable.add(mapScrollPane).expand().fill();
		
		uiCreated = true;
	}
	
	private void updateTileButtons() {
		if (tileButtonTable == null) return;
		
		tileButtonTable.clear();
		
		for (int i = 0; i < tileTextures.size; i++) {
			final int tileId = i;
			
			// Filter tiles based on mode (SP or MP)
			// SP-only tiles: 2-11 (stair, keys, player, enemy) - not available in MP
			// MP-only tiles: 17 (logs) - not available in SP
			// Common tiles: 0, 1, 12-16 (ground, wall, box, barrel, cactus, plant) - available in both
			boolean isSPOnly = (tileId >= 2 && tileId <= 11);
			boolean isMPOnly = (tileId == 17);
			boolean shouldShow = false;
			
			if (mapEditor.isMultiplayer) {
				// MP mode: show common tiles and MP-only tiles, hide SP-only tiles
				shouldShow = !isSPOnly;
			} else {
				// SP mode: show common tiles and SP-only tiles, hide MP-only tiles
				shouldShow = !isMPOnly;
			}
			
			if (!shouldShow) {
				continue; // Skip this tile
			}
			
			Texture tex = tileTextures.get(i);
			
			// Create button even if texture is null (for missing tiles like tile 14)
			ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
			if (tex != null) {
				style.imageUp = new TextureRegionDrawable(new TextureRegion(tex));
			} else {
				// Use a placeholder texture for missing tiles (like tile 14)
				// Try to use ground texture or background as placeholder
				Texture placeholder = backgroundTexture;
				if (placeholder == null && tileTextures.size > 0) {
					// Find first non-null texture
					for (int j = 0; j < tileTextures.size; j++) {
						if (tileTextures.get(j) != null) {
							placeholder = tileTextures.get(j);
							break;
						}
					}
				}
				if (placeholder != null) {
					style.imageUp = new TextureRegionDrawable(new TextureRegion(placeholder));
				}
			}
			
			ImageButton tileButton = new ImageButton(style);
			tileButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					selectedTileId = tileId;
					statusLabel.setText("Tile: " + tileId + (tex == null ? " (missing)" : ""));
				}
			});
			// Scale tile button size on mobile - make them much larger for touch
			float scaledTileButtonSize = Gdx.app.getType() == Application.ApplicationType.Android ? 
				Math.max(80f, TILE_BUTTON_SIZE * mobileScaleFactor) : TILE_BUTTON_SIZE;
			float tilePad = Gdx.app.getType() == Application.ApplicationType.Android ? 
				Math.max(3f, 2 * mobileScaleFactor) : 1;
			tileButtonTable.add(tileButton).size(scaledTileButtonSize, scaledTileButtonSize).pad(tilePad);
			if ((i + 1) % 2 == 0) {
				tileButtonTable.row();
			}
		}
	}
	
	private void updateMapDisplay() {
		mapTable.clear();
		
		// Create a table with clickable cells for the map
		for (int y = mapEditor.mapDimension - 1; y >= 0; y--) {
			for (int x = 0; x < mapEditor.mapDimension; x++) {
				final int mapX = x;
				final int mapY = y;
				int tileId = mapEditor.getTile(x, y);
				
				TextureRegionDrawable drawable;
				if (tileId < tileTextures.size && tileTextures.get(tileId) != null) {
					drawable = new TextureRegionDrawable(new TextureRegion(tileTextures.get(tileId)));
				} else {
					// Default empty cell - use background or first available texture
					Texture defaultTex = backgroundTexture;
					if (defaultTex == null && tileTextures.size > 0) {
						// Find first non-null texture (usually ground.png which is map_0)
						for (int i = 0; i < tileTextures.size; i++) {
							if (tileTextures.get(i) != null) {
								defaultTex = tileTextures.get(i);
								break;
							}
						}
					}
					// If still null, use ground texture from assets
					if (defaultTex == null) {
						try {
							defaultTex = new Texture(Gdx.files.internal("ground.png"));
						} catch (Exception e) {
							// If ground.png doesn't exist either, we'll skip this cell
							continue;
						}
					}
					drawable = new TextureRegionDrawable(new TextureRegion(defaultTex));
				}
				
				ImageButton cellButton = new ImageButton(drawable);
				cellButton.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						mapEditor.setTile(mapX, mapY, selectedTileId);
						updateMapDisplay();
					}
				});
				
				// Keep map tiles at original size (don't scale them)
				mapTable.add(cellButton).size(mapEditor.tileSize, mapEditor.tileSize);
			}
			mapTable.row();
		}
	}
	
	private void saveMap() {
		try {
			// Use file picker for save dialog
			if (IGPEGame.filePicker != null) {
				// For desktop, we need to use a save dialog
				// Since FilePicker only has pickFile (for open), we'll use a workaround
				// Create a simple save dialog using the file picker interface
				saveMapWithDialog();
			} else {
				// Fallback: save with timestamp
				String mode = mapEditor.isMultiplayer ? "MP" : "SP";
				String filename = mode + "_custom_map_" + System.currentTimeMillis() + ".map";
				mapEditor.saveMap(filename);
				statusLabel.setText("Saved: " + filename);
			}
		} catch (Exception e) {
			statusLabel.setText("Error: " + e.getMessage());
			Gdx.app.error("EditorScreen", "Error saving map", e);
		}
	}
	
	private void saveMapWithDialog() {
		// Use file picker if available (works on both desktop and Android)
		if (IGPEGame.filePicker != null) {
			String mode = mapEditor.isMultiplayer ? "MP" : "SP";
			String suggestedFileName = mode + "_map.map";
			
			IGPEGame.filePicker.saveFile(suggestedFileName, new it.unical.igpe.utils.FilePicker.FilePickerCallback() {
				@Override
				public void onFileSelected(String filePath) {
					try {
						// For Android, filePath is a URI string, need to handle differently
						if (Gdx.app.getType() == Application.ApplicationType.Android) {
							// On Android, we need to write to the URI
							// The filePath is actually a content:// URI
							writeToUri(filePath);
						} else {
							// Desktop: filePath is a regular file path
							mapEditor.saveMap(filePath);
							java.io.File file = new java.io.File(filePath);
							statusLabel.setText("Saved: " + file.getName());
						}
					} catch (Exception e) {
						statusLabel.setText("Error: " + e.getMessage());
						Gdx.app.error("EditorScreen", "Error saving map", e);
					}
				}
				
				@Override
				public void onCancelled() {
					statusLabel.setText("Save cancelled");
				}
			});
		} else {
			statusLabel.setText("File picker not available");
		}
	}
	
	private void writeToUri(String uriString) {
		try {
			// Get the map data as string
			String mapData = mapEditor.saveMapToString();
			
			// Write to URI using Android's content resolver
			// Use reflection to access Android classes (only available on Android)
			Class<?> uriClass = Class.forName("android.net.Uri");
			Object uri = uriClass.getMethod("parse", String.class).invoke(null, uriString);
			
			// Get Android context using reflection
			Object androidApp = Gdx.app;
			java.lang.reflect.Method getContextMethod = androidApp.getClass().getMethod("getContext");
			Object context = getContextMethod.invoke(androidApp);
			
			// Get ContentResolver
			java.lang.reflect.Method getContentResolverMethod = context.getClass().getMethod("getContentResolver");
			Object resolver = getContentResolverMethod.invoke(context);
			
			// Open output stream
			java.lang.reflect.Method openOutputStreamMethod = resolver.getClass().getMethod("openOutputStream", uriClass, String.class);
			java.io.OutputStream outputStream = (java.io.OutputStream) openOutputStreamMethod.invoke(resolver, uri, "w");
			
			if (outputStream != null) {
				outputStream.write(mapData.getBytes("UTF-8"));
				outputStream.close();
				statusLabel.setText("Saved successfully");
			} else {
				throw new Exception("Could not open output stream");
			}
		} catch (Exception e) {
			statusLabel.setText("Error: " + e.getMessage());
			Gdx.app.error("EditorScreen", "Error writing to URI", e);
		}
	}
	
	private void loadMap() {
		try {
			// Use file picker if available
			if (IGPEGame.filePicker != null) {
				IGPEGame.filePicker.pickFile(new it.unical.igpe.utils.FilePicker.FilePickerCallback() {
					@Override
					public void onFileSelected(String filePath) {
						try {
							mapEditor.loadMap(filePath);
							updateMapDisplay();
							statusLabel.setText("Map loaded from: " + filePath);
						} catch (Exception e) {
							statusLabel.setText("Error loading map: " + e.getMessage());
							Gdx.app.error("EditorScreen", "Error loading map", e);
						}
					}
					
					@Override
					public void onCancelled() {
						statusLabel.setText("Load cancelled");
					}
				});
			} else {
				// Desktop: try to load from assets folder
				// User can place .map files in assets folder and we'll list them
				statusLabel.setText("On desktop: Use file picker (if available) or place .map files in assets/");
			}
		} catch (Exception e) {
			statusLabel.setText("Error loading map: " + e.getMessage());
			Gdx.app.error("EditorScreen", "Error loading map", e);
		}
	}
	
	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
		// Don't recreate UI - it's already created in constructor
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		// Handle keyboard input for camera movement
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			cameraOffsetX -= 5;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			cameraOffsetX += 5;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			cameraOffsetY += 5;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			cameraOffsetY -= 5;
		}
		
		stage.act(delta);
		stage.draw();
	}
	
	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		camera.setToOrtho(false, width, height);
	}
	
	@Override
	public void pause() {}
	
	@Override
	public void resume() {}
	
	@Override
	public void hide() {}
	
	@Override
	public void dispose() {
		stage.dispose();
		batch.dispose();
		if (tileTextures != null) {
			for (Texture tex : tileTextures) {
				if (tex != null) {
					tex.dispose();
				}
			}
		}
		if (backgroundTexture != null) {
			backgroundTexture.dispose();
		}
		if (gridTexture != null) {
			gridTexture.dispose();
		}
	}
}
