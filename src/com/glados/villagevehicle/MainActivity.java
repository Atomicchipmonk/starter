package com.glados.villagevehicle;



import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.glados.villagevehicle.backend.VehicleBluetooth;
import com.glados.villagevehicle.backend.VehicleNFC;
import com.glados.villagevehicle.backend.VehicleService;
import com.glados.villagevehicle.backend.VehicleUtils;
import com.glados.villagevehicle.database.MySQLiteHelper;
import com.glados.villagevehicle.security.VehicleSecurity;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends Activity{

	public final static String TAG = "MainActivity";
	
	private final static int SCAN_BT = 1;
	private final static int REQUEST_ENABLE_BT = 2;
	
	private final static String VEHICLE_ID = "vid";
	
	private String currentUser = "";
	private byte[] aesKey;
	
	Button scanButton;
	Button startButton;
	
	
	//testing purposes only
	Button btInitButton;
	ToggleButton lockButton;
	//remove after final
	
	
	ToggleButton ignitionButton;
	boolean ignitionOn = false;
	boolean lockOn = false;
	TextView vehicleText;
	
	NfcAdapter mNfcAdapter;
	VehicleNFC mVehicleNFC;
	//BluetoothAdapter mBluetoothAdapter;
	VehicleService mVehicleService;
	boolean serviceUp = false;
	
	MySQLiteHelper mDatabase;
	
	Context mainContext = this;
	
	private int currentVehicleID = -1;
	private String currentVehicleMAC = "";
	
	private DataUpdateReceiver dataUpdateReceiver;
	
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	mVehicleService = ((VehicleService.LocalBinder) service).getService();
            if (!mVehicleService.initialize()) {
                Log.e("MainActivity", "Unable to initialize Bluetooth");
                finish();
            }
            Log.v("MainActivity", "Connecting to service");
            if(currentVehicleID != -1){
            	mVehicleService.setCurrentVehicle(currentVehicleID, aesKey);
            	mVehicleService.connect(currentVehicleMAC);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mVehicleService = null;
        }
    };
    
    
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        
        //pulls in content from startup intent
        Intent incomingIntent = this.getIntent();
        currentUser = incomingIntent.getStringExtra(LoginActivity.CURRENT_USER); 
        aesKey = incomingIntent.getByteArrayExtra(LoginActivity.USER_KEY);

        //check to see if started with beam
        if(currentUser == null || aesKey == null){
        	Log.v(TAG, "Started without required user/key");
        	this.finish();
        	return;
        }
        
        //initialize database
        mDatabase = new MySQLiteHelper(getApplication());
              
        //set up all the buttons
        scanButton = (Button) this.findViewById(R.id.scanButton);
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				Intent intent = new Intent(mainContext , ScanActivity.class);
				intent.putExtra(LoginActivity.CURRENT_USER, currentUser);
				startActivityForResult(intent, SCAN_BT);
			} 
		});
		
		startButton = (Button) this.findViewById(R.id.startButton);
		startButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN){
					mVehicleService.start(true);					
					
				} else if(event.getAction() == MotionEvent.ACTION_UP){
					mVehicleService.start(false);
				}
				return true;
			}
		});
		
		ignitionButton = (ToggleButton) this.findViewById(R.id.ignitionButton);
		ignitionButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				ignitionOn = !ignitionOn;
				if(!currentVehicleMAC.equals("")){
					mVehicleService.ignition(ignitionOn);
	            }
			} 
		});
		
		//testing purposes only!
		btInitButton = (Button) this.findViewById(R.id.btInitButton);
		btInitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				if(!currentVehicleMAC.equals("")){

					mVehicleService.initNewPassword();
					
	            }
			} 
		});
		lockButton = (ToggleButton) this.findViewById(R.id.lockButton);
		lockButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				lockOn = !lockOn;
				if(!currentVehicleMAC.equals("")){
					mVehicleService.unlock(lockOn);
	            }
			} 
		});
		
		vehicleText = (TextView) this.findViewById(R.id.currentVehicle);
		vehicleText.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				if(!currentVehicleMAC.equals("")){
					if(mVehicleService.getConnectionState() == BluetoothProfile.STATE_DISCONNECTED){
						mVehicleService.connect(currentVehicleMAC);
					} else if(mVehicleService.getConnectionState() == BluetoothProfile.STATE_CONNECTED){
						mVehicleService.disconnect();
					} else {
						mVehicleService.disconnect();
						mVehicleService.connect(currentVehicleMAC);
					}
	            }
			} 
		});
		mVehicleNFC = new VehicleNFC(this);
		
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    	if (mNfcAdapter == null) {
    		Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
    		finish();
    		return;
    	}
	        // Register NFC callback
    	mNfcAdapter.setNdefPushMessageCallback(mVehicleNFC, this);
    	mNfcAdapter.setOnNdefPushCompleteCallback(mVehicleNFC, this);
    	

    	
    	//if known user/vehicle, set up to use previous vehicle
        int currentVehicle = mDatabase.getCurrentVehicle(currentUser);
        Log.v(TAG, "currentUser: " + currentUser);
        Log.v(TAG, "currentVID: " + currentVehicle);
        if(currentVehicle != -1){
        	setVehicle(currentVehicle);
        }
		
    }


    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	//bind bluetooth service
    	Intent gattServiceIntent = new Intent(this, VehicleService.class);
        boolean worked = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.v(TAG, "Starting BT Services = " + worked);
        
        //required to catch NFC intent 			----- this is messy, possible cleanup
    	if (mNfcAdapter != null) {
            PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        	try {
        	    ndef.addDataType("*/*");    /* Handles all MIME based dispatches. 
        	                                   You should specify only the ones that you need. */
        	}
        	catch (MalformedMimeTypeException e) {
        	    throw new RuntimeException("fail", e);
        	}

        	IntentFilter[] mFilters = new IntentFilter[] {
        	        ndef,
        	};

        	String[][] mTechLists = new String[][] { new String[] { NfcF.class.getName() } };
        	mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
        }

    	if (dataUpdateReceiver == null) {
    		dataUpdateReceiver = new DataUpdateReceiver();
    	}
    	IntentFilter intentFilter = new IntentFilter(VehicleBluetooth.REFRESH_DATA_INTENT);
    	registerReceiver(dataUpdateReceiver, intentFilter);
    	
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        //set whatever vehicle is in use to current
        mDatabase.setCurrentVehicle(currentUser, currentVehicleID);
        
        mVehicleService.closeBluetooth();
        
        //cancel NFC scrape
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
        
        //close service connection
        //mVehicleService.closeBluetooth();		------Maybe no need to kill BT conn?
        if(mServiceConnection != null){
    		unbindService(mServiceConnection);
    	}
        
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	Log.v("MainActivity", "requestCode: " + requestCode);
    	Log.v("MainActivity", "resultCode: " + resultCode);
    	switch (requestCode) {
    		case SCAN_BT:
    			switch (resultCode){
    			case RESULT_CANCELED:
    				break;
    			case RESULT_OK:
    				break;
    				
    			default:
    					currentVehicleID = resultCode;
    					setVehicle(currentVehicleID);
    			}
    			break;
    		case REQUEST_ENABLE_BT:
    			//do something?
    	}
    }
    
    public void setVehicle(int vid){
    	currentVehicleID = vid;
    	String vehicleString = mDatabase.getVehicle(vid)[1] + "-" + mDatabase.getVehicle(vid)[2];
		vehicleText.setText(vehicleString);
		currentVehicleMAC = mDatabase.getVehicle(vid)[2];
		Log.v("MainActivity", currentVehicleMAC);
		try{
			mVehicleService.setCurrentVehicle(vid, aesKey);
			mVehicleService.connect(currentVehicleMAC);
		} catch (Exception E){
			Log.v(TAG, "Service not up, will connect on service connection");
		}
		
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
    	 setIntent(intent);
    	 Log.v(TAG, "New intent");
    	 mVehicleNFC.processIntent(getIntent());
     }
    
    
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VehicleBluetooth.REFRESH_DATA_INTENT)) {
            	switch (intent.getIntExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_DISCONNECTED)){
            	case BluetoothProfile.STATE_DISCONNECTED:
            		vehicleText.setTextColor(Color.DKGRAY);
            		vehicleText.clearAnimation();
            		break;
            	case BluetoothProfile.STATE_CONNECTED:
            		vehicleText.setTextColor(Color.WHITE);
            		vehicleText.clearAnimation();
            		break;
            	case BluetoothProfile.STATE_DISCONNECTING:
            	case BluetoothProfile.STATE_CONNECTING:
            		vehicleText.setTextColor(Color.LTGRAY);
            		Animation anim = new AlphaAnimation(0.0f, 1.0f);
            		anim.setDuration(200); //You can manage the blinking time with this parameter
            		anim.setStartOffset(20);
            		anim.setRepeatMode(Animation.REVERSE);
            		anim.setRepeatCount(Animation.INFINITE);
            		vehicleText.startAnimation(anim);
            		break;
            	}
              // Do stuff - maybe update my view based on the changed DB contents
            }
        }
    }
    
    
    
    
    
    
    
    
    
    //DEMO PURPOSES ONLY
    public String[] getCurrentVehicle(){
    	return mDatabase.getVehicle(currentVehicleID); 
    }
    
    public long[] getNext30SecondKey(){
    	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    	long currentTime = Calendar.getInstance(//timezone UTC?
    			).getTimeInMillis();
    	return mVehicleService.getCurrent30SecondPassword(currentTime + VehicleService.MILLIS_BETWEEN_ROLLOVER);
    }

    public void ingestNFCMessage(byte[] byteMessage){
    	String[] messageString = mVehicleNFC.extractMessage(byteMessage);
    	int vehicleID;
    	try{
    		vehicleID = Integer.parseInt(mDatabase.getVehicle(messageString[1])[0]);
    	
    	}catch( Exception e){
    		mDatabase.addVehicle(new String[]{messageString[0],messageString[1],currentUser});
    		vehicleID = Integer.parseInt(mDatabase.getVehicle(messageString[1])[0]);
    	}
    	
    	try{
    	long passKey = Long.parseLong(messageString[2]);
    	long keyTime = Long.parseLong(messageString[3]);
    	
    	mVehicleService.loadPasswords(vehicleID, new long[]{passKey, keyTime});
    	
    	} catch (Exception e){
    		Log.v(TAG, e.toString());
    		Log.v(TAG, messageString.toString());
    	}
    }
}
