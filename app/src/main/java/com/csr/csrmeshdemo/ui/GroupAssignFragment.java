
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.csr.csrmeshdemo.CheckedListItem;
import com.csr.csrmeshdemo.listeners.DataListener;
import com.csr.csrmeshdemo.DeviceController;
import com.csr.csrmeshdemo.listeners.GroupListener;
import com.csr.csrmeshdemo.listeners.InfoListener;
import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.listeners.RemovedListener;
import com.csr.csrmeshdemo.Utils;
import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.entities.SingleDevice;
import com.csr.csrmeshdemo.ui.CheckedListFragment.ItemListener;
import com.csr.mesh.AttentionModelApi;
import com.csr.mesh.ConfigModelApi;

/**
 * Fragment used to configure devices. Handles assigning devices to groups, get firmware version, remove a device or
 * group, rename a device or group and add a new group. Contains two side by side CheckedListFragment fragments.
 * 
 */
public class GroupAssignFragment extends Fragment 
implements ItemListener, GroupListener, InfoListener, RemovedListener, DataListener {
    private static final String TAG = "GroupAssignFragment";
    
    private static final int STATE_NORMAL = 0;
    private static final int STATE_EDIT_GROUP = 1;
    private static final int STATE_EDIT_DEVICE = 2;
    
    // Constants for application level device info protocol that is sent over data stream.        
    private static final int DEVICE_INFO_OFFSET_OPCODE = 0;
    private static final int DEVICE_INFO_OFFSET_LENGTH = 1;
    private static final int DEVICE_INFO_OFFSET_DATA = 2;
    private static final int DEVICE_INFO_HEADER_LENGTH = DEVICE_INFO_OFFSET_DATA;
    private static final int CSR_DEVICE_INFO_RSP = 0x02;
    
    private int mState = STATE_NORMAL;

    private View mRootView;

    private CheckedListFragment mGroupFragment;
    private CheckedListFragment mDeviceFragment;

    protected static final String GROUPS_FRAGMENT_TAG = "groupsfrag";
    protected static final String DEVICE_FRAGMENT_TAG = "lightsfrag";
    
    public static final int UNKNOWN_BATTERY_LEVEL = -1;
    public static final int UNKNOWN_BATTERY_STATE = -1;

    private DeviceController mController;

    // Id of device or group with attention enabled.
    private int mAttentionId;
    
    // Used when editing group membership of a single selected device.
    private GroupList mEditGroupList = null;
    // Used when a group is selected; add each modified list of groups to this list.
    private SparseArray<GroupList> mModifiedGroups = new SparseArray<GroupList>();

    private Button mApplyButton;
    private Button mCancelButton;
    private Button mAddGroupButton;

    private Device mSelectedDevice = null;

    private ProgressDialog mProgress;

    private Fragment mMainFragment;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mController = (DeviceController) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DeviceController callback interface.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainFragment = this;
        // show the actionBar menu.
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.group_assignment, container, false);
        }

        mProgress = new ProgressDialog(getActivity());

        // Add the child fragments. Nested fragments are supported from API level 17
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        mGroupFragment = new CheckedListFragment();
        mDeviceFragment = new CheckedListFragment();

        Bundle groupBundle = new Bundle();
        groupBundle.putInt(CheckedListFragment.EXTRA_MENU_RESOURCE, R.menu.config_popup_group);
        Bundle deviceBundle = new Bundle();
        deviceBundle.putInt(CheckedListFragment.EXTRA_MENU_RESOURCE, R.menu.config_popup_device);

        mGroupFragment.setArguments(groupBundle);
        mDeviceFragment.setArguments(deviceBundle);

        ft.add(R.id.groupsFrame, mGroupFragment, GROUPS_FRAGMENT_TAG);
        ft.add(R.id.lightsFrame, mDeviceFragment, DEVICE_FRAGMENT_TAG);
        ft.commitAllowingStateLoss();

        mApplyButton = (Button) mRootView.findViewById(R.id.buttonApply);
        mCancelButton = (Button) mRootView.findViewById(R.id.buttonCancel);
        mAddGroupButton = (Button) mRootView.findViewById(R.id.buttonNewGroup);
        hideButtons();

        // Apply changes to group membership.
        mApplyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideButtons();
                
                boolean updatesMade = false;
                if (mState == STATE_EDIT_GROUP) {
	                // For groups there can be more than one group update (one per device).
	                updatesMade = updateNextDeviceInGroup();                                	                                    
                }
                else if (mState == STATE_EDIT_DEVICE) {
                	// For devices we only need to send a single group update.
                    updatesMade = sendGroupUpdate();
                }
                if (!updatesMade) {
                	// Nothing was changed.
                	Toast.makeText(getActivity(), getActivity().getString(R.string.group_no_changes), 
                			Toast.LENGTH_SHORT).show();
                    resetUI();
                    mEditGroupList = null;
                }
            }
        });

        // Cancel changes to group membership.
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetUI();
                mEditGroupList = null;
            }
        });

        // Add a new group.
        mAddGroupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                newGroup();
            }
        });

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        resetDeviceList();
    }
    
    private void resetDeviceList() {
    	// clear items
        mGroupFragment.clearItems();
        mDeviceFragment.clearItems();
        
        // add items
        mGroupFragment.addItems(mController.getGroups());
        mDeviceFragment.addItems(mController.getDevices());
        if (mDeviceFragment.getDevices().size() == 0) {
            mGroupFragment.setClickEnabled(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        disableDeviceAttention();
    }

    /**
     * Show the Apply and Cancel buttons, hide Add Group button.
     */
    private void showButtons() {
        mApplyButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        mAddGroupButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Hide the Apply and Cancel buttons, show Add Group button.
     */
    private void hideButtons() {
        mApplyButton.setVisibility(View.INVISIBLE);
        mCancelButton.setVisibility(View.INVISIBLE);
        mAddGroupButton.setVisibility(View.VISIBLE);
    }

    /**
     * Event handler called when a group or a light is selected.
     * 
     * @param fragmentId
     *            Resource id of the fragment that generated the event. Used to differentiate lights and groups.
     * @param device
     *            Device selected.
     */
    @Override
    public void onItemSelected(String fragmentTag, Device device) {
        if (fragmentTag.compareTo(GROUPS_FRAGMENT_TAG) == 0) {
            onGroupSelected(device.getDeviceId(), mGroupFragment.getSelectedItemPosition());
        }
        else if (fragmentTag.compareTo(DEVICE_FRAGMENT_TAG) == 0) {
            onDeviceSelected(device, mDeviceFragment.getSelectedItemPosition());
        }
    }

    /**
     * Event handler called when a group's or a light's checkbox is checked or unchecked.
     * 
     * @param fragmentId
     *            Resource id of the fragment that generated the event. Used to differentiate lights and groups.
     * @param id
     *            Id of the device or the group that was checked or unchecked.
     * @param checked
     *            Checkbox state.
     */
    @Override
    public void onItemCheckStatusChanged(String fragmentTag, int id, boolean checked) {    	
        if (fragmentTag.compareTo(GROUPS_FRAGMENT_TAG) == 0 && mEditGroupList != null) {
            // The check event was on the group fragment so we must be editing a single light.
        	// The id is the group id.
        	// Is this group id already related to device object being edited?
            boolean inGroup = mEditGroupList.groupMembership.contains(id);
            if (checked) {
                if (!inGroup) {
                    mEditGroupList.groupMembership.add(id);
                }
            }
            else if (inGroup) {
                int index = mEditGroupList.groupMembership.indexOf(id);
                mEditGroupList.groupMembership.remove(index);
            }
        }
        else if (fragmentTag.compareTo(DEVICE_FRAGMENT_TAG) == 0) {
            // The check event was on the device fragment so we must be editing a whole group.
        	// The id is the device id.
        	// The group id will be the id of the selected device.
            int groupId = mSelectedDevice.getDeviceId();
            // Get list of groups this device belongs to.
            List<Integer> membership = ((SingleDevice)mDeviceFragment.getItem(id).getDevice()).getGroupMembership();
            
            // Get the list of modified groups for this device if it exists.
            GroupList deviceGroups = mModifiedGroups.get(id);
            if (deviceGroups == null) {
                deviceGroups = new GroupList(id);
                
                deviceGroups.groupMembership.addAll(membership);
                // Add to the list of modified groups. The key is the device id.
                mModifiedGroups.put(id, deviceGroups);
            }
            
            if (checked) {
                if (!deviceGroups.groupMembership.contains(groupId)) {
                    deviceGroups.groupMembership.add(groupId);
                }
            }
            else {
                int index = deviceGroups.groupMembership.indexOf(groupId);
                
                // Avoid crashes
                if (index != -1) {
                	deviceGroups.groupMembership.remove(index);
                }
            }
        }
    }

    /**
     * Event handler called when a device is selected.
     * 
     * @param device
     * 			Device selected.
     * @param position
     * 			Position of the item in the devices list.
     */
    private void onDeviceSelected(Device device, int position) {
        if (mState == STATE_NORMAL) {
            mState = STATE_EDIT_DEVICE;
                        
            SingleDevice sDevice = (SingleDevice) device;
            if (sDevice.isModelSupported(AttentionModelApi.MODEL_NUMBER)) {
            	enableDeviceAttention(device.getDeviceId());
            }
            mGroupFragment.setContextMenuEnabled(false);
            mDeviceFragment.setContextMenuEnabled(false);

            // Make a new group list for the selected device for editing.
            mSelectedDevice = mDeviceFragment.getSelectedDevice();
            mEditGroupList = new GroupList(mSelectedDevice.getDeviceId());
            mEditGroupList.groupMembership.addAll(((SingleDevice)mSelectedDevice).getGroupMembership());

            // Show the save / cancel menu.
            showButtons();
            mDeviceFragment.setCheckBoxesVisible(false);
            mGroupFragment.setCheckBoxesVisible(true);

            // Set check box states of groups that have this light in.
            mGroupFragment.setAllCheckboxes(false);
            for (int groupId : mEditGroupList.groupMembership) {
                mGroupFragment.setDeviceCheckBoxState(groupId, true, false);
            }
        }
        else if (mState == STATE_EDIT_GROUP) {
            if (mDeviceFragment.getSelectedDevice() instanceof SingleDevice) {
                // Editing a group so interpret this click as a request to toggle the checkbox.
                boolean checkedState = mDeviceFragment.getCheckBoxState(position);
                mDeviceFragment.setCheckBoxState(position, !checkedState, true);
            }
        }
    }

    /**
     * Event handler called when a group is selected.
     * 
     * @param groupId
     *            The id of the selected group.
     */
    private void onGroupSelected(int groupId, int position) {
        if (mState == STATE_NORMAL) {
            mState = STATE_EDIT_GROUP;

            mGroupFragment.setContextMenuEnabled(false);
            mDeviceFragment.setContextMenuEnabled(false);

            mSelectedDevice = mGroupFragment.getSelectedDevice();

            // Show the save / cancel menu.
            showButtons();
            mDeviceFragment.setCheckBoxesVisible(true);
            mGroupFragment.setCheckBoxesVisible(false);

            // Set check box states of devices that belong to this group
            mDeviceFragment.setAllCheckboxes(false);
            for (Device d : mDeviceFragment.getDevices()) {
            	SingleDevice dev = (SingleDevice)d; 
                if (dev.getGroupMembershipValues().contains(mSelectedDevice.getDeviceId())
                        && (dev instanceof SingleDevice)) {
                    mDeviceFragment.setDeviceCheckBoxState(dev.getDeviceId(), true, false);
                }
            }
        }
        else if (mState == STATE_EDIT_DEVICE) {
            // Editing a device so interpret this click as a request to toggle the checkbox
            boolean checkedState = mGroupFragment.getCheckBoxState(position);
            mGroupFragment.setCheckBoxState(position, !checkedState, true);
        }
    }

    /**
     * Show a modal progress dialogue until hideProgress is called.
     * 
     * @param message
     *            The message to display in the dialogue.
     */
    private void showProgress(String message) {
        mProgress.setMessage(message);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
        mProgress.show();
    }

    /**
     * Hide the progress dialogue.
     */
    private void hideProgress() {
        mProgress.dismiss();
    }

    private void enableDeviceAttention(int deviceId) {
    	if (deviceId > 0) {
	    	mAttentionId = deviceId;
	    	// Turn on attention for this device.
	        mController.setSelectedDeviceId(deviceId);
	        mController.setAttentionEnabled(true);
    	}
    }

    private void disableDeviceAttention() {
    	// Send message to clear attention state
        if (mAttentionId > 0) {
        	mController.setSelectedDeviceId(mAttentionId);
        	mController.setAttentionEnabled(false);
        	mAttentionId = 0;
        }
    }

    /**
     * Reset user interface to initial state and set state variable to indicate groups are not being edited.
     */
    private void resetUI() {
        hideButtons();
        mGroupFragment.setContextMenuEnabled(true);
        mDeviceFragment.setContextMenuEnabled(true);
        mGroupFragment.setCheckBoxesVisible(false);
        mDeviceFragment.setCheckBoxesVisible(false);
        mGroupFragment.selectNone();
        mDeviceFragment.selectNone();
        mDeviceFragment.clearSelection();
        mGroupFragment.clearSelection();
        disableDeviceAttention();
        mState = STATE_NORMAL;
    }

    
    /**
     * Show dialogue asking user to confirm if a device should be removed, and remove it if OK is pressed.
     * 
     * @param deviceId
     *            The id of the device to be removed.
     */
    private void confirmRemove(final int deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.confirm_remove)).setCancelable(false)
                .setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Perform remove operation.
                        mController.setSelectedDeviceId(deviceId);
                        mController.removeDevice((RemovedListener) mMainFragment);
                        if (deviceId >= Device.DEVICE_ADDR_BASE) {
                            // Removing groups is instant so no need for progress.
                            showProgress(getActivity().getString(R.string.removing));
                        }
                    }
                }).setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void confirmRemoveLocal(final int deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.confirm_remove_local)).setCancelable(false)
                .setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Perform remove operation.
                        mController.setSelectedDeviceId(deviceId);
                        mController.removeDeviceLocally((RemovedListener) mMainFragment);
                        
                        // refreshList
                        mGroupFragment.clearItems();
                        mDeviceFragment.clearItems();
                        mGroupFragment.addItems(mController.getGroups());
                        mDeviceFragment.addItems(mController.getDevices(ConfigModelApi.MODEL_NUMBER));
                        
                    }
                }).setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    /**
     * Add a new group.
     */
    private void newGroup() {
        // Show dialogue to allow user to enter a name for the new group.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.enter_group_name));
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(CheckedListFragment.MAX_DEVICE_NAME_LENGTH) });
        builder.setView(input);

        builder.setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                Device newGroup = mController.addLightGroup(newName);
                mGroupFragment.addItem(newGroup);
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable text) {

                if (text.length() <= 0) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No behaviour.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No behaviour.
            }
        });

    }

    /**
     * Send a group update to the remote device. The device with the edited groups is contained in mEditDevice.
     * @return True if the update was sent, false if there were no changes to send.
     */
    private boolean sendGroupUpdate() {
    	List<Integer> existingGroupMembership = 
    			((SingleDevice)mDeviceFragment.getItem(mEditGroupList.deviceId).getDevice()).getGroupMembership();    	
    	if (existingGroupMembership.size() == mEditGroupList.groupMembership.size()) {
    		// If the lists are the same size then calculate the intersection of the set 
    		// of existing groups for the device and the set of groups we want to change it to.
    		// If the intersection is an empty set then there is nothing to do.
    		
        	// Number of devices that are in both lists.
    		int matching = 0;
	    	for (int deviceId : existingGroupMembership) {
	    		if (mEditGroupList.groupMembership.contains(deviceId)) {
	    			matching++;
	    		}
	    	}
	    	if (matching == mEditGroupList.groupMembership.size()) {
	    		return false;
	    	}
    	}
    	// If there are devices that weren't in both lists then perform the update.    	
        showProgress(getActivity().getString(R.string.group_update));
        mController.setSelectedDeviceId(mEditGroupList.deviceId);
        mController.setDeviceGroups(mEditGroupList.groupMembership, (GroupListener) mMainFragment);	            	
    	return true;
    }
    
    /**
     * Send the group update for the next device in the modified group if there is one.
     * This method will only send one group update, and will be called again when that group update completes.
     * @return True if an update was sent, false if there were no more updates to send.
     */
    private boolean updateNextDeviceInGroup() {
    	boolean sentUpdate = false;
    	if (mState == STATE_EDIT_GROUP) {
            if (mModifiedGroups.size() > 0) {
            	// Loop until a group update is sent.
            	do {
	                mEditGroupList = mModifiedGroups.valueAt(0);
                    CheckedListItem device = mDeviceFragment.getItem(mEditGroupList.deviceId);
                    if (device == null) {
                        break;
                    }
	                mSelectedDevice = device.getDevice();
	                mModifiedGroups.removeAt(0);
	                sentUpdate = sendGroupUpdate();
            	} while (!sentUpdate && mModifiedGroups.size() > 0);
            }
    	}
    	return sentUpdate;
    }
    
    @Override
    public void groupsUpdated(int deviceId, boolean success, String msg) {
    	boolean done = false;
        if (success) {        	
            ((SingleDevice)mSelectedDevice).setGroupIds(mEditGroupList.groupMembership);
            if (mState == STATE_EDIT_DEVICE || mModifiedGroups.size() == 0) {
                done = true;
            }
            else {
            	// If no update was sent then we are done, so a message will be displayed.
            	done = !updateNextDeviceInGroup();
            }
        }
        else {
        	// The update failed, so don't try and update any more devices.
        	done = true;
        }
        if (done) {
        	if (msg!=null) {
        		Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
        	}
            resetUI();
            hideProgress();
        }
    }

    @Override
    public void itemRename(String fragmentTag, int deviceId, String name) {
        mController.setDeviceName(deviceId, name);
    }

    @Override
    public void itemRemove(String fragmentTag, int deviceId) {
        confirmRemove(deviceId);
    }

    @Override
    public void itemInfo(String fragmentTag, int deviceId) {
        showProgress(getActivity().getString(R.string.waiting_for_response));
        mController.setSelectedDeviceId(deviceId);
        mController.getFwVersion((InfoListener) mMainFragment);
    }
    
	public void itemVersion(String fragmentTag, int deviceId) {
    	showProgress(getActivity().getString(R.string.waiting_for_response));
        mController.setSelectedDeviceId(deviceId);
        mController.getVID_PID_VERSION((InfoListener) mMainFragment);
    	
	}
	
	public void itemRequestModels(String fragmentTag, int deviceId) {
    	showProgress(getActivity().getString(R.string.waiting_for_response));
        mController.setSelectedDeviceId(deviceId);
        mController.requestModelsSupported((InfoListener) mMainFragment);
    	
	}
	
    @Override
	public void itemGetData(String fragmentTag, int deviceId) {	
    	showProgress(getActivity().getString(R.string.waiting_for_response));
    	startUITimeOut();
    	mController.setSelectedDeviceId(deviceId);
    	mController.getDeviceData((DataListener)mMainFragment);
    	mDeviceInfoData = "Data received from these devices:";
    	mDeviceInfoAlert = null;
	}

    
    /**
     * Start the timeout which manages if the progress bar dialog should be closed and not block the UI anymore.
     */
    private void startUITimeOut() {
    	mController.startUITimeOut();
	}
    
    /**
     * Cancel the timeout which manages if the progress bar dialog should be closed and not block the UI anymore.
     */
    private void cancelUITimeOut() {
    	mController.stopUITimeOut();
	}
    

	@Override
    public void onItemContextMenuClick(int menuGroupId, int position, int menuId) {
        if (menuGroupId == R.id.group_menu_group) {
            mGroupFragment.handleContextMenu(position, menuId);
        }
        else if (menuGroupId == R.id.device_menu_group) {
            mDeviceFragment.handleContextMenu(position, menuId);
        }
    }

    @Override
    public void onFirmwareVersion(int deviceId, int major, int minor, boolean success) {
        hideProgress();
        if (success) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getActivity().getString(R.string.firmware_version_is) + " " + major + "." + minor).setCancelable(false)
                    .setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            Toast.makeText(getActivity(), getActivity().getString(R.string.firmware_get_fail), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
	public void onDeviceInfoReceived(byte[] vid, byte[] pid, byte[] version , int batteryPercent, int batteryState, int deviceId,boolean success) {
    	hideProgress();
        if (success) {
        	ArrayList<String> modelsLabel = mController.getModelsLabelSupported(deviceId);
        	
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	builder.setTitle(R.string.menu_device_info);
        	String modelsListText="";
        	// making fancy the way we shows the list of models supported.
        	if(modelsLabel==null || modelsLabel.size()==0){
        		modelsListText = getActivity().getString(R.string.not_support_any_model);
        	}else{
        		for(int i=0;i<modelsLabel.size();i++){
        			modelsListText += "- " + modelsLabel.get(i) + System.getProperty("line.separator");
        		}
        		
        	}
        	modelsListText +=System.getProperty("line.separator") +"Vendor ID: 0x" + Utils.hexString(vid).toUpperCase();
        	modelsListText +=System.getProperty("line.separator") +"Product ID: 0x" + Utils.hexString(pid).toUpperCase();
        	modelsListText +=System.getProperty("line.separator") +"Version number: " + String.format("%02d", version[0])+"."+ String.format("%02d", version[1]) +"."+String.format("%02d", version[2]) +"."+ String.format("%02d", version[3]);
        	
        	if (batteryPercent != UNKNOWN_BATTERY_LEVEL) {
        		modelsListText +=System.getProperty("line.separator");
        		modelsListText +=getString(R.string.battery_level) +": " + batteryPercent + "%";
        	}
            if (batteryState != UNKNOWN_BATTERY_STATE) {
                modelsListText +=System.getProperty("line.separator");
                modelsListText += getString(R.string.battery_state) + ": " + getResources().getStringArray(R.array.battery_states)[batteryState];
            }
            builder.setMessage(modelsListText).setCancelable(false)
                    .setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else if (getActivity() != null) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.configuration_get_fail), Toast.LENGTH_SHORT).show();
        }
		
	}

    @Override
    public void onDeviceRemoved(int deviceId, boolean success) {
        hideProgress();
        if (success) {
            mGroupFragment.clearItems();
            mDeviceFragment.clearItems();
            mGroupFragment.addItems(mController.getGroups());
            mDeviceFragment.addItems(mController.getDevices(ConfigModelApi.MODEL_NUMBER));
            
            if (mDeviceFragment.getDevices().size() == 0) {
                mGroupFragment.setClickEnabled(false);
            }
        }
        else {
            confirmRemoveLocal(deviceId);
        }
    }
    
    private class GroupList {
        public int deviceId;
        public ArrayList<Integer> groupMembership = new ArrayList<Integer>();
        
        public GroupList(int deviceId) {
            this.deviceId = deviceId;
        }
    }

    /**
     * Process the data in the application level device info message sent over data stream model.
     * @param data The data received from the remote device. Should be a CSR_DEVICE_INFO_RSP message. 
     * @return The message contained in the CSR_DEVICE_INFO_RSP message.
     */
    private String getDeviceInfoString(byte [] data) {
    	// Default message is failure.
		String message = "Failed to get data";
		if (data != null && data.length > DEVICE_INFO_HEADER_LENGTH) {
			int length = data[DEVICE_INFO_OFFSET_LENGTH];
			if (length > 0 && data.length >= DEVICE_INFO_HEADER_LENGTH + length) {			
				message = new String(data,DEVICE_INFO_OFFSET_DATA,length);
			}
		}
		return message;
    }
    
	@Override
	public void dataReceived(int deviceId, byte [] data) {
		cancelUITimeOut();
		hideProgress();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getDeviceInfoString(data))
        	   .setCancelable(false)
               .setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
               });
        AlertDialog alert = builder.create();
        alert.show();
	}
	
	@Override
	public void UITimeout(){
		hideProgress();
    	Toast.makeText(getActivity(), "The operation has timed out", Toast.LENGTH_SHORT).show();
	}
	
	AlertDialog mDeviceInfoAlert = null;
	String mDeviceInfoData = null;
	@Override
	public void dataGroupReceived(int deviceId) {
		hideProgress();
		cancelUITimeOut();
		
		mDeviceInfoData += System.getProperty("line.separator") + deviceId;
				
		String message = new String(mDeviceInfoData);
		if (mDeviceInfoAlert == null ) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(message)
	        	   .setCancelable(false)
	               .setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int id) {
	                        dialog.cancel();
	                    }
	               });
	        mDeviceInfoAlert = builder.create();
	        mDeviceInfoAlert.show();
		} else {
			mDeviceInfoAlert.hide();
			mDeviceInfoAlert.setMessage(mDeviceInfoData);
			mDeviceInfoAlert.show();
		}
		
	}

	@Override
	public void onDeviceConfigReceived(boolean success) {
		hideProgress();
		if (success) {
			Toast.makeText(getActivity(),"Device configuration updated.", Toast.LENGTH_SHORT).show();
	    	resetDeviceList();
		}
    	
	}

	/**
	 * Reload the displayed devices and groups.
	 */
	public void refreshUI() {
		resetDeviceList();
	}

}
