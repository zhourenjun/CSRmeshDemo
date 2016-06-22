
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.listeners;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.widget.Toast;

import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.ui.AboutFragment;
import com.csr.csrmeshdemo.ui.AssociationFragment;
import com.csr.csrmeshdemo.ui.GroupAssignFragment;
import com.csr.csrmeshdemo.ui.LightControlFragment;
import com.csr.csrmeshdemo.ui.SecuritySettingsFragment;
import com.csr.csrmeshdemo.ui.TemperatureFragment;

/**
 * Handles displaying the correct fragment when options are selected from the navigation menu in the action bar.
 * 
 */
public class SimpleNavigationListener implements ActionBar.OnNavigationListener {
    private FragmentManager mFragmentManager;
    private Activity mActivity;
    private int currentPosition = 0;
    private int mSavedPosition = POSITION_INVALID;

    private Fragment mCurrentFragment;

    // True if navigation away from network security settings is allowed.
    private boolean mEnableNavigation;

    public static final int POSITION_INVALID = -1;
    public static final int POSITION_LIGHT_CONTROL = 0;
    public static final int POSITION_TEMP_CONTROL = 1;
    public static final int POSITION_ASSOCIATION = 2;
    public static final int POSITION_GROUP_CONFIG = 3;
    public static final int POSITION_NETWORK_SETTINGS = 4;
    public static final int POSITION_ABOUT = 5;
    public static final int POSITION_MAX = POSITION_ABOUT;

    public SimpleNavigationListener(FragmentManager fragmentManager, Activity activity) {
        mFragmentManager = fragmentManager;
        mEnableNavigation = true;
        mActivity = activity;
        activity.setContentView(R.layout.main);
        
    }

    public void setNavigationEnabled(boolean enabled) {
        mEnableNavigation = enabled;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        mCurrentFragment = null;
        currentPosition = itemPosition;
        if (mEnableNavigation || itemPosition == POSITION_NETWORK_SETTINGS) {
            switch (itemPosition) {           
            case POSITION_LIGHT_CONTROL:
            	mCurrentFragment = new LightControlFragment();
                break;
            case POSITION_TEMP_CONTROL:
            	mCurrentFragment = new TemperatureFragment();
                break;
            case POSITION_ASSOCIATION:
            	mCurrentFragment = new AssociationFragment();
                break;            	
            case POSITION_GROUP_CONFIG:
            	mCurrentFragment = new GroupAssignFragment();
                break;
            case POSITION_NETWORK_SETTINGS:
            	mCurrentFragment = new SecuritySettingsFragment();
                break;                        
            case POSITION_ABOUT:
            	mCurrentFragment = new AboutFragment();
                break;
            }

            if (mCurrentFragment != null) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.listcontainer, mCurrentFragment);
                ft.commitAllowingStateLoss();
            }
            
        }
        else {
            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.key_required), Toast.LENGTH_SHORT)
                    .show();
            mActivity.getActionBar().setSelectedNavigationItem(POSITION_GROUP_CONFIG);
        }

        return true;
    }

	public int getCurrentPosition(){
        return currentPosition;
    }

    /**
     * Saves which fragment is currently selected. Multiple calls will overwrite the last saved position.
     */
    public void savePosition() {
        mSavedPosition = currentPosition;
    }

    /**
     * Saves selected fragment position. Multiple calls will overwrite the last saved position.
     */
    public void savePosition(int position) {
        if (mSavedPosition > POSITION_MAX) {
            throw new IllegalArgumentException("Position is out of range.");
        }
        mSavedPosition = position;
    }

    /**
     * Restores the last saved selected fragment position.
     */
    public void restorePosition() {
        if (mSavedPosition == POSITION_INVALID) {
            throw new IllegalStateException("Position has not been previously saved.");
        }
        ActionBar bar = mActivity.getActionBar();
        if (bar != null) {
            bar.setSelectedNavigationItem(mSavedPosition);
        }
    }

	/**
	 * Refresh the configuration fragment if this is currently active.
	 */
	public void refreshConfigFragment() {
		if (mCurrentFragment instanceof GroupAssignFragment) {
			GroupAssignFragment groupFragment = (GroupAssignFragment) mCurrentFragment;
			groupFragment.refreshUI();
		} 
		else if (mCurrentFragment instanceof LightControlFragment) {
			LightControlFragment lightFragment = (LightControlFragment) mCurrentFragment;
			lightFragment.refreshUI();
		}
	}
}
