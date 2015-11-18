package com.glados.villagevehicle.security;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.glados.villagevehicle.backend.VehicleUtils;
import com.glados.villagevehicle.database.MySQLiteHelper;

import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class VehicleSecurity {

	
	private SecretKey privateKey;
	String currentUser;
	MySQLiteHelper mDatabase;
	
	private static String SECRET_SALT = "secret_salt";
	private byte[] salt;
	
	final int outputKeyLength = 256;
	
	public VehicleSecurity(String currentUser, MySQLiteHelper mDatabase){
		this.currentUser = currentUser;
		this.mDatabase = mDatabase;
	}
	
	public byte[] isPinCorrect(String pin){
		try {
		privateKey = generateKey(pin.toCharArray(), mDatabase.getSalt(currentUser));
		byte[] userHash = mDatabase.getHash(currentUser);
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(privateKey.getEncoded());
		Log.v("VehicleSecurity", "User hash: " + userHash.toString());
		Log.v("VehicleSecurity", "Compare hash: " + hash.toString());
		if(java.util.Arrays.equals(userHash, hash)){
			return privateKey.getEncoded();
		} else {
			return null;
		}
		} catch (Exception e){
			return null;
		}
		
	}
	
	public boolean newPin(String pin){
		try {
			Log.v("VehicleSecurity", "new pin: " + pin);
		byte[] newSalt = newSalt();
		privateKey = generateKey(pin.toCharArray(), newSalt);
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(privateKey.getEncoded());
		
		mDatabase.addNewUser(currentUser, hash, newSalt);
			return true;
		} catch (Exception e) {
			return false;
		}		
	}
	
	
	private SecretKey generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException { 
		// Number of PBKDF2 hardening rounds to use. Larger values increase 
		// computation time. You should select a value that causes computation 
		// to take >100ms. 
		final int iterations = 1000;
		// Generate a 256-bit key
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); 
		KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength); 
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec); 
		return secretKey; 
	}
	
	private byte[] newSalt(){
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[outputKeyLength];
		random.nextBytes(salt);
		return salt;
	}
	
	public static byte[][] encrypt(long[] clearText, byte[] key) throws Exception{
		byte[][] cypherText = new byte[clearText.length][];
		for(int i = 0; i < clearText.length; i++){
			cypherText[i] = encrypt(clearText[i], key);
		}
		return cypherText;
	}
	
	public static long[] decrypt(byte[][] cypherText, byte[] key) throws Exception{
		long[] clearText = new long[cypherText.length];
		for(int i = 0; i < cypherText.length; i++){
			clearText[i] = decrypt(cypherText[i], key);
		}
		return clearText;
		
	}


    public static byte[] encrypt(long plainText, byte[] aesKey) throws Exception
    {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedBytes = cipher.doFinal(VehicleUtils.longToBytes(plainText));

        return encryptedBytes;
    }

    
    public static long decrypt(byte[] encrypted, byte[] aesKey) throws Exception
    {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, aesKey);
        
        byte[] plainBytes = cipher.doFinal(encrypted);

        return VehicleUtils.bytesToLong(plainBytes);
    }
    
    //may need to fix this so its not creating and destroying a whole bunch of ciphers
    private static Cipher getCipher(int cipherMode, byte[] aesKey)
            throws Exception
    {
        String encryptionAlgorithm = "AES";
        SecretKeySpec keySpecification = new SecretKeySpec(aesKey, encryptionAlgorithm);
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(cipherMode, keySpecification);

        return cipher;
    }
    
	
}
