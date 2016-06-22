
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.csr.csrmeshdemo.listeners.AssociationListener;
import com.csr.csrmeshdemo.listeners.AssociationStartedListener;
import com.csr.csrmeshdemo.DeviceController;
import com.csr.csrmeshdemo.MainActivity;
import com.csr.csrmeshdemo.R;
/**
 * Fragment used to discover and associate devices. Devices can be associated by tapping them or by pressing the QR code
 * button and scanning a QR code that will provide the UUID and authorisation code. If authorisation has been enabled on
 * the security settings screen then when a UUID is tapped the short code is prompted for.
 * 
 */
public class AssociationFragment extends Fragment implements AssociationListener, AssociationStartedListener {
    private static final int MAX_SHORT_CODE_LENGTH = 24;
    private DeviceController mController;
    private ArrayList<ScanInfo> mNewDevices = new ArrayList<ScanInfo>();
    private HashMap<Integer, Appearance> mAppearances = new HashMap<Integer, Appearance>();

    // Time to wait until check if the info of the devices is valid.
    // This controls how quickly devices are removed from the list if no longer seen.
    private static final int CHECKING_SCAN_INFO_TIME_MS = (15 * 1000);

    private static final int MAX_TTL = 128 - 1;

    private UuidResultsAdapter resultsAdapter;
    private ProgressDialog mProgress = null;

    private CheckBox mSortCheckBox;

    private AssociationFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mFragment = this;
        startCheckingScanInfo();
       
    }

    /**
     * Start checking if the list of devices we are displaying contains a valid info or should be removed from the list.
     */
    private void startCheckingScanInfo() {
    	mController.postRunnable(checkScanInfoRunnable);
	}
    
    /**
     * Stop checking if the list of devices we are displaying contains a valid info or should be removed from the list.
     */
    private void stopCheckingScanInfo(){
    	mController.removeRunnable(checkScanInfoRunnable);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.association_fragment, container, false);

        resultsAdapter = new UuidResultsAdapter(getActivity(), mNewDevices);
        ListView listView = (ListView) view.findViewById(R.id.device_list);
        listView.setAdapter(resultsAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int hash = mNewDevices.get(position).uuidHash;
                mController.activateAttentionMode(hash, true);

                return true;
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int hash = mNewDevices.get(position).uuidHash;
                
                if (mController.isAuthRequired()) {
                    associateShortCode(hash, position);
                }
                else {
                    mController.associateDevice(hash,null);
                    mNewDevices.remove(position);
                    resultsAdapter.notifyDataSetChanged();
                }
            }
        });

        final Button mQrButton = (Button) view.findViewById(R.id.qr_code);
        mQrButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.associateWithQrCode(mFragment);
            }
        });

        mSortCheckBox = (CheckBox)view.findViewById(R.id.sort_check);
        mSortCheckBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSortCheckBox.isChecked()) {
                    resultsAdapter.notifyDataSetChanged();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setProgressBarIndeterminateVisibility(true);
        mController.discoverDevices(true, this);
        startCheckingScanInfo();
    }

    @Override
    public void onStop() {
        super.onStop();
        mController.discoverDevices(false, null);
        getActivity().setProgressBarIndeterminateVisibility(false);
        stopCheckingScanInfo();
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
    public void newUuid(UUID uuid, int uuidHash, int rssi, int ttl) {
        boolean existing = false;
        for (ScanInfo info : mNewDevices) {
            if (info.uuid.equalsIgnoreCase(uuid.toString())) {
                info.rssi = rssi;
                info.ttl = ttl;
                // check if we already have appearance info according with the uuidHash
                if (mAppearances.containsKey(uuidHash)) {
                	info.setAppearance(mAppearances.get(uuidHash));
                }
                info.updated();
                resultsAdapter.notifyDataSetChanged();
                existing = true;
                break;
            }
        }
        if (!existing) {
        	ScanInfo info = new ScanInfo(uuid.toString().toUpperCase(), rssi, uuidHash,ttl);
        	// check if we already have appearance info according with the uuidHash
        	if (mAppearances.containsKey(uuidHash)) {
            	info.setAppearance(mAppearances.get(uuidHash));
            }
            mNewDevices.add(info);        
            resultsAdapter.notifyDataSetChanged();
        }        
    }
    
	@Override
	public void newAppearance(int uuidHash, byte[] appearance,String shortName) {
        for (ScanInfo info : mNewDevices) {
            if (info.uuidHash == uuidHash) {
            	info.setAppearance(new Appearance(appearance, shortName));
                info.updated();
                resultsAdapter.notifyDataSetChanged();
                break;
            }
        }
        mAppearances.put(uuidHash, new Appearance(appearance, shortName));
		
	}

    /**
     * Show modal progress dialogue whilst associating a device.
     */
    private void showProgress() {
        getActivity().setProgressBarIndeterminateVisibility(false);
        if (mProgress == null) {
	        mProgress = new ProgressDialog(getActivity());
	        mProgress.setMessage(getActivity().getString(R.string.associating));
	        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mProgress.setIndeterminate(false);
	        mProgress.setCancelable(false);
	        mProgress.show();
	        mProgress.setMessage("Sending association request");
        }
    }

    /**
     * Hide the progress dialogue when association is finished.
     */
    private void hideProgress() {
    	if (mProgress != null) {
    		mProgress.dismiss();
        	mProgress=null;
    	}
        getActivity().setProgressBarIndeterminateVisibility(true);
    }

    /**
     * Associate a device after first prompting for a short code.
     * 
     * @param hash
     *            The 31-bit UUID hash of the device to associate.
     * @param position
     *            Position of device in ListView.
     */
    private void associateShortCode(final int hash, final int position) {
        final AlertDialog.Builder shortCodeDialog = new AlertDialog.Builder(getActivity());
        shortCodeDialog.setTitle(getActivity().getString(R.string.short_code_prompt));
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_SHORT_CODE_LENGTH) });
        shortCodeDialog.setView(input);

        shortCodeDialog.setPositiveButton(getActivity().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (mController.associateDevice(hash & 0x7FFFFFFF, input.getText().toString())) {
                        mNewDevices.remove(position);
                        resultsAdapter.notifyDataSetChanged();
                    }
                    else {
                        Toast.makeText(getActivity(), getActivity().getString(R.string.short_code_match_fail), Toast.LENGTH_LONG).show();
                    }
                }
                catch (IllegalArgumentException e) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.shortcode_invalid), Toast.LENGTH_LONG).show();
                }
            }
        });
        shortCodeDialog.setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = shortCodeDialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        input.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private boolean deletingHyphen;
            private int hyphenStart;
            private boolean deletingBackward;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing.
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (isFormatting)
                    return;

                // Make sure user is deleting one char, without a selection
                final int selStart = Selection.getSelectionStart(s);
                final int selEnd = Selection.getSelectionEnd(s);
                if (s.length() > 1 // Can delete another character
                        && count == 1 // Deleting only one character
                        && after == 0 // Deleting
                        && s.charAt(start) == '-' // a hyphen
                        && selStart == selEnd) { // no selection
                    deletingHyphen = true;
                    hyphenStart = start;
                    // Check if the user is deleting forward or backward
                    if (selStart == start + 1) {
                        deletingBackward = true;
                    }
                    else {
                        deletingBackward = false;
                    }
                }
                else {
                    deletingHyphen = false;
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (isFormatting)
                    return;

                isFormatting = true;

                // If deleting hyphen, also delete character before or after it
                if (deletingHyphen && hyphenStart > 0) {
                    if (deletingBackward) {
                        if (hyphenStart - 1 < text.length()) {
                            text.delete(hyphenStart - 1, hyphenStart);
                        }
                    }
                    else if (hyphenStart < text.length()) {
                        text.delete(hyphenStart, hyphenStart + 1);
                    }
                }
                if ((text.length() + 1) % 5 == 0) {
                    text.append("-");
                }

                isFormatting = false;

                if (text.length() < MAX_SHORT_CODE_LENGTH) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });

    }

    @Override
    public void deviceAssociated(boolean success, String message) {
        hideProgress();

        // notify the user.
        if (message != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void associationStarted() {
        // Association was triggered by MainActivity, so display progress dialogue.
    	showProgress();
    }
    
    private class UuidResultsAdapter extends BaseAdapter {
        private Activity activity;
        private ArrayList<ScanInfo> data;
        private LayoutInflater inflater = null;

        public UuidResultsAdapter(Activity a, ArrayList<ScanInfo> object) {
            activity = a;
            data = object;
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView nameText;
            TextView rssiText;
            TextView appearanceText;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.uuid_list_row, null);
                nameText = (TextView) convertView.findViewById(R.id.ass_uuid);
                rssiText = (TextView) convertView.findViewById(R.id.ass_rssi);
                appearanceText = (TextView) convertView.findViewById(R.id.apperance_text);
                convertView.setTag(new ViewHolder(nameText, rssiText, appearanceText));
            }
            else {
                ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                nameText = viewHolder.uuid;
                rssiText = viewHolder.rssi;
                appearanceText = viewHolder.appearanceText;
            }

            ScanInfo info = (ScanInfo) data.get(position);            
            nameText.setText(info.uuid); 
            int hops = MAX_TTL - info.ttl;
            if (hops == 0) {
                rssiText.setText(String.valueOf(info.rssi) + "dBm");
            }
            else if (hops == 1) {
                rssiText.setText(String.valueOf(info.rssi) + "dBm " + hops + " hop");
            }
            else {
                rssiText.setText(String.valueOf(info.rssi) + "dBm " + hops + " hops");
            }
            appearanceText.setText(info.getAppearance() != null ? info.getAppearance().mShortName : "?");
            
            return convertView;
        }
        
        @Override
        public void notifyDataSetChanged() {
        	// Sort in ascending order by hops, then RSSI.
            if (mSortCheckBox.isChecked()) {
                Collections.sort(data);
            }

        	super.notifyDataSetChanged();
        }
        
        
    }
    
    private class Appearance {
    	private byte[] mAppearanceCode;
    	private String mShortName;

    	public Appearance(byte[] appearanceCode, String shortName) {
    		setAppearanceCode(appearanceCode);
    		setShortName(shortName);
    	}

		public String getShortName() {
			return mShortName;
		}

		public void setShortName(String mShortName) {
			this.mShortName = mShortName;
		}

		public byte[] getAppearanceCode() {
			return mAppearanceCode;
		}

		public void setAppearanceCode(byte[] mAppearanceCode) {
			this.mAppearanceCode = mAppearanceCode;
		}
    }
   
    private class ScanInfo implements Comparable<ScanInfo> {

    	private static final long TIME_SCAN_INFO_VALID_MS = 15 * 1000;

		public String uuid;        
        public int rssi;
        public int uuidHash;
        public long timeStamp;
        public int ttl;
        public Appearance appearance;

        public ScanInfo(String uuid, int rssi, int uuidHash, int ttl) {
            this.uuid = uuid;
            this.rssi = rssi;
            this.uuidHash = uuidHash;
            this.ttl = ttl;
            updated();
        }

		public void updated() {
			this.timeStamp = System.currentTimeMillis();
		}

		@Override
		public int compareTo(ScanInfo info) {
            final int LESS_THAN = -1;
            final int GREATER_THAN = 1;
            final int EQUAL = 0;

            // Compare to is used for sorting the list in ascending order.
            // Smaller number of hops (highest TTL) should be at the top of the list.
            // For items with the same TTL, largest signal strength (highest RSSI) should be at the top of the list.
			if (this.ttl > info.ttl) {
                return LESS_THAN;
            }
            else if (this.ttl < info.ttl) {
                return GREATER_THAN;
            }
            else if (this.rssi > info.rssi) {
                return LESS_THAN;
            }
			else if (this.rssi < info.rssi) {
                return GREATER_THAN;
            }
            else {
                return EQUAL;
            }
		}

		public Appearance getAppearance() {
			return appearance;
		}
		
		/**
		 * This method check if the timeStamp of the last update is still valid or not (time<TIME_SCANINFO_VALID).
		 * @return true if the info is still valid
		 */
		public boolean isInfoValid(){
			return ((System.currentTimeMillis()-this.timeStamp) < TIME_SCAN_INFO_VALID_MS);
		}
		
		public void setAppearance(Appearance newAppearance) {
			appearance = newAppearance;
		}
    }
    
    private static class ViewHolder {
        public final TextView uuid;
        public final TextView rssi;   
        public final TextView appearanceText;

        public ViewHolder(TextView uuid, TextView rssi, TextView appearanceText) {
            this.uuid = uuid;
            this.rssi = rssi;
            this.appearanceText = appearanceText;
        }
    }
    static String TAG_PROGRESS="AssociationProgress";
	@Override
	public void associationProgress(final int progress, final String message) {
		
		// different cases that we should avoid to set the progress
		if (progress < 0 || progress > 100 || mProgress == null || !mProgress.isShowing()) {
			return;
		}
		Log.d(TAG_PROGRESS,message);
		// run in the UI thread
		getActivity().runOnUiThread(new Runnable() {
		    public void run() {
		    	mProgress.setProgress(progress);
		    	// we don't change the message anymore
		    	//mProgress.setMessage(message);  
		    	
		    }
		});
		
		
		
	}
	
	/**
	 * Runnable which checks if the info of the list of the devices (scan info list) is still valid or should be removed from the list.
	 */
	private Runnable checkScanInfoRunnable = new Runnable() {
        @Override
        public void run() {
        	Iterator<ScanInfo> it = mNewDevices.iterator();
        	while(it.hasNext()){
        		ScanInfo info=it.next();
        		if(!info.isInfoValid()){
        			it.remove();
        		}
        	}
        	resultsAdapter.notifyDataSetChanged();
        	((MainActivity)getActivity()).getMeshHandler().postDelayed(checkScanInfoRunnable, CHECKING_SCAN_INFO_TIME_MS);
        }        
    };

}
