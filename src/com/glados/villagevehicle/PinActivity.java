package com.glados.villagevehicle;


/*Client ID: 	381130279932.apps.googleusercontent.com
Redirect URIs: 	urn:ietf:wg:oauth:2.0:oob http://localhost
Application type: 	Android
Package name: 	com.ultimasquare.pinview
Certificate fingerprint (SHA1): 	86:F2:4D:FD:34:98:BF:0C:47:94:34:D4:8C:68:A3:84:B7:D7:B2:0F
Deep Linking: 	Disabled*/


import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.glados.villagevehicle.database.MySQLiteHelper;
import com.glados.villagevehicle.security.VehicleSecurity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class PinActivity extends Activity {

	
	public static final int NEW_PIN_REQUEST = 1;
	public static final int PIN_LOGIN_REQUEST = 0;
	
	
	
	VehicleSecurity sec;
	String currentUser;
	
	Boolean newLogin = false;
	Boolean firstTime = true;
	
	String userEntered;
	String userEnteredTwice;


String userPin="8888";
final int PIN_LENGTH = 4;


boolean keyPadLockedFlag = false;
Context appContext;

TextView titleView;

TextView pinBox0;
TextView pinBox1;
TextView pinBox2;
TextView pinBox3;



TextView statusView;

Button button0;
Button button1;
Button button2;
Button button3;
Button button4;
Button button5;
Button button6;
Button button7;
Button button8;
Button button9;
Button button10;
Button buttonExit;
Button buttonDelete;
EditText passwordInput;
ImageView backSpace;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent incomingIntent = this.getIntent();
    
    int incomingRequest = incomingIntent.getIntExtra(LoginActivity.REQUEST_CODE, PIN_LOGIN_REQUEST);
    if(incomingRequest == NEW_PIN_REQUEST){
    	newLogin = true;
    }
    
    String temp = incomingIntent.getStringExtra(LoginActivity.CURRENT_USER);
    if(temp != null){
    	currentUser = temp;
    	sec = new VehicleSecurity(currentUser, new MySQLiteHelper(getApplication()));
    } else {
    	//no user sent
    	setResult(RESULT_CANCELED, null);
    	finish();
    }
    
    
    
    
    
    appContext = this;
    userEntered = "";


    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
    WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.pin_layout);

    Typeface xpressive=Typeface.createFromAsset(getAssets(), "fonts/XpressiveBold.ttf");

	
    
    
    
    
    statusView = (TextView) findViewById(R.id.statusview);
    passwordInput = (EditText) findViewById(R.id.editText);
    backSpace = (ImageView) findViewById(R.id.imageView);
    buttonExit = (Button) findViewById(R.id.buttonExit);
    backSpace.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            passwordInput.setText(passwordInput.getText().toString().substring(0,passwordInput.getText().toString().length()-2));
        }
    });
    buttonExit.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { 
        	setResult(RESULT_CANCELED, null);
            finish();

        	}
        }
    );
    buttonExit.setTypeface(xpressive);


    buttonDelete = (Button) findViewById(R.id.buttonDeleteBack);
    buttonDelete.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {

            if (keyPadLockedFlag == true)
            {
                return;
            }

            if (userEntered.length()>0)
            {
                userEntered = userEntered.substring(0,userEntered.length()-1);
                passwordInput.setText("");
            }


        }

        }
    );

    titleView = (TextView)findViewById(R.id.time);
    titleView.setTypeface(xpressive);



    View.OnClickListener pinButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {

            if (keyPadLockedFlag == true)
            {
                return;
            }

            Button pressedButton = (Button)v;


            if (userEntered.length() < PIN_LENGTH)
            {
                userEntered = userEntered + pressedButton.getText();
                Log.v("PinView", "User entered="+userEntered);

                //Update pin boxes
                passwordInput.setText(passwordInput.getText().toString()+"*");
                passwordInput.setSelection(passwordInput.getText().toString().length());

                
                
	                if (userEntered.length()==PIN_LENGTH)
	                {
	                	if (newLogin == false) {
		                    //Check if entered PIN is correct
		                	byte[] key = sec.isPinCorrect(userEntered);
		                    if (key != null)
		                    {
		                        statusView.setTextColor(Color.GREEN);
		                        statusView.setText("Correct");
		                        Log.v("PinView", "Correct PIN");
		                        
		                        Intent intent = new Intent(appContext , MainActivity.class);
		            			intent.putExtra(LoginActivity.CURRENT_USER, currentUser);
		            			intent.putExtra(LoginActivity.USER_KEY, key);
		            			startActivity(intent);
		                        
		            			for(int i = 0; i < key.length; i++){
		            				key[i] = 0;
		            			}
		            			
		                        finish();
		                    }
		                    else
		                    {
		                        statusView.setTextColor(Color.RED);
		                        statusView.setText("Wrong PIN. Keypad Locked");
		                        keyPadLockedFlag = true;
		                        Log.v("PinView", "Wrong PIN");
		                        new LockKeyPadOperation().execute("");
		                    }
	                	}
	                	else
	                	{
	                		if(firstTime){
	                			userEnteredTwice = userEntered;
	                			
	                			passwordInput.setText("");

	                            userEntered = "";

	                            statusView.setText("Plese Re-enter PIN");

	                            //userEntered = userEntered + pressedButton.getText();
	                            Log.v("PinView", "User entered="+userEnteredTwice);

	                            firstTime = false;
	                            //Update pin boxes
	                            //passwordInput.setText("8");
	                		} else {
	                			Log.v("PinActivity", userEntered+userEnteredTwice);
	                			if(userEnteredTwice.equals(userEntered)){
	                				Log.v("PinActivity", "Good login");
	                				sec.newPin(userEntered);
	                				setResult(RESULT_OK);
	                				finish();
	                			} else {
	                				
	                				userEnteredTwice = "";
		                			
		                			passwordInput.setText("");

		                            userEntered = "";

		                            statusView.setText("Passwords Did Not Match");

		                            userEntered = userEntered + pressedButton.getText();
		                            Log.v("PinView", "User entered="+userEntered);
		                            firstTime = true;
		                            //Update pin boxes
		                            //passwordInput.setText("8");
	                			}
	                		}
	                		//user requesting new login
	                		
	                	}
                } else {
                	
                }
            }
            else
            {
                //Roll over
                passwordInput.setText("");

                userEntered = "";

                statusView.setText("");

                userEntered = userEntered + pressedButton.getText();
                Log.v("PinView", "User entered="+userEntered);

                //Update pin boxes
                passwordInput.setText("8");

            }


        }
      };


    button0 = (Button)findViewById(R.id.button0);
    button0.setTypeface(xpressive);
    button0.setOnClickListener(pinButtonHandler);

    button1 = (Button)findViewById(R.id.button1);
    button1.setTypeface(xpressive);
    button1.setOnClickListener(pinButtonHandler);

    button2 = (Button)findViewById(R.id.button2);
    button2.setTypeface(xpressive);
    button2.setOnClickListener(pinButtonHandler);


    button3 = (Button)findViewById(R.id.button3);
    button3.setTypeface(xpressive);
    button3.setOnClickListener(pinButtonHandler);

    button4 = (Button)findViewById(R.id.button4);
    button4.setTypeface(xpressive);
    button4.setOnClickListener(pinButtonHandler);

    button5 = (Button)findViewById(R.id.button5);
    button5.setTypeface(xpressive);
    button5.setOnClickListener(pinButtonHandler);

    button6 = (Button)findViewById(R.id.button6);
    button6.setTypeface(xpressive);
    button6.setOnClickListener(pinButtonHandler);

    button7 = (Button)findViewById(R.id.button7);
    button7.setTypeface(xpressive);
    button7.setOnClickListener(pinButtonHandler);

    button8 = (Button)findViewById(R.id.button8);
    button8.setTypeface(xpressive);
    button8.setOnClickListener(pinButtonHandler);

    button9 = (Button)findViewById(R.id.button9);
    button9.setTypeface(xpressive);
    button9.setOnClickListener(pinButtonHandler);





    buttonDelete = (Button)findViewById(R.id.buttonDeleteBack);
    buttonDelete.setTypeface(xpressive);



}

@Override
public void onBackPressed() {
    // TODO Auto-generated method stub

    //App not allowed to go back to Parent activity until correct pin entered.
    return;
    //super.onBackPressed();
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    //getMenuInflater().inflate(R.menu.activity_pin_entry_view, menu);
    return true;
}


private class LockKeyPadOperation extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
          for(int i=0;i<2;i++) {
              try {
                  Thread.sleep(1000);
              } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
          }

          return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
            statusView.setText("");

        //Roll over
        passwordInput.setText("");
            
            userEntered = "";

            keyPadLockedFlag = false;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}
}