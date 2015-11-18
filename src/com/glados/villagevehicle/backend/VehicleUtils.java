package com.glados.villagevehicle.backend;

import java.nio.ByteBuffer;

import android.util.Log;

public class VehicleUtils {

	
	public static final String TAG = "VehicleUtils";
	
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
	
	public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    

   public static long LSB(byte[] by){
    	
    	long value = 0;
    	for (int i = 0; i < by.length; i++)
    	{
    	   value += ((long) by[i] & 0xffL) << (8 * i);
    	}
    	
    	return value;
    }
    
    public static long MSB(byte[] by){
    	
    	long value = 0;
    	for (int i = 0; i < by.length; i++)
    	{
    	   value = (value << 8) + (by[i] & 0xff);
    	}
    	
    	return value;
    }
    
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);    

    public static byte[] longToBytes(long x) {
    	buffer.clear();
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
    	buffer.clear();
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip 
        return buffer.getLong();
    }

}
