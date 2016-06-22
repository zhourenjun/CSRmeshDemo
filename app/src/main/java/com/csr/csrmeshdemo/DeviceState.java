
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo;

public abstract class DeviceState {
	public enum StateType {
		NONE,
		LIGHT,
		POWER,
		INTERNAL_TEMPERATURE
	}
		
	
	// True if state for this model is in sync with the physical device.
	private boolean mStateKnown = false;

	public DeviceState() {
		this.mStateKnown = false;
	}
	
	public DeviceState(DeviceState other) {
		this.mStateKnown = other.mStateKnown;
	}
	
	/**
	 * Make a copy of this object initialised with this object's field values. Creates a new object.
	 * @return The new Model object.
	 */
	public abstract DeviceState deepCopy();
	
    /**
     * Check if the state stored in this device object is valid. For lights, valid means an RGB and power state value
     * have been stored after sending these values to a physical light device.
     * 
     * @return True if the state is known.
     */
    public boolean isStateKnown() {
        return mStateKnown;
    }

    /**
     * Set the flag that indicates if the device state is valid.
     * 
     * @param known
     *            True if the state is known.
     */
    public void setStateKnown(boolean known) {
        this.mStateKnown = known;
    }
    
    public abstract StateType getType();
}
