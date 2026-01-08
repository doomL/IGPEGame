package it.unical.igpe.android;

import android.content.Intent;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import it.unical.igpe.game.IGPEGame;

public class AndroidLauncher extends AndroidApplication {
	public static final int FILE_PICKER_REQUEST_CODE = 1001;
	private AndroidFilePicker filePicker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useImmersiveMode = true;

		// Initialize Android debug utilities
		AndroidDebug.setActivity(this);

		filePicker = new AndroidFilePicker(this);
		initialize(new IGPEGame(filePicker), config);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == FILE_PICKER_REQUEST_CODE && filePicker != null) {
			filePicker.handleActivityResult(resultCode, data);
		}
	}
}
