
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo;

import com.csr.mesh.PowerModelApi;
import com.csr.mesh.PowerModelApi.PowerState;

public class PowState extends DeviceState {

	PowerState mPowerState;
	
	public PowState() {
		super();
		mPowerState = PowerState.OFF;
	}
	
	public PowState(PowState other) {
		super(other);
		this.mPowerState = other.mPowerState;
	}

	@Override
	public DeviceState deepCopy() {
		PowState state = new PowState(this);
		state.mPowerState = ((PowState)this).mPowerState;			
		return state;
	}

	@Override
	public StateType getType() {
		return StateType.POWER;
	}

	public PowerState getPowerState() {
		return mPowerState;
	}

	public void setPowerState(PowerState powerState) {
		this.mPowerState = powerState;
	}
}
