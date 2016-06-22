
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import java.util.ArrayList;
import java.util.List;

import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.listeners.AssociationListener;
import com.csr.csrmeshdemo.listeners.AssociationStartedListener;
import com.csr.csrmeshdemo.listeners.DataListener;
import com.csr.csrmeshdemo.listeners.GroupListener;
import com.csr.csrmeshdemo.listeners.InfoListener;
import com.csr.csrmeshdemo.listeners.RemovedListener;
import com.csr.csrmeshdemo.listeners.TemperatureListener;
import com.csr.mesh.PowerModelApi.PowerState;


	/**
	 * Defines an interface that Fragments can use to communicate with the Activity.
	 * 
	 */
	public interface DeviceController {

        /**
         * Discover devices that are advertising to be associated.
         */
        public void discoverDevices(boolean enabled, AssociationListener listener);

        /**
         * Associate a device.
         *
         * @param uuidHash
         *            31-bit uuid hash of device.
         * @param shortCode (it can be null)
         *            Device's short code.
         * @return True if deviceHash matches short code deviceHash, false if it didn't match and association was cancelled.
         */
        public boolean associateDevice(int uuidHash, String shortCode);

            /**
             * Activate Attention mode.
             * @param uuidHash
             * @param enabled Enable or disabled the mode.
             */
        public void activateAttentionMode(int uuidHash, boolean enabled);

        /**
         * Get a list of devices that support one or more models.
         *
         * @param modelNumber
         *            A variable length argument list of models.
         * @return List of Device objects that belong to at least one of the models in modelNumber.
         */
        public List<Device> getDevices(int ... modelNumber);

        /**
         * Get a list of groups.
         *
         * @return List of Device objects, each on representing a group.
         */
        public List<Device> getGroups();

        /**
         * Get an associated device by device id.
         *
         * @param deviceId
         *            The device id to look for.
         * @return Device object representing the device.
         */
        public Device getDevice(int deviceId);

        /**
         * Set the name of a device or a group.
         *
         * @param deviceId
         *            Id of device or group to modify.
         * @param name
         *            The new name for the device or group.
         */
        public void setDeviceName(int deviceId, String name);

        /**
         * Get the currently selected device.
         *
         * @return Device object representing the selected device.
         */
        public int getSelectedDeviceId();

        /**
         * Set group membership for the currently selected device.
         *
         * @param groups
         *            List of groups to assign the current device to. List should contain integer group ids.
         * @param listener
         *            The listener that will receive updates when the group assignment is complete.
         */
        public void setDeviceGroups(List<Integer> groups, GroupListener listener);

        /**
         * Add a new light group.
         *
         * @param groupName
         *            The name for the new group.
         * @return The Device object representing the group.
         */
        public Device addLightGroup(String groupName);

        /**
         * Send a colour to the light.
         *
         * @param color
         *            RGB hue and saturation of light encoded as an int.
         * @param brightness
         *            Value that will be added to the hue and saturation (color).
         */
        public void setLightColor(int color, int brightness);

        /**
         * Send command to a light to set the power state.
         *
         * @param state
         *          State to set.
         */
        public void setLightPower(PowerState state);

        /**
         * Set power of device locally but don't send a command to remote device.
         * Used because RGB command has a side effect on the remote light that it also set the power
         * state to on if it is currently off.
         *
         * @param state
         * 			State to set.
         */
        public void setLocalLightPower(PowerState state);


        /**
         * Set which device to send commands to.
         *
         * @param deviceId
         *            Device id.
         */
        public void setSelectedDeviceId(int deviceId);

        /**
         * Reset the currently selected device (remove association).
         */
        public void removeDevice(RemovedListener listener);

        /**
         * Remove device locally without sending a message to remove association on remote device.
         *
         */
        public void removeDeviceLocally(RemovedListener listener);

        /**
         * Get the firmware version of the currently selected device. To receive the returned state a Fragment must have
         * registered itself via <code>registerForLightConfigUpdates</code>
         */
        public void getFwVersion(InfoListener listener);

        /**
         * Set security settings.
         *
         * @param networkKeyPhrase
         *            The phrase used to generate the network key.
         * @param authRequired
         *            True if authorisation should be used when associating (will cause short code to be prompted for).
         */
        public void setSecurity(String networkKeyPhrase, boolean authRequired);

        /**
         * Find out if authorisation is currently enabled.
         *
         * @return True if authorisation is required.
         */
        public boolean isAuthRequired();

        /**
         * Get the set network key phrase used to generate the network key.
         *
         * @return The network phrase.
         */
        public String getNetworkKeyPhrase();

        /**
         * Open the QR code reader, and start association with the device retrieved from the QR code.
         *
         * @param listener
         *            Call back to indicate to caller that association was started with a device.
         */
        public void associateWithQrCode(AssociationStartedListener listener);

        /**
         * Control if a devices attention mechanism (e.g. flashing a light) is enabled or not.
         * @param enabled True if attention should be enabled.
         */
        public void setAttentionEnabled(boolean enabled);


        /**
         * Get the address of the mesh bridge device that is currently connected.
         * @return String containing bridge device Bluetooth address.
         */
        public String getBridgeAddress();


        /**
         * Get the labels of the models supported by the device.
         * @param deviceId
         * @return list of models supported
         */
        public ArrayList<String> getModelsLabelSupported(int deviceId);


         /**
         * Get the VID, PID and Version of the currently selected device. To receive the returned state a Fragment must have
         * registered itself via <code>registerForLightConfigUpdates</code>
         */
        public void getVID_PID_VERSION(InfoListener mMainFragment);

        /**
         * Get info from a device using the Data model.
         */
        public void getDeviceData(DataListener listener);


        /**
         * Call to this method to start the timer for the progress dialog we show in request data.
         */
        public void startUITimeOut();


        /**
         * Call to this method to cancel the timer for the progress dialog we show in request data.
         */
        public void stopUITimeOut();

        /**
         * Broadcast a new desired temperature range.
         * @param temperature Desired temperature value.
         */
        public void setDesiredTemperature(float temperature);

        /**
         * Set the listener for the call backs implemented by TemperatureFragment
         *
         * @param listener
         */
        public void setTemperatureListener(TemperatureListener listener);

        /**
         * Enable or disable continuous scanning for BLE adverts (and hence mesh packets).
         * If this is disabled then scanning for BLE adverts is only enabled for a fixed time period after
         * a packet is sent.
         *
         * @param ennabled
         */
        public void setContinuousScanning(boolean ennabled);



        /**
         * Ask to the Activity for posting a Runnable method in a handler.
         * @param checkScanInfoRunnable
         */
        public void postRunnable(Runnable checkScanInfoRunnable);

        /**
         * Ask to the Activity for remove a Runnable method from a handler.
         * @param checkScanInfoRunnable
         */
        public void removeRunnable(Runnable checkScanInfoRunnable);


        /**
         * Get the models supported and store in the database.
         * @param listener
         */
        public void requestModelsSupported(InfoListener listener);



        public int getMaxTTLForMASP();
        public int getTTLForMCP();
        public void setTTLForMCP(int ttl);

        public TemperatureStatus getTemperatureStatus();

        /**
         * Request to the MainActivity the current temperature and desired temperature from the new device selected.
         */
        public void requestCurrentTemperature();
    }

