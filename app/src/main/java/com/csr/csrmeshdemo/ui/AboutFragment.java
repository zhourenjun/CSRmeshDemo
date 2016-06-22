
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import android.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.R.id;
import com.csr.csrmeshdemo.R.layout;
import com.csr.csrmeshdemo.R.string;

/**
 * Fragment to show app version number.
 * 
 */
public class AboutFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.about_fragment, container, false);

        final TextView version = (TextView) rootView.findViewById(R.id.textVersion);

        String versionName = getActivity().getString(R.string.unknown);
        try {
            versionName =
                    getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e) {
            // Do nothing.
        }

        version.setText(getActivity().getString(R.string.app_name) + " version " + versionName);

        return rootView;
    }
}
