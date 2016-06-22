
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.csr.csrmeshdemo.DeviceController;
import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.R.id;
import com.csr.csrmeshdemo.R.layout;
import com.csr.csrmeshdemo.R.string;

/**
 * Fragment to allow user to set network phrase and enable/disable authorisation.
 * 
 */
public class SecuritySettingsFragment extends Fragment {

    private DeviceController mController;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String phrase = mController.getNetworkKeyPhrase();
        boolean auth = mController.isAuthRequired();
        View view = inflater.inflate(R.layout.security_settings_fragment, container, false);
        Button okBtn = (Button) view.findViewById(R.id.network_assocoiation_ok);
        final CheckBox deviceAuthenticated = (CheckBox) view.findViewById(R.id.checkbox);
        deviceAuthenticated.setChecked(auth);
        final EditText ttl = (EditText) view.findViewById(R.id.ttl);
        final EditText passPhraseView = (EditText) view.findViewById(R.id.network_pass);
        final TextView bridgeAddress = (TextView)view.findViewById(R.id.textBridgeAddress);
        bridgeAddress.setText(mController.getBridgeAddress());
        ttl.setText("" +mController.getTTLForMCP());
        if (phrase != null) {
            passPhraseView.setText(phrase);
        }
        okBtn.setClickable(true);
        
        okBtn.setOnClickListener(new OnClickListener() {                
            @Override
            public void onClick(View v) {
            	int newTTL = -1;
            	try {
            		newTTL = Integer.parseInt(ttl.getText().toString());
            	} catch(Exception e) {}
            	
               if (newTTL < 0 || newTTL > 0xFF) { 
            	   Toast.makeText(getActivity(), "Time to live (TTL) value can't be lower than 0 or higher than 127", Toast.LENGTH_SHORT)
                   .show();
               }else if (passPhraseView.getText().toString().trim().length() > 0) {
                    // Hide soft keyboard.
                    InputMethodManager imm =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(passPhraseView.getWindowToken(), 0);
                    // Tell MainActivity about security settings. This will also switch to another Fragment.
                    mController.setSecurity(passPhraseView.getText().toString(), deviceAuthenticated.isChecked());
                    mController.setTTLForMCP(newTTL);
                }
                else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.key_required), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        return view;
    }
}
