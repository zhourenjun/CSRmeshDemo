
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.listeners;

public interface GroupListener {
    /**
     * Called when a device's group membership has been updated, or if there was a failure updating it.
     * 
     * @param deviceId
     *            Id of device that was updated (or not).
     * @param success
     *            True on success.
     * @param msg
     *            Message to display in toast.
     */
    public void groupsUpdated(int deviceId, boolean success, String msg);
    
}
