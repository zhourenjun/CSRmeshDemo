
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.csr.csrmeshdemo.database.DataBaseDataSource;
import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.entities.GroupDevice;
import com.csr.csrmeshdemo.entities.Setting;
import com.csr.csrmeshdemo.entities.SingleDevice;
import com.csr.mesh.ActuatorModelApi;
import com.csr.mesh.LightModelApi;
import com.csr.mesh.PowerModelApi;
import com.csr.mesh.SensorModelApi;
import com.csr.mesh.SwitchModelApi;

public class DeviceStore {
	private LinkedHashMap<Integer, Device> mDevices = new LinkedHashMap<Integer, Device>();
	private LinkedHashMap<Integer, Device> mGroups = new LinkedHashMap<Integer, Device>();
	private DataBaseDataSource mDataBase;
	private Setting mSetting;
	
	
	// json keys.
	static final String NEXT_DEVICE_INDEX_KEY = "nextDeviceIndex";
	static final String NEXT_GROUP_INDEX_KEY = "nextGroupIndex";
	static final String DEVICES_KEY = "devices";
	static final String GROUPS_KEY = "groups";
	static final String DEVICE_ID_KEY = "deviceID";
	static final String DEVICE_NAME_KEY = "name";
	static final String DEVICE_HASH_KEY = "hash";
	static final String DEVICE_MODELS_KEY = "models";
	static final String MODEL_TYPE_KEY = "type";
	static final String MODEL_NUMBER_INSTANCES_KEY = "ModelNumberInstances";
	static final String MODEL_GROUP_INSTANCES_KEY = "groupInstances";
	static final String MODEL_GROUP_X_KEY = "groups[x]";
	static final String MODEL_GROUP_INDEX_KEY = "GroupIndex";
	static final String GROUP_ID_KEY = "groupID";
	static final String GROUP_NAME_KEY = "name";
	
	public DeviceStore(Context context) {
		mDataBase = new DataBaseDataSource(context);
	}

	/**
     * Get a device by id (single device or group).
     * 
     * @param deviceId
     *            The device id to get.
     * @return Device object that contains the device's state or null if the device doesn't exist.
     */
    public Device getDevice(int deviceId) {
    	if (mDevices.containsKey(deviceId)) {
            return new SingleDevice((SingleDevice)mDevices.get(deviceId));
        }
    	else if (mGroups.containsKey(deviceId)) {
            return new GroupDevice((GroupDevice)mGroups.get(deviceId));
        }
        return null;
    }
    
    
    /**
     * Get all the devices, including groups.
     * @return
     * 		   The list of all devices (SingleDevices and GroupDevices)
     */
    public List<Device> getAllDevices() {
    	ArrayList<Device> devices = new ArrayList<Device>();
    	for (Device dev : mDevices.values()) {
    		devices.add(dev);
    	}
    	for (Device dev : mGroups.values()) {
    		devices.add(dev);
    	}
    	return devices;
    }
    
    
    /**
     * Update a device/group name. This new value will be stored also in the database.
     * 
     * @param deviceId
     * 			Device/Group id.
     * @param name
     * 			New name to be applied.
     */
    public void updateDeviceName(int deviceId, String name) {
    	Device dev = null;
    	if (mDevices.containsKey(deviceId)) {
            dev = mDevices.get(deviceId);
          //update in the database
        	mDataBase.updateDeviceName(deviceId,name);
        }
    	else if (mGroups.containsKey(deviceId)) {
    		dev = mGroups.get(deviceId);
    		//update in the database
        	mDataBase.updateGroupName(deviceId,name);
        }
    	if (dev != null) {
    		dev.setName(name);
    	}
    	
    	
    }
    
    /**
     * Get a SingleDevice by device id.
     * @param deviceId
     * 		  Device id.
     * @return
     * 		  SingleDevice object if it exists. If not the method will return null.
     */
    public SingleDevice getSingleDevice(int deviceId) {
    	if (mDevices.containsKey(deviceId)) {
            return new SingleDevice((SingleDevice)mDevices.get(deviceId));
        }
    	return null;
    }
    
    /**
     * Get a GroupDevice by device id.
     * @param deviceId
     * 		  Group id.
     * @return
     * 		  GroupDevice object if it exists. If not the method will return null.
     */
    public GroupDevice getGroupDevice(int deviceId) {
    	if (mGroups.containsKey(deviceId)) {
            return new GroupDevice((GroupDevice)mDevices.get(deviceId));
        }
    	return null;
    }
    
    
    /**
     * Get a list of all SingleDevices
     * @return
     */
    public List<Device> getAllSingleDevices() {
    	ArrayList<Device> devices = new ArrayList<Device>();
    	for (Device dev : mDevices.values()) {
    		devices.add((Device)dev);
    	}
    	return devices;
    }
    
    /**
     * Get a list of all GroupDevices
     * @return
     */
    public List<Device> getAllGroups() {
    	ArrayList<Device> groups = new ArrayList<Device>();
    	for (Device dev : mGroups.values()) {
    		groups.add(dev);
    	}
    	return groups;
    }
    
    
    /**
     * Create a new GroupDevice or update if it already exists. This will apply also in the database if storeDataBase is true.
     * @param device
     * 			GroupDevice to be added.
     * @param storeDataBase
     * 			Apply changes into the database.
     */
    public void addGroupDevice(GroupDevice device, boolean storeDataBase) {
    	// Add all states
    	device.setState(new LightState());
    	device.setState(new PowState());
    	mGroups.put(device.getDeviceId(), new GroupDevice(device));    
    	
    	// add into the database
    	if (storeDataBase) {
    		mDataBase.createOrUpdateGroup(device,mSetting.getId());
    	}
    }
    
	/**
	 * Remove a device/group by id
	 * @param deviceId
	 * 			device/group id.
	 */
	public void removeDevice(int deviceId) {
		if (mDevices.containsKey(deviceId)) {
			mDevices.remove(deviceId);
			
			// remove device from database
			mDataBase.removeSingleDevice(deviceId);
		}
		else if (mGroups.containsKey(deviceId)) {
			mGroups.remove(deviceId);
			
			// remove group from database
			mDataBase.removeGroup(deviceId);
		}
	}
	
	
	/**
	 * Add a new device (SingleDevice or GroupDevice) and insert into the database. 
	 * @param device
	 */
	public void addDevice(Device device) {
		if (device instanceof GroupDevice) {
			addGroupDevice((GroupDevice)device,true);
		}
		else {
			addSingleDevice((SingleDevice)device,true);
		}
	}
	
	/**
	 * Load all the SingleDevices and GroupDevices from the database.
	 */
	public void loadAllDevices() {
		// clear devices and groups lists.
		clearDevices();
		
		// get SingleDevices and GroupDevices from database.
		ArrayList<SingleDevice> devices = mDataBase.getAllSingleDevices();
    	ArrayList<GroupDevice> groups = mDataBase.getAllGroupDevices();
    	
    	// group and device index to be used as last index (we take the higher one in each case).
    	int groupIndex = Device.GROUP_ADDR_BASE;
    	int deviceIndex = Device.DEVICE_ADDR_BASE;
		
		// add devices
		for (int i=0; i < devices.size(); i++ ) {
			addSingleDevice(devices.get(i),false);
			deviceIndex = Math.max(deviceIndex, devices.get(i).getDeviceId());
		}
		// add groups
		for (int i=0; i < groups.size(); i++ ) {
			addGroupDevice(groups.get(i),false);
			groupIndex = Math.max(groupIndex, groups.get(i).getDeviceId());
		}
		mSetting.setLastDeviceIndex(deviceIndex);
		mSetting.setLastGroupIndex(groupIndex);
	}

	
	/**
	 * Get Setting object
	 * @return
	 */
	public Setting getSetting() {
		return mSetting;
	}

	/**
	 * Set and save in the database (if storeDatabase = true) the current setting to be used.
	 * @param setting
	 * @param storeDatabase
	 */
	public void setSetting(Setting setting, boolean storeDatabase) {
		if (storeDatabase) {
			setting = mDataBase.createSetting(setting);
		}
		mSetting = setting;
	}

	/**
	 * Load setting object by id. This method will clear the list of Single and Group devices.
	 * @param settingsID
	 */
	public void loadSetting(int settingsID) {
		mSetting = mDataBase.getSetting(settingsID);		
		
		// clear devices and groups lists.
		clearDevices();
	}

	/**
	 * Insert into the dataBase a new set of Settings value.
	 * @param setting
	 */
	public void addSetting(Setting setting) {
		mSetting = mDataBase.createSetting(setting);
		
		// clear devices and groups lists.
		clearDevices();		
	}
	
	
	/**
	 * Clear the single and group devices lists.
	 */
	private void clearDevices() {
		mDevices.clear();
		mGroups.clear();
	}
	
	
	/**
	 * Add a new SingleDevice assigning a new name (according with the models supported) if it haven't been assigned yet.
	 * If storeInDataBase is true, it will be also stored in the database. 
	 * @param device
	 * 		  SingleDevice to be stored
	 * @param storeInDatabase
	 * 		  Determine if the device should be stored in the database or not.
	 */
	private void addSingleDevice(SingleDevice device, boolean storeInDatabase) {
    	String name = device.getName();
    	// The human readable device number starting at 1
    	final int deviceNumber = device.getDeviceId() - Device.DEVICE_ADDR_BASE;

    	if (device.isModelSupported(LightModelApi.MODEL_NUMBER)) {
    		if (name == null) {    			
    			name = "Light "+ deviceNumber;   
    		}
    		device.setState(new LightState());
    	}
    	else if (device.isModelSupported(SwitchModelApi.MODEL_NUMBER)) {
    		if (name == null) {
    			name = "Switch " + deviceNumber;
    		}
    	}
		else if (device.isModelSupported(SensorModelApi.MODEL_NUMBER)) {
    		if (name == null) {
    			name = "Sensor " + deviceNumber;
    		}
    	}
		else if (device.isModelSupported(ActuatorModelApi.MODEL_NUMBER)) {
    		if (name == null) {
    			name = "Actuator " + deviceNumber;
    		}
    	}
		
		// Set state if necessary.
    	if (device.isModelSupported(PowerModelApi.MODEL_NUMBER)) {
    		device.setState(new PowState());
    	}
    	
    	// If after all checks, the device still doesn't have assigned any name, then name it "Device"
    	if (name == null) {
    		name = "Device " + deviceNumber;
    	}    	
    	device.setName(name);    	
    	
    	mDevices.put(device.getDeviceId(), new SingleDevice(device));
    	
    	// Add into the database
    	if (storeInDatabase) {
    		mDataBase.createOrUpdateSingleDevice(device, mSetting.getId());
    	}	
    }
	
	
	
	/**
	 * This method take data (settings, list of devices and list of groups) from a json string and 
	 * use it to replace (in the database) the current configuration. 
	 * 
	 * @param jsonStr Json to get the configuration from.
	 * @param networkKey Network key to use when the configuration will be saved..
	 * @return success.
	 */
	public boolean setConfigurationFromJson(String jsonStr, String networkKey) {
		
		
		Setting settings = new Setting();
		settings.setNetworkKey(networkKey);
		settings.setId(mSetting.getId());
		
		ArrayList<SingleDevice> singleDevices = new ArrayList<SingleDevice>();
		ArrayList<GroupDevice> groupDevices = new ArrayList<GroupDevice>();
		
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			
			int nextDeviceIndex = jsonObj.getInt(NEXT_DEVICE_INDEX_KEY);
			int nextGroupIndex = jsonObj.getInt(NEXT_GROUP_INDEX_KEY);
			
			JSONArray devices = jsonObj.getJSONArray(DEVICES_KEY);
			JSONArray groups = jsonObj.getJSONArray(GROUPS_KEY);
			
			settings.setLastDeviceIndex(nextDeviceIndex-1);
			settings.setLastGroupIndex(nextGroupIndex-1);

			// looping through groups
			for(int i=0; i < groups.length(); i++) {
				int groupId = groups.getJSONObject(i).getInt(GROUP_ID_KEY);
				String groupName = groups.getJSONObject(i).getString(GROUP_NAME_KEY);

				GroupDevice group = new GroupDevice(groupId, groupName);
				groupDevices.add(group);
			}
			// looping through devices
			for(int devicesIndex=0; devicesIndex < devices.length(); devicesIndex++) {
				int deviceId = devices.getJSONObject(devicesIndex).getInt(DEVICE_ID_KEY);
				String deviceName = devices.getJSONObject(devicesIndex).getString(DEVICE_NAME_KEY);
				int uuidHash = devices.getJSONObject(devicesIndex).getInt(DEVICE_HASH_KEY);
//				long modelSupportLow = devices.getJSONObject(devicesIndex).getLong(DEVICE_MODELSUPPORT_LOW);
//				long modelSupportHigh = devices.getJSONObject(devicesIndex).getLong(DEVICE_MODELSUPPORT_HIGH);
				
				SingleDevice device = new SingleDevice(deviceId, deviceName, uuidHash, 0, 0);
				
				JSONArray models = devices.getJSONObject(devicesIndex).getJSONArray(DEVICE_MODELS_KEY);
				for (int modelsIndex=0; modelsIndex < models.length(); modelsIndex++) {
					int type = models.getJSONObject(modelsIndex).getInt(MODEL_TYPE_KEY);
					device.setModelToSupport(type);

					JSONArray groupInstances = models.getJSONObject(modelsIndex).getJSONArray(MODEL_GROUP_INSTANCES_KEY);


					device.setMinimumSupportedGroups(groupInstances.length());
					device.setNumGroupIndices(groupInstances.length());
					for (int i=0; i< groupInstances.length(); i++) {
						int groupX = groupInstances.getJSONObject(i).getInt(MODEL_GROUP_X_KEY);

						if (groupX < groupDevices.size())
							device.setGroupId(i, groupDevices.get(groupX).getDeviceId());
					}
				}
				singleDevices.add(device);
			}
			

		} catch (JSONException e) {
			return false;
		}
		// reset database
		mDataBase.cleanDatabase();
		// store new settings
		mDataBase.createSetting(settings);
		// store devices
		for(int i=0; i<singleDevices.size(); i++)
			mDataBase.createOrUpdateSingleDevice(singleDevices.get(i), settings.getId());
		// store groups
		for(int i=0; i<groupDevices.size(); i++)
			mDataBase.createOrUpdateGroup(groupDevices.get(i), settings.getId());
		
		// Everything was successful, so return true.
		return true;
	}

	
	public String getDataBaseAsJson() {
		
		JSONObject objJson = new JSONObject();
		try {
			// settings
			objJson.put(NEXT_DEVICE_INDEX_KEY, mSetting.getLastDeviceIndex()+1);
			objJson.put(NEXT_GROUP_INDEX_KEY, mSetting.getLastGroupIndex()+1);
			
			// devices
			JSONArray jsonDevices = new JSONArray();
			List<Device> devices = getAllSingleDevices();
			
			for (int i=0; i<devices.size(); i++) {
				
				SingleDevice singleDevice = (SingleDevice)devices.get(i);
				JSONObject deviceJson = new JSONObject();
				
				deviceJson.put(DEVICE_ID_KEY, singleDevice.getDeviceId());
				deviceJson.put(DEVICE_NAME_KEY, singleDevice.getName());
				deviceJson.put(DEVICE_HASH_KEY, singleDevice.getUuidHash());
				
				//get models supported
				List<Integer> modelsSupported = singleDevice.getModelsSupported();
				// get groups grouped
				List<Integer> groups = singleDevice.getGroupMembershipValues();

				// creating json to insert the device models supported
				JSONArray jsonModelsList = new JSONArray();

				for (int modelIndex=0; modelIndex < modelsSupported.size(); modelIndex++) {
					JSONObject modelJSON = new JSONObject();
					modelJSON.put(MODEL_TYPE_KEY, modelsSupported.get(modelIndex));
					//modelJSON.put(MODEL_NUMBER_INSTANCES_KEY, singleDevice.getMinimumSupportedGroups());
					
					// creating json to insert the groups assigned to the device
					JSONArray jsonGroupsAssigned = new JSONArray();
					
					// we don't store group instance per model, we store the maximum group supported by device
					for (int j=0; j< groups.size(); j++) {
						JSONObject groupJSON = new JSONObject();
						groupJSON.put(MODEL_GROUP_X_KEY, groups.get(j));
						jsonGroupsAssigned.put(groupJSON);
					}
					modelJSON.put(MODEL_GROUP_INSTANCES_KEY,jsonGroupsAssigned);
					
					jsonModelsList.put(modelJSON);
				}
				// add models to the json device
				deviceJson.put(DEVICE_MODELS_KEY,jsonModelsList);
				
				// add device to the list of json devices.
				jsonDevices.put(deviceJson);
			}
			objJson.put(DEVICES_KEY, jsonDevices);
			
			// groups
			JSONArray jsonGroups = new JSONArray();
			List<Device> groups = getAllGroups();
			
			for (int i=0; i<groups.size(); i++) {
				
				GroupDevice groupDevice = (GroupDevice)groups.get(i);
				JSONObject groupJson = new JSONObject();
				
				groupJson.put(GROUP_ID_KEY, groupDevice.getDeviceId());
				groupJson.put(GROUP_NAME_KEY, groupDevice.getName());
				
				jsonGroups.put(groupJson);
			}
			objJson.put(GROUPS_KEY, jsonGroups);
		}catch (Exception e) {
			return null;
		}
		
		return objJson.toString();
	}

}
