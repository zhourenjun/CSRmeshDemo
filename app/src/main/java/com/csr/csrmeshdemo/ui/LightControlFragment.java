/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.csr.csrmeshdemo.DeviceState.StateType;
import com.csr.csrmeshdemo.R.id;
import com.csr.csrmeshdemo.R.layout;
import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.entities.SingleDevice;
import com.csr.csrmeshdemo.DeviceAdapter;
import com.csr.csrmeshdemo.DeviceController;
import com.csr.csrmeshdemo.DevicesComparator;
import com.csr.csrmeshdemo.LightState;
import com.csr.csrmeshdemo.PowState;
import com.csr.csrmeshdemo.R;
import com.csr.mesh.LightModelApi;
import com.csr.mesh.PowerModelApi.PowerState;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;

/**
 * Fragment that allows controlling the colour of lights using HSV colour wheel.
 * 
 */
public class LightControlFragment extends Fragment {
    public static final String TAG = "LightControlFragment";

    private View mRootView;
    private HSVCircle mColorWheel = null;
    private SeekBar mBrightSlider = null;
    private DeviceController mController;
    private DeviceAdapter mDeviceListAdapter;
    private Spinner mDeviceSpinner = null;
    private Switch mPowerSwitch = null;
    
    private int mCurrentColor = Color.rgb(0, 0, 0);

    private boolean mEnableEvents = true;
    private boolean mEnablePowerSwitchEvent = true;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            // The last two arguments ensure LayoutParams are inflated properly.
            mRootView = inflater.inflate(R.layout.light_control_tab, container, false);

            mColorWheel = (HSVCircle) mRootView.findViewById(R.id.colorWheel);
            mColorWheel.setOnTouchListener(colorWheelTouch);

            mBrightSlider = (SeekBar) mRootView.findViewById(R.id.seekBrightness);
            mBrightSlider.setOnSeekBarChangeListener(brightChange);

            mDeviceSpinner = (Spinner) mRootView.findViewById(R.id.spinnerLight);
            
            mPowerSwitch = (Switch) mRootView.findViewById(R.id.powerSwitch);
            mPowerSwitch.setOnCheckedChangeListener(powerChange);
        }
        return mRootView;
    }

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
    public void onStart() {
        super.onStart();
        if (mDeviceListAdapter == null) {
            mDeviceListAdapter = new DeviceAdapter(getActivity());
            mDeviceSpinner.setAdapter(mDeviceListAdapter);
            mDeviceSpinner.setOnItemSelectedListener(deviceSelect);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mColorWheel != null) {
            mColorWheel.onDestroyView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDeviceListAdapter.clear();        
    }

    @Override
    public void onResume() {
        super.onResume();

        loadDevices();
    }

    private void loadDevices() {
    	List<Device> groups = mController.getGroups();

        for (Device dev : groups) {
        	if (dev.getDeviceId() == 0) {
        		dev.setName(getString(R.string.all_lights));
        	}
            mDeviceListAdapter.addDevice(dev);
        }

        // Add individual lights already associated.
        List<Device> lights = mController.getDevices(LightModelApi.MODEL_NUMBER);
        // sort devices list.
        Collections.sort(lights, new DevicesComparator());
        // add devices to adapter.
        for (Device dev : lights) {
            mDeviceListAdapter.addDevice(dev);
        }

        selectSpinnerDevice();
		
	}

	/**
     * Called when a colour is selected on the colour wheel.
     */
    protected OnTouchListener colorWheelTouch = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {                
                int x = (int) event.getX();
                int y = (int) event.getY();

                HSVCircle circleView = (HSVCircle) v;

                int pixelColor = 0;
                try {
                    pixelColor = circleView.getPixelColorAt(x, y);
                }
                catch (IndexOutOfBoundsException e) {
                    return true;
                }

                // Don't use values from the background of the image (outside the wheel).
                if (Color.alpha(pixelColor) < 0xFF) {
                    return true;
                }

                // Force power button on.
                mEnablePowerSwitchEvent = false;
                mPowerSwitch.setChecked(true);
                // Save the new power state but don't send a message to the device.
                mController.setLocalLightPower(PowerState.ON);
                mEnablePowerSwitchEvent = true;
                
                // The cursor is a small circle drawn over the colour wheel image over the selected colour.
                // Set the cursor position and invalidate the imageView so that it is redrawn.
                mColorWheel.setCursor(x, y);
                mColorWheel.invalidate();

                // Extract the R,G,B from the colour.
                mCurrentColor = Color.rgb(Color.red(pixelColor), Color.green(pixelColor), Color.blue(pixelColor));
                mController.setLightColor(mCurrentColor, mBrightSlider.getProgress());
                return true;
            }
            else {
                return false;
            }
        }
    };

    /**
     * Called when the brightness slider changes position.
     */
    protected OnSeekBarChangeListener brightChange = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // No behaviour.
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // No behaviour.
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!mEnableEvents)
                return;
            
            // Force power button on.
            mEnablePowerSwitchEvent = false;
            mPowerSwitch.setChecked(true);
            // Save the new power state but don't send a message to the device.
            mController.setLocalLightPower(PowerState.ON);
            mEnablePowerSwitchEvent = true;
            
            // Set a new colour to send.
            mController.setLightColor(mCurrentColor, progress);
        }
    };

    /**
     * Called when power button is pressed.
     */
    protected OnCheckedChangeListener powerChange = new OnCheckedChangeListener() {
        
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mEnablePowerSwitchEvent) {
                mController.setLightPower(isChecked ? PowerState.ON : PowerState.OFF);
            }
        }
    };
    
    /**
     * Event handler for when a new device is selected from the Spinner.
     * 
     * @param position
     *            Position within Spinner of selected device.
     */
    protected void deviceSelected(int position) {
        if (position == 0) {
            mController.setSelectedDeviceId(0);
            updateControls(0);
        }
        else {
            int deviceId = mDeviceListAdapter.getItemDeviceId(position);
            mController.setSelectedDeviceId(deviceId);
            updateControls(deviceId);
        }
    }
    
    /**
     * Update the UI with the state of the selected device, if its state is known.
     * 
     * @param selectedDeviceId
     *            The device id of the selected device.
     */
    protected void updateControls(int selectedDeviceId) {        
        Device dev = mController.getDevice(selectedDeviceId);
        
        if (dev != null) {
        	LightState state = (LightState)dev.getState(StateType.LIGHT);
        	PowState powState = (PowState)dev.getState(StateType.POWER);
            // Set UI to reflect the selected light's state if the state is valid.
            mEnableEvents = false;
            mEnablePowerSwitchEvent = false;
            mPowerSwitch.setChecked(powState.getPowerState() == PowerState.ON);
            mEnablePowerSwitchEvent = true;
            if (state.isStateKnown()) {
                int r = (int) state.getRed() & 0xFF;
                int g = (int) state.getGreen() & 0xFF;
                int b = (int) state.getBlue() & 0xFF;
                float[] hsv = new float[3];
                Color.RGBToHSV(r, g, b, hsv);
                mColorWheel.setCursor(hsv[0], hsv[1]);
                mBrightSlider.setProgress((int) (hsv[2] * 100.0f));
                mCurrentColor = Color.rgb(r, g, b);
                mColorWheel.invalidate();
            }
            else {
                mColorWheel.setCursor(0, 0);
                mColorWheel.invalidate();
            }
            mEnableEvents = true;
        }
        else {
            mEnableEvents = false;
            mBrightSlider.setProgress(100);
            mEnableEvents = true;
        }
    }

    protected void enableEvents(boolean enabled) {
        mEnableEvents = enabled;
    }

    /**
     * Called when a new device is selected from the spinner.
     */
    private OnItemSelectedListener deviceSelect = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            deviceSelected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    /**
     * Get the selected device id and set the spinner to it.
     */
    protected void selectSpinnerDevice() {
        int selectedDeviceId = mController.getSelectedDeviceId();        
        if (selectedDeviceId != Device.DEVICE_ID_UNKNOWN) {
    		Device dev = mController.getDevice(selectedDeviceId);
    		if (dev instanceof SingleDevice && 
    			((SingleDevice)dev).isModelSupported(LightModelApi.MODEL_NUMBER)) {
    			mDeviceSpinner.setSelection(mDeviceListAdapter.getDevicePosition(selectedDeviceId), true);
    		}
        }
        else {
            // No active device, so select the first device in the spinner if there is one.
            if (mDeviceListAdapter.getCount() > 0) {
                if (mDeviceSpinner.getSelectedItemPosition() == 0) {
                    // Make sure the event handler is called even if index zero was already selected.
                    updateControls(mDeviceListAdapter.getItemDeviceId(0));
                }
                else {
                    mDeviceSpinner.setSelection(0);
                }
            }
        }
    }
    
    /**
	 * Reload the displayed devices and groups.
	 */
	public void refreshUI() {
		mDeviceListAdapter.clear(); 
		loadDevices();
	}

}
