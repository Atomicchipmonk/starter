package com.glados.villagevehicle.backend;

import com.glados.villagevehicle.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class VehicleNFC implements CreateNdefMessageCallback, OnNdefPushCompleteCallback{

	private static final String TAG = "VehicleNFC";
	MainActivity mMainActivity;
	
	public VehicleNFC(MainActivity main){
		mMainActivity = main;
	}
	
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		Log.v(TAG, "Beam sent");
		
		long[] nextKey = mMainActivity.getNext30SecondKey();
		String[] vehicle = mMainActivity.getCurrentVehicle();
		
		
		Log.v(TAG, "Next key: " + Long.toHexString(nextKey[0]));
		Log.v(TAG, "Next time: " + nextKey[1]);
		Log.v(TAG, "Vehicle MAC: " + vehicle[1]);
		Log.v(TAG, "Vehicle Name: " + vehicle[2]);
		
		byte[] message = craftMessage(vehicle[1], vehicle[2], nextKey[0], nextKey[1]);
		
		
		String text = ("Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis());
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { android.nfc.NdefRecord.createMime(
                        "application/com.glados.villagevehicle", message)
         /**
          * The Android Application Record (AAR) is commented out. When a device
          * receives a push with an AAR in it, the application specified in the AAR
          * is guaranteed to run. The AAR overrides the tag dispatch system.
          * You can add it back in to guarantee that this
          * activity starts when receiving a beamed message. For now, this code
          * uses the tag dispatch system.
          */
          //,NdefRecord.createApplicationRecord("com.glados.villagevehicle")
        });
        return msg;
	}
	
	public void processIntent(Intent intent) {
		Log.v(TAG, "Beam intent received");
		
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        mMainActivity.ingestNFCMessage(msg.getRecords()[0].getPayload());
        Toast.makeText(mMainActivity, new String(msg.getRecords()[0].getPayload()), Toast.LENGTH_LONG).show();

    }

	@Override
	public void onNdefPushComplete(NfcEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	private byte[] craftMessage(String vehicleMAC, String vehicleName, long passKey, long keyTime){
		String message = "";
		message += vehicleMAC + "//";
		message += vehicleName + "//";
		message += passKey + "//";
		message += keyTime;
		return message.getBytes();
	}
	
	public String[] extractMessage(byte[] byteMessage){
		String[] message = new String(byteMessage).split("//");
		
		//this is where you should throw an intent with the 4 pieces
		return message;
	}

}
