package com.glados.villagevehicle;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity{
	
	private static final String TAG = "LoginActivity";
	
	private String currentUser = "";
	public static final String CURRENT_USER = "current_user";
	public static final String USER_KEY = "user_key";
	public static final String REQUEST_CODE = "request";
			
	public static final int AEON_LOGIN_REQUEST = 1;
	public static final int FACEBOOK_LOGIN_REQUEST = 2;
	public static final int GOOGLE_LOGIN_REQUEST = 3;
	
	
	
	
	Context mainContext;
	
	TextView currentUserText;
	Button loginButton;
	Button clearButton;
	Button googleButton;
	Button facebookButton;
	Button aeonButton;
	
	
	
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
        setContentView(R.layout.login_layout);
        mainContext = this;
        
        
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        currentUser = sharedPref.getString(CURRENT_USER, "");
        if (!currentUser.equals("")){
        	Intent intent = new Intent(mainContext , PinActivity.class);
			intent.putExtra(CURRENT_USER, currentUser);
			startActivity(intent);
        }
        
        currentUserText= (TextView) this.findViewById(R.id.currentUser);
        
        loginButton = (Button) this.findViewById(R.id.loginButton);
    	loginButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if(!currentUser.equals("")){
    				Intent intent = new Intent(mainContext , PinActivity.class);
    				intent.putExtra(CURRENT_USER, currentUser);
    				startActivity(intent);
    			} else {
    				Toast.makeText(getApplicationContext(), "No user specified",
    						   Toast.LENGTH_LONG).show();
    			}
    		}
    	});
    	
    	clearButton = (Button) this.findViewById(R.id.clearButton);
    	clearButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			
    		}
    	});
        
    	googleButton = (Button) this.findViewById(R.id.googleLogin);
    	googleButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			
    		}
    	});
        
    	facebookButton = (Button) this.findViewById(R.id.facebookLogin);
    	facebookButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			
    		}
    	});
        
    	aeonButton = (Button) this.findViewById(R.id.aeonLogin);
    	aeonButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			
    			Intent intent = new Intent(mainContext , AeonActivity.class);
    			startActivityForResult(intent, AEON_LOGIN_REQUEST);
    		}
    	});
        
        
        
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(CURRENT_USER, currentUser);
		editor.commit();
	}
	
	/*
	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
		Log.v("LoginActivity", "Saving");
		if(!currentUser.equals("")){
			savedInstanceState.putString(CURRENT_USER, currentUser);
		}

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
    	String temp = savedInstanceState.getString(CURRENT_USER);
        if (temp != null){
        	currentUser = temp;
			Intent intent = new Intent(mainContext , PinActivity.class);
			intent.putExtra(CURRENT_USER, currentUser);
			startActivity(intent);
			
        }
    	super.onRestoreInstanceState(savedInstanceState);
        Log.v("LoginActivity", "Restoring");
        // Restore state members from saved instance
        
    }*/
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	Log.v("MainActivity", "requestCode: " + requestCode);
    	Log.v("MainActivity", "resultCode: " + resultCode);
    	switch (requestCode) {
    		case GOOGLE_LOGIN_REQUEST:
    		case FACEBOOK_LOGIN_REQUEST:
    		case AEON_LOGIN_REQUEST:
    			switch (resultCode){
    			case RESULT_CANCELED:
    				break;
    			case RESULT_OK:
    				String temp = data.getStringExtra(CURRENT_USER);
    				if(temp != null){
    					currentUser = temp;
    					currentUserText.setText(currentUser);
    					Log.v(TAG, currentUser);
    				}
    				break;
    			}
    			break;
    	}
    }
	
	
	
	
}
