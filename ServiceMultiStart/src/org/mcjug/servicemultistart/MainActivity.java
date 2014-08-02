package org.mcjug.servicemultistart;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.View.OnClickListener;

import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;


public class MainActivity extends Activity {

	static final String TAG = "MainActivity";
	private CheckBox mCheckboxBoot, mCheckboxAppLoad;
	private Config config;
	
	//private IntentFilter filter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
	private ScheduleReceiver scheduleReceiver;
	
	
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

	private String receivedBroadcastMessage = " ? ";
	
	private BroadcastReceiver localReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  Log.v(TAG, "LocalBroadcastReceiver onReceive: Broadcast intent detected " + intent.getAction());
				// broadcastResult = intent.getAction();
				if (intent.hasExtra(Config.LOADEDSTRING)) {
					receivedBroadcastMessage = intent.getExtras().getString(Config.LOADEDSTRING);
		        }
				else {
					receivedBroadcastMessage = "LOADEDSTRING not found";
				}
				Log.v(TAG, "LocalBroadcastReceiver onReceive broadcastResult: " + receivedBroadcastMessage);
				// finish();
		  }
		};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		config = new Config (getApplicationContext());
		
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
		
		Log.v(TAG, "broadcastReceiver initialized: " + receivedBroadcastMessage + "/" + config.serviceMode.name());
		updateTicker(receivedBroadcastMessage);
		handler.postDelayed (runnableTicker, 1000);
		
		
	}

	@Override
    protected void onResume() {
        super.onResume();
        registerReceiver(localReceiver, new IntentFilter(DownloaderService.NOTIFICATION));	
        Log.v(TAG, "onResume: localBroadcastReceiver registered");
        
        if (config.isCheckboxAppLoadChecked() && (config.serviceMode.getServiceRunMode() > 0)) {
        	scheduleReceiver = new ScheduleReceiver(); 
        	//new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        	registerReceiver (scheduleReceiver, new IntentFilter(DownloaderService.NOTIFICATION));
        	Log.v(TAG, "onResume: scheduleReceiver registered");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        config.saveConfig(getApplicationContext());
        Log.v(TAG, "onPause");
        unregisterReceiver(localReceiver);
        Log.v(TAG, "onPause: broadcastReceiver unregistered");
    }
    
	@Override
	protected void onDestroy() {
		doUnbindService();
	    config.saveConfig(getApplicationContext());
	    Log.v(TAG, "onDestroy: broadcastReceiver unregistered");
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

	public void onRadioButtonClicked(View v) {
	    RadioButton button = (RadioButton) v;
	    switch(button.getId()) {
	    	case R.id.radioOnClick:
	    		config.serviceMode = Config.ServiceRunModes.ON_CLICK;
	    		addNote("Run service on Click");
	    		break;
	    	case R.id.radioOneMin:
	    		config.serviceMode = Config.ServiceRunModes.ONE_MIN;
	    		addNote("Run service every minute");
	    		break;
	    	default:
	    		config.serviceMode = Config.ServiceRunModes.FIVE_MIN;
	    		addNote("Run service every five minute");
	    		break;
	    }
	    
	    Log.v(TAG, "onRadioButtonClicked " + button.getText() + " service mode " + config.serviceMode);
	}
	
	public void onButtonStartClick(View v) {
		Log.v(TAG, "onButtonStartClick");
		startService(new Intent(this, DownloaderService.class));
		doBindService();
	}
	
	public void onButtonStopClick (View v) {
		Log.v(TAG, "onButtonStopClick");
		doUnbindService();
	}
	
	public void onButtonInfoClick (View v) {
		startService(new Intent(this, DownloaderService.class));
		addNote (receivedBroadcastMessage);
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
		  // updateTicker(broadcastReceiver.getBroadcastResult());
		   updateTicker(receivedBroadcastMessage);
	      handler.postDelayed(this, 10000);
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
