
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.entities;

public class GroupDevice extends Device {

	public GroupDevice(int deviceId, String name) {
		super(deviceId, name);		
	}

	public GroupDevice(GroupDevice other) {
		super(other);
	}	
}
