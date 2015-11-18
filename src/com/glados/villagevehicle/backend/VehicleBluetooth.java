package com.glados.villagevehicle.backend;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;


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
import android.os.Message;
import android.util.Log;

public class VehicleBluetooth {

	private final static String TAG = VehicleBluetooth.class.getSimpleName();
	public static final String REFRESH_DATA_INTENT = "refresh_bluetooth";
	public static final String BT_CONNECTED_DATA_INTENT = "connected_bluetooth";
	
	
	private VehicleService vehicleService;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = VehicleUtils.STATE_DISCONNECTED;

    
    public final static UUID UUID_VEHICLE_CONTROL =
            UUID.fromString(VehicleGattAttributes.VEHICLE_CONTROL_SERVICE);

    
    private final MersenneTwister64 mt64 = new MersenneTwister64();
    
    private long passwordSeeds[] = new long[4];
    
    
    BTQueueThread btqt = new BTQueueThread();
    LinkedList<BTMessage> messageQueue = new LinkedList<BTMessage>();
    
    Object messageOut = new Object();
    Boolean writeMessageOut = false;
    Boolean readMessageOut = false;
    
    int messageExpected = -1;
    
    public VehicleBluetooth(VehicleService vs){
    	this.vehicleService = vs;
    }
    

    class BTQueueThread extends Thread{
    	@Override
        public void run() {
            try {
            	while(true){
    				synchronized(messageQueue){
    				if(messageQueue.isEmpty()){
    						messageQueue.wait();
            		} else {
            			
            			if(mConnectionState == VehicleUtils.STATE_CONNECTED){
	            			BTMessage nextMessage = messageQueue.peekFirst();
	            			
	            			
	            			//while((writeMessageOut == false || readMessageOut == false) &&
	            			//		nextMessage.consumeTrial()){
	            				synchronized(messageOut){
	            					setCharacteristic(nextMessage);
	            					messageOut.wait(1500);
	            					Log.v(TAG, "Message returned");
	            				}
	            				
	            				synchronized(messageOut){
	            					readResponse();
	            					messageOut.wait(1500);
	            					Log.v(TAG, "Message read");
	            					
	            				}
	            				messageQueue.pollFirst();
	            			//}
            			}
    					}
    				}
            	}
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    	
    }
    
    //initialize
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
    	
    	Intent sendIntent = new Intent(VehicleBluetooth.REFRESH_DATA_INTENT);
        sendIntent.putExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_DISCONNECTED);
        vehicleService.sendBroadcast(sendIntent);
        
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) vehicleService.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        btqt.start();
        
        return true;
    }
    
    
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                mConnectionState = BluetoothProfile.STATE_CONNECTED;
               // broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                Intent sendIntent = new Intent(VehicleBluetooth.REFRESH_DATA_INTENT);
                sendIntent.putExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_CONNECTED);
                vehicleService.sendBroadcast(sendIntent);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                Intent sendIntent = new Intent(VehicleBluetooth.REFRESH_DATA_INTENT);
                sendIntent.putExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_DISCONNECTED);
                vehicleService.sendBroadcast(sendIntent);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	//set up response notify
            	//setResponseNotifications();
               // broadcastUpdate(VehicleUtils.ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	synchronized(messageOut){
        		if(status == 0){
        			readMessageOut = true;	
        		}
        		messageOut.notify();
        		
        		handleResponse(messageQueue.peekFirst(),characteristic.getValue());
        		
        		//if expected != response, do something (send pass) or notify user
            	//Log.v(TAG, "Char Read: " + VehicleUtils.bytesToHex(characteristic.getValue()));
        		
        	}
        }
   
        
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
        	synchronized(messageOut){
        		if(status == 0){
        			writeMessageOut = true;	
        		}
        		messageOut.notify();
        	}
        	Log.v(TAG, "Characteristic Write");
        	
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	Log.v(TAG, "Char Changed, Notify worked");
        	//broadcastUpdate(VehicleUtils.ACTION_DATA_AVAILABLE, characteristic);
        }
    };
    
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = VehicleUtils.STATE_CONNECTING;
                
                Intent sendIntent = new Intent(VehicleBluetooth.REFRESH_DATA_INTENT);
                sendIntent.putExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_CONNECTING);
                vehicleService.sendBroadcast(sendIntent);
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(vehicleService, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = VehicleUtils.STATE_CONNECTING;
        
        Intent sendIntent = new Intent(VehicleBluetooth.REFRESH_DATA_INTENT);
        sendIntent.putExtra(VehicleBluetooth.BT_CONNECTED_DATA_INTENT, BluetoothProfile.STATE_CONNECTING);
        vehicleService.sendBroadcast(sendIntent);
        return true;
    }
    
    public int getConnectionState(){
    	return mConnectionState;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
	

    public void readResponse(){
    	BluetoothGattCharacteristic responseChar = mBluetoothGatt.getService(
    			UUID_VEHICLE_CONTROL).getCharacteristic(
    					UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_CHARACTERISTIC));
    	mBluetoothGatt.readCharacteristic(responseChar);
    	Log.v(TAG, "Read response call");
    }
    

    
    public void setResponseNotifications(){
    	BluetoothGattCharacteristic responseChar = mBluetoothGatt.getService(
    			UUID_VEHICLE_CONTROL).getCharacteristic(UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_CHARACTERISTIC));
    	mBluetoothGatt.setCharacteristicNotification(responseChar, true);
    
    	BluetoothGattDescriptor descriptor = responseChar.getDescriptor(
    	        UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_READ_DESCRIPTION));
    	descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    	mBluetoothGatt.writeDescriptor(descriptor);
    }
    
    
    public void logAllServiceAndChar(){
    	for(BluetoothGattService s: mBluetoothGatt.getServices()){
    		Log.v("VehicleService", "Service: " + s.getUuid());
    		for(BluetoothGattCharacteristic c : s.getCharacteristics()){
    			Log.v("VehicleService", "Characteristic: " + c.getUuid());
    		}
    	}
    }
    
    public boolean setCharacteristic(BTMessage btm){
    	
    	BluetoothGattCharacteristic btchar = null;
    	
    	switch (btm.getDest()){
    	case 1:
    		btchar = mBluetoothGatt.getService(UUID_VEHICLE_CONTROL)
    		.getCharacteristic(UUID.fromString(VehicleGattAttributes.VEHICLE_PASSWORD_CHARACTERISTIC));
    		//messageExpected = 3;
    		break;
    	case 2:
    		btchar = mBluetoothGatt.getService(UUID_VEHICLE_CONTROL)
    		.getCharacteristic(UUID.fromString(VehicleGattAttributes.VEHICLE_OPCODE_CHARACTERISTIC));
    		//messageExpected = 4;
    		break;
    	case 3:
    		btchar = mBluetoothGatt.getService(UUID_VEHICLE_CONTROL)
    		.getCharacteristic(UUID.fromString(VehicleGattAttributes.VEHICLE_OPERAND_CHARACTERISTIC));
    		//messageExpected = 5;
    		break;
    	}
    	if(btchar != null){
    		btchar.setValue(btm.val);
    		return mBluetoothGatt.writeCharacteristic(btchar);
    	} else {
    		Log.v(TAG, "No characteristic exists with that number");
    		return false;
    	}
    	
    }
    
    public boolean handleResponse(BTMessage btm, byte[] returnVal){
    	Log.v(TAG, "READ-Opcode Sent: " + btm.getDest());
    	Log.v(TAG, "READ-Operand Sent: " + VehicleUtils.bytesToHex(btm.getVal()));
    	Log.v(TAG, "READ-Read returned: " + VehicleUtils.bytesToHex(returnVal));
    	
    	
    	if(VehicleUtils.bytesToHex(btm.getResponse()).equals(VehicleUtils.bytesToHex(returnVal))){
    		
    		return true;
    	} else if (VehicleUtils.bytesToHex(returnVal).equals(VehicleUtils.bytesToHex(VehicleGattAttributes.VEHICLE_RESPONSE_PASSWORD_INCORRECT))){
    		Log.v(TAG, "No password exists for this time");
    	} else if (VehicleUtils.bytesToHex(returnVal).equals(VehicleUtils.bytesToHex(VehicleGattAttributes.VEHICLE_RESPONSE_PASSWORD_PREVIOUS))){
    		Log.v(TAG, "Password Previous");
    	} else if (VehicleUtils.bytesToHex(returnVal).equals(VehicleUtils.bytesToHex(VehicleGattAttributes.VEHICLE_RESPONSE_PASSWORD_NEXT))){
    		Log.v(TAG, "Password Next");
    	}/*else if (VehicleUtils.bytesToHex(returnVal).equals(VehicleUtils.bytesToHex(VehicleGattAttributes.VEHICLE_RESPONSE_OPERAND_INVALID_STATE))){
    		long currentTime = Calendar.getInstance(//timezone UTC?
        			).getTimeInMillis();
    		Long timeKey = vehicleService.getTimeBasedPassword(currentTime);
    		Log.v(TAG, "Operand Failed");
    		if(timeKey != null){
        		sendPassword(timeKey);
        		return true;
    		} else {
    			Log.v(TAG, "No password exists for this time");
    			return false;
    		}
    	}*/
    	
    	Log.v(TAG, "Response Not Handled: " + VehicleUtils.bytesToHex(returnVal));
    	return false;
    }
    
    
    public boolean sendOperation(byte[] opcode, byte[] operand){

    	Log.v(TAG, "SEND-Opcode Sent: " + VehicleUtils.bytesToHex(opcode));
    	Log.v(TAG, "SEND-Operand Sent: " + VehicleUtils.bytesToHex(operand));
    	
    	BTMessage opcodeMessage = new BTMessage(2, opcode, VehicleGattAttributes.VEHICLE_RESPONSE_OPCODE_ACCEPTED);
    	
    	BTMessage operandMessage = new BTMessage(3, operand, VehicleGattAttributes.VEHICLE_RESPONSE_OPERAND_ACCEPTED);
    	
    	
    	synchronized(messageQueue){
    		
	    	messageQueue.add(opcodeMessage);
	    	messageQueue.add(operandMessage);
	    	messageQueue.notify();
    	}
    	
    	return true;
    }
    
    public boolean sendOperandOnly(byte[] operand){

    	Log.v(TAG, "SEND-Operand Sent: " + VehicleUtils.bytesToHex(operand));

    	BTMessage operandMessage = new BTMessage(3, operand, VehicleGattAttributes.VEHICLE_RESPONSE_OPERAND_ACCEPTED);
    	
    	synchronized(messageQueue){
	    	messageQueue.add(operandMessage);
	    	messageQueue.notify();
    	}
    	
    	
    	return true;
    }
    
    public boolean sendPassword(long password){

    	BTMessage passwordMessage = new BTMessage(1, VehicleUtils.longToBytes(password), VehicleGattAttributes.VEHICLE_RESPONSE_PASSWORD_CORRECT);
    	
    	synchronized(messageQueue){
	    	messageQueue.add(passwordMessage);
	    	messageQueue.notify();
    	}

    	return true;
    }
 
    
    public long[] setPasswordRandom(){
    	
    	byte[] a = new byte[8];
    	byte[] b = new byte[8];
    	byte[] c = new byte[8];
    	byte[] d = new byte[8];

    	new SecureRandom().nextBytes(a);
    	new SecureRandom().nextBytes(b);
    	new SecureRandom().nextBytes(c);
    	new SecureRandom().nextBytes(d);
    	
     	
    	/*
    	byte[] a = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x23,(byte)0x45};
    	byte[] b = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x02,(byte)0x34,(byte)0x56};
    	byte[] c = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x03,(byte)0x45,(byte)0x67};
    	byte[] d = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x04,(byte)0x56,(byte)0x78};
    	*/
    	
    	
    	
    	
    	
    	
    	passwordSeeds[0] = VehicleUtils.MSB(a);
    	passwordSeeds[1] = VehicleUtils.MSB(b);
    	passwordSeeds[2] = VehicleUtils.MSB(c);
    	passwordSeeds[3] = VehicleUtils.MSB(d);
    	
    	BTMessage passwordMessageA = new BTMessage(1, a, VehicleGattAttributes.VEHICLE_RESPONSE_SEED_RECEIVED);
    	BTMessage passwordMessageB = new BTMessage(1, b, VehicleGattAttributes.VEHICLE_RESPONSE_SEED_RECEIVED);
    	BTMessage passwordMessageC = new BTMessage(1, c, VehicleGattAttributes.VEHICLE_RESPONSE_SEED_RECEIVED);
    	BTMessage passwordMessageD = new BTMessage(1, d, VehicleGattAttributes.VEHICLE_RESPONSE_SEED_SET);

    	synchronized(messageQueue){
    		messageQueue.add(passwordMessageA);
    		messageQueue.add(passwordMessageB);
    		messageQueue.add(passwordMessageC);
    		messageQueue.add(passwordMessageD);
    		messageQueue.notify();
    	}
    	
    	
    	Log.v(TAG, "Seed a: " + Long.toHexString(passwordSeeds[0]));
    	Log.v(TAG, "Seed b: " + Long.toHexString(passwordSeeds[1]));
    	Log.v(TAG, "Seed c: " + Long.toHexString(passwordSeeds[2]));
    	Log.v(TAG, "Seed d: " + Long.toHexString(passwordSeeds[3]));
    	
    	mt64.setSeed(passwordSeeds);
//    	
//    	Log.v(TAG, "Initialize 64: " + Long.toHexString(mt64.next64()));
//    	Log.v(TAG, "Initialize 64: " + Long.toHexString(mt64.next64()));
//    	Log.v(TAG, "Initialize 64: " + Long.toHexString(mt64.next64()));
//    	Log.v(TAG, "Initialize 64: " + Long.toHexString(mt64.next64()));
//    	Log.v(TAG, "Initialize 64: " + Long.toHexString(mt64.next64()));
//    	
//    	
    	return passwordSeeds;
    }
    
    public void purgeOperations(){
    	synchronized(messageQueue){
    		messageQueue.clear();
    	}
    }
    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    
    
    
    public class BTMessage{
    	
    	private int dest;
    	private byte[] val;
    	private byte[] response;
    	private int repeat = 5;
    	
    	public BTMessage(int dest, byte[] val, byte[] response){
    		this.dest = dest;
    		this.val = val;
    		this.response = response;
    	}
    	public byte[] getResponse(){
    		return response;
    	}
    	
    	public int getDest(){
    		return dest;
    	}
    	public byte[] getVal(){
    		return val;
    	}
    	
    	public boolean consumeTrial(){
    		repeat--;
    		if(repeat > 0){
    			return true;
    		} else {
    			return false;
    		}
    		
    	}
    	
    }
    
}









//----------------------------- REFERENCE FOR LATER -----------------------


// public int readResponse(){
 //	BluetoothGattCharacteristic responseChar = mBluetoothGatt.getService(
 //			UUID_VEHICLE_CONTROL).getCharacteristic(UUID.fromString(VehicleGattAttributes.VEHICLE_PASSWORD_CHARACTERISTIC));
 	
 	
 	
 //}
// 
// public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
//     if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//         Log.w(TAG, "BluetoothAdapter not initialized");
//         return;
//     }
//     
//     if(UUID_VEHICLE_CONTROL.equals(characteristic.getUuid())){
//     	BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//     			UUID.fromString(VehicleGattAttributes.VEHICLE_PASSWORD_CHARACTERISTIC));
//     	byte[] b = new byte[64];
//     	new Random().nextBytes(b);
//     	descriptor.setValue(b);
//     	mBluetoothGatt.writeDescriptor(descriptor);
//     }
// }
 
// 
//
// /**
//  * Enables or disables notification on a give characteristic.
//  *
//  * @param characteristic Characteristic to act on.
//  * @param enabled If true, enable notification.  False otherwise.
//  */
// public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
//                                           boolean enabled) {
//     if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//         Log.w(TAG, "BluetoothAdapter not initialized");
//         return;
//     }
//     mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//
//     // This is specific to Heart Rate Measurement.
//     if (VehicleGattAttributes.VEHICLE_RESPONSE_CHARACTERISTIC.equals(
//     		characteristic.getUuid())) {
//         BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                 UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_READ_DESCRIPTION));
//         descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//         mBluetoothGatt.writeDescriptor(descriptor);
//     }
// }

/**
 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
 * callback.
 *
 * @param characteristic The characteristic to read from.
 */
//public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//        Log.w(TAG, "BluetoothAdapter not initialized");
//        return;
//    }
//    mBluetoothGatt.readCharacteristic(characteristic);
//}
//
//public void readResponseDescriptor(){
//	BluetoothGattDescriptor responseDesc = mBluetoothGatt.getService(
//			UUID_VEHICLE_CONTROL).getCharacteristic(
//					UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_CHARACTERISTIC)).getDescriptor(
//							UUID.fromString(VehicleGattAttributes.VEHICLE_RESPONSE_READ_DESCRIPTION));
//	mBluetoothGatt.readDescriptor(responseDesc);
//}
//

////broadcast update
//private void broadcastUpdate(final String action) {
//  final Intent intent = new Intent(action);
//  vehicleService.sendBroadcast(intent);
//}
//
//private void broadcastUpdate(final String action,
//                           final BluetoothGattCharacteristic characteristic) {
//  final Intent intent = new Intent(action);
//
//  // This is special handling for the Heart Rate Measurement profile.  Data parsing is
//  // carried out as per profile specifications:
//  // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
//  if (VehicleGattAttributes.VEHICLE_RESPONSE_CHARACTERISTIC.equals(characteristic.getUuid())) {
//      int flag = characteristic.getProperties();
//      int format = BluetoothGattCharacteristic.FORMAT_SINT16;
//      //if ((flag & 0x01) != 0) {
//      //    format = BluetoothGattCharacteristic.FORMAT_SINT16;
//      //    Log.d(TAG, "Heart rate format UINT16.");
//      //} else {
//      //    format = BluetoothGattCharacteristic.FORMAT_UINT8;
//      //    Log.d(TAG, "Heart rate format UINT8.");
//      //}
//      final int returnCode = characteristic.getIntValue(format, 1);
//      Log.v(TAG, String.format("Received return code", returnCode));
//      intent.putExtra(VehicleUtils.EXTRA_DATA, String.valueOf(returnCode));
//  } else {
//      // For all other profiles, writes the data formatted in HEX.
//      final byte[] data = characteristic.getValue();
//      if (data != null && data.length > 0) {
//          final StringBuilder stringBuilder = new StringBuilder(data.length);
//          for(byte byteChar : data)
//              stringBuilder.append(String.format("%02X ", byteChar));
//          intent.putExtra(VehicleUtils.EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
//      }
//      Log.v(TAG, "Read other value");
//  }
//  vehicleService.sendBroadcast(intent);
//}
//
