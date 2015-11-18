package com.glados.villagevehicle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AeonActivity extends Activity {

	private static final String TAG = "AeonActivity";
	
	private static String newUser;
	
	EditText userText;
	EditText userPass;
	Button nextButton;
	
	Context mainContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.aeon_login_layout);
	
	    mainContext = this;
	    
	    userText = (EditText) this.findViewById(R.id.aeonUserText);
    	//userPass = (EditText) this.findViewById(R.id.aeonPasswordText);
	    
	    
	    nextButton = (Button) this.findViewById(R.id.aeonReturn);
	    nextButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			newUser = userText.getEditableText().toString();
    			//Log.v(TAG, "newUser set: " + newUser);
    			Intent intent = new Intent(mainContext , PinActivity.class);
    			intent.putExtra(LoginActivity.CURRENT_USER, newUser);
    			intent.putExtra(LoginActivity.REQUEST_CODE, PinActivity.NEW_PIN_REQUEST);
    			startActivityForResult(intent, PinActivity.NEW_PIN_REQUEST);
    			
    		}
    	});
    	

	    
	}
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	Log.v(TAG, "requestCode: " + requestCode);
    	Log.v(TAG, "resultCode: " + resultCode);
    	switch (requestCode) {
    		case PinActivity.NEW_PIN_REQUEST:
    			if (resultCode == RESULT_OK){
	    			Intent intent = new Intent();
	    			//Log.v(TAG, "newUser: " + newUser);
	    			intent.putExtra(LoginActivity.CURRENT_USER, newUser);
	    			setResult(RESULT_OK, intent);
	    			finish();
    			} else {
    				setResult(RESULT_CANCELED, null);
	    			finish();
    			}
	    			break;
    	}
    }
	
	
	//will need to download all the seeds here as well once logged in

}
