package com.glados.villagevehicle.backend;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.glados.villagevehicle.database.MySQLiteHelper;
import com.glados.villagevehicle.security.VehicleSecurity;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class VehicleService extends Service {
    private final static String TAG = VehicleService.class.getSimpleName();

    private VehicleBluetooth mVehicleBluetooth;
    
    public static final int MILLIS_BETWEEN_ROLLOVER = 30000;
    
    private int currentVehicle = -1;
    private MySQLiteHelper mDatabase;
    
    private Timer timer = new Timer();
    
    public static final long START_REPEAT = 25;
    
    private byte[] aesKey = null;
    
    
    //Everything required to start and bind the service
    
    private final IBinder mBinder = new LocalBinder();
    
    public class LocalBinder extends Binder {
        public VehicleService getService() {
            return VehicleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
    	closeBluetooth();
        return super.onUnbind(intent);
    }

    public void closeBluetooth(){
    	mVehicleBluetooth.close();
    }
    
    public boolean initialize(){
    	mVehicleBluetooth = new VehicleBluetooth(this);
    	mDatabase = new MySQLiteHelper(getApplication());
    	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    	return mVehicleBluetooth.initialize();
    }
    
    public void setCurrentVehicle(int vid, byte[] aesKey){
    	currentVehicle = vid;
    	this.aesKey = aesKey;
    }
    
    //Passthrough button commands

    public boolean unlock(boolean input){
    	if(input){
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_LOCK, 
    				VehicleGattAttributes.VEHICLE_OPERAND_ON);
    	} else {
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_LOCK, 
    				VehicleGattAttributes.VEHICLE_OPERAND_OFF);
    	}
    	return true;
    }
    
    public boolean ignition(boolean input){
    	if(input){
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_IGNITION, 
    				VehicleGattAttributes.VEHICLE_OPERAND_ON);
    	} else {
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_IGNITION, 
    				VehicleGattAttributes.VEHICLE_OPERAND_OFF);
    	}
    	return true;
    }
    
    public boolean panic(boolean input){
    	if(input){
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_PANIC, 
    				VehicleGattAttributes.VEHICLE_OPERAND_ON);
    	} else {
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_PANIC, 
    				VehicleGattAttributes.VEHICLE_OPERAND_OFF);
    	}
    	return true;
    }
    
    public boolean start(boolean input){
    	if(input){
    		sendPassword();
    		mVehicleBluetooth.sendOperation(VehicleGattAttributes.VEHICLE_OPCODE_START, 
    				VehicleGattAttributes.VEHICLE_OPERAND_ON);
    		TimerTask start = new TimerTask() {
				public void run() {
					mVehicleBluetooth.sendOperandOnly(VehicleGattAttributes.VEHICLE_OPERAND_ON);
				}
			};
			timer = new Timer();
			timer.schedule(start, 10, START_REPEAT);
    	} else {
    		timer.cancel();
    		timer.purge();
    		mVehicleBluetooth.purgeOperations();	
    	}
    	return true;
    }
    
    public boolean connect(String mac){
    	mVehicleBluetooth.connect(mac);
    	
    	return true;
    }
    
    public boolean sendPassword(){
    	long time = Calendar.getInstance(//timezone UTC?
    			).getTimeInMillis();
    	Long password = getTimeBasedPassword(time);
    	if(password != null){
    		mVehicleBluetooth.sendPassword(password);
    		return true;
    	} else {
    		if(reloadPasswordStore()){
    			return sendPassword();
    		} else {
    			Log.v(TAG, "Failed to reload password");
    			return false;
    		}
    	}
    }
    
    //Private password commands
    //set this to private!!!
    public boolean initNewPassword(){
    	long[] seeds = mVehicleBluetooth.setPasswordRandom();
    	long time = Calendar.getInstance(//timezone UTC?
    			).getTimeInMillis();
    	
    	MersenneTwister64 mt64 = new MersenneTwister64();
    	
    	mt64.setSeed(seeds);
    	
    	long[] state = mt64.getState();
    	
		try {
			byte[][] seedCypherText = VehicleSecurity.encrypt(seeds, aesKey);
	    	mDatabase.updateSeeds(currentVehicle, seedCypherText, time - (MILLIS_BETWEEN_ROLLOVER*2));
	    	
	    	byte[][] stateCypherText = VehicleSecurity.encrypt(state, aesKey);
	    	mDatabase.updateState(currentVehicle, stateCypherText, time - (MILLIS_BETWEEN_ROLLOVER*2));
	    	
	    	reloadPasswordStore();
	    	
		} catch (Exception e) {
			Log.v(TAG, "Encrypt failed on init seeds and reload PW store");
			e.printStackTrace();
		}
    	
    	return true;
    }
    
    private Long getTimeBasedPassword(long currentTime){
    	
    	byte[] timeKey = mDatabase.getKeyForTime(currentVehicle, currentTime);

    	long currentKey;
    	try {
	    	if(timeKey != null){
				currentKey = VehicleSecurity.decrypt(timeKey, aesKey);
				Log.v(TAG, "Current key: " + Long.toHexString(currentKey));
				return currentKey;
	    	} else {
	    		return null;
	    	}
    	} catch (Exception e) {
			Log.v(TAG, e.toString());
		}
    	return null;
    }
    
    private boolean reloadPasswordStore(){
    	
    	long stateTime = mDatabase.getStateTime(currentVehicle);
    	if(stateTime == -1L){
    		return false;
    	}
    	
    	long currentTime = Calendar.getInstance(//timezone UTC?
    			).getTimeInMillis();
    	long differentialTime = currentTime - stateTime;
    	Log.v(TAG, "Time elapsed: " + differentialTime);
    	int initialIterations = ((int) Math.floor((double)(differentialTime/(long)MILLIS_BETWEEN_ROLLOVER)) - 1);
    	
    	if(initialIterations < 0){
    		return true;
    	}
    	
    	
    	byte[][] encryptedState = mDatabase.getState(currentVehicle);
    	long[] state;
		try {
			state = VehicleSecurity.decrypt(encryptedState, aesKey);

    	MersenneTwister64 mt64 = new MersenneTwister64();
    	mt64.setState(state);
    	

    	
    	Log.v(TAG, "Num initial rollovers: " + initialIterations);
    	if(initialIterations > 0){
	    	for(int i = 0; i < initialIterations; i++){
	    		mt64.next64();
	    		stateTime += (long) MILLIS_BETWEEN_ROLLOVER;	    		
	    	}
    	}
   	
    	
    	byte[][] keys = new byte[10][];
    	long[] keyTimes = new long[10];
    	long newKey;
    	
    	for(int k = 0; k < 10; k++){
    		newKey = mt64.next64();
    		stateTime += (long) MILLIS_BETWEEN_ROLLOVER;
    		Log.v(TAG, "New key: " + Long.toHexString(newKey) + " - " + stateTime);
			keys[k] = VehicleSecurity.encrypt(newKey, aesKey);
			keyTimes[k] = stateTime;
    	}
    	
    	mDatabase.addKeys(currentVehicle, keys, keyTimes);
    	
    	long[] newstate = mt64.getState();
    	
    	byte[][] stateCypherText = VehicleSecurity.encrypt(newstate, aesKey);
    	mDatabase.updateState(currentVehicle, stateCypherText, stateTime);
    	
    	
    	
    	return true;
		} catch (Exception e1) {
			Log.v(TAG, e1.toString());
			return false;
		}
    	

    }
    
    public long[] getTimeBasedPasswords(long from, long to){
    	//returns {pass,time,pass,time,etc}
    	
    	
    	long stateTime = mDatabase.getStateTime(currentVehicle);
    	if(stateTime == -1L){
    		return null;
    	}

    	long differentialTime = from - stateTime;
    	//Log.v(TAG, "Time elapsed: " + differentialTime);
    	int initialIterations = ((int) Math.floor((double)(differentialTime/(long)MILLIS_BETWEEN_ROLLOVER)) - 1);
    	
    	
    	
    	byte[][] encryptedState = mDatabase.getState(currentVehicle);
    	long[] state;
		try {
			state = VehicleSecurity.decrypt(encryptedState, aesKey);

    	MersenneTwister64 mt64 = new MersenneTwister64();
    	mt64.setState(state);
    	

    	
    	Log.v(TAG, "Num initial rollovers: " + initialIterations);
    	if(initialIterations > 0){
	    	for(int i = 0; i < initialIterations; i++){
	    		mt64.next64();
	    		stateTime += (long) MILLIS_BETWEEN_ROLLOVER;	    		
	    	}
    	}
   	
    	int numKeys = (int) Math.ceil((to - from) / MILLIS_BETWEEN_ROLLOVER);
    	
    	long[] keys = new long[numKeys*2];


    	
    	for(int k = 0; k < numKeys*2; k+=2){
    		keys[k] = mt64.next64();
    		stateTime += (long) MILLIS_BETWEEN_ROLLOVER;
    		//Log.v(TAG, "New key: " + Long.toHexString(newKey) + " - " + stateTime);
			keys[k+1] = stateTime;
    	}
    	
    	return keys;
		} catch (Exception e1) {
			Log.v(TAG, e1.toString());
			return null;
		}
    }
    
    public boolean loadPasswords(int vehicleID, long[] keys){

    	
    	if(vehicleID == -1){
    		return false;
    	}
    	
    	byte[][] encryptedKeys = new byte[keys.length/2][];
    	long[] keyTimes = new long[keys.length/2];
    	long newKey;
    	
    	try{
	    	for(int k = 0; k < keys.length / 2; k++){
	    		
	    		newKey = keys[k*2];
	    		keyTimes[k] = keys[(k*2)+1];
	    		Log.v(TAG, "New key: " + Long.toHexString(newKey) + " - " + keyTimes[k]);
				encryptedKeys[k] = VehicleSecurity.encrypt(newKey, aesKey);
	    	}
	    	
	    	mDatabase.addKeys(vehicleID, encryptedKeys, keyTimes);
	    	
	    	
    	return true;
		} catch (Exception e1) {
			Log.v(TAG, e1.toString());
			return false;
		}   	
    }
    

    public long[] getCurrent30SecondPassword(long currentTime){
    	reloadPasswordStore();
    	
    	byte[] passKey = mDatabase.getKeyForTime(currentVehicle, currentTime);
    	long keyTime = mDatabase.getKeyStartTime(currentVehicle, currentTime);
    	long currentKey;
    	try {
	    	if(passKey != null){
				currentKey = VehicleSecurity.decrypt(passKey, aesKey);
				Log.v(TAG, "Current key: " + Long.toHexString(currentKey));
				return new long[]{currentKey, keyTime};
	    	} else {
	    		return null;
	    	}
    	} catch (Exception e) {
			Log.v(TAG, e.toString());
		}
    	return null;
    }
    
    public void disconnect(){
    	mVehicleBluetooth.disconnect();
    }
   
    public int getConnectionState(){
    	return mVehicleBluetooth.getConnectionState();
    }
    
}