package org.mcjug.servicemultistart;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
// import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class DownloaderService extends Service {

	static final String TAG = "DownloaderService";
	private int result = Activity.RESULT_CANCELED;
	public static final String NOTIFICATION = "DownloaderServiceBroadcast";
	private static final String RESULT = "result";

	static final SimpleDateFormat sdf = new SimpleDateFormat(" > yyyy-M-dd hh:mm:ss <", Locale.US);
	private Date mDate;
	
	@Override
	  public void onCreate() {
		  super.onCreate();
		  mDate = new Date();
		  Log.v(TAG, "Service onCreate:" + sdf.format(mDate));
		  result = Activity.RESULT_OK;
		  publishResults("starting service", result);
	  }
	
	private final IBinder mBinder = new ServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "DownloaderService onBind");
		return mBinder;
	}
	
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "DownloaderService onStartCommand");
		handleIntent (intent);
		return Service.START_NOT_STICKY;
	}

	public class ServiceBinder extends Binder {
		DownloaderService getService () {
			Log.v(TAG, "DownloaderService ServiceBinder getService");
			return DownloaderService.this;
		}
	}
	
	protected void handleIntent(final Intent intent) {
		new Thread(new Runnable() {
			   public void run() {
				   result = Activity.RESULT_OK;
				   publishResults (getServiceTicker(), result);
			   }
		}).start();
	}

	private int tickerNumber = 0;

	private String getServiceTicker () {
		tickerNumber++;
		Log.v(TAG, "DownloaderService Ticker " + tickerNumber);
		String currentTimeString = new SimpleDateFormat("> HH:mm", Locale.US).format(new Date());
		return ("SRV" + tickerNumber + currentTimeString);
	}
	
	public int getResult () {
		return result;
	}
	
	private void publishResults(String loadedMessage, int result) {
		Log.v(TAG, "DownloaderService publishResults");
		Intent intent = new Intent(NOTIFICATION);
		intent.putExtra(Config.LOADEDSTRING, loadedMessage);
		intent.putExtra(RESULT, result);
		sendBroadcast(intent);
	}
}
