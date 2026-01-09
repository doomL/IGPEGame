package it.unical.igpe.android;

import android.content.Intent;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import it.unical.igpe.game.IGPEGame;

public class AndroidLauncher extends AndroidApplication {
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

		if (filePicker != null && (requestCode == 1001 || requestCode == 1002)) {
			filePicker.handleActivityResult(requestCode, resultCode, data);
		}
	}
}
