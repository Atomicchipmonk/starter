package com.glados.villagevehicle.backend;


import java.util.HashMap;

public class VehicleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String VEHICLE_CONTROL_SERVICE = "2fc982ac-095e-42e3-b63d-2d537d22fd72";
    public static String VEHICLE_PASSWORD_CHARACTERISTIC = "000082ad-0000-1000-8000-00805f9b34fb";
    public static String VEHICLE_OPCODE_CHARACTERISTIC = "000082ae-0000-1000-8000-00805f9b34fb";
    public static String VEHICLE_OPERAND_CHARACTERISTIC = "000082af-0000-1000-8000-00805f9b34fb";
    public static String VEHICLE_RESPONSE_CHARACTERISTIC = "000082b0-0000-1000-8000-00805f9b34fb";
    public static String VEHICLE_RESPONSE_READ_DESCRIPTION = "00002902-0000-1000-8000-00805f9b34fb";
    //public static String VEHICLE_RESPONSE_NOTIFY_DESCRIPTION = "000082b0-0000-1000-8000-00805f9b34fb";
    
    
    //opcode
    public static byte[] VEHICLE_OPCODE_LOCK = {(byte)0x01};
    public static byte[] VEHICLE_OPCODE_IGNITION = {(byte)0x02};
    public static byte[] VEHICLE_OPCODE_START = {(byte)0x03};
    public static byte[] VEHICLE_OPCODE_PANIC  = {(byte)0x04};
  
    //operands
    public static byte[] VEHICLE_OPERAND_ON = {(byte)0x01};
    public static byte[] VEHICLE_OPERAND_OFF = {(byte)0x00};
    
    //responses
    public static byte[] VEHICLE_RESPONSE_SEED_RECEIVED = {(byte)0x01};
    public static byte[] VEHICLE_RESPONSE_SEED_SET = {(byte)0x02};
    public static byte[] VEHICLE_RESPONSE_PASSWORD_CORRECT = {(byte)0x03};
    public static byte[] VEHICLE_RESPONSE_OPCODE_ACCEPTED  = {(byte)0x04};
    public static byte[] VEHICLE_RESPONSE_OPERAND_ACCEPTED  = {(byte)0x05};
    public static byte[] VEHICLE_RESPONSE_PASSWORD_PREVIOUS = {(byte)0x06};
    public static byte[] VEHICLE_RESPONSE_PASSWORD_NEXT = {(byte)0x07};
    
    public static byte[] VEHICLE_RESPONSE_UNKNOWN_ERROR = {(byte)0xFF};
    public static byte[] VEHICLE_RESPONSE_PASSWORD_INCORRECT = {(byte)0xFD};
    public static byte[] VEHICLE_RESPONSE_OUT_OF_PASSWORD_ATTEMPTS = {(byte)0xFE};
    public static byte[] VEHICLE_RESPONSE_INVALID_OPCODE = {(byte)0xFC};
    public static byte[] VEHICLE_RESPONSE_OPCODE_INVALID_STATE = {(byte)0xFB};
    public static byte[] VEHICLE_RESPONSE_OPERAND_INVALID_STATE  = {(byte)0xFA};
    
    
    static {
        attributes.put("000082ad-0000-1000-8000-00805f9b34fb", "Password");
        attributes.put("000082ae-0000-1000-8000-00805f9b34fb", "Operation");
        attributes.put("000082af-0000-1000-8000-00805f9b34fb", "Opcode");
        attributes.put("000082b0-0000-1000-8000-00805f9b34fb", "Return Value");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
    
    

}
