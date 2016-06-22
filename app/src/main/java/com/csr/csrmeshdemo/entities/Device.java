
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.entities;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.csr.csrmeshdemo.DeviceState;
import com.csr.csrmeshdemo.DeviceState.StateType;

public abstract class Device {
	// Addresses in the range 0x0000 - 0x7FFF are intended for groups.
    public static final int GROUP_ADDR_BASE = 0x0000;
    // Addresses from 0x8000 to 0xFFFF are intended for addressing individual devices.
    public static final int DEVICE_ADDR_BASE = 0x8000;

    public static final int DEVICE_ID_UNKNOWN = 0x10000;
         
	private int mDeviceId;
	private String mName;	
	
	private HashMap<StateType, DeviceState> mState = new HashMap<StateType, DeviceState>();
	
	// Keys used for json
    static final String JSON_KEY_ID = "id";
    static final String JSON_KEY_UUID_HASH = "uuid_hash";
    static final String JSON_KEY_NAME = "name";
    static final String JSON_KEY_IS_GROUP = "is_group";
    static final String JSON_KEY_GROUP_MEMBERSHIP = "group_membership";
    static final String JSON_KEY_MODEL_SUPPORT_LOW = "model_low";
    static final String JSON_KEY_MODEL_SUPPORT_HIGH = "model_high";
    
    public Device(int deviceId, String name) {
		this.mDeviceId = deviceId;
		this.mName = name;
	}
	
	/**
     * Copy constructor
     * 
     * @param other
     *            Device object to make a copy of.
     */
    public Device(Device other) {
        this.mDeviceId = other.mDeviceId;        
        this.mName = other.mName;        
        this.mState.clear();
        for (Map.Entry<StateType, DeviceState> entry : other.mState.entrySet()) {        	
            this.mState.put(entry.getKey(), entry.getValue().deepCopy());
        }
    }

    /**
     * Copy another device object's fields into this object.
     * 
     * @param other
     *            Device object to copy.
     */
    public void copy(Device other) {
        this.mDeviceId = other.mDeviceId;        
        this.mName = other.mName;        
    }
    
	public int getDeviceId() {
		return mDeviceId;
	}
	
	public void setDeviceId(int deviceId) {
		this.mDeviceId = deviceId;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		this.mName = name;
	}	
	
	/**
     * Set state using a state object.
     * If the state doesn't exist then it will be added, otherwise existing values will be updated.
     * @param state The state object to update the device with.
     */
    public void setState(DeviceState state) {
    	mState.put(state.getType(), state.deepCopy());
    }
    
    /**
     * Get a copy of the device's state.
     * @param clickState The type of state to get.
     * @return DeviceState object containing the state of the requested type.
     */
    public DeviceState getState(StateType type) {
    	return mState.get(type).deepCopy();
    }
}
