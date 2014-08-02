package org.mcjug.servicemultistart;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartServiceReceiver  extends BroadcastReceiver {

	static final String TAG = "StartServiceReceiver";
	private Config config;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		config = new Config (context);
		if (config.isCheckboxAppLoadChecked() || config.isCheckboxBootChecked()) {
			Intent service = new Intent(context, DownloaderService.class);
			context.startService(service);
			Log.v(TAG, "StartServiceReceiver fired, service started " + config.isCheckboxBootChecked() + "/"+ config.isCheckboxAppLoadChecked());
		}
		else {
			Log.v(TAG, "StartServiceReceiver fired, both checkboxes are off");
		}
		
	}

}
