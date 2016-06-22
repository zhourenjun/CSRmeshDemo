
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.csr.mesh.AttentionModelApi;
import com.csr.mesh.BearerModelApi;
import com.csr.mesh.FirmwareModelApi;
import com.csr.mesh.GroupModelApi;
import com.csr.mesh.LightModelApi;
import com.csr.mesh.PingModelApi;
import com.csr.mesh.PowerModelApi;
import com.csr.mesh.SwitchModelApi;

public class SingleDevice extends Device {

	private final int NUM_BITS_MODEL_SUPPORTED = 64;
	private final int MODEL_SUPPORT_HIGHEST_BIT_POSITION = NUM_BITS_MODEL_SUPPORTED - 1;

	private int mUuidHash;
	
	// List of group ids a device belongs to. This is shared between all models that have groups set,
	// as the user is not allowed to set models per group.
	private ArrayList<Integer> mGroupMembership = new ArrayList<Integer>();

	private long mModelSupportBitmapLow;
	private long mModelSupportBitmapHigh;
	
	// If a model has an entry in here (indexed by model id), then the number of model group ids has been set.
	private HashMap<Integer,Integer> mNumModelIds = new HashMap<Integer,Integer>();
	
	// The minimum number of group indices supported across all models (does not include models that support zero groups).
	private int mMinGroupsSupported;
	
	public SingleDevice(int deviceId, String name, int uuidHash,
			long modelSupportBitmapLow, long modelSupportBitmapHigh) {
		super(deviceId, name);
		this.mUuidHash = uuidHash;
		this.mModelSupportBitmapLow = modelSupportBitmapLow;
		this.mModelSupportBitmapHigh = modelSupportBitmapHigh;
		this.mMinGroupsSupported = 0;
	}

	/**
	 * Copy constructor
	 * 
	 * @param other
	 *            Device object to make a copy of.
	 */
	public SingleDevice(SingleDevice other) {
		super(other);
		this.mUuidHash = other.mUuidHash;
		this.mModelSupportBitmapLow = other.mModelSupportBitmapLow;
		this.mModelSupportBitmapHigh = other.mModelSupportBitmapHigh;
		this.mMinGroupsSupported = other.mMinGroupsSupported;
		this.mGroupMembership.clear();
		for (int groupId : other.mGroupMembership) {
			mGroupMembership.add(groupId);
		}
		mNumModelIds.putAll(other.mNumModelIds);
	}

	/**
	 * Get UUID hash of device.
	 * 
	 * @return
	 */
	public int getUuidHash() {
		return mUuidHash;
	}

	/**
	 * Get the value at all group indices for this device. Includes values set
	 * to zero. Not valid for a group device.
	 * 
	 * @return List of values in order of index.
	 */
	public List<Integer> getGroupMembershipValues() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (Integer id : mGroupMembership) {
			result.add(id);
		}
		return result;
	}
	
	/**
	 * Set model supported for the device.
	 * @param modelSupportBitmapLow
	 * @param modelSupportBitmapHigh
	 */
	public void setModelSupport(long modelSupportBitmapLow, long modelSupportBitmapHigh){
		this.mModelSupportBitmapLow = modelSupportBitmapLow;
		this.mModelSupportBitmapHigh = modelSupportBitmapHigh;
	}

	/**
	 * Get the list of groups this device belongs to. Returns the value at all
	 * group indices except those set to zero. Not valid for a group device.
	 * 
	 * @return List of values in order of index.
	 */
	public List<Integer> getGroupMembership() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (Integer id : mGroupMembership) {
			if (id != 0) {
				result.add(id);
			}
		}
		return result;
	}

	/**
	 * Set the group ids that this device belongs to.
	 * 
	 * @param groupIds
	 *            List of integers representing group ids this device belongs
	 *            to.
	 */
	public void setGroupIds(List<Integer> groupIds) {
		mGroupMembership.clear();
		for (int id : groupIds) {
			if (id != 0) {
				mGroupMembership.add(id);
			}
		}
	}

	/**
	 * Set a group id that this device belongs to.
	 * 
	 * 
	 * @param index
	 *            Group index to set.
	 * @param groupId
	 *            Integers representing a group id this device belongs to.
	 * 
	 * @return if operation was done successfully.
	 * @throws IndexOutOfBoundsException if index is not between 0 and the number of group indexes device supports.
	 * 
	 */
	
	public void setGroupId(int index, int groupId) {
		if (index >= 0 && index < mMinGroupsSupported) {
			try {
				mGroupMembership.set(index, groupId);
			} catch (IndexOutOfBoundsException exception) {
				IndexOutOfBoundsException exceptionToThrow = new IndexOutOfBoundsException("index must be between 0 and Group membership size.");
				exceptionToThrow.setStackTrace(exception.getStackTrace());
				throw exceptionToThrow;
			}
		}
	}

	/**
	 * Clear all group memberships.
	 */
	public void clearGroups() {
		mGroupMembership.clear();
	}
	
	/**
	 * Set the number of group indices supported by the device. Adds zero
	 * entries to the group membership array to pad it to the requested size.
	 * 
	 * @param num
	 *            Number of group indices.
	 */
	public void setNumGroupIndices(int num) {
		int numToAdd = num - mGroupMembership.size();
		for (int i = 0; i < numToAdd; i++) {
			mGroupMembership.add(0);
		}
	}

	/**
	 * Get the number of supported group indices.
	 * 
	 * @return Integer number of group indices.
	 */
	public int getNumGroupIndices() {
		return mGroupMembership.size();
	}

	/**
	 * Set the number of group indices supported by the device. Adds zero
	 * entries to the group membership array to pad it to the requested size.
	 * 
	 * @param num
	 *            Number of group indices.
	 * @param modelNo
	 * 			  Model number to set.
	 */
	public void setNumSupportedGroups(int num, int modelNo) {		
		if (!isModelSupported(modelNo)) {
			throw new IllegalArgumentException("Specified model is not supported by this device");
		}
		// If this is the first model to be queried for number of supported groups then reset the minimum number of supported groups.
		if (mNumModelIds.size() == 0) {
			mMinGroupsSupported = 0;
		}
		mNumModelIds.put(modelNo, num);
		// Update minimum number of supported groups across all models.
		// If a model doesn't support any groups then it is ignored.
		if (num > 0) {
			// Create enough entries in the group membership array the first time
			// a value greater than zero is set. This guarantees it will have enough entries.
			// Although we may not end up needing to use them all, we can't tell in advance.
			if (mGroupMembership.size() == 0) {
				for (int i = 0; i < num; i++) {
					mGroupMembership.add(0);
				}
			}
			if (mMinGroupsSupported == 0) {
				mMinGroupsSupported = num; 
			}
			else if (num < mMinGroupsSupported) {
				mMinGroupsSupported = num;
			}
		}		
	}

	/**
	 * Find out if the number of supported indices has been queried yet for this device.
	 * @param modelNo Model number to check.
	 * @return True if number of group indices has already been successfully queried.
	 */
	public boolean isNumSupportedGroupsKnown(int modelNo) {		
		return (mNumModelIds.get(modelNo) != null);
	}
	
	/**
	 * Get the number of supported group indices for a model.
	 * 
	 * @return Integer number of group indices or -1 if unknown.
	 */
	public int getNumSupportedGroups(int modelNo) {
		if (!isModelSupported(modelNo)) {
			throw new IllegalArgumentException("Specified model is not supported by this device");
		}		
		if (!isNumSupportedGroupsKnown(modelNo)) {
			throw new IllegalArgumentException("Number of supported groups has not been set for this model.");
		}
		return mNumModelIds.get(modelNo);
	}

	/**
	 * Get the minimum number of group indices supported across all models.
	 * Does not take int account models that support zero groups.
	 * @return Minimum number of supported groups.
	 */
	public int getMinimumSupportedGroups() {
		return mMinGroupsSupported;		
	}
	
	/**
	 * Set the minimum number of group indices supported across all models from a stored value.
	 * This value will be updated after a call to {@link #setNumSupportedGroups(int, int)} for a specific model.
	 * Does not take into account models that support zero groups.
	 * @param minimum Minimum number of supported groups.
	 */
	public void setMinimumSupportedGroups(int minimum) {
		mMinGroupsSupported = minimum;
		if (mGroupMembership.size() == 0) {
			for (int i = 0; i < minimum; i++) {
				mGroupMembership.add(0);
			}
		}
	}
	
	/**
	 * Check if the device supports all of the specified models.
	 * 
	 * @param modelNumber
	 *            Variable length argument list of model numbers.
	 * @return True if the device supports all specified models.
	 */
	public boolean isModelSupported(int... modelNumber) {

		// if no modelNumber, then return true as the device is "supporting" all
		// the modelNumbers.
		if (modelNumber.length == 0) {
			return true;
		}

		long maskLow = 0;
		long maskHigh = 0;
		for (int n : modelNumber) {
			if (n < NUM_BITS_MODEL_SUPPORTED) {
				maskLow |= (0x01L << n);
			} else {
				maskHigh |= (0x01L << (n - MODEL_SUPPORT_HIGHEST_BIT_POSITION));
			}
		}
		long resultLow = maskLow & mModelSupportBitmapLow;
		long resultHigh = maskHigh & mModelSupportBitmapHigh;

		return (resultLow == maskLow && resultHigh == maskHigh);
	}

	/**
	 * Check if the device supports at least one of the specified models.
	 * 
	 * @param modelNumber
	 *            Variable length argument list of model numbers.
	 * @return True if the device supports at least one of the specified models.
	 */
	public boolean isAnyModelSupported(int... modelNumber) {

		// if we don't have any modelNumber to check, the device "support" all
		// the specified models.
		if (modelNumber.length == 0) {
			return true;
		}

		for (int n : modelNumber) {
			long mask = 0;
			long result = 0;
			if (n < NUM_BITS_MODEL_SUPPORTED) {
				mask |= (0x01L << n);
				result = mask & mModelSupportBitmapLow;
			} else {
				mask |= (0x01L << (n - MODEL_SUPPORT_HIGHEST_BIT_POSITION));
				result = mask & mModelSupportBitmapHigh;
			}
			if (result == mask) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Integer> getModelsSupported() {
		ArrayList<Integer> modelsSupported = new ArrayList<Integer>();
		
		for (int i=0 ; i<128; i++) {
			if (isModelSupported(i))
				modelsSupported.add(i);
		}
		
		return modelsSupported;
	}
	
	public void setModelToSupport(int modelNumber) {
		long mask = 0;
		 if (modelNumber < NUM_BITS_MODEL_SUPPORTED) {
			 mask = (0x01L << modelNumber);
			 mModelSupportBitmapLow = (mask | mModelSupportBitmapLow);
		 } else {
			 mask = (0x01L << (modelNumber - MODEL_SUPPORT_HIGHEST_BIT_POSITION));
			 mModelSupportBitmapHigh = (mask | mModelSupportBitmapHigh);
		 }
	}

	/**
	 * Get a list of the names of all the models that the device supports.
	 * 
	 * @return list of models supported
	 */
	public ArrayList<String> getModelsLabelSupported() {
		int[] modelNumbersSupported = { AttentionModelApi.MODEL_NUMBER,
				BearerModelApi.MODEL_NUMBER, FirmwareModelApi.MODEL_NUMBER,
				GroupModelApi.MODEL_NUMBER, LightModelApi.MODEL_NUMBER,
				PingModelApi.MODEL_NUMBER, PowerModelApi.MODEL_NUMBER,
				SwitchModelApi.MODEL_NUMBER };

		String[] modelLabelsSupported = { "Attention Model", "Bearer Model",
				"Firmware Model", "Group Model", "Light Model", "Ping Model",
				"Power Model", "Switch Model" };
		ArrayList<String> modelsSupported = new ArrayList<String>();

		// return null if size modelNumbersSupported is not the same as
		// modelLabelsSupported
		if (modelNumbersSupported.length != modelLabelsSupported.length)
			return null;

		for (int index = 0; index < modelNumbersSupported.length; index++) {
			int n = modelNumbersSupported[index];
			if (isAnyModelSupported(n)) {
				modelsSupported.add(modelLabelsSupported[index]);
			}
		}

		return modelsSupported;

	}

	public long getModelSupportBitmapLow() {
		return mModelSupportBitmapLow;
	}

	public long getModelSupportBitmapHigh() {
		return mModelSupportBitmapHigh;
	}
}
