package org.mcjug.servicemultistart;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class ScheduleReceiver extends BroadcastReceiver {

	static final String TAG = "ScheduleReceiver";
	private Config config;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		config = new Config (context);
		if ((config.isCheckboxAppLoadChecked() || config.isCheckboxBootChecked()) && (config.serviceMode.getServiceRunMode() > 0)) {
			
			Intent intentStartServiceReceiver = new Intent(context, StartServiceReceiver.class);

			PendingIntent pending = PendingIntent.getBroadcast(context, 0, intentStartServiceReceiver, PendingIntent.FLAG_CANCEL_CURRENT);

			Calendar boot_time = Calendar.getInstance();
			// start 60 seconds after boot completed
			boot_time.add(Calendar.SECOND, 60);
			long repeatTime = 1000 * 60 * config.serviceMode.getServiceRunMode();

			AlarmManager alarmManagerService = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			// fetch every REPEAT_TIME seconds. InexactRepeating allows Android to optimize the energy consumption
			alarmManagerService.setInexactRepeating(AlarmManager.RTC_WAKEUP, boot_time.getTimeInMillis(), repeatTime, pending);

			Log.v(TAG, "ScheduleReceiver fired, service started "  + config.isCheckboxBootChecked() + "/"+ config.isCheckboxAppLoadChecked());
			
		}
		else {
			Log.v(TAG, "ScheduleReceiver fired, not configured to init service ");
		}
		
	}

}
