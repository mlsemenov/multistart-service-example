package org.mcjug.maininterface;

import org.mcjug.jsonobjects.*;
import org.mcjug.schedulerservice.DownloaderService;
import org.mcjug.schedulerservice.ServiceConfig;
import org.mcjug.schedulerservice.ServiceHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

public class MainActivity extends Activity {

	static final String TAG = "MainActivity";
	private static final int DIALOG_MODE_CHOICE = 1;
	
	private CheckBox mCheckboxBoot, mCheckboxAppLoad;
	private ServiceConfig config;
	
	//private ScheduleReceiver scheduleReceiver;
	private ServiceHandler serviceHandler;
		
	
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
		
		if (config.getHandlerDataSourceType() == null) {
			config.setHandlerDataSourceType(ServiceConfig.DataSourceTypes.SIMPLE_MESSAGE);
		}
		
		serviceHandler = new ServiceHandler(getApplicationContext(), config.getHandlerDataSourceType());
        if (config.isCheckboxAppLoadChecked()) {
        	if((config.serviceMode.getServiceRunMode() > 0)) {
            	initiateScheduler();
        	}
        	else {
        		// If RUN_ONCE run downloader service once
        		Intent intent = new Intent(this, DownloaderService.class);
        		intent.putExtra("URL", config.getURL());
        		startService(intent);
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
			serviceHandler.startServiceOnce(config.getURL());
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
		if (config.getHandlerDataSourceType() == ServiceConfig.DataSourceTypes.SIMPLE_MESSAGE ) {
			SimpleMessage sm = serviceHandler.getSimpleMessage();
			if (sm == null) {
				addNote ("Broadcast Message: " + serviceHandler.getReceivedBroadcastMessage());	
			}
			else {
				addNote ("Broadcast Simple Message: " + sm.toString());
			}			
		}
		else if (config.getHandlerDataSourceType() == ServiceConfig.DataSourceTypes.AA_MEETING) {
			Meetings mtg = serviceHandler.getMeetings();
			if (mtg == null) {
				addNote ("Broadcast Message: " + serviceHandler.getReceivedBroadcastMessage());
			}
			else {
				addNote ("Broadcast AA Message: " + mtg.toString());
			}
		}
		else {
			MeetingTypes mtgTypes = serviceHandler.getMeetingTypes();
			if (mtgTypes == null) {
				addNote ("Broadcast Message: " + serviceHandler.getReceivedBroadcastMessage());
			}
			else {
				addNote ("Broadcast AA Message Type: " + mtgTypes.toString());
			}
		}
	}
	
	public void onButtonConfigureClick(View v) {
		//saveConfig();
    	config.saveConfig(getApplicationContext());
	}
	
    @Override
    protected Dialog onCreateDialog(int id) {
    	return new AlertDialog.Builder(MainActivity.this)
        //.setIconAttribute(android.R.attr.alertDialogIcon)
        .setTitle(R.string.alert_dialog_mode_choice)
        .setSingleChoiceItems (R.array.select_mode_items, config.getHandlerDataSourceTypeInt(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	Log.v(TAG, "Alert Radio Button Clicked : " + whichButton);
            }
        })
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	dialog.dismiss();
                int selectedTypeValue = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                config.setHandlerDataSourceType(selectedTypeValue);
                serviceHandler.setSelectedServiceType(config.getHandlerDataSourceType());
            	Log.v(TAG, "Alert Screen - user clicked Yes, set " + config.getHandlerDataSourceType());
            }
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              	Log.v(TAG, "Alert Screen - user clicked No : " + whichButton);
            }
        })
       .create();
    }
    
	@SuppressWarnings("deprecation")
	public void onButtonSelectModeClick (View v) {
		showDialog(DIALOG_MODE_CHOICE);
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
