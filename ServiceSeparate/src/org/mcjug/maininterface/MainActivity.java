package org.mcjug.maininterface;

import org.mcjug.jsonobjects.SimpleMessage;
import org.mcjug.schedulerservice.DownloaderService;
import org.mcjug.schedulerservice.ServiceConfig;
import org.mcjug.schedulerservice.ServiceHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
//import android.content.BroadcastReceiver;
//import android.content.IntentFilter;

public class MainActivity extends Activity {

	static final String TAG = "MainActivity";
	private CheckBox mCheckboxBoot, mCheckboxAppLoad;
	private ServiceConfig config;
	
	//private ScheduleReceiver scheduleReceiver;
	private ServiceHandler serviceHandler;
	
	/*
	@SuppressWarnings("unused")
	private DownloaderService mBoundService;
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	Log.v(TAG, "MainActivity onServiceConnected");
	    	mBoundService = ((DownloaderService.ServiceBinder)service).getService();
	    }
	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	    }
	};
	
	
	boolean mIsBound = false;
	void doBindService() {
		Intent intent = new Intent(this, DownloaderService.class);
	    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}
*/
	

	/*
	 * 
	 *
	private String receivedMessage = " ? ";

	private BroadcastReceiver localReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  Log.v(TAG, "LocalBroadcastReceiver onReceive: Broadcast intent detected " + intent.getAction());
				// broadcastResult = intent.getAction();
				if (intent.hasExtra(ServiceConfig.LOADEDSTRING)) {
					receivedBroadcastMessage = intent.getExtras().getString(ServiceConfig.LOADEDSTRING);
		        }
				else {
					receivedBroadcastMessage = "LOADEDSTRING not found";
				}
				Log.v(TAG, "LocalBroadcastReceiver onReceive broadcastResult: " + receivedBroadcastMessage);
		  }
		};
	
	*/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		config = new ServiceConfig (getApplicationContext());
		
		RadioButton radioButton;
    	switch (config.serviceMode.getServiceRunMode()) {
	    	case 0: radioButton = (RadioButton) findViewById(R.id.radioOnClick); break;
	    	case 1: radioButton = (RadioButton) findViewById(R.id.radioOneMin); break;
	    	default: radioButton = (RadioButton) findViewById(R.id.radioFiveMin); break;
    	}
    	radioButton.setChecked(true);
    	
    	mCheckboxBoot = (CheckBox) findViewById(R.id.checkBoxBoot);
    	mCheckboxAppLoad = (CheckBox) findViewById(R.id.checkBoxAppLoad );
    	
    	
    	mCheckboxBoot.setChecked(config.isCheckboxBootChecked());
    	mCheckboxAppLoad.setChecked(config.isCheckboxAppLoadChecked());
    	   	
		addListenerOnCheckboxBoot();
		addListenerOnCheckboxAppLoad();
		
		serviceHandler = new ServiceHandler(getApplicationContext());
        if (config.isCheckboxAppLoadChecked()) {
        	if((config.serviceMode.getServiceRunMode() > 0)) {
            	initiateScheduler();
        	}
        	else {
        		// If RUN_ONCE run downloader service once
        		startService(new Intent(this, DownloaderService.class));
    			Log.v(TAG, "onCreate: run DownloaderService once");
        	}
        }
		
        
        String receivedMessage = serviceHandler.getReceivedBroadcastMessage();
        Log.v(TAG, "onCreate broadcastReceiver initialized: " + receivedMessage + "/" + config.serviceMode.name());
        //serviceHandler
        
		updateTicker(receivedMessage);
		handler.postDelayed (runnableTicker, 1000);
		// Show that the message is not fresh:
		receivedMessage += "_";
	}

	@Override
    protected void onResume() {
        super.onResume();
        serviceHandler.startReceiver();
        //registerReceiver(localReceiver, new IntentFilter(DownloaderService.NOTIFICATION));	
        Log.v(TAG, "onResume: localBroadcastReceiver registered");
        // && !config.isActiveScheduleReceiver()
        if (config.isCheckboxAppLoadChecked() && (config.serviceMode.getServiceRunMode() > 0)) {
        	initiateScheduler();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        config.saveConfig(getApplicationContext());
        Log.v(TAG, "onPause");
        //unregisterReceiver(localReceiver);
        serviceHandler.stopReceiver();
        Log.v(TAG, "onPause: serviceHandler broadcast Receiver unregistered");
        serviceHandler.stopScheduler();
    }
    
	@Override
	protected void onDestroy() {
		//doUnbindService();
	    config.saveConfig(getApplicationContext());
	    Log.v(TAG, "onDestroy: broadcastReceiver unregistered");
	    
	    serviceHandler.stopScheduler();
	    serviceHandler.cancelNotification();
		super.onDestroy();
	}


	
    public void addListenerOnCheckboxBoot () {
   	 	mCheckboxBoot.setOnClickListener(new OnClickListener() {
     
    	  @Override
    	  public void onClick(View v) {
    		  addNote("is checkBoxBoot checked? " + ((CheckBox) v).isChecked());
    		  config.setCheckboxBootIsChecked (((CheckBox) v).isChecked());
    	  }
    	});
      }
    
    public void addListenerOnCheckboxAppLoad () {
    	mCheckboxAppLoad.setOnClickListener(new OnClickListener() { 
    	  @Override
    	  public void onClick(View v) {
    		  addNote("is checkBoxAppLoad checked? " + ((CheckBox) v).isChecked());
    		  config.setCheckboxAppLoadChecked (((CheckBox) v).isChecked());
    	  }
    	});
      }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void initiateScheduler () {
		serviceHandler.startScheduler();
	}

	/*
	private void unRegisterScheduleReceiver () {
		serviceHandler.unRegisterScheduleReceiver();
		if (scheduleReceiver != null) {
			if (scheduleReceiver.isRegistered) {
				Log.v(TAG, "unRegisterReceiver ScheduleReceiver");
	        	unregisterReceiver(scheduleReceiver);	
			}
			else
				Log.v(TAG, "unRegisterReceiver ScheduleReceiver is not possible");
        }
	}
	  */
	
	public void onRadioButtonClicked(View v) {
	    RadioButton button = (RadioButton) v;
	    switch(button.getId()) {
	    	case R.id.radioOnClick:
	    		config.serviceMode = ServiceConfig.ServiceRunModes.RUN_ONCE;
	    		addNote("Run service on Click");
	    		break;
	    	case R.id.radioOneMin:
	    		config.serviceMode = ServiceConfig.ServiceRunModes.ONE_MIN;
	    		addNote("Run service every minute");
	    		break;
	    	default:
	    		config.serviceMode = ServiceConfig.ServiceRunModes.FIVE_MIN;
	    		addNote("Run service every five minute");
	    		break;
	    }
	    
	    Log.v(TAG, "onRadioButtonClicked " + button.getText() + " service mode " + config.serviceMode);
	}
	
	public void onButtonStartClick(View v) {
		
		if (config.serviceMode != ServiceConfig.ServiceRunModes.RUN_ONCE && !config.isActiveScheduleReceiver()) {
			initiateScheduler();
			Log.v(TAG, "onButtonStartClick -- ScheduleReceiver ");
		}
		else {
			//startService(new Intent(this, DownloaderService.class));
			serviceHandler.startServiceOnce();
			Log.v(TAG, "onButtonStartClick -- DownloaderService");
		}
		
        serviceHandler.startReceiver();
        //doBindService();
	}
	
	public void onButtonStopClick (View v) {
		Log.v(TAG, "onButtonStopClick");
		//doUnbindService();
	}
	
	public void onButtonInfoClick (View v) {
		SimpleMessage sm = serviceHandler.getSimpleMessage();
		if (sm == null) {
			addNote ("Broadcast Message: " + serviceHandler.getReceivedBroadcastMessage());	
		}
		else {
			addNote ("Broadcast Simple Message: " + sm.toString());
		}
		
	}
	
	public void onButtonConfigureClick(View v) {
		//saveConfig();
    	config.saveConfig(getApplicationContext());
	}
	
	
	private Handler handler = new Handler();
	
	//@SuppressWarnings("unused")
	private Runnable runnableTicker = new Runnable() {
	   @Override
	   public void run() {
		  String receivedMessage = serviceHandler.getReceivedBroadcastMessage();
		  updateTicker(receivedMessage);
	      handler.postDelayed(this, 10000);
	      
	      /***** Uncomment this to imitate service activity: 
	      if (receivedMessage.length() < 40)
	    	  receivedMessage += ".";
	      else {
	    	  receivedMessage = receivedMessage.substring(0, receivedMessage.indexOf(".."));
	      }
	      */
	   }
	};

	private int tickerNumber = 0;

	private void updateTicker (String message) {
		tickerNumber++;
		TextView mText = (TextView) findViewById(R.id.textViewTicker);
		mText.setText(tickerNumber + ". " + message);
	}

	
	private int noteNumber = 0;

	public void addNote (String note) {
		noteNumber++;
		TextView mText = (TextView) findViewById(R.id.textViewNote);
		mText.setText(noteNumber + ". " + note + "\n");
	}
	

}
