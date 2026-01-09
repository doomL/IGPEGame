package it.unical.igpe.GUI.screens;

import com.badlogic.gdx.audio.Music;

import it.unical.igpe.GUI.SoundManager;
import it.unical.igpe.editor.EditorScreen;
import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.net.screens.MultiplayerGameScreen;
import it.unical.igpe.net.screens.MultiplayerOverScreen;
import it.unical.igpe.net.screens.MultiplayerPauseScreen;
import it.unical.igpe.utils.GameConfig;

public class ScreenManager {
	public static MainMenuScreen MMS;
	public static LevelChooseScreen LCS;
	public static GameScreen GS;
	public static LevelCompletedScreen LCompletedS;
	public static OptionScreen OS;
	public static PauseScreen PS;
	public static LoadingScreen LS;
	public static MultiScreen MS;
	public static MultiplayerGameScreen MGS;
	public static MultiplayerPauseScreen MPS;
	public static MultiplayerOverScreen MOS;
	public static EditorScreen ES;
	
	public ScreenManager() {
		SoundManager.load();
		while(!SoundManager.manager.update())
			SoundManager.manager.finishLoading();
		
		SoundManager.manager.get(SoundManager.MenuMusic, Music.class).setVolume(GameConfig.MUSIC_VOLUME);
		SoundManager.manager.get(SoundManager.MenuMusic, Music.class).setLooping(true);
		
		MMS = new MainMenuScreen();
		LCS = new LevelChooseScreen();
		LCompletedS = new LevelCompletedScreen();
		OS = new OptionScreen();
		PS = new PauseScreen();
		LS = new LoadingScreen();
		MS = new MultiScreen();
		MOS = new MultiplayerOverScreen();
		ES = new EditorScreen();
		IGPEGame.game.setScreen(MMS);
	}
	
	public static void CreateMGS() {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("=== ScreenManager.CreateMGS() called ===");
			it.unical.igpe.utils.DebugUtils.showMessage("About to create MultiplayerGameScreen...");
			MGS = new MultiplayerGameScreen();
			it.unical.igpe.utils.DebugUtils.showMessage("MultiplayerGameScreen created, MGS assigned");
			// Don't create MultiplayerPauseScreen here - it creates SpriteBatch which needs OpenGL context
			// Create it lazily when needed (when user pauses)
			MPS = null;
			it.unical.igpe.utils.DebugUtils.showMessage("MultiplayerPauseScreen will be created lazily when needed");
			it.unical.igpe.utils.DebugUtils.showMessage("=== ScreenManager.CreateMGS() completed ===");
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("CRITICAL: Exception in CreateMGS()", e);
			e.printStackTrace();
			MGS = null;
			MPS = null;
			throw e;
		}
	}
}
