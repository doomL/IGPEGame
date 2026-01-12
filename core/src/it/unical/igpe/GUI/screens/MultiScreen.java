package it.unical.igpe.GUI.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.net.GameClient;
import it.unical.igpe.net.GameServer;
import it.unical.igpe.net.MultiplayerWorld;
import it.unical.igpe.net.NetworkScanner;
import it.unical.igpe.utils.FilePicker;
import it.unical.igpe.utils.GameConfig;
import com.badlogic.gdx.Application;

public class MultiScreen implements Screen {
	private SpriteBatch batch;
	private Stage stage;
	private Table tableChoose;
	private Table tableServer;
	private Table tableClient;
	private TextButton returnButton;
	private TextButton chosenReturnClientButton;
	private TextButton chosenReturnServerButton;
	private TextButton Client;
	private TextButton Server;
	private TextButton connectClient;
	private TextButton createServer;
	private Label clientLabel;
	private Label serverLabel;
	private Label IPClientLabel;
	private Label PortClientLabel;
	private Label PortServerLabel;
	private Label nameClientLabel;
	private Label multiLabel;
	private Label nameServerLabel;
	private Label serverKillsLabel;
	private Label mapServerLabel;
	private Label localIPLabel;
	private Label scanStatusLabel;
	private TextField nameText;
	private TextField IPClientText;
	private TextField PortClientText;
	private TextField PortServerText;
	private TextField serverNameText;
	private TextField serverKills;
	private TextButton defaultMapButton;
	private TextButton chooseMapButton;
	private TextButton scanButton;
	private String selectedMap = "arena.map"; // Default map
	private String selectedMapFullPath = "arena.map"; // Full path for custom maps
	private String selectedMapContent = null; // Map content for custom maps
	private float mobileScaleFactor = 1.0f; // Scale factor for mobile devices
	
	public MultiScreen() {
		// Calculate scale factor for mobile devices
		if (Gdx.app.getType() == Application.ApplicationType.Android) {
			float density = Gdx.graphics.getDensity();
			// Scale UI elements moderately on mobile for better visibility and touch interaction
			mobileScaleFactor = Math.max(1.5f, Math.min(2.2f, density * 0.8f));
			it.unical.igpe.utils.DebugUtils.showMessage("MultiScreen mobile scale factor: " + mobileScaleFactor);
		}
		
		// Calculate font scales for mobile
		float labelFontScale = 0.8f * mobileScaleFactor;
		float smallLabelFontScale = 0.7f * mobileScaleFactor;
		float textFieldFontScale = 0.7f * mobileScaleFactor;
		
		batch = new SpriteBatch();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, GameConfig.BACKGROUNDWIDTH, GameConfig.BACKGROUNDHEIGHT);

		stage = new Stage();
		
		tableChoose = new Table();
		tableServer = new Table();
		tableClient = new Table();
		tableChoose.setFillParent(true);
		tableServer.setFillParent(true);
		tableClient.setFillParent(true);
		stage.addActor(tableChoose);
		stage.addActor(tableServer);
		stage.addActor(tableClient);
		tableChoose.setDebug(false);
		tableServer.setDebug(false);
		tableClient.setDebug(false);

		tableChoose.setVisible(true);
		tableClient.setVisible(false);
		tableServer.setVisible(false);

		returnButton = new TextButton("Return", IGPEGame.skinsoldier);
		returnButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				IGPEGame.game.setScreen(ScreenManager.MMS);
			}
		});

		chosenReturnClientButton = new TextButton("Return", IGPEGame.skinsoldier);
		chosenReturnClientButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				tableChoose.setVisible(true);
				tableClient.setVisible(false);
				tableServer.setVisible(false);
			}
		});

		chosenReturnServerButton = new TextButton("Return", IGPEGame.skinsoldier);
		chosenReturnServerButton.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				tableChoose.setVisible(true);
				tableClient.setVisible(false);
				tableServer.setVisible(false);
			}
		});

		Client = new TextButton("Connect", IGPEGame.skinsoldier);
		Client.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				tableChoose.setVisible(false);
				tableClient.setVisible(true);
				tableServer.setVisible(false);
			}
		});

		Server = new TextButton("Create Server", IGPEGame.skinsoldier);
		Server.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				tableChoose.setVisible(false);
				tableClient.setVisible(false);
				tableServer.setVisible(true);
			}
		});

		scanButton = new TextButton("Scan", IGPEGame.skinsoldier);
		scanButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				scanStatusLabel.setText("Scanning...");
				// Run scan in a separate thread to avoid blocking UI
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							int port = Integer.parseInt(PortClientText.getText());
							java.util.List<String> servers = NetworkScanner.scanForServers(port, 100);
							Gdx.app.postRunnable(new Runnable() {
								@Override
								public void run() {
									if (servers.isEmpty()) {
										scanStatusLabel.setText("No servers found");
									} else {
										// Use first found server
										IPClientText.setText(servers.get(0));
										scanStatusLabel.setText("Found: " + servers.size() + " server(s)");
									}
								}
							});
						} catch (Exception e) {
							Gdx.app.postRunnable(new Runnable() {
								@Override
								public void run() {
									scanStatusLabel.setText("Scan failed");
								}
							});
						}
					}
				}).start();
			}
		});
		
		connectClient = new TextButton("Connect", IGPEGame.skinsoldier);
		connectClient.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				IGPEGame.game.socketClient = new GameClient(IPClientText.getText(),
						Integer.parseInt(PortClientText.getText()));
				IGPEGame.game.socketClient.start();
				MultiplayerWorld.username = nameText.getText();
				ScreenManager.CreateMGS();
				LoadingScreen.isMP = true;
				IGPEGame.game.setScreen(ScreenManager.LS);
			}
		});

		createServer = new TextButton("Create", IGPEGame.skinsoldier);
		createServer.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					it.unical.igpe.utils.DebugUtils.showMessage("Starting server creation...");

					// Validate inputs
					String portText = PortServerText.getText();
					String killsText = serverKills.getText();
					String usernameText = serverNameText.getText();

					if (portText == null || portText.trim().isEmpty()) {
						it.unical.igpe.utils.DebugUtils.showError("Port cannot be empty");
						return;
					}
					if (killsText == null || killsText.trim().isEmpty()) {
						it.unical.igpe.utils.DebugUtils.showError("Max kills cannot be empty");
						return;
					}
					if (usernameText == null || usernameText.trim().isEmpty()) {
						it.unical.igpe.utils.DebugUtils.showError("Username cannot be empty");
						return;
					}

					int port = Integer.parseInt(portText);
					int maxKills = Integer.parseInt(killsText);

					// Clean up existing server if any
					if (IGPEGame.game.socketServer != null) {
						it.unical.igpe.utils.DebugUtils.showMessage("Closing existing server...");
						try {
							IGPEGame.game.socketServer.close();
							// Wait for socket to be released
							Thread.sleep(200);
						} catch (Exception e) {
							it.unical.igpe.utils.DebugUtils.showError("Error closing existing server", e);
						}
						IGPEGame.game.socketServer = null;
					}

					it.unical.igpe.utils.DebugUtils.showMessage("Creating GameServer on port: " + port + " with map: " + selectedMap);
					// Pass map content if available (Android), otherwise pass path (Desktop)
					IGPEGame.game.socketServer = new GameServer(port, selectedMap, selectedMapContent);

					if (IGPEGame.game.socketServer == null) {
						it.unical.igpe.utils.DebugUtils.showError("Failed to create GameServer");
						return;
					}

					IGPEGame.game.socketServer.MaxKills = maxKills;
					it.unical.igpe.utils.DebugUtils.showMessage("Starting server thread...");
					IGPEGame.game.socketServer.start();

					// Wait for server to initialize (socket creation happens in background thread)
					it.unical.igpe.utils.DebugUtils.showMessage("Waiting for server to initialize...");
					int waitCount = 0;
					while (!IGPEGame.game.socketServer.isValid() && waitCount < 50) {
						try {
							Thread.sleep(100);
							waitCount++;
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}

					if (!IGPEGame.game.socketServer.isValid()) {
						it.unical.igpe.utils.DebugUtils.showError("Server failed to initialize - check if port is already in use");
						IGPEGame.game.socketServer = null;
						return;
					}
					it.unical.igpe.utils.DebugUtils.showMessage("Server initialized successfully");

					it.unical.igpe.utils.DebugUtils.showMessage("Creating GameClient...");
					// On Android, use localhost IP or device IP
					String serverIP = "127.0.0.1";
					IGPEGame.game.socketClient = new GameClient(serverIP, port);

					if (IGPEGame.game.socketClient == null) {
						it.unical.igpe.utils.DebugUtils.showError("Failed to create GameClient");
						// Clean up server since we can't connect
						if (IGPEGame.game.socketServer != null) {
							IGPEGame.game.socketServer.close();
							IGPEGame.game.socketServer = null;
						}
						return;
					}

					it.unical.igpe.utils.DebugUtils.showMessage("Starting client thread...");
					IGPEGame.game.socketClient.start();

					// Wait a bit for client to initialize
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}

					// Set the map name and content for the client (same as server since we're hosting)
					MultiplayerWorld.serverMapName = selectedMap;
					// If we have map content (custom map), set it so client can use it immediately
					if (selectedMapContent != null && !selectedMapContent.isEmpty()) {
						MultiplayerWorld.serverMapContent = selectedMapContent;
						it.unical.igpe.utils.DebugUtils.showMessage("Set map content for local client (length: " + selectedMapContent.length() + ")");
					} else {
						MultiplayerWorld.serverMapContent = null; // Default map, client will load from assets
					}

					MultiplayerWorld.username = usernameText;
					it.unical.igpe.utils.DebugUtils.showMessage("Creating multiplayer game screen...");
					// Create screen on OpenGL thread to avoid OpenGL context issues
					com.badlogic.gdx.Gdx.app.postRunnable(new Runnable() {
						@Override
						public void run() {
							try {
								it.unical.igpe.utils.DebugUtils.showMessage("Creating MGS on OpenGL thread...");
								ScreenManager.CreateMGS();
								it.unical.igpe.utils.DebugUtils.showMessage("MGS created, switching to LoadingScreen");
								LoadingScreen.isMP = true;
								IGPEGame.game.setScreen(ScreenManager.LS);
							} catch (Exception e) {
								it.unical.igpe.utils.DebugUtils.showError("CRITICAL: Failed to create MGS on OpenGL thread", e);
								e.printStackTrace();
							}
						}
					});
					it.unical.igpe.utils.DebugUtils.showMessage("Server created successfully!");
				} catch (NumberFormatException e) {
					it.unical.igpe.utils.DebugUtils.showError("Invalid number format (port or kills)", e);
				} catch (Exception e) {
					it.unical.igpe.utils.DebugUtils.showError("Error creating server", e);
				}
			}
		});

		clientLabel = new Label("Client", IGPEGame.skinsoldier);
		clientLabel.setFontScale(labelFontScale);
		serverLabel = new Label("Server", IGPEGame.skinsoldier);
		serverLabel.setFontScale(labelFontScale);
		multiLabel = new Label("MULTIPLAYER", IGPEGame.skinsoldier);
		multiLabel.setFontScale(0.9f * mobileScaleFactor);
		IPClientLabel = new Label("IP", IGPEGame.skinsoldier);
		IPClientLabel.setFontScale(smallLabelFontScale);
		PortClientLabel = new Label("Port", IGPEGame.skinsoldier);
		PortClientLabel.setFontScale(smallLabelFontScale);
		PortServerLabel = new Label("Port", IGPEGame.skinsoldier);
		PortServerLabel.setFontScale(smallLabelFontScale);
		nameClientLabel = new Label("Username", IGPEGame.skinsoldier);
		nameClientLabel.setFontScale(smallLabelFontScale);
		nameText = new TextField("", IGPEGame.skinsoldier);
		nameText.getStyle().font.getData().setScale(textFieldFontScale);
		IPClientText = new TextField("127.0.0.1", IGPEGame.skinsoldier);
		IPClientText.getStyle().font.getData().setScale(textFieldFontScale);
		PortClientText = new TextField("1234", IGPEGame.skinsoldier);
		PortClientText.getStyle().font.getData().setScale(textFieldFontScale);
		PortServerText = new TextField("1234", IGPEGame.skinsoldier);
		PortServerText.getStyle().font.getData().setScale(textFieldFontScale);
		nameServerLabel = new Label("Username", IGPEGame.skinsoldier);
		nameServerLabel.setFontScale(smallLabelFontScale);
		serverNameText = new TextField("", IGPEGame.skinsoldier);
		serverNameText.getStyle().font.getData().setScale(textFieldFontScale);
		serverKills = new TextField("10", IGPEGame.skinsoldier);
		serverKills.getStyle().font.getData().setScale(textFieldFontScale);
		serverKillsLabel = new Label("Max Kills", IGPEGame.skinsoldier);
		serverKillsLabel.setFontScale(smallLabelFontScale);
		mapServerLabel = new Label("Map: " + selectedMap, IGPEGame.skinsoldier);
		mapServerLabel.setFontScale(smallLabelFontScale);
		
		// Map selection buttons
		defaultMapButton = new TextButton("Default", IGPEGame.skinsoldier);
		defaultMapButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMap = "arena.map";
				selectedMapFullPath = "arena.map";
				selectedMapContent = null; // Clear any custom map content
				mapServerLabel.setText("Map: " + selectedMap);
			}
		});
		
		chooseMapButton = new TextButton("Choose", IGPEGame.skinsoldier);
		chooseMapButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (IGPEGame.filePicker == null) {
					return;
				}
				
				if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
					if(GameConfig.isFullscreen)
						Gdx.graphics.setWindowedMode(GameConfig.WIDTH, GameConfig.HEIGHT);
				}
				
				IGPEGame.filePicker.pickFile(new FilePicker.FilePickerCallback() {
					@Override
					public void onFileSelected(String filePath) {
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
						// Desktop: use file path (can access files directly)
						selectedMapFullPath = filePath;
						selectedMapContent = null; // Desktop doesn't need content
						
						// Extract just the filename for display and client communication
						String filename = filePath;
						if (filePath.contains("/")) {
							filename = filePath.substring(filePath.lastIndexOf("/") + 1);
						}
						if (filePath.contains("\\")) {
							filename = filePath.substring(filePath.lastIndexOf("\\") + 1);
						}
						selectedMap = filename;
						mapServerLabel.setText("Map: " + selectedMap);
					}
					
					@Override
					public void onFileSelectedWithContent(String filePath, String fileContent) {
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
						// Android: store map content
						selectedMapContent = fileContent;
						selectedMapFullPath = filePath; // Keep filename for display
						
						// Extract just the filename for display
						String filename = filePath;
						if (filePath.contains("/")) {
							filename = filePath.substring(filePath.lastIndexOf("/") + 1);
						}
						selectedMap = filename;
						mapServerLabel.setText("Map: " + selectedMap);
						it.unical.igpe.utils.DebugUtils.showMessage("Stored map content for multiplayer (length: " + fileContent.length() + ")");
					}
					
					@Override
					public void onCancelled() {
						if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
							if(GameConfig.isFullscreen)
								Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
						}
					}
				});
			}
		});
		
		// Local IP display
		String localIP = NetworkScanner.getLocalIP();
		localIPLabel = new Label("IP: " + localIP, IGPEGame.skinsoldier);
		localIPLabel.setFontScale(smallLabelFontScale);
		scanStatusLabel = new Label("", IGPEGame.skinsoldier);
		scanStatusLabel.setFontScale(0.6f * mobileScaleFactor);

		// Scale padding and sizes for mobile
		float padSmall = 1f * mobileScaleFactor;
		float padMedium = 2f * mobileScaleFactor;
		float padLarge = 3f * mobileScaleFactor;
		float buttonHeightSmall = 28f * mobileScaleFactor;
		float buttonHeightLarge = 32f * mobileScaleFactor;
		float textFieldWidthSmall = 80f * mobileScaleFactor;
		float textFieldWidthMedium = 150f * mobileScaleFactor;
		float textFieldWidthLarge = 180f * mobileScaleFactor;
		float textFieldWidthXLarge = 200f * mobileScaleFactor;
		float textFieldHeight = 28f * mobileScaleFactor;
		
		tableChoose.add(multiLabel).pad(padLarge);
		tableChoose.row();
		tableChoose.add(Client).pad(padMedium).height(buttonHeightLarge);
		tableChoose.row();
		tableChoose.add(Server).pad(padMedium).height(buttonHeightLarge);
		tableChoose.row();
		tableChoose.add(returnButton).pad(padMedium).height(buttonHeightLarge);

		tableClient.add(clientLabel).pad(padMedium);
		tableClient.row();
		tableClient.add(nameClientLabel).pad(padSmall);
		tableClient.add(nameText).width(textFieldWidthXLarge).height(textFieldHeight).pad(padSmall);
		tableClient.row();
		tableClient.add(IPClientLabel).pad(padSmall);
		tableClient.add(IPClientText).width(textFieldWidthXLarge).height(textFieldHeight).pad(padSmall);
		tableClient.row();
		tableClient.add(PortClientLabel).pad(padSmall);
		tableClient.add(PortClientText).width(textFieldWidthMedium).height(textFieldHeight).pad(padSmall);
		tableClient.row();
		tableClient.add(scanButton).pad(padSmall).height(buttonHeightSmall);
		tableClient.add(scanStatusLabel).pad(padSmall);
		tableClient.row();
		tableClient.add(connectClient).pad(padSmall).height(buttonHeightLarge);
		tableClient.row();
		tableClient.add(chosenReturnClientButton).pad(padSmall).height(buttonHeightSmall);

		tableServer.add(serverLabel).pad(padMedium);
		tableServer.row();
		tableServer.add(nameServerLabel).pad(padSmall);
		tableServer.add(serverNameText).width(textFieldWidthLarge).height(textFieldHeight).pad(padSmall);
		tableServer.row();
		tableServer.add(serverKillsLabel).pad(padSmall);
		tableServer.add(serverKills).width(textFieldWidthSmall).height(textFieldHeight).pad(padSmall);
		tableServer.row();
		tableServer.add(PortServerLabel).pad(padSmall);
		tableServer.add(PortServerText).width(textFieldWidthLarge).height(textFieldHeight).pad(padSmall);
		tableServer.row();
		tableServer.add(mapServerLabel).pad(padSmall);
		tableServer.row();
		tableServer.add(defaultMapButton).pad(padSmall).height(buttonHeightSmall);
		tableServer.row();
		tableServer.add(chooseMapButton).pad(padSmall).height(buttonHeightSmall);
		tableServer.row();
		tableServer.add(localIPLabel).pad(padSmall);
		tableServer.row();
		tableServer.add(createServer).pad(padSmall).height(buttonHeightLarge);
		tableServer.row();
		tableServer.add(chosenReturnServerButton).pad(padSmall).height(buttonHeightSmall);
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
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

}
