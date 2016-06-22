
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo;

public class LightState extends DeviceState {
	private byte mRed;
	private byte mGreen;
	private byte mBlue;
	
	public LightState() {
		super();
		mRed = 0;
		mGreen = 0;
		mBlue = 0;
	}
	
	public LightState(DeviceState other) {
		super(other);
		this.mRed = ((LightState)other).mRed;
		this.mGreen = ((LightState)other).mGreen;
		this.mBlue = ((LightState)other).mBlue;			
	}
	
	@Override
	public DeviceState deepCopy() {
		LightState state = new LightState(this);
		state.mRed = ((LightState)this).mRed;
		state.mGreen = ((LightState)this).mGreen;
		state.mBlue = ((LightState)this).mBlue;		
		return state;
	}
	
	public byte getRed() {
        return mRed;
    }

    public void setRed(byte red) {
        this.mRed = red;
    }

    public byte getGreen() {
        return mGreen;
    }

    public void setGreen(byte green) {
        this.mGreen = green;
    }

    public byte getBlue() {
        return mBlue;
    }

    public void setBlue(byte blue) {
        this.mBlue = blue;
    }

	@Override
	public StateType getType() {		
		return StateType.LIGHT;
	}
}