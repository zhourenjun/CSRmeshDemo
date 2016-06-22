
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo.entities;

/**
 * This class represents the configuration
 */
public class Setting {
	
	public static int UKNOWN_ID = -1;
	/*package*/ static final int MAX_TTL_VALUE = 0xFF;

	private int id = UKNOWN_ID;
	private String networkKey;
	private int lastGroupIndex = Device.GROUP_ADDR_BASE;
	private int lastDeviceIndex = Device.DEVICE_ADDR_BASE;
	private boolean authRequired = false;
	private int ttl = MAX_TTL_VALUE;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNetworkKey() {
		return networkKey;
	}
	public void setNetworkKey(String networkKey) {
		this.networkKey = networkKey;
	}
	public int getLastGroupIndex() {
		return lastGroupIndex;
	}
	public void setLastGroupIndex(int lastGroupIndex) {
		this.lastGroupIndex = lastGroupIndex;
	}
	public int getLastDeviceIndex() {
		return lastDeviceIndex;
	}
	public void setLastDeviceIndex(int lastDeviceIndex) {
		this.lastDeviceIndex = lastDeviceIndex;
	}
	public boolean isAuthRequired() {
		return authRequired;
	}
	public void setAuthRequired(boolean authRequired) {
		this.authRequired = authRequired;
	}
	
	public int getTTL() {
		return ttl;
	}
	public void setTTL(int ttl) {
		this.ttl = ttl;
	}
	
	public int getNextGroupIndexAndIncrement(){
		int index = lastGroupIndex;
		lastGroupIndex++;
		return index;
	}
	
	
}
