
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo.listeners;

public interface DataListener {
	public void dataReceived(int deviceId, byte [] data);
	public void UITimeout();
	public void dataGroupReceived(int deviceId);
}
