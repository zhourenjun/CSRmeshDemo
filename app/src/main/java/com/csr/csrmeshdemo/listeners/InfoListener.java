
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.listeners;

public interface InfoListener {
    /**
     * Called when firmware information is received from a remote device.
     * 
     * @param deviceId
     *            Id of device.
     * @param major
     *            Major version of device.
     * @param minor
     *            Minor version of device.
     * @param success
     *            True if version was received sucessfully, false if there was a timeout (other fields are zero in this
     *            case).
     */
    public void onFirmwareVersion(int deviceId, int major, int minor, boolean success);
    
    
    /**
     * Called when VID_PID_Version information is received from a remote device.
     * 
     * @param vid
     *            Vendor ID.
     * @param pid
     *            Product ID.
     * @param version
     *            version.
     * @param batteryPercent
     *            battery percent.
     * @param batteryState
     *            battery state.
     * @param deviceId
     *            Id of device.
     * @param success
     *            True if version was received sucessfully, false if there was a timeout (other fields are zero in this
     *            case).
     */
    public void onDeviceInfoReceived(byte[] vid, byte[] pid, byte[] version, int batteryPercent, int batteryState, int deviceId, boolean success);


	/**Called when new device configuration has been received.
	 * @param success
	 */
	public void onDeviceConfigReceived(boolean success);
}
