
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import com.csr.csrmeshdemo.R;

/**
 * Array adapter used by the ListView in CheckedListFragment (which is used by GroupAssignFragment to display a list of
 * groups and devices side by side).
 * 
 */
public class CheckedItemArrayAdapter extends ArrayAdapter<CheckedListItem> {
    public static final int NO_ITEM_SELECTED = -1;
    private ArrayList<CheckedListItem> items;
    private Context context = null;
    private ItemCheckedListener listener = null;
    private boolean showCheckBoxes = false;
    private int selectedPos = NO_ITEM_SELECTED;
    private Drawable selectedBackground;

    public CheckedItemArrayAdapter(ListFragment hostFragment, int textViewResourceId, ArrayList<CheckedListItem> items) {
        super(hostFragment.getActivity(), textViewResourceId, items);
        try {
            listener = (ItemCheckedListener) hostFragment;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(hostFragment.toString() + " must implement ItemCheckedListener");
        }
        this.items = items;
        this.context = hostFragment.getActivity();
        selectedBackground = context.getResources().getDrawable(R.color.selected_color);
    }

    public interface ItemCheckedListener {
        public void checkBoxClicked(boolean checked, int deviceId);
    }

    private OnCheckedChangeListener checkChangedListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            CheckBox check = (CheckBox) buttonView;
            boolean checked = check.isChecked();
            int position = (Integer) check.getTag();
            // Update the items in the array list or else the check box state will be lost on the UI when scrolled.
            items.get(position).setChecked(checked);
            listener.checkBoxClicked(checked, items.get(position).getDevice().getDeviceId());
        }
    };

    public void setCheckBoxesVisible(boolean visible) {
        if (showCheckBoxes != visible) {
            showCheckBoxes = visible;
            notifyDataSetChanged();
        }
    }

    public void setSelectedPosition(int position) {
        selectedPos = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            // If we haven't got an inflated View to reuse then inflate now.
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.group_list_row, null);
            ViewHolder holder = new ViewHolder();
            holder.text = (TextView) rowView.findViewById(R.id.textGroupName);
            holder.lightCheck = (CheckBox) rowView.findViewById(R.id.checkBoxLight);
            holder.lightCheck.setOnCheckedChangeListener(checkChangedListener);
            rowView.setTag(holder);
        }
        // Already got an inflated View and a reference to its containing Views via the ViewHolder.
        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.text.setText(items.get(position).getDevice().getName());
        // Disable on checked change listener whilst setting checked state.
        holder.lightCheck.setOnCheckedChangeListener(null);
        holder.lightCheck.setChecked(items.get(position).isChecked());
        // Enable on checked change listener.
        holder.lightCheck.setOnCheckedChangeListener(checkChangedListener);
        holder.lightCheck.setTag(position);
        holder.lightCheck.setVisibility(showCheckBoxes ? View.VISIBLE : View.INVISIBLE);

        // If the item at this position has been selected then show it with the selectedBackground colour
        // unless the checkboxes are visible.

        rowView.setAlpha(1.0f);
        if (selectedPos == position && !showCheckBoxes) {
            rowView.setBackground(selectedBackground);
        }
        else if (!showCheckBoxes && selectedPos != NO_ITEM_SELECTED) {
            rowView.setBackground(null);
            rowView.setAlpha(0.2f);
        }
        else {
            rowView.setBackground(null);
        }
		        
        return rowView;
    }

    static class ViewHolder {
        public TextView text;
        public CheckBox lightCheck;
    }
}