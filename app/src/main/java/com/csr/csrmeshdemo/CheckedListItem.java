
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import com.csr.csrmeshdemo.entities.Device;

/**
 * Contains the checked state of an item in a CheckedListFragment ListView.
 * Also contains the device object represented by the item.
 *
 */
public final class CheckedListItem {	
	private boolean checked;
	private Device device;
	
	public CheckedListItem(boolean checked, Device device) {
	    this.device = device;	
		this.checked = checked;		
	}

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }		
}
