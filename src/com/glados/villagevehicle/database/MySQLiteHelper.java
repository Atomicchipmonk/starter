package com.glados.villagevehicle.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.glados.villagevehicle.backend.MersenneTwister64;
import com.glados.villagevehicle.backend.VehicleService;
import com.glados.villagevehicle.backend.VehicleUtils;
import com.glados.villagevehicle.security.VehicleSecurity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper{

	 // All Static variables
	
	
		private static final String TAG = "SQLite Helper";
		// Date format
		private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd-HH:mm:ss");
		
	    // Database Version
	    private static final int DATABASE_VERSION = 12;
	 
	    // Database Name
	    private static final String DATABASE_NAME = "vehicle_db";
	 
	    // Contacts table name
	    private static final String TABLE_VEHICLE = "vehicle";
	    private static final String TABLE_USER = "user";
	    private static final String TABLE_SEEDS = "seeds";
	    private static final String TABLE_STATE = "keystore";
	    private static final String TABLE_TIME_KEYS = "timekeystore";
	    
	    
	    
	    // Contacts Table Columns names
	    
	    private static final String KEY_ID = "id";
	    private static final String KEY_NAME = "name";
	    private static final String KEY_MAC = "mac";
	    private static final String KEY_OWNER = "owner";
	    
	    private static final String KEY_PROFILE = "profile";
	    private static final String KEY_HASH = "hash";
	    private static final String KEY_SALT = "salt";
	    private static final String KEY_CURRENT_VEHICLE = "currentvehicle";
	    
	    private static final String KEY_VEHICLE_ID = "vid";
	    private static final String KEY_SEED_A = "a";
	    private static final String KEY_SEED_B = "b";
	    private static final String KEY_SEED_C = "c";
	    private static final String KEY_SEED_D = "d";
	    
	    private static final String KEY_STATE = "state"; //1 to n keys (twist)
	    private static final String KEY_TIME_KEYS = "timekeys"; //1 to n keys (twist)
	    private static final String KEY_TIME = "time"; //1 to n keys (twist)
	    
	    
	    //private Context context;
	    
	    private SQLiteDatabase mDatabase;
	    
	    
	    public MySQLiteHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        //this.context = context;
	        mDatabase = this.getWritableDatabase();
	        }
	    
	 
	    //TABLE HANDLING
	    // Creating Tables
	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	
	    	String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
	                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_OWNER + " STRING, " +
	        		KEY_SALT + " BLOB, " + KEY_HASH + " BLOB, " +
	        		KEY_CURRENT_VEHICLE + " INTEGER)";
	    	
	        String CREATE_VEHICLE_TABLE = "CREATE TABLE " + TABLE_VEHICLE + "("
	                + KEY_VEHICLE_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " STRING, " + 
	        		KEY_MAC + " STRING, " + KEY_OWNER + " INTEGER)";
	        
	        String CREATE_SEED_TABLE = "CREATE TABLE " + TABLE_SEEDS + "("
	                + KEY_VEHICLE_ID + " INTEGER PRIMARY KEY," + KEY_TIME + " INTEGER," + KEY_SEED_A + " BLOB, " + KEY_SEED_B + " BLOB, " + 
	        		KEY_SEED_C + " BLOB, " + KEY_SEED_D + " BLOB)";
	        
	        
	        //312 current keys
	        String CREATE_STATE_TABLE = "CREATE TABLE " + TABLE_STATE + "("
	                + KEY_VEHICLE_ID + " INTEGER PRIMARY KEY, " + KEY_TIME + " INTEGER";
	        for(int i = 0; i < MersenneTwister64.TWIST_NUMBERS+1; i++){
	        	CREATE_STATE_TABLE = CREATE_STATE_TABLE +", "+KEY_STATE + i + " BLOB";
	        }
	        CREATE_STATE_TABLE = CREATE_STATE_TABLE +")";
	        
	        //keys broken out for the next X amount of time
	        String CREATE_GENERATED_TABLE = "CREATE TABLE " + TABLE_TIME_KEYS + "("
	                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_VEHICLE_ID + " INTEGER, " + 
	        		KEY_TIME_KEYS + " BLOB, " + KEY_TIME + " INTEGER)";
	       
	        
	        
	        db.execSQL(CREATE_USER_TABLE);
	        db.execSQL(CREATE_VEHICLE_TABLE);
	        db.execSQL(CREATE_SEED_TABLE);
	        db.execSQL(CREATE_STATE_TABLE);
	        db.execSQL(CREATE_GENERATED_TABLE);

	        //build keytable and settingstable as well
	        
	    }
	    
	 
	    // Upgrading database
	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        // Drop older table if existed
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VEHICLE);
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEEDS);
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATE);
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_KEYS);
	        // Create tables again
	        onCreate(db);
	    }
	    
	    
	    //All user operations - add, get salt/hash

		public void addNewUser(String currentUser, byte[] hash, byte[] salt) {
			Log.v("MySQLiteHelper", "CurrentUser: " + currentUser);
			Log.v("MySQLiteHelper", "hash: " + VehicleUtils.bytesToHex(hash));
			Log.v("MySQLiteHelper", "salt: " + VehicleUtils.bytesToHex(salt));
			Cursor cursor = mDatabase.query(TABLE_USER, null, KEY_OWNER + "=?",
		   	           new String[] { currentUser }, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();

		   	    }
	    	
	        ContentValues values = new ContentValues();
	        values.put(KEY_OWNER, currentUser);
	        values.put(KEY_HASH, hash);
	        values.put(KEY_SALT, salt);
	        values.put(KEY_CURRENT_VEHICLE, -1);

	        
	        // Inserting Row
	        
	        mDatabase.insert(TABLE_USER, null, values);
			
		}
		
		public int getCurrentVehicle(String currentUser){
			String keys[] = new String[1];
		   	keys[0] = KEY_CURRENT_VEHICLE;
			Cursor cursor = mDatabase.query(TABLE_USER, keys, KEY_OWNER + "=?",
	           new String[] { currentUser }, null, null, null, null);
	
		    if (cursor != null){
		        cursor.moveToFirst();
		        
		    } else { 
		    	return -1;
		    }
		    int currentVehicle = cursor.getInt(0);
		    Log.v(TAG, "current VID: " + currentVehicle); 
		    Log.v(TAG, "current user: "+ currentUser);
		    return currentVehicle;
		}
		
		public void updateUser(String currentUser, byte[] hash, byte[] salt){
			//todo
		}
		
		public void deleteUser(String currentUser){
			//todo
		}
		

		public byte[] getSalt(String currentUser) {
			String keys[] = new String[1];
		   	keys[0] = KEY_SALT;
			Cursor cursor = mDatabase.query(TABLE_USER, keys, KEY_OWNER + "=?",
	           new String[] { currentUser }, null, null, null, null);

	    if (cursor != null){
	        cursor.moveToFirst();
	        
	    } else { 
	    	return null;
	    }
	    byte[] salt = cursor.getBlob(0);
	    Log.v("MySQLiteHelper", "salt: " + VehicleUtils.bytesToHex(salt)); 
	    return salt;
		}


		public byte[] getHash(String currentUser) {
			String keys[] = new String[1];
		   	keys[0] = KEY_HASH;
			Cursor cursor = mDatabase.query(TABLE_USER, keys, KEY_OWNER + "=?",
	           new String[] { currentUser }, null, null, null, null);

	    if (cursor != null){
	        cursor.moveToFirst();
	        
	    } else { 
	    	return null;
	    }
	    byte[] hash = cursor.getBlob(0);
	    Log.v("MySQLiteHelper", "hash: " + VehicleUtils.bytesToHex(hash)); 
	    return hash;
		}


		
		
		//All vehicle operations - add, get, modify
		
	    public void addVehicle(String[] data){
	    	 
	        ContentValues values = new ContentValues();
	        //values.put(KEY_MILLIS, Calendar.getInstance().getTimeInMillis());
	        values.put(KEY_NAME, data[0]);
	        values.put(KEY_MAC, data[1]);
	        values.put(KEY_OWNER, data[2]);
	        
	        // Inserting Row
	        Log.v("MySQLiteHelper", values.toString());
	        mDatabase.insert(TABLE_VEHICLE, null, values);
	    }
	    
	    
	  //basic retrieval of a line of data
	    public String[] getVehicle(int id){
	   	 
	   	 String keys[] = new String[3];
	   	 keys[0] = KEY_VEHICLE_ID;
	   	 keys[1] = KEY_NAME;
	   	 keys[2] = KEY_MAC;
	   	 
	   	 	try{
	   	 		
	   	 		
	   	    Cursor cursor = mDatabase.query(TABLE_VEHICLE, keys, KEY_VEHICLE_ID + "=?",
	   	           new String[] { String.valueOf(id) }, null, null, null, null);

	   	    //Cursor cursor = mDatabase.query(TABLE_DATA, keys, null,
	   	    //        null, null, null, null, null);
	   	    if (cursor != null){
	   	        cursor.moveToFirst();

	   	    }
		    	    
	   	    String vehicle[] = new String[3];
	   	    vehicle[0] = cursor.getString(0);
	   	    vehicle[1] = cursor.getString(1);
	   	    vehicle[2] = cursor.getString(2);
	   	    cursor.close();
	   	    return vehicle;
	   	    
	   	    } catch (Exception e){
	   	   	 Log.v("MySQLiteHelper", e.toString());
	   	    	String vehicle[] = {"No Vehicle Found","N/A"};
	   	    	return vehicle;
	   	    }
	   	    
	    }

		  //basic retrieval of a line of data
		    public String[] getVehicle(String mac){
		   	 
		   	 String keys[] = new String[3];
		   	 keys[0] = KEY_VEHICLE_ID;
		   	 keys[1] = KEY_NAME;
		   	 keys[2] = KEY_MAC;
		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_VEHICLE, keys, KEY_MAC + "=?",
		   	           new String[] { mac }, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return null;
		   	    }
			    	    
		   	    String vehicle[] = new String[3];
		   	    vehicle[0] = cursor.getString(0);
		   	    vehicle[1] = cursor.getString(1);
		   	    vehicle[2] = cursor.getString(2);
		   	    cursor.close();
		   	    return vehicle;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return null;
		   	    }
		   	    
		    }
	    
	    public void deleteVehicle(int v_id){
	    	mDatabase.delete(TABLE_VEHICLE, KEY_ID + " = ?",
	                new String[] { Integer.toString(v_id) });
	    }
	    
	    public void setCurrentVehicle(String currentUser, int vehicleID){
	    	 
	    	Cursor cursor = mDatabase.query(TABLE_USER, null, KEY_OWNER + "=?",
		   	           new String[] { currentUser }, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();

		   	    }
	    	
	        ContentValues values = new ContentValues();
	        values.put(KEY_CURRENT_VEHICLE, vehicleID);

	        
	        // Inserting Row
	        
	        mDatabase.update(TABLE_USER, values, null, null);
	    }


	    public void updateSeeds(int vehicleID, byte[][] seeds, long time){
	    	 
	        ContentValues values = new ContentValues();
	        values.put(KEY_TIME, time);
	        values.put(KEY_VEHICLE_ID, vehicleID);
	        values.put(KEY_SEED_A, seeds[0]);
	        values.put(KEY_SEED_B, seeds[1]);
	        values.put(KEY_SEED_C, seeds[2]);
	        values.put(KEY_SEED_D, seeds[3]);
	        
	        // Inserting Row
	        Log.v("MySQLiteHelper", "Seeds updated, state and keys dropped");
	        mDatabase.replace(TABLE_SEEDS, null, values);
	        mDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_STATE);
	        mDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_KEYS);
	        
	        String CREATE_STATE_TABLE = "CREATE TABLE " + TABLE_STATE + "("
	                + KEY_VEHICLE_ID + " INTEGER PRIMARY KEY, " + KEY_TIME + " INTEGER";
	        for(int i = 0; i < 313; i++){
	        	CREATE_STATE_TABLE = CREATE_STATE_TABLE +", "+KEY_STATE + i + " BLOB";
	        }
	        CREATE_STATE_TABLE = CREATE_STATE_TABLE +")";
	        
	        //keys broken out for the next X amount of time
	        String CREATE_GENERATED_TABLE = "CREATE TABLE " + TABLE_TIME_KEYS + "("
	                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_VEHICLE_ID + " INTEGER, " + 
	        		KEY_TIME_KEYS + " BLOB, " + KEY_TIME + " INTEGER)";
	        
	        mDatabase.execSQL(CREATE_STATE_TABLE);
	        mDatabase.execSQL(CREATE_GENERATED_TABLE);
	        
	    }
	    
	    public byte[][] getSeeds(int vehicleID){
	    	String keys[] = new String[5];
		   	 keys[0] = KEY_VEHICLE_ID;
		   	 keys[1] = KEY_SEED_A;
		   	 keys[2] = KEY_SEED_B;
		   	 keys[3] = KEY_SEED_C;
		   	 keys[4] = KEY_SEED_D;
		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_SEEDS, keys, KEY_VEHICLE_ID + "=?",
		   	           new String[] { String.valueOf(vehicleID) }, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return null;
		   	    }
			    	    
		   	    byte[] seeds[] = new byte[4][];
		   	    seeds[0] = cursor.getBlob(0);
		   	    seeds[1] = cursor.getBlob(1);
		   	    seeds[2] = cursor.getBlob(2);
		   	    seeds[3] = cursor.getBlob(3);
		   	    cursor.close();
		   	    return seeds;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return null;
		   	    }
	    }
	    
	    //saving current state
	    public void updateState(int vehicleID, byte[][] state, long time){
	    	 
	        ContentValues values = new ContentValues();
	        values.put(KEY_TIME, time);
	        values.put(KEY_VEHICLE_ID, vehicleID);
	        for(int i = 0; i < MersenneTwister64.TWIST_NUMBERS+1; i++){
	        	values.put(KEY_STATE + i, state[i]);
	        }
	        
	        
	        // Inserting Row
	       // Log.v("MySQLiteHelper", values.toString());
	        //mDatabase.insert(TABLE_SEEDS, null, values);
	        mDatabase.replace(TABLE_STATE, null, values);
	    }
	    
	    public byte[][] getState(int vehicleID){
		    	String keys[] = new String[MersenneTwister64.TWIST_NUMBERS+1];
		    	for(int i = 0; i < MersenneTwister64.TWIST_NUMBERS+1; i++){
		        	keys[i] = KEY_STATE + i;
		        }

			   	 
			   	 	try{
			   	 		
			   	 		
			   	    Cursor cursor = mDatabase.query(TABLE_STATE, keys, KEY_VEHICLE_ID + "=?",
			   	           new String[] { String.valueOf(vehicleID) }, null, null, null, null);

			   	    if (cursor != null){
			   	        cursor.moveToFirst();
			   	        
			   	    } else { 
			   	    	return null;
			   	    }
				    	    
		   	    
			   	    byte[] state[] = new byte[MersenneTwister64.TWIST_NUMBERS+1][];
			   	    for(int i = 0; i < MersenneTwister64.TWIST_NUMBERS+1; i++){
			        	state[i] = cursor.getBlob(i);
			        }
			   	    cursor.close();
			   	    return state;
			   	    
			   	    } catch (Exception e){
			   	   	 Log.v("MySQLiteHelper", e.toString());
			   	    	return null;
			   	    }
		    
	    }
	    
	    public long getStateTime(int vehicleID){
	    	String keys[] = new String[1];
	    	keys[0] = KEY_TIME;

		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_STATE, keys, KEY_VEHICLE_ID + "=?",
		   	           new String[] { String.valueOf(vehicleID) }, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return -1L;
		   	    }
	   	    
		   	    long time = cursor.getLong(0);

		   	    cursor.close();
		   	    return time;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return -1L;
		   	    }
	    
    }


		public byte[] getKeyForTime(int vehicleID, long currentTime) {
			String keys[] = new String[2];
	    	keys[0] = KEY_TIME_KEYS;
	    	keys[1] = KEY_TIME;

	    	long prevTime = currentTime - (VehicleService.MILLIS_BETWEEN_ROLLOVER);
	    	//long nextTime = currentTime + (VehicleService.MILLIS_BETWEEN_ROLLOVER);
		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_TIME_KEYS, keys, KEY_VEHICLE_ID + "=?",
		   	           new String[] { String.valueOf(vehicleID)}, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return null;
		   	    }
	   	    
		   	    long keyTime;
		   	    boolean hasKey = false;
		   	    Log.v(TAG,"Time span: " + prevTime + " - " + currentTime);
	   	    	
	   	    	
		   	    for(int i = 0; i < cursor.getCount(); i++){
		   	    	keyTime = cursor.getLong(1);
		   	    	
		   	    	
		   	    	
		   	    	if(keyTime > prevTime && keyTime <= currentTime){
		   	    		hasKey = true;
		   	    		break;
		   	    	}
		   	    	
		   	    	cursor.moveToNext();
		   	    }
		   	    
		   	    if(hasKey == false){
		   	    	return null;
		   	    }
		   	    
		   	    byte[] timeKey = cursor.getBlob(0);
		   	    Log.v(TAG, "Key Time: " + cursor.getLong(1));
		   	    
		   	    cursor.close();
		   	    return timeKey;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return null;
		   	    }
		}
		
		
		public byte[] getKeyForTime(int vehicleID, long timeA, long timeB) {
			String keys[] = new String[2];
	    	keys[0] = KEY_TIME_KEYS;
	    	keys[1] = KEY_TIME;

	    	long prevTime = timeA - (VehicleService.MILLIS_BETWEEN_ROLLOVER);
	    	//long nextTime = currentTime + (VehicleService.MILLIS_BETWEEN_ROLLOVER);
		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_TIME_KEYS, keys, KEY_VEHICLE_ID + " =?",
		   	           new String[] { String.valueOf(vehicleID)}, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return null;
		   	    }
	   	    
		   	    long keyTime;
		   	    boolean hasKey = false;
		   	    
		   	    while(cursor.moveToNext()){
		   	    	keyTime = cursor.getLong(1);
		   	    	if(keyTime > prevTime && keyTime <= timeB){
		   	    		hasKey = true;
		   	    	}
		   	    }
		   	    
		   	    if(hasKey == false){
		   	    	cursor.close();
		   	    	return null;
		   	    }
		   	    
		   	    byte[] timeKey = cursor.getBlob(0);

		   	    cursor.close();
		   	    return timeKey;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return null;
		   	    }
		}
		
		public long getKeyStartTime(int vehicleID, long timeA){
			String keys[] = new String[1];
	    	keys[0] = KEY_TIME;

	    	long prevTime = timeA - (VehicleService.MILLIS_BETWEEN_ROLLOVER);
	    	//long nextTime = currentTime + (VehicleService.MILLIS_BETWEEN_ROLLOVER);
		   	 
		   	 	try{
		   	 		
		   	 		
		   	    Cursor cursor = mDatabase.query(TABLE_TIME_KEYS, keys, KEY_VEHICLE_ID + " =?",
		   	           new String[] { String.valueOf(vehicleID)}, null, null, null, null);

		   	    if (cursor != null){
		   	        cursor.moveToFirst();
		   	        
		   	    } else { 
		   	    	return -1;
		   	    }
	   	    
		   	    long keyTime = -1;
		   	    boolean hasKey = false;
		   	    
		   	    while(cursor.moveToNext()){
		   	    	keyTime = cursor.getLong(0);
		   	    	if(keyTime > prevTime && keyTime <= timeA){
		   	    		hasKey = true;
		   	    		break;
		   	    	}
		   	    }
		   	    
		   	    cursor.close();
		   	    if(hasKey == false){
		   	    	return -1;
		   	    }


		   	    return keyTime;
		   	    
		   	    } catch (Exception e){
		   	   	 Log.v("MySQLiteHelper", e.toString());
		   	    	return -1;
		   	    }
		}
		
		public void addKeys(int vehicleID, byte[][] keys, long[] times){

			for(int i = 0; i < keys.length; i++ ){
		        ContentValues values = new ContentValues();
		        values.put(KEY_TIME, times[i]);
		        values.put(KEY_VEHICLE_ID, vehicleID);
		        values.put(KEY_TIME_KEYS, keys[i]);
		        
		        
		        // Inserting Row
		        //Log.v("MySQLiteHelper", values.toString());
		        //mDatabase.insert(TABLE_SEEDS, null, values);
		        mDatabase.insert(TABLE_TIME_KEYS, null, values);
			}
		}
		
		
		//purges all keys up to a certain time
		public void purgeKeys(long time){
			
		}
		

		
}
