
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo;

import com.csr.mesh.DataModelApi;


public class DeviceInfoProtocol {


	private static byte REQUEST_INFO_OPCODE= 0x01;
	private static byte SET_INFO_OPCODE= 0x03;
	private static byte RESET_INFO_OPCODE= 0x04;
	
	
	/**
	 * Resets device info. Any further device info requests returns default string.
	 * 
	 * @param deviceId
	 */
	static public void resetDeviceInfo(int deviceId){
		
		
		byte [] data = {RESET_INFO_OPCODE,0,0,0,0,0,0,0,0,0};
		// we send the data using blocks so we don't need acknowledged packet.
		DataModelApi.sendData(deviceId, data, false);
	}
	
	
	/**
	 * Used to request device information. It would be delivered by streaming.
	 * 
	 * @param deviceId
	 */
	static public void requestDeviceInfo(int deviceId){
		
		
		byte [] data = {REQUEST_INFO_OPCODE,0,0,0,0,0,0,0,0,0};
		// we send the data using blocks so we don't need acknowledged packet.
		DataModelApi.sendData(deviceId, data, false);
	}
	
	/**
	 * Used to request device information. It would be delivered by streaming.
	 * 
	 * @param deviceId
	 * @param deviceInfo
	 */
	static public void setDeviceInfo(int deviceId,String deviceInfo){
		
		
		/*
		
		byte [] data = {SET_INFO_OPCODE,2,'h','o','\0',0,0,0,0,0};
		// we send the data using blocks so we don't need acknowledged packet.
		DataModelApi.sendData(deviceId, data, false);
		
		*/
	}
}
