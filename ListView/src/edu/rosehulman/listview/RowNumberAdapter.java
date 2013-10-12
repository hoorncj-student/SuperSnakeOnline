package edu.rosehulman.listview;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class RowNumberAdapter extends BaseAdapter {
	
	private Context mContext;
	private int mCount;
	private String[] mMonthNames;
	
	public RowNumberAdapter(Context context) {
		mContext = context;
		mCount = 0;
		mMonthNames = mContext.getResources().getStringArray(R.array.month_names);
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	public String getItem(int position) {
		return mMonthNames[position%12];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RowView view = null;
		if(convertView == null) {
			view  = new RowView(mContext);
		} else {
			view = (RowView)convertView;
		}
		view.setLeftText(position+". ");
		view.setRightText(mMonthNames[position%12]);
		//view.setBackgroundColor(Color.HSVToColor(new float[] {(position * 10)%360,1,1}));
		return view;
	}
	
	public void addRow() {
		mCount++;
	}

}
