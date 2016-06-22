
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csr.csrmeshdemo.R;

public class NotificationFragment extends Fragment {
	public static final String EXTRA_TITLE = "NOTIFYTITLE";
	public static final String EXTRA_SUBTITLE = "NOTIFYSUBTITLE";

	private NotificationFragmentCallbacks mDismissCallback;
	private String mTitle;
	private String mSubtitle;
	private TextView titleView, subtitleView;

	/*
	Call backs for state changes in the notification fragment.
	 */
	public interface NotificationFragmentCallbacks {
		void onNotificationDismiss();
		void onNotificationClick();
		boolean isNotificationClickEnabled();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mDismissCallback = (NotificationFragmentCallbacks)activity;
		}
		catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement NotificationFragmentCallbacks callback interface.");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {

		View containerView = inflater.inflate(R.layout.fragment_notification, container, false);

		Bundle args = getArguments();
		mTitle = args.getString(EXTRA_TITLE);
		mSubtitle = args.getString(EXTRA_SUBTITLE);

		// Get UI elements.
		titleView = (TextView)containerView.findViewById(R.id.title);
		subtitleView = (TextView)containerView.findViewById(R.id.subtitle);

		if (mTitle == null) {
			titleView.setVisibility(View.GONE);
		}
		else {
			titleView.setVisibility(View.VISIBLE);
			titleView.setText(mTitle);
		}

		if (mSubtitle == null) {
			subtitleView.setVisibility(View.GONE);
		}
		else {
			subtitleView.setVisibility(View.VISIBLE);
			subtitleView.setText(mSubtitle);
		}

		containerView.setOnTouchListener(new SwipeDismissViewTouchListener(containerView, mDismissCallback));
		

		containerView.setOnClickListener(mOnClickListener);
	    
		return containerView;
	}
	
	OnClickListener mOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			mDismissCallback.onNotificationClick();
		}
	};
	
	public void setTitle(String title) {
		mTitle = title;
		if (title == null) {
			titleView.setVisibility(View.GONE);
		}
		else {
			titleView.setVisibility(View.VISIBLE);
			titleView.setText(title);
		}
	}
	
	public void setSubTitle(String subTitle) {
		mSubtitle = subTitle;
		if (subTitle == null) {
			subtitleView.setVisibility(View.GONE);
		}
		else {
			subtitleView.setVisibility(View.VISIBLE);
			subtitleView.setText(subTitle);
		}
	}
	
}
