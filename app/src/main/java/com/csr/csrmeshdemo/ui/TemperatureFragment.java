
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.csr.csrmeshdemo.DeviceAdapter;
import com.csr.csrmeshdemo.DeviceController;
import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.TemperatureControllerInterface;
import com.csr.csrmeshdemo.TemperatureStatus;
import com.csr.csrmeshdemo.listeners.TemperatureListener;
import com.csr.csrmeshdemo.entities.Device;
import com.csr.csrmeshdemo.entities.SingleDevice;
import com.csr.mesh.SensorModelApi;

/**
 * Fragment to show temperature control.
 * 
 */
public class TemperatureFragment extends Fragment implements TemperatureListener, TemperatureControllerInterface {
	
	// UI elements
	private Spinner mDeviceSpinner;
	private RelativeLayout mGetButton;
	private TextView mCurrentValue;
	
	// Controllers
	private DeviceAdapter mDeviceListAdapter;	
	private DeviceController mController;
	private TemperatureCircle mTemperatureControler;
	
	// Fragment variables
	private double mValue = TEMPERATURE_DEFAULT_VALUE;
	DecimalFormat mDecimalFactor = new DecimalFormat("0.0");
	
	// static values
	private static final double MAX_TEMPERATURE_VALUE = 50.0;
	private static final double MIN_TEMPERATURE_VALUE = 0.0;
	private static final double TEMPERATURE_DEFAULT_VALUE = 20.0;
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.temperature_fragment, container, false);
        
        mDeviceSpinner = (Spinner)rootView.findViewById(R.id.spinnerSensorGroup);
        mGetButton = (RelativeLayout)rootView.findViewById(R.id.buttonGet);
        mCurrentValue = (TextView)rootView.findViewById(R.id.currentValue);
        mTemperatureControler = (TemperatureCircle)rootView.findViewById(R.id.temperatureControler);
        mGetButton.setOnClickListener(mGetButtonListener);
        return rootView;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mController = (DeviceController) activity;
            mController.setTemperatureListener(this);
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DeviceController callback interface.");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (mDeviceListAdapter == null) {
            mDeviceListAdapter = new DeviceAdapter(getActivity());
            mDeviceSpinner.setAdapter(mDeviceListAdapter);
            mDeviceSpinner.setOnItemSelectedListener(deviceSelect);
        }
        // enable continue scanning
        mController.setContinuousScanning(true);
        
        mTemperatureControler.setTemperatureInterface(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();     
        
        // disable continue scanning
        mController.setContinuousScanning(false);
        mController.setTemperatureListener(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDeviceListAdapter.clear();        
    }

    @Override
    public void onResume() {
        super.onResume();

        List<Device> groups = mController.getGroups();
        List<Device> devices = mController.getDevices(SensorModelApi.MODEL_NUMBER);

        // just add the list of groups except the general group
        for (Device group : groups) {
        	
        	// add broadcast group
        	if (group.getDeviceId() == 0) {
        		group.setName(getString(R.string.all_thermostats));
        		mDeviceListAdapter.addDevice(group);
        	}
        	else {
        		for (Device dev : devices) {
        			SingleDevice singleDevice = (SingleDevice)dev;
        			if (singleDevice.getGroupMembershipValues().contains(group.getDeviceId())) {
        				mDeviceListAdapter.addDevice(group);

        				break;
        			}
        		}
        	}
        }
        
        selectSpinnerDevice();
        setNewValue(TEMPERATURE_DEFAULT_VALUE, false);
        
    }
    
    private OnClickListener mGetButtonListener = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			//nothing to do.
		}
	};
	
	
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
     * Event handler for when a new device is selected from the Spinner.
     * 
     * @param position
     *            Position within Spinner of selected device.
     */
    protected void deviceSelected(int position) {        
        int deviceId = mDeviceListAdapter.getItemDeviceId(position);
        
        mController.setSelectedDeviceId(deviceId);
        mController.requestCurrentTemperature();

        resetUI();
    }

    /**
     * This method request to the MainActivity about the last known temperature value for the current deviceId selected.
     */
    public void updateUIfromLastRequests() {
        TemperatureStatus temperatureStatus = mController.getTemperatureStatus();
        if (temperatureStatus != null) {
            if (temperatureStatus.getCurrentTemperature() != Double.MIN_VALUE) {
                mCurrentValue.setText(mDecimalFactor.format(temperatureStatus.getCurrentTemperature()) + "");
            }
            if (temperatureStatus.getDesiredTemperature() != Double.MIN_VALUE) {
                setNewValue(temperatureStatus.getDesiredTemperature(), false);
            }
            mTemperatureControler.setValueConfirmed(temperatureStatus.isDesiredTemperatureConfirmed());
        }
    }
    
    /**
     * Get the selected device id and set the spinner to it.
     */
    protected void selectSpinnerDevice() {
    	
        int selectedDeviceId = mController.getSelectedDeviceId();
        if (selectedDeviceId != Device.DEVICE_ID_UNKNOWN) {
        	Device dev = mController.getDevice(selectedDeviceId);
    		if (dev instanceof SingleDevice &&  ((SingleDevice)dev).isModelSupported(SensorModelApi.MODEL_NUMBER)) {
    			
    			int position = 0;
    			try {
    				position = mDeviceListAdapter.getDevicePosition(selectedDeviceId);
    			} catch (Exception e){}
    			mDeviceSpinner.setSelection(position, true);
    		}
    		else {
    			if (mDeviceListAdapter.getCount() > 0) {
                	mDeviceSpinner.setSelection(0);
                }
    		}
        }
        else {
            // No active device, so select the first device in the spinner if there is one.
            if (mDeviceListAdapter.getCount() > 0) {
            	mDeviceSpinner.setSelection(0);
            }
        }
        resetUI();
    }

    /**
     * Reset UI values and request latest current known temperature value for the current deviceId selected.
     */
	public void resetUI(){
		mCurrentValue.setText(getString(R.string.unknown_temperature_value));
        mTemperatureControler.setValueConfirmed(false);
        setNewValue(TEMPERATURE_DEFAULT_VALUE, false);

        updateUIfromLastRequests();
	}

	@Override
	public void buttonUpClicked() {
		double newValue = mValue + 0.5;
		if (newValue <= MAX_TEMPERATURE_VALUE) {
			setNewValue(newValue,true);
		}
	}

	@Override
	public void buttonDownClicked() {
		double newValue = mValue - 0.5;
		if (newValue >= MIN_TEMPERATURE_VALUE) {
			setNewValue(newValue,true);
		}
	}

	@Override
	public void buttonTextClicked() {
		showNumberInput();
	}
	
	private void showNumberInput() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		alert.setTitle(getString(R.string.desired_temperature));
		alert.setMessage(getString(R.string.type_temperature_desired));

		// Set an EditText view to get user input 
		final EditText input = new EditText(getActivity());
		input.setFocusable(true);
		input.setHint( getString(R.string.desired_temperature) + "(" + MIN_TEMPERATURE_VALUE + "-" + MAX_TEMPERATURE_VALUE +" degrees)");
		input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		alert.setView(input);

		alert.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  try {
			  double doubleVal = Double.parseDouble(value);
			  if (doubleVal >= 0 && doubleVal <= 50) {
				  setNewValue(doubleVal,true);
			  }
              else {
				  Toast.makeText(getActivity(), getString(R.string.desired_temperature_range_value).replace("%s1", "" + MIN_TEMPERATURE_VALUE).replace("%s2", ""+ MAX_TEMPERATURE_VALUE), Toast.LENGTH_SHORT).show();
			  }
		  } catch(Exception e){
			  Toast.makeText(getActivity(), getString(R.string.wrong_desired_temperature_value), Toast.LENGTH_SHORT).show();
		  }
		  }
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    dialog.dismiss();
		  }
		});

		alert.show();
		
	}

    /**
     * Print a new desired temperature value. If request is true, the value will be set to mesh network to be acknowledged.
     * @param value Desired temperature value (celsius)
     * @param request True, if we want to be sent to mesh. False, if we only want to update the UI.
     */
	private void setNewValue(double value, boolean request) {
		mValue = value;
		mTemperatureControler.setValue(mDecimalFactor.format(mValue));

        if (request) {
            mController.setDesiredTemperature((float) value);
            mTemperatureControler.setValueConfirmed(false);
        }
	}



    public void confirmDesiredTemperature() {
        mTemperatureControler.setValueConfirmed(true);
    }

    public void setCurrentTemperature(double value) {
        mCurrentValue.setText(mDecimalFactor.format(value));
    }

    public void setDesiredTemperature(double value) {
        setNewValue(value, false);
    }
}
