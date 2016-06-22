
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.csr.csrmeshdemo.DeviceState.StateType;
import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.entities.GroupDevice;
import com.csr.csrmeshdemo.entities.Setting;
import com.csr.csrmeshdemo.entities.SingleDevice;
import com.csr.csrmeshdemo.listeners.AssociationListener;
import com.csr.csrmeshdemo.listeners.AssociationStartedListener;
import com.csr.csrmeshdemo.listeners.DataListener;
import com.csr.csrmeshdemo.listeners.GroupListener;
import com.csr.csrmeshdemo.listeners.InfoListener;
import com.csr.csrmeshdemo.listeners.RemovedListener;
import com.csr.csrmeshdemo.listeners.SimpleNavigationListener;
import com.csr.csrmeshdemo.listeners.TemperatureListener;
import com.csr.csrmeshdemo.ui.GroupAssignFragment;
import com.csr.csrmeshdemo.ui.NotificationFragment;
import com.csr.mesh.ActuatorModelApi;
import com.csr.mesh.AttentionModelApi;
import com.csr.mesh.BatteryModelApi;
import com.csr.mesh.ConfigModelApi;
import com.csr.mesh.DataModelApi;
import com.csr.mesh.FirmwareModelApi;
import com.csr.mesh.GroupModelApi;
import com.csr.mesh.LightModelApi;
import com.csr.mesh.MeshService;
import com.csr.mesh.PowerModelApi;
import com.csr.mesh.SensorModelApi;
import com.csr.mesh.SwitchModelApi;
import com.csr.mesh.sensor.DesiredAirTemperature;
import com.csr.mesh.sensor.InternalAirTemperature;
import com.csr.mesh.sensor.SensorValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements DeviceController, NotificationFragment.NotificationFragmentCallbacks {
    private static final String TAG = "MainActivity";

    /*package*/ static final int DEVICE_LOCAL_ADDRESS =  0x8000;
    /*package*/ static final int ATTENTION_DURATION_MS = 20000;
    /*package*/ static final int ATTRACTION_DURATION_MS = 5000;
    
    /*package*/ static final int MAX_TTL_MASP = 0xFF;

    // ???
    private static final int SCANCODE_RESULT_CODE = 0;
    private static final int PICKFILE_RESULT_CODE = 1;
    private static final int SHARING_RESULT_CODE = 2;
    private static final int REQUEST_BT_RESULT_CODE = 3;
    
    // ?????? ms
    private static final int TRANSMIT_COLOR_PERIOD_MS = 240;

    // ??????
    private static final int TRANSMIT_TEMPERATURE_PERIOD_MS = 500;

    // ??????
    private static final int REMOVE_ACK_WAIT_TIME_MS = (10 * 1000);

    // ?????????
    private static final int PROGRESS_DIALOG_TIME_MS = (10 * 1000);

    private static final int DATA_BUFFER_SIZE = 200;

    private boolean mConnected = false;
    private HashSet<String> mConnectedDevices = new HashSet<String>();

    private DeviceStore mDeviceStore;
    private int testMeshId = 0;

    // ????.
    private int mSendDeviceId = Device.DEVICE_ID_UNKNOWN;

    // ?????????
    private int mColorToSend = Color.rgb(0, 0, 0);

    // ???????
    private boolean mNewColor = false;

    private SensorValue mTemperatureToSend = null;

    private int mGroupAcksWaiting = 0;
    private boolean mGroupSuccess = true;
        
    private ArrayList<Integer> mNewGroups = new ArrayList<Integer>();
    private List <Integer> mGroupsToSend;
    private int mLastActuatorMeshId = 0;
    private boolean mPendingDesiredTemperatureRequest = false;
    
    // ????id??,?????????????????
    private Queue<Integer> mModelsToQueryForGroups = new LinkedList<Integer>();

    // value: temperature status       key: deviceID.
    private HashMap<Integer, TemperatureStatus> mTemperatureStatus = new HashMap<>();

    private SparseIntArray mDeviceIdtoUuidHash = new SparseIntArray();
    private SparseArray<String> mUuidHashToAppearance = new SparseArray<String>();
    
    private MeshService mService = null;

    private int mRemovedUuidHash;
    private int mRemovedDeviceId;
    private int mAssociationTransactionId = -1;  //????id

    private ProgressDialog mProgress;

    private SimpleNavigationListener mNavListener;

    private byte [] mData = new byte[DATA_BUFFER_SIZE];
    
    // Keys  ???????
    private static final String SETTING_LAST_ID = "lastID";

    // Listeners
    private GroupListener mGroupAckListener;
    private InfoListener mInfoListener;
    private AssociationListener mAssListener;
    private AssociationStartedListener mAssStartedListener;
    private RemovedListener mRemovedListener;
    private DataListener mDataListener;
	private TemperatureListener mTemperatureListener;
	
	// ??????id  ???id ???
	byte[] vid;
	byte[] pid;
	byte[] version;
	
	// ????????
	File tmpSharingFile = null;

    private NotificationFragment mNotificationFragment;
    private boolean mRemoveNotificationAfterClick = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mDeviceStore = new DeviceStore(this);

        showProgress(getString(R.string.connecting));        
        
        // ??mesh??
        Intent bindIntent = new Intent(this, MeshService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed()
    {
        mService.disconnectBridge();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.setHandler(null);
        mMeshHandler.removeCallbacksAndMessages(null);        
        unbindService(mServiceConnection);
    }

    // ?? notification fragments /////

    @Override
    public void onNotificationDismiss() {
        mNotificationFragment = null;
    }

    @Override
    public void onNotificationClick() {


        // ????
        if (mAssociationTransactionId != -1) {
            mService.cancelTransaction(mAssociationTransactionId);  //????  ?????
        }else {
            mNavListener.restorePosition(); //?????????fragment
        }

        // ????
        if (mRemoveNotificationAfterClick) {
            removeNotificationFragment();
        }
    }

    @Override
    public boolean isNotificationClickEnabled() {
        return mRemoveNotificationAfterClick;
    }

    private void showNotificationFragment(String title, String subtitle, int position, boolean replace, boolean removeWithClick) {
        if (mNotificationFragment == null || replace) {
            mNavListener.savePosition(position);
            mRemoveNotificationAfterClick = removeWithClick;

            if (mNotificationFragment == null) {
                Bundle fragBundle = new Bundle();
                fragBundle.putString(NotificationFragment.EXTRA_TITLE, title);
                fragBundle.putString(NotificationFragment.EXTRA_SUBTITLE, subtitle);
                mNotificationFragment = new NotificationFragment();
                mNotificationFragment.setArguments(fragBundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                ft.replace(R.id.notifier, mNotificationFragment);
                ft.commitAllowingStateLoss();
            }
            else {
                mNotificationFragment.setTitle(title);
                mNotificationFragment.setSubTitle(subtitle);
            }
        }
    }

    private void removeNotificationFragment() {
        if (mNotificationFragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(mNotificationFragment);
            ft.commitAllowingStateLoss();
            mNotificationFragment = null;
        }
    }

    // End of notification fragment methods ////


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	if (mNavListener != null && mNavListener.getCurrentPosition() == SimpleNavigationListener.POSITION_GROUP_CONFIG) {
    		MenuInflater inflater = getMenuInflater();
    	    inflater.inflate(R.menu.menu_config, menu);
    	}
    	 return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // ??action bar ????
        switch (item.getItemId()) {
            case R.id.menu_share:
            	shareConfiguration();
            	return true;
            case R.id.menu_load:
            	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            	intent.setType("file/*");
            	startActivityForResult(intent, PICKFILE_RESULT_CODE);
            	return true;
            case android.R.id.home:
            	finish();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * ????
     */
    private void shareConfiguration() {
        // ??????.
    	String fileName = "CSRmesh_" + System.currentTimeMillis() + ".json";
    	String configuration = mDeviceStore.getDataBaseAsJson();
    	tmpSharingFile = Utils.writeToSDFile(fileName,configuration);
    	if (tmpSharingFile == null) {
    		Toast.makeText(this, getString(R.string.error_share_configuration), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	Intent shareIntent = new Intent();
    	shareIntent.setAction(Intent.ACTION_SEND);
    	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpSharingFile));
    	shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_configuration));
    	shareIntent.setType("file/*");
    	startActivityForResult(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)), SHARING_RESULT_CODE);
	}

	/**
     * ??????
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((MeshService.LocalBinder) rawBinder).getService();
            if (mService != null) {
            	// ??????setting ID
            	SharedPreferences activityPrefs = getPreferences(Activity.MODE_PRIVATE);
            	int lastIdUsed = activityPrefs.getInt(SETTING_LAST_ID, Setting.UKNOWN_ID);
                restoreSettings(lastIdUsed);  //????

                connect();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    //????
    private LeScanCallback mScanCallBack = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        	mService.processMeshAdvert(device, scanRecord, rssi);
        }
    };
    //?????mesh
    private void connect() {
    	mService.setHandler(mMeshHandler);    //?????CSRmesh??????????
        mService.setLeScanCallback(mScanCallBack);  //??????
        mService.setMeshListeningMode(true, true);
        mService.autoConnect(1, 15000, 0, 2);
    }
    
    /**
     * Executed when LE link to bridge is connected.
     */
    private void onConnected() {
        hideProgress();

        mConnected = true;
        
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if (mNavListener == null) {
	        // Set up drop down list in action bar that selects fragments.
	        String[] screenNames = getResources().getStringArray(R.array.screen_names);
	        ArrayAdapter<String> screenNamesAdapter = new ArrayAdapter<String>(getActionBar().getThemedContext(), android.R.layout.simple_list_item_1,screenNames);
	        mNavListener = new SimpleNavigationListener(getFragmentManager(), this);
	        actionBar.setListNavigationCallbacks(screenNamesAdapter, mNavListener);
	
	        // Don't show app title as navigation list is being used.
	        actionBar.setDisplayShowTitleEnabled(false);
        }
        // Set active fragment based on settings.
        if (mDeviceStore.getSetting() == null || mDeviceStore.getSetting().getNetworkKey() == null) {
            getActionBar().setSelectedNavigationItem(SimpleNavigationListener.POSITION_NETWORK_SETTINGS);
            mNavListener.setNavigationEnabled(false);
        }
        startPeriodicTransmit();
        
        // check if there is any required action from the user.
        checkIfDeviceRequiredAction();
    }
    
   

    /**
     * ??mesh service?????.
     */
    private final Handler mMeshHandler = new MeshHandler(this);

    private static class MeshHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MeshHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        public void handleMessage(Message msg) {
            MainActivity parentActivity = mActivity.get();
            switch (msg.what) {
                case MeshService.MESSAGE_LE_CONNECTED: {  //??
                    parentActivity.mConnectedDevices.add(msg.getData().getString(MeshService.EXTRA_DEVICE_ADDRESS));
                    if (!parentActivity.mConnected) {
                        parentActivity.onConnected();
                    }
                    break;
                }
                case MeshService.MESSAGE_LE_DISCONNECTED: {  //????
                    int numConnections = msg.getData().getInt(MeshService.EXTRA_NUM_CONNECTIONS);
                    String address = msg.getData().getString(MeshService.EXTRA_DEVICE_ADDRESS);
                    if (address != null) {
                        String toRemove = null;
                        for (String s : parentActivity.mConnectedDevices) {
                            if (s.compareTo(address) == 0) {
                                toRemove = s;
                                break;
                            }
                        }
                        if (toRemove != null) {
                            parentActivity.mConnectedDevices.remove(toRemove);
                        }
                    }
                    if (numConnections == 0) {
                        parentActivity.mConnected = false;
                        parentActivity.showProgress(parentActivity.getString(R.string.connecting));
                    }
                    break;
                }
                case MeshService.MESSAGE_LE_DISCONNECT_COMPLETE:
                    parentActivity.finish();
                    break;
                case MeshService.MESSAGE_REQUEST_BT:

                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    parentActivity.startActivityForResult(enableBtIntent, REQUEST_BT_RESULT_CODE);
                    break;
                case MeshService.MESSAGE_TIMEOUT:{
                    int expectedMsg = msg.getData().getInt(MeshService.EXTRA_EXPECTED_MESSAGE);
                    int id;
                    int meshRequestId;
                    if (msg.getData().containsKey(MeshService.EXTRA_UUIDHASH_31)) {
                        id = msg.getData().getInt(MeshService.EXTRA_UUIDHASH_31);
                    }
                    else {
                        id = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
                    }
                    meshRequestId = msg.getData().getInt(MeshService.EXTRA_MESH_REQUEST_ID);
                    parentActivity.onMessageTimeout(expectedMsg, id, meshRequestId);
                    break;
                }
                case MeshService.MESSAGE_DEVICE_DISCOVERED: {
                    ParcelUuid uuid = msg.getData().getParcelable(MeshService.EXTRA_UUID);
                    int uuidHash = msg.getData().getInt(MeshService.EXTRA_UUIDHASH_31);
                    int rssi = msg.getData().getInt(MeshService.EXTRA_RSSI);
                    int ttl = msg.getData().getInt(MeshService.EXTRA_TTL);
                    if (parentActivity.mRemovedListener != null && parentActivity.mRemovedUuidHash == uuidHash) {
                        // This was received after a device was removed, so let the removed listener know.

                        parentActivity.mDeviceStore.removeDevice(parentActivity.mRemovedDeviceId);
                        parentActivity.mRemovedListener.onDeviceRemoved(parentActivity.mRemovedDeviceId, true);
                        parentActivity.mRemovedListener = null;
                        parentActivity.mRemovedUuidHash = 0;
                        parentActivity.mRemovedDeviceId = 0;
                        parentActivity.mService.setDeviceDiscoveryFilterEnabled(false);
                        removeCallbacks(parentActivity.removeDeviceTimeout);
                    } else if (parentActivity.mAssListener != null) {
                        // This was received after discover was enabled so let the UUID listener know.
                        parentActivity.mAssListener.newUuid(uuid.getUuid(), uuidHash, rssi, ttl);
                    }
                    break;
                }
                case MeshService.MESSAGE_DEVICE_APPEARANCE: {
                    // This is the appearance received when a device is in association mode.
                    // If appearance has been explicitly requested via CONFIG_DEVICE_INFO, then the appearance
                    // will be received in a MESSAGE_CONFIG_DEVICE_INFO.
                    byte[] appearance = msg.getData().getByteArray(MeshService.EXTRA_APPEARANCE);
                    String shortName = msg.getData().getString(MeshService.EXTRA_SHORTNAME);
                    int uuidHash = msg.getData().getInt(MeshService.EXTRA_UUIDHASH_31);
                    if (parentActivity.mAssListener != null) {
                        parentActivity.mUuidHashToAppearance.put(uuidHash, shortName);
                        // This was received after discover was enabled so let the UUID listener know.
                        parentActivity.mAssListener.newAppearance(uuidHash, appearance, shortName);
                    }
                    break;
                }
                case MeshService.MESSAGE_DEVICE_ASSOCIATED: {
                    // New device has been associated and is telling us its device id.
                    // Request supported models before adding to DeviceStore, and the UI.
                    int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
                    int uuidHash = msg.getData().getInt(MeshService.EXTRA_UUIDHASH_31);
                    Log.d(TAG, "New device associated with id " + String.format("0x%x", deviceId));

                    if (parentActivity.mDeviceStore.getDevice(deviceId) == null) {
                        // Save the device id with the UUID hash so that we can store the UUID hash in the device
                        // object when MESSAGE_CONFIG_MODELS is received.
                        parentActivity.mDeviceIdtoUuidHash.put(deviceId, uuidHash);

                        // We add the device with no supported models. We will update that once we get the info.
                        if (uuidHash != 0) {
                            parentActivity.addDevice(deviceId, uuidHash, null, 0, false);
                        }

                        // If we don't already know about this device request its model support.
                        // We only need the lower 64-bits, so just request those.
                        ConfigModelApi.getInfo(deviceId, ConfigModelApi.DeviceInfo.MODEL_LOW);
                    }
                    break;
                }
                case MeshService.MESSAGE_CONFIG_DEVICE_INFO: {
                    int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
                    int uuidHash = parentActivity.mDeviceIdtoUuidHash.get(deviceId);

                    ConfigModelApi.DeviceInfo infoType =
                            ConfigModelApi.DeviceInfo.values()[msg.getData().getByte(MeshService.EXTRA_DEVICE_INFO_TYPE)];
                    if (infoType == ConfigModelApi.DeviceInfo.MODEL_LOW) {
                        long bitmap = msg.getData().getLong(MeshService.EXTRA_DEVICE_INFORMATION);
                        // If the uuidHash was saved for this device id then this is an expected message, so process it.
                        if (uuidHash != 0) {
                            // Remove the uuidhash from the array as we have received its model support now.
                            parentActivity.mDeviceIdtoUuidHash
                                    .removeAt(parentActivity.mDeviceIdtoUuidHash.indexOfKey(deviceId));
                            String shortName = parentActivity.mUuidHashToAppearance.get(uuidHash);
                            if (shortName != null) {
                                parentActivity.mUuidHashToAppearance.remove(uuidHash);
                            }
                            parentActivity.addDevice(deviceId, uuidHash, shortName, bitmap, true);
                            parentActivity.deviceAssociated(true, null);
                        } else if (parentActivity.mDeviceIdtoUuidHash.size() == 0) {
                            if (parentActivity.mInfoListener != null) {
                                SingleDevice device = parentActivity.mDeviceStore.getSingleDevice(deviceId);
                                if (device != null) {
                                    device.setModelSupport(bitmap, 0);
                                    parentActivity.mDeviceStore.addDevice(device);
                                    parentActivity.mInfoListener.onDeviceConfigReceived(true);
                                } else {
                                    parentActivity.mInfoListener.onDeviceConfigReceived(false);
                                }


                            }
                        }
                    } else if (infoType == ConfigModelApi.DeviceInfo.VID_PID_VERSION) {
                        parentActivity.vid = msg.getData().getByteArray(MeshService.EXTRA_VID_INFORMATION);
                        parentActivity.pid = msg.getData().getByteArray(MeshService.EXTRA_PID_INFORMATION);
                        parentActivity.version = msg.getData().getByteArray(MeshService.EXTRA_VERSION_INFORMATION);
                        if (parentActivity.mDeviceStore.getSingleDevice(deviceId).isModelSupported(BatteryModelApi.MODEL_NUMBER)) {
                            parentActivity.getBatteryState(parentActivity.mInfoListener);
                        } else if (parentActivity.mInfoListener != null) {
                            parentActivity.mInfoListener.onDeviceInfoReceived(parentActivity.vid, parentActivity.pid, parentActivity.version, GroupAssignFragment.UNKNOWN_BATTERY_LEVEL,GroupAssignFragment.UNKNOWN_BATTERY_STATE, deviceId, true);
                        } else {
                            // shouldn't happen. Just in case for avoiding endless loops.
                            parentActivity.hideProgress();
                        }

                    }
                    break;
                }
                case MeshService.MESSAGE_BATTERY_STATE: {

                    int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
                    byte batteryLevel = msg.getData().getByte(MeshService.EXTRA_BATTERY_LEVEL);
                    byte batteryState = msg.getData().getByte(MeshService.EXTRA_BATTERY_STATE);



                    if (parentActivity.mInfoListener != null) {
                        parentActivity.mInfoListener.onDeviceInfoReceived(parentActivity.vid, parentActivity.pid, parentActivity.version, batteryLevel,batteryState, deviceId, true);
                    } else {
                        // shouldn't happen. Just in case for avoiding endless loops.
                        parentActivity.hideProgress();
                    }
                    break;
                }
                case MeshService.MESSAGE_GROUP_NUM_GROUPIDS: {
                    if (parentActivity.mGroupAckListener != null) {
                        int numIds = msg.getData().getByte(MeshService.EXTRA_NUM_GROUP_IDS);
                        int modelNo = msg.getData().getByte(MeshService.EXTRA_MODEL_NO);
                        int expectedModelNo = parentActivity.mModelsToQueryForGroups.peek();
                        int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);

                        if (expectedModelNo == modelNo) {
                            SingleDevice currentDev = parentActivity.mDeviceStore.getSingleDevice(deviceId);
                            if (currentDev != null) {
                                currentDev.setNumSupportedGroups(numIds, modelNo);
                                parentActivity.mDeviceStore.addDevice(currentDev);
                                // We know how many groups are supported for this model now so remove it from the queue.
                                parentActivity.mModelsToQueryForGroups.remove();
                                if (parentActivity.mModelsToQueryForGroups.isEmpty()) {
                                    // If there are no more models to query then we can assign groups now.
                                    parentActivity.assignGroups(currentDev.getMinimumSupportedGroups());
                                } else {
                                    // Otherwise ask how many groups the next model supports, by taking the next model number from the queue.
                                    GroupModelApi.getNumModelGroupIds(parentActivity.mSendDeviceId, parentActivity.mModelsToQueryForGroups.peek());
                                }
                            } else {
                                parentActivity.mGroupAckListener.groupsUpdated(parentActivity.mSendDeviceId, false, parentActivity.getString(R.string.group_query_fail));
                            }
                        }
                    }
                    break;
                }
                case MeshService.MESSAGE_GROUP_MODEL_GROUPID: {
                    // This is the ACK returned after calling setModelGroupId.
                    if (parentActivity.mGroupAckListener != null && parentActivity.mGroupAcksWaiting > 0) {
                        parentActivity.mGroupAcksWaiting--;
                        int index = msg.getData().getByte(MeshService.EXTRA_GROUP_INDEX);
                        int groupId = msg.getData().getInt(MeshService.EXTRA_GROUP_ID);
                        // Update the group membership of this device in the device store.
                        SingleDevice updatedDev = parentActivity.mDeviceStore.getSingleDevice(parentActivity.mSendDeviceId);
                        try {
                            updatedDev.setGroupId(index, groupId);

                        } catch (IndexOutOfBoundsException exception) {
                            parentActivity.mGroupSuccess = false;
                        }
                        parentActivity.mDeviceStore.addDevice(updatedDev);


                        if (parentActivity.mGroupAcksWaiting == 0) {
                            // Tell the listener that the update was OK.
                            parentActivity.mGroupAckListener.groupsUpdated(
                                    parentActivity.mSendDeviceId, true,
                                    parentActivity.mGroupSuccess ? parentActivity.getString(R.string.group_update_ok) : parentActivity.getString(R.string.group_update_with_problems));
                        }
                    }
                    break;
                }
            case MeshService.MESSAGE_FIRMWARE_VERSION:
                    parentActivity.mInfoListener.onFirmwareVersion(msg.getData().getInt(MeshService.EXTRA_DEVICE_ID), msg
                                    .getData().getInt(MeshService.EXTRA_VERSION_MAJOR),
                            msg.getData().getInt(MeshService.EXTRA_VERSION_MINOR), true);
                    parentActivity.mInfoListener = null;
                    break;
            case MeshService.MESSAGE_ACTUATOR_VALUE_ACK: {
                if (parentActivity.mTemperatureListener == null) {
                    // do nothing
                    return;
                }

                // Clear mLastActuatorMeshId if this is the mesh Id we were expecting.
                int meshRequestId = msg.getData().getInt(MeshService.EXTRA_MESH_REQUEST_ID);

                if (parentActivity.mLastActuatorMeshId == meshRequestId) {
                    parentActivity.mPendingDesiredTemperatureRequest = false;
                    parentActivity.mLastActuatorMeshId = 0;

                    // notify to the listener
                    parentActivity.mTemperatureListener.confirmDesiredTemperature();
                }

                int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
                // update device's temperature status
                TemperatureStatus status = parentActivity.mTemperatureStatus.get(deviceId);
                if (status == null) {
                    status = new TemperatureStatus();
                }
                status.setDesiredTemperatureConfirmed(true);
                parentActivity.mTemperatureStatus.put(deviceId, status);


                break;
            }

			case MeshService.MESSAGE_SENSOR_VALUE: {
                int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);

                SensorValue value1 = (SensorValue) msg.getData().getParcelable(MeshService.EXTRA_SENSOR_VALUE1);
                SensorValue value2 = null;
                if (msg.getData().containsKey(MeshService.EXTRA_SENSOR_VALUE2)) {
                    value2 = (SensorValue) msg.getData().getParcelable(MeshService.EXTRA_SENSOR_VALUE2);
                }

                TemperatureStatus status = parentActivity.mTemperatureStatus.get(deviceId);
                if (status == null) {
                    status = new TemperatureStatus();
                }



                parentActivity.storeAndNotifyNewSensorValue(value1,status,deviceId);
                parentActivity.storeAndNotifyNewSensorValue(value2,status,deviceId);



			}
			break;
            case MeshService.MESSAGE_RECEIVE_STREAM_DATA:
            	if (parentActivity.mDataListener != null) {
            		int deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
            		byte [] data = msg.getData().getByteArray(MeshService.EXTRA_DATA);
            		int sqn = msg.getData().getInt(MeshService.EXTRA_DATA_SQN);
            		if (deviceId == parentActivity.mSendDeviceId && sqn + data.length < DATA_BUFFER_SIZE) {
            			System.arraycopy(data, 0, parentActivity.mData, sqn, data.length); 
            		}
            	}
            	break;
            case MeshService.MESSAGE_ASSOCIATING_DEVICE:
            		int progress = msg.getData().getInt(MeshService.EXTRA_PROGRESS_INFORMATION);
            		parentActivity.notifyAssociationFragment(progress);
            	break;
            case MeshService.MESSAGE_RECEIVE_STREAM_DATA_END:
            	if (parentActivity.mDataListener != null) {
            		int  deviceId = msg.getData().getInt(MeshService.EXTRA_DEVICE_ID);
            		if (deviceId == parentActivity.mSendDeviceId) {
            			parentActivity.mDataListener.dataReceived(deviceId, parentActivity.mData);
            		} else {
            			parentActivity.mDataListener.dataGroupReceived(deviceId);
            		}
            	}
                break;
            case MeshService.MESSAGE_TRANSACTION_NOT_CANCELLED: {
                Toast.makeText(parentActivity, "Association couldn't be cancelled.", Toast.LENGTH_SHORT).show();
                break;
            }
                case MeshService.MESSAGE_TRANSACTION_CANCELLED: {
                parentActivity.deviceAssociated(false, parentActivity.getString(R.string.association_cancelled));
                break;
            }
        }
        }
    }

    /**
     * Called when a response is not seen to a sent command.
     * 
     * @param expectedMessage
     *            The message that would have been received in the Handler if there hadn't been a timeout.
     */
    private void onMessageTimeout(int expectedMessage, int id, int meshRequestId) {
        switch (expectedMessage) {

            case MeshService.MESSAGE_ACTUATOR_VALUE_ACK: {
                // Clear mLastActuatorMeshId if this is the mesh Id we were expecting.
                if (mLastActuatorMeshId == meshRequestId) {
                    mPendingDesiredTemperatureRequest = false;
                    mLastActuatorMeshId = 0;
                }

            }
            case MeshService.MESSAGE_GROUP_MODEL_GROUPID:
        	if (mGroupAcksWaiting > 0) {
                if (mGroupAckListener != null) {
                    // Timed out waiting for group update ACK.
                    mGroupAckListener.groupsUpdated(mSendDeviceId, false,
                            getString(R.string.group_timeout));
                }
                mGroupAcksWaiting = 0;
            }
        	break;
            case MeshService.MESSAGE_DEVICE_ASSOCIATED:
                // Fall through.
            case MeshService.MESSAGE_CONFIG_MODELS:
                // If we couldn't find out the model support for the device then we have to report association failed.
                deviceAssociated(false, getString(R.string.association_failed));
                if (mInfoListener!= null) {
                    mInfoListener.onDeviceConfigReceived(false);
                }
            break;
            case MeshService.MESSAGE_FIRMWARE_VERSION:
                if (mInfoListener != null) {
                    mInfoListener.onFirmwareVersion(0, 0, 0, false);
                }
            break;
            case MeshService.MESSAGE_BATTERY_STATE:
                if (mInfoListener!= null) {
                    mInfoListener.onDeviceInfoReceived(vid,pid,version, GroupAssignFragment.UNKNOWN_BATTERY_LEVEL,GroupAssignFragment.UNKNOWN_BATTERY_STATE, mSendDeviceId, true);
                }
                break;
            case MeshService.MESSAGE_GROUP_NUM_GROUPIDS:
                if (mGroupAckListener != null) {
                    mGroupAckListener.groupsUpdated(mSendDeviceId, false, getString(R.string.group_query_fail));
                }
                break;
            case MeshService.MESSAGE_CONFIG_DEVICE_INFO:

                // if we were waiting to get the configModels once we associate the device, we just assume we couldn't get the models
                // that the device support, but the association was successful.
                if (mDeviceIdtoUuidHash.size() > 0) {

                    Device device =mDeviceStore.getDevice(mDeviceIdtoUuidHash.keyAt(0));
                    mDeviceIdtoUuidHash.removeAt(0);
                    if (device != null) {
                        String name = device.getName();
                        Toast.makeText(getApplicationContext(),
                                name == null ? "Device" : name + " " + getString(R.string.added),
                                Toast.LENGTH_SHORT).show();
                    }
                    deviceAssociated(true,null);
                }
                if (mInfoListener!= null) {
                    mInfoListener.onDeviceConfigReceived(false);
                }
                if (mInfoListener != null) {
                    mInfoListener.onDeviceInfoReceived(new byte[0],new byte[0],new byte[0],GroupAssignFragment.UNKNOWN_BATTERY_LEVEL,GroupAssignFragment.UNKNOWN_BATTERY_STATE, 0, false);
                }
                break;
            }
    }

    private void storeAndNotifyNewSensorValue (SensorValue value, TemperatureStatus status, int deviceId) {

        if (value == null) return;

        if (value instanceof InternalAirTemperature) {
            // store the temperature in the status array.
            double tempCelsius = ((InternalAirTemperature) value).getCelsiusValue();
            status.setCurrentTemperature(tempCelsius);
            mTemperatureStatus.put(deviceId, status);

            // notify to temperatureFragment if the info received is related to the selected device.
            if (mTemperatureListener != null && deviceId == mSendDeviceId) {
                mTemperatureListener.setCurrentTemperature(tempCelsius);
            }
        }
        else if (value instanceof DesiredAirTemperature) {
            double tempCelsius = ((DesiredAirTemperature) value).getCelsiusValue();
            status.setDesiredTemperature(tempCelsius);
            status.setDesiredTemperatureConfirmed(true);

            // notify to temperatureFragment if the info received is related to the selected device.
            if (mTemperatureListener != null && deviceId == mSendDeviceId && !mPendingDesiredTemperatureRequest) {

               mTemperatureListener.setDesiredTemperature(tempCelsius);

            }
        }




    }

    /**
     * Send group assign messages to the currently selected device using the groups contained in mNewGroups.
     */
    private void assignGroups(int numSupportedGroups) {
        if (mSendDeviceId == Device.DEVICE_ID_UNKNOWN)
            return;
        // Check the number of supported groups matches the number requested to be set.
        if (numSupportedGroups >= mNewGroups.size()) {

            mGroupAcksWaiting = 0;            
            
            // Make a copy of existing groups for this device.
            mGroupsToSend = mDeviceStore.getSingleDevice(mSendDeviceId).getGroupMembershipValues();
            // Loop through existing groups.
            for (int i = 0; i < mGroupsToSend.size(); i++) {
                int groupId = mGroupsToSend.get(i);
                if (groupId != 0) {
	                int foundIndex = mNewGroups.indexOf(groupId);                
	                if (foundIndex > -1) {
	                    // The device is already a member of this group so remove it from the list of groups to add.
	                    mNewGroups.remove(foundIndex);
	                }
	                else {
	                    // The device should no longer be a member of this group, so set that index to -1 to flag
	                    // that a message must be sent to update this index.
	                    mGroupsToSend.set(i, -1);
	                }
                }
            }
            // Now loop through currentGroups, and for every index set to -1 or zero send a group update command for 
            // that index with one of our new groups if one is available. If there are no new groups to set, then just
            // send a message for all indices set to -1, to set them to zero.
            boolean commandSent = false;            
            for (int i = 0; i < mGroupsToSend.size(); i++) {
                int groupId = mGroupsToSend.get(i);
                if (groupId == -1 || groupId == 0) {
                    if (mNewGroups.size() > 0) {
                        int newGroup = mNewGroups.get(0);
                        mNewGroups.remove(0);
                        commandSent = true;
                        sendGroupCommands(mSendDeviceId, i, newGroup);
                    }
                    else if (groupId == -1) {
                    	commandSent = true;
                        sendGroupCommands(mSendDeviceId, i, 0);
                    }
                }
            }
            if (!commandSent) {
                // There were no changes to the groups so no updates were sent. Just tell the listener
            	// that the operation is complete.
            	if (mGroupAckListener != null) {
            		mGroupAckListener.groupsUpdated(mSendDeviceId, true, getString(R.string.group_no_changes));
            	}
            }
        }
        else {
            // Not enough groups supported on device.
            if (mGroupAckListener != null) {
                mGroupAckListener.groupsUpdated(mSendDeviceId, false,
                getString(R.string.group_max_fail) + " " + numSupportedGroups + " " + getString(R.string.groups));
            }
        }
    }

    private void sendGroupCommands(int deviceId, int index, int group) { 
	mGroupSuccess = true;
    	
    	SingleDevice dev = mDeviceStore.getSingleDevice(deviceId);
    	
        if (dev.isModelSupported(LightModelApi.MODEL_NUMBER) && dev.getNumSupportedGroups(LightModelApi.MODEL_NUMBER) != 0) {
        	mGroupAcksWaiting++;
            GroupModelApi.setModelGroupId(deviceId, LightModelApi.MODEL_NUMBER,index, 0, group );
            // If a light also supports power then set groups for that too.
        	if (dev.isModelSupported(LightModelApi.MODEL_NUMBER) && dev.getNumSupportedGroups(LightModelApi.MODEL_NUMBER) != 0) {
	            mGroupAcksWaiting++;
	            GroupModelApi.setModelGroupId(deviceId, PowerModelApi.MODEL_NUMBER, index, 0, group);        
        	}
        }
        else if (dev.isModelSupported(SwitchModelApi.MODEL_NUMBER) && dev.getNumSupportedGroups(SwitchModelApi.MODEL_NUMBER) != 0) {
            mGroupAcksWaiting++;
            GroupModelApi.setModelGroupId(deviceId, SwitchModelApi.MODEL_NUMBER, index, 0, group);
        }
        else if (dev.isModelSupported(SensorModelApi.MODEL_NUMBER) && dev.getNumSupportedGroups(SensorModelApi.MODEL_NUMBER) != 0) {
        	mGroupAcksWaiting++;
        	GroupModelApi.setModelGroupId(deviceId, SensorModelApi.MODEL_NUMBER, index, 0, group);
        }
        else if (dev.isModelSupported(ActuatorModelApi.MODEL_NUMBER) && dev.getNumSupportedGroups(ActuatorModelApi.MODEL_NUMBER) != 0) {
        	mGroupAcksWaiting++;
        	GroupModelApi.setModelGroupId(deviceId, ActuatorModelApi.MODEL_NUMBER, index, 0, group);
        }
        
        // Check if device supports data model and that it supports groups. If it does, then setModelGroupId        
        if (dev.isModelSupported(DataModelApi.MODEL_NUMBER) &&
            dev.getNumSupportedGroups(DataModelApi.MODEL_NUMBER) != 0) {        	
        	mGroupAcksWaiting++;
        	GroupModelApi.setModelGroupId(deviceId, DataModelApi.MODEL_NUMBER, index, 0, group);
        }
    	
    }
    
    // Runnables that execute after a timeout /////
    
    /**
     * This is the implementation of the periodic timer that will call sendLightRgb() every TRANSMIT_PERIOD_MS if
     * mNewColor is set to TRUE.
     */    
    private Runnable transmitColorCallback = new Runnable() {
        @Override
        public void run() {
            if (mNewColor) {
                if (mSendDeviceId != Device.DEVICE_ID_UNKNOWN) {                    
                    byte red = (byte) (Color.red(mColorToSend) & 0xFF);
                    byte green = (byte) (Color.green(mColorToSend) & 0xFF);
                    byte blue = (byte) (Color.blue(mColorToSend) & 0xFF);

                    LightModelApi.setRgb(mSendDeviceId, red, green, blue, (byte)0xFF, 0, false);

                    Device light = mDeviceStore.getDevice(mSendDeviceId);
                    LightState state = (LightState)light.getState(StateType.LIGHT);
                    if (light != null) {
                    	state.setRed(red);
                    	state.setGreen(green);
                    	state.setBlue(blue);
                    	state.setStateKnown(true);
                    	light.setState(state);                    	
                        mDeviceStore.addDevice(light);
                    }
                }
                // Colour sent so clear the flag.
                mNewColor = false;
            }                        
            mMeshHandler.postDelayed(this, TRANSMIT_COLOR_PERIOD_MS);
        }
    };

    /**
     * This is the implementation of the periodic temperature timer that will call setDesiredTemperature() every TRANSMIT_TEMPERATURE_PERIOD_MS if
     * mNewTemperature is set to TRUE.
     */
    private Runnable transmitTempCallback = new Runnable() {
        @Override
        public void run() {

                if (mLastActuatorMeshId != 0) {
                    mService.killTransaction(mLastActuatorMeshId);
                }

                if (mSendDeviceId != Device.DEVICE_ID_UNKNOWN && mTemperatureToSend != null) {
                    mLastActuatorMeshId = ActuatorModelApi.setValue(mSendDeviceId, mTemperatureToSend, true);
                }

                // update device's temperature status
                TemperatureStatus status = mTemperatureStatus.get(mSendDeviceId);
                if (status == null) {
                    status = new TemperatureStatus();
                }
                status.setDesiredTemperatureConfirmed(false);
                double celsiusValue = ((DesiredAirTemperature) mTemperatureToSend).getCelsiusValue();
                status.setDesiredTemperature(celsiusValue);
                mTemperatureStatus.put(mSendDeviceId,status);
        }
    };

    
    private Runnable removeDeviceTimeout = new Runnable() {
		@Override
		public void run() {
			// Handle timeouts on removing devices.
            if (mRemovedListener != null) {
                // Timed out waiting for device UUID that indicates device removal happened.
                mRemovedListener.onDeviceRemoved(mRemovedDeviceId, false);
                mRemovedListener = null;
                mRemovedUuidHash = 0;
                mRemovedDeviceId = 0;
                mService.setDeviceDiscoveryFilterEnabled(false);                
            }
		}
    };
    
    private Runnable progressTimeOut = new Runnable() {
        @Override
        public void run() {
        	
        	if (mDataListener != null) {
                mDataListener.UITimeout();
            }
        	
        }        
    };  
        
    // End of timeout handlers /////
    
    /**
     * Start transmitting colours and temperatures to the current send address. Colours are sent every TRANSMIT_PERIOD_MS ms and temperature values every TRANSMIT_TEMP_PERIOD_MS ms.
     */
    private void startPeriodicTransmit() {
        mMeshHandler.postDelayed(transmitColorCallback, TRANSMIT_COLOR_PERIOD_MS);
    }


    /**
     * Show a modal progress dialogue until hideProgress is called.
     * 
     * @param message
     *            The message to display in the dialogue.
     */
    private void showProgress(String message) {
    	if (mProgress == null) {
    		mProgress = new ProgressDialog(MainActivity.this);
	        mProgress.setMessage(message);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	        mProgress.setIndeterminate(true);
	        mProgress.setCancelable(true);
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mService.disconnectBridge();
                }
            });
	        mProgress.show();
    	}
    }

    /**
     * Hide the progress dialogue.
     */
    private void hideProgress() {
    	if (mProgress != null) {
    		mProgress.dismiss();
        	mProgress=null;
    	}
    }
    
    /**
     * Add a device to the device store, creating state based on model support.
     * @param deviceId Device id of the device to add.
     * @param uuidHash 31-bit UUID hash of the device to add.
     * @param shortName Appearance short name if known, otherwise null.
     * @param modelSupportBitmapLow The low part of the model support bitmap. Currently the only part we care about.
     */
    private void addDevice(int deviceId, int uuidHash, String shortName, long modelSupportBitmapLow, boolean showToast) {    	
    	String name = null;
    	if (shortName != null) {
    		int id = deviceId - Device.DEVICE_ADDR_BASE;
    		name = String.format(shortName.trim() + " %d", id);
    	}
    	SingleDevice device = new SingleDevice(deviceId, name, uuidHash, modelSupportBitmapLow, 0);
    	mDeviceStore.addDevice(device);
    	
    	if (showToast) {
    		Toast.makeText(getApplicationContext(), device.getName() + " " + getString(R.string.added),
				Toast.LENGTH_SHORT).show();
    		
    		checkIfDeviceRequiredAction();
    	}

    }
    
    
    /**
     * Check if there is any device which needs any user action and create the notification for it.
     */
    private void checkIfDeviceRequiredAction() {
		List<Device> devices = mDeviceStore.getAllSingleDevices();
		for (Device device : devices) {
			SingleDevice singleDevice = (SingleDevice) device;
			
			// check if there is any sensor or actuator device which haven't been grouped yet. 
			if ((singleDevice.isModelSupported(SensorModelApi.MODEL_NUMBER) || singleDevice.isModelSupported(ActuatorModelApi.MODEL_NUMBER))
					 && (singleDevice.getGroupMembership().size() == 0)) {
				showNotificationFragment(getString(R.string.user_action_req),
                        getString(R.string.need_device_group),
                        SimpleNavigationListener.POSITION_GROUP_CONFIG, false, true);
			}
					
		}
	}

	/**
     * Restore app settings including devices and groups.
     */
    private void restoreSettings(int settingsID) {
    	
    	// Try to get the settings if we know the ID.
    	if (settingsID != Setting.UKNOWN_ID) {
    		mDeviceStore.loadSetting(settingsID);
    	}
    	// save in sharePreferences the last settings used.
        SharedPreferences activityPrefs = getPreferences(Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPrefs.edit();

        if (mDeviceStore.getSetting() != null) {
        	
        	// set the networkKey to MeshService.
            if (mDeviceStore.getSetting().getNetworkKey() != null) {
                mService.setNetworkPassPhrase(mDeviceStore.getSetting().getNetworkKey());
            }
            
            // save in sharePreferences the last settings used.
            editor.putInt(SETTING_LAST_ID, settingsID);
        	editor.commit();
        	
        	// get all the SingleDevices and GroudDevices from the dataBase.
        	mDeviceStore.loadAllDevices();
        	
        	// set next device id to be used according with the last device used in the database.
        	mService.setNextDeviceId(mDeviceStore.getSetting().getLastDeviceIndex()+1);
        	
        	// set TTL to the library
        	mService.setTTL(mDeviceStore.getSetting().getTTL());
        }
        else {
        	// No setting founded. We need to create one...
        	Setting setting = new Setting();
        	setting.setLastGroupIndex(Device.GROUP_ADDR_BASE + 5);
        	mDeviceStore.setSetting(setting, true);
        	
        	// add group devices. By default we add 5 groups (1 for "All" with id=0 and 4 extra with ids 1-4).
        	for (int i=0; i < 5 ; i++) {
        		GroupDevice group;
        		if (i==0) {
        			group = new GroupDevice(Device.GROUP_ADDR_BASE, getString(R.string.all_lights));
        		} 
        		else {
        			group = new GroupDevice(Device.GROUP_ADDR_BASE + i, getString(R.string.group) + " " + i);
        		}
        		
        		// store the group in the database.
        		mDeviceStore.addGroupDevice(group,true);
        	}
        	
        	// save in sharePreferences the last settings used.
        	editor.putInt(SETTING_LAST_ID, mDeviceStore.getSetting().getId());
         	editor.commit();
        	
        }
        
    }

    @Override
    public void discoverDevices(boolean enabled, AssociationListener listener) {
        if (enabled) {
            mAssListener = listener;
        }
        else {
            mAssListener = null;
        }
        
        //avoiding crashes
        if (mService != null) {
            mService.setDeviceDiscoveryFilterEnabled(enabled);
        }
    }

    @Override
    public boolean associateDevice(int uuidHash, String shortCode) {
    	try {
	    	if (shortCode == null) {
                mAssociationTransactionId = mService.associateDevice(uuidHash, 0, false);
	            notifyAssociationFragment(0);
	            return true;
	    	} else {
	    		int decodedHash = MeshService.getDeviceHashFromShortcode(shortCode);
	        	
	    		if (decodedHash == uuidHash) {
                    mAssociationTransactionId = mService.associateDevice(uuidHash, MeshService.getAuthorizationCode(shortCode), true);
	        		notifyAssociationFragment(0);
	        		return true;
	        	}
	        	return false;
	    	}
    	}
        catch (Exception e) {
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    		return false;
    	}
    	
    }

    @Override
    public Device getDevice(int deviceId) {
        return mDeviceStore.getDevice(deviceId);
    }

    @Override
    public void setSelectedDeviceId(int deviceId) {
        Log.d(TAG, String.format("Device id is now 0x%x", deviceId));
        mSendDeviceId = deviceId;
    }

    @Override
    public void requestCurrentTemperature() {
        SensorModelApi.getValue(mSendDeviceId, SensorValue.SensorType.INTERNAL_AIR_TEMPERATURE,SensorValue.SensorType.DESIRED_AIR_TEMPERATURE);
    }

    @Override
    public int getSelectedDeviceId() {
        return mSendDeviceId;
    }

    @Override
    public void setLightColor(int color, int brightness) {
        if (brightness < 0 || brightness > 99) {
            throw new NumberFormatException("Brightness value should be between 0 and 99");
        }

        // Convert currentColor to HSV space and make the brightness (value) calculation. Then convert back to RGB to
        // make the colour to send.
        // Don't modify currentColor with the brightness or else it will deviate from the HS colour selected on the
        // wheel due to accumulated errors in the calculation after several brightness changes.
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = ((float) brightness + 1) / 100.0f;
        mColorToSend = Color.HSVToColor(hsv);

        // Indicate that there is a new colour for next time the timer fires.
        mNewColor = true;
    }

    @Override
    public void setLightPower(PowerModelApi.PowerState state) {
        PowerModelApi.setState(mSendDeviceId, state, false);
        setLocalLightPower(state);
    }
    
    
    @Override
    public void setLocalLightPower(PowerModelApi.PowerState state) {
        Device dev = mDeviceStore.getDevice(mSendDeviceId);
        if (dev != null) {
        	PowState powState = (PowState)dev.getState(StateType.POWER);
        	powState.setPowerState(state);
        	mDeviceStore.addDevice(dev);        	
        }
    }
    
    @Override
    public void removeDevice(RemovedListener listener) {
        if (mSendDeviceId < Device.DEVICE_ADDR_BASE && mSendDeviceId >= Device.GROUP_ADDR_BASE) {
            mDeviceStore.removeDevice(mSendDeviceId);
            listener.onDeviceRemoved(mSendDeviceId, true);
            mSendDeviceId = Device.GROUP_ADDR_BASE;
        }
        else {
            mRemovedUuidHash = mDeviceStore.getSingleDevice(mSendDeviceId).getUuidHash();
            mRemovedDeviceId = mSendDeviceId;
            mRemovedListener = listener;
            // Enable discovery so that the device uuid message is received when the device is unassociated.
            mService.setDeviceDiscoveryFilterEnabled(true);            
            // Send CONFIG_RESET
            ConfigModelApi.resetDevice(mSendDeviceId);
            mSendDeviceId = Device.GROUP_ADDR_BASE;
            // Start a timer so that we don't wait for the ack forever.
            mMeshHandler.postDelayed(removeDeviceTimeout, REMOVE_ACK_WAIT_TIME_MS);
        }
    }

    @Override
    public void getFwVersion(InfoListener listener) {
        mInfoListener = listener;
        FirmwareModelApi.getVersionInfo(mSendDeviceId);
    }
    
    @Override
    public void getVID_PID_VERSION(InfoListener listener) {
        mInfoListener = listener;
        // reset values
        vid = null;
        pid = null;
        version = null;
        
        // ask for new values
        ConfigModelApi.getInfo(mSendDeviceId, ConfigModelApi.DeviceInfo.VID_PID_VERSION);
    }
    
    @Override
    public void requestModelsSupported(InfoListener listener) {
       mInfoListener = listener;
       ConfigModelApi.getInfo(mSendDeviceId, ConfigModelApi.DeviceInfo.MODEL_LOW);
	}
    
	private void getBatteryState(InfoListener listener){
    	mInfoListener = listener;
        BatteryModelApi.getState(mSendDeviceId);
    }
    

    @Override
    public void setDeviceGroups(List<Integer> groups, GroupListener listener) {
    	if (mSendDeviceId == Device.DEVICE_ID_UNKNOWN)
            return;
        mNewGroups.clear();
        mGroupAckListener = listener;
        boolean inProgress = false;
        for (int group : groups) {
            mNewGroups.add(group);
        }
        SingleDevice selectedDev = mDeviceStore.getSingleDevice(mSendDeviceId);
            
        // 
        
        // Send message to find out how many group ids the device supports for each model type.
        // Once a response is received to this command sendGroupAssign will be called to assign the groups.
        if (selectedDev.isModelSupported(LightModelApi.MODEL_NUMBER) && !selectedDev.isNumSupportedGroupsKnown(LightModelApi.MODEL_NUMBER)) {
            // Only query light model and assume power model supports the same number.
        	mModelsToQueryForGroups.add(LightModelApi.MODEL_NUMBER);                       
            inProgress = true;
        }        
        if (selectedDev.isModelSupported(SwitchModelApi.MODEL_NUMBER) && !selectedDev.isNumSupportedGroupsKnown(SwitchModelApi.MODEL_NUMBER)) {
        	mModelsToQueryForGroups.add(SwitchModelApi.MODEL_NUMBER);
        	inProgress = true;
        }   
        if (selectedDev.isModelSupported(SensorModelApi.MODEL_NUMBER) && !selectedDev.isNumSupportedGroupsKnown(SensorModelApi.MODEL_NUMBER)) {
        	mModelsToQueryForGroups.add(SensorModelApi.MODEL_NUMBER);
        	inProgress = true;
        }
        if (selectedDev.isModelSupported(ActuatorModelApi.MODEL_NUMBER) && !selectedDev.isNumSupportedGroupsKnown(ActuatorModelApi.MODEL_NUMBER)) {
        	mModelsToQueryForGroups.add(ActuatorModelApi.MODEL_NUMBER);
        	inProgress = true;
        }
        if (selectedDev.isModelSupported(DataModelApi.MODEL_NUMBER) && !selectedDev.isNumSupportedGroupsKnown(DataModelApi.MODEL_NUMBER)) {
        	mModelsToQueryForGroups.add(DataModelApi.MODEL_NUMBER);
        	inProgress = true;
        }
        if (inProgress) {
        	GroupModelApi.getNumModelGroupIds(mSendDeviceId,mModelsToQueryForGroups.peek());        	
        }
        else {
            // We already know the number of supported groups from a previous query, so go straight to assigning.
            assignGroups(selectedDev.getMinimumSupportedGroups());
            inProgress = true;
        }
        
        
        // There isn't any operation to do, so the dialog should be dismissed.
        if (!inProgress) {
        	mGroupAckListener.groupsUpdated(mSendDeviceId, false, getString(R.string.group_query_fail));
        }
    	
    }

    @Override
    public void setDeviceName(int deviceId, String name) {
    	mDeviceStore.updateDeviceName(deviceId, name);        
    }

    @Override
    public void setSecurity(String networkKeyPhrase, boolean authRequired) {
    	Setting setting = mDeviceStore.getSetting();
        if (setting != null) {
        	// Set the new setting values
        	setting.setNetworkKey(networkKeyPhrase);
        	setting.setAuthRequired(authRequired);
        }
        else {
        	// if we don't have settings yet we need to create one and set the new setting values.
        	setting = new Setting();
        	setting.setNetworkKey(networkKeyPhrase);
        	setting.setAuthRequired(authRequired);
        }
        // store the setting in the database.
        mDeviceStore.setSetting(setting, true);
        
        // set the new NetworkPassPhrase to the MeshService
        mService.setNetworkPassPhrase(mDeviceStore.getSetting().getNetworkKey());
        
        // change to the association fragment.
        mNavListener.setNavigationEnabled(true);

        // If there are devices already associated we lead the user to go to association page otherwise we lead the user to go to light control.
        if (mDeviceStore.getAllSingleDevices().size() > 0) {
            getActionBar().setSelectedNavigationItem(SimpleNavigationListener.POSITION_LIGHT_CONTROL);
        }
        else {
            getActionBar().setSelectedNavigationItem(SimpleNavigationListener.POSITION_ASSOCIATION);
        }


    }

    @Override
    public boolean isAuthRequired() {
    	if (mDeviceStore.getSetting() != null) { 
    		return mDeviceStore.getSetting().isAuthRequired();
    	}
    	else {
    		return false;
    	}
    }

    @Override
    public String getNetworkKeyPhrase() {
    	if (mDeviceStore.getSetting() != null) { 
    		return mDeviceStore.getSetting().getNetworkKey();
    	}
    	else {
    		return null;
    	}
    }

    @Override
    public void associateWithQrCode(AssociationStartedListener listener) {
        mAssStartedListener = listener;
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, SCANCODE_RESULT_CODE);
        }
        catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCANCODE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("SCAN_RESULT");
                long auth = 0;                
                UUID uuid = null;
                
               
				Uri uri=Uri.parse(url);
				String uuidS = uri.getQueryParameter("UUID");
				String ac = uri.getQueryParameter("AC");
				
				// Trying to get the UUID and AC from a URL
				if (uuidS != null && ac != null && uuidS.length() == 32 && ac.length() == 16) {
					long uuidMsb =
                            ((Long.parseLong(uuidS.substring(0, 8), 16) & 0xFFFFFFFFFFFFFFFFL) << 32)
                                    | ((Long.parseLong(uuidS.substring(8,16),16) & 0xFFFFFFFFFFFFFFFFL));
                    long uuidLsb =
                            ((Long.parseLong(uuidS.substring(16,24),16) & 0xFFFFFFFFFFFFFFFFL) << 32)
                                    | ((Long.parseLong(uuidS.substring(24),16) & 0xFFFFFFFFFFFFFFFFL));
                    
                    auth = ((Long.parseLong(ac.substring(0,8), 16) & 0xFFFFFFFFFFFFFFFFL) << 32)
                            | ((Long.parseLong(ac.substring(8), 16) & 0xFFFFFFFFFFFFFFFFL));
                    
                    uuid = new UUID(uuidMsb, uuidLsb);
                   
				}
				else { // trying to get the UUID and AC directly from params.
					
	                Pattern	pattern =                		
	                            Pattern.compile("&UUID=([0-9A-F]{8})"
	                            + "([0-9A-F]{8})([0-9A-F]{8})([0-9A-F]{8})" 
	                    		+ "&AC=([0-9A-F]{8})([0-9A-F]{8})",                        
	                                    Pattern.CASE_INSENSITIVE);
	                Matcher  matcher = pattern.matcher(url);
	                        if (matcher.find()) {
	                        	long uuidMsb =
	                                    ((Long.parseLong(matcher.group(1), 16) & 0xFFFFFFFFFFFFFFFFL) << 32)
	                                            | ((Long.parseLong(matcher.group(2), 16) & 0xFFFFFFFFFFFFFFFFL));
	                            long uuidLsb =
	                                    ((Long.parseLong(matcher.group(3), 16) & 0xFFFFFFFFFFFFFFFFL) << 32)
	                                            | ((Long.parseLong(matcher.group(4), 16) & 0xFFFFFFFFFFFFFFFFL));

	                            uuid = new UUID(uuidMsb, uuidLsb);
	                            
	                            auth = ((Long.parseLong(matcher.group(5), 16) & 0xFFFFFFFFFFFFFFFFL) << 32)
	                                            | ((Long.parseLong(matcher.group(6), 16) & 0xFFFFFFFFFFFFFFFFL));
	                           
	                        
	                        }
                   
				}
				
				// checking if we got the uuid
				if (uuid != null && mService != null) {
					 if (mAssStartedListener != null) {
	                     mAssStartedListener.associationStarted();
                         mAssociationTransactionId = mService.associateDevice(MeshService.getDeviceHashFromUuid(uuid), auth, true);
	                 }
	                 
	            } else {
	                	
	                // bad QR code	
	               	 Toast.makeText(this, getString(R.string.qr_to_uuid_fail), Toast.LENGTH_LONG).show();
	            }
            
            }
        } else if (requestCode == SHARING_RESULT_CODE) {
        	if (tmpSharingFile != null) {
        		tmpSharingFile.delete();
        	}
        } else if (requestCode == PICKFILE_RESULT_CODE) {
        	

        	
        	if (data == null || data.getData() == null) {
        		Toast.makeText(getApplicationContext(), getString(R.string.error_opening_file), Toast.LENGTH_SHORT).show();
        		return;
        	}

            Uri uri =data.getData();
        	File file = new File(uri.getPath());

            // Check the extension of the file. App only accept .json extensions.
            if (!Utils.getFileExtension(file).equalsIgnoreCase(".json")) {
                Toast.makeText(this,getString(R.string.invalid_file_extension), Toast.LENGTH_SHORT).show();

                // no continue.
                return;
            }

            //Start reading a file...
        	final StringBuilder json = new StringBuilder();

        	try {
        	    BufferedReader br = new BufferedReader(new FileReader(file));
        	    String line;

        	    while ((line = br.readLine()) != null) {
        	        json.append(line);
        	    }
        	    br.close();
        	}
        	catch (IOException e) {
        	    Toast.makeText(getApplicationContext(), getString(R.string.error_opening_file), Toast.LENGTH_SHORT).show();
        	    return;
        	}
        	// end reading file

            // Confirm replacing database.
            confirmReplacingDatabase(json.toString());
        }
    }

    @Override
    public Device addLightGroup(String groupName) {
        GroupDevice result = new GroupDevice(mDeviceStore.getSetting().getNextGroupIndexAndIncrement(), groupName);
        mDeviceStore.addGroupDevice(result, true);
        return result;
    }

	@Override
	public void setAttentionEnabled(boolean enabled) {
		AttentionModelApi.setState(mSendDeviceId, enabled, ATTENTION_DURATION_MS);
	}

    @Override
    public void removeDeviceLocally(RemovedListener removedListener) {
    	
        mDeviceStore.removeDevice(mSendDeviceId);
        removedListener.onDeviceRemoved(mSendDeviceId, true);
        mSendDeviceId = Device.GROUP_ADDR_BASE;
        removedListener = null;        
    }

	@Override
	public String getBridgeAddress() {
		if (mConnected) {
            return mConnectedDevices.toString();
		}
		else {
			return null;
		}
	}	

	@Override
	public void setDesiredTemperature(float celsius) {
		double kelvin = Utils.convertCelsiusToKelvin(celsius);
        mTemperatureToSend = new DesiredAirTemperature((float)kelvin);

        if (mSendDeviceId != Device.DEVICE_ID_UNKNOWN && mTemperatureToSend != null) {
            mPendingDesiredTemperatureRequest = true;
        }
        mMeshHandler.removeCallbacks(transmitTempCallback);
        mMeshHandler.postDelayed(transmitTempCallback, TRANSMIT_TEMPERATURE_PERIOD_MS);
	}


	@Override
	public List<Device> getDevices(int ... modelNumber) {
		ArrayList<Device> result = new ArrayList<Device>();
		for (Device dev : mDeviceStore.getAllSingleDevices()) {			
			if (((SingleDevice)dev).isAnyModelSupported(modelNumber)) {
				result.add((SingleDevice)dev);
			}
		}
		return result;
	}
	
	@Override
	public ArrayList<String> getModelsLabelSupported(int deviceId) {
		
		Device device =mDeviceStore.getDevice(deviceId);
		if (device instanceof SingleDevice) {
			return ((SingleDevice)device).getModelsLabelSupported();
		}
		return null;
	}
	 
   
	@Override
	public List<Device> getGroups() {
		return mDeviceStore.getAllGroups();
	}

	@Override
	public void getDeviceData(DataListener listener) {
		this.mDataListener = listener;
		mService.setContinuousLeScanEnabled(true);
		DeviceInfoProtocol.requestDeviceInfo(mSendDeviceId);
	}
	
	public Handler getMeshHandler(){
		return mMeshHandler;
	}

	@Override
	public void startUITimeOut() {
		mMeshHandler.postDelayed(progressTimeOut, PROGRESS_DIALOG_TIME_MS);
		
	}

	@Override
	public void stopUITimeOut() {
		mMeshHandler.removeCallbacks(progressTimeOut);
	}

	@Override
	public void setContinuousScanning(boolean enabled) {
		mService.setContinuousLeScanEnabled(enabled);
	}

	@Override
	public void setTemperatureListener(TemperatureListener listener) {
		this.mTemperatureListener = listener;
	}

	@Override
	public void postRunnable(Runnable checkScanInfoRunnable) {
		getMeshHandler().post(checkScanInfoRunnable);
	}

	@Override
	public void removeRunnable(Runnable checkScanInfoRunnable) {
		getMeshHandler().removeCallbacks(checkScanInfoRunnable);		
	}

	@Override
	public int getMaxTTLForMASP() {
		return MAX_TTL_MASP;
	}

	@Override
	public int getTTLForMCP() {
		return mService.getTTL();
	}

	@Override
	public void setTTLForMCP(int ttl) {
		// set ttl to the library
		mService.setTTL(ttl);
		Setting settings =mDeviceStore.getSetting();
		settings.setTTL(ttl);
		
		// save new settings with the new TTL value
		mDeviceStore.setSetting(settings, true);
	}
	
	/**
	 * Notify device association has finished.
	 * @param success
	 */
	private void deviceAssociated(boolean success, String message) {
        // device associated so we need to clean the transaction id.
        mAssociationTransactionId = -1;

		if (mAssListener != null) {
            mAssListener.deviceAssociated(success, message);
        }
		removeNotificationFragment();
		if (success) {
			// Reload configuration fragment.
            mNavListener.refreshConfigFragment();
		}
	}
	
	private void notifyAssociationFragment(int progress) {
		showNotificationFragment(getString(R.string.association_progress) + progress + "%",
                getString(R.string.association_notify),
                SimpleNavigationListener.POSITION_ASSOCIATION, true, false);
	}

    public TemperatureStatus getTemperatureStatus() {
        return mTemperatureStatus.get(mSendDeviceId);
    }

    public void activateAttentionMode(int uuidHash, boolean enabled) {

        // enable the new uuidHash.
        mService.setAttentionPreAssociation(uuidHash, enabled, ATTRACTION_DURATION_MS);

        // notify the user.
        if (enabled) {
            Toast.makeText(MainActivity.this, getString(R.string.attraction_enabled), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmReplacingDatabase(final String json) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_replacing_db)).setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new Thread(new Runnable() {

                            @Override
                            public void run() {

                                // read json and set to the database.
                                final boolean success = mDeviceStore.setConfigurationFromJson(json.toString(),mDeviceStore.getSetting().getNetworkKey());
                                runOnUiThread(new Runnable() {
                                    public void run() {

                                        // notify to the user.
                                        Toast.makeText(getApplicationContext(), success?getString(R.string.import_config_complete):getString(R.string.import_config_error), Toast.LENGTH_SHORT).show();

                                        // reload settings.
                                        SharedPreferences activityPrefs = getPreferences(Activity.MODE_PRIVATE);
                                        int lastIdUsed = activityPrefs.getInt(SETTING_LAST_ID, Setting.UKNOWN_ID);
                                        restoreSettings(lastIdUsed);

                                        // reload configuration fragment.
                                        mNavListener.refreshConfigFragment();
                                    }
                                });
                            }
                        }).start();

                    }
                }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


}
