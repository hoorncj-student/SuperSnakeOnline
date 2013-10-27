package edu.rosehulman.supersnakeonline;

import java.util.ArrayList;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OpponentAdapter extends BaseAdapter {
	
	private ArrayList<Opponent> mOpponents;
	private Context mContext;
	
	public OpponentAdapter(Context context) {
		mContext = context;
		mOpponents = new ArrayList<Opponent>();
	}

	@Override
	public int getCount() {
		return mOpponents.size();
	}

	@Override
	public Object getItem(int position) {
		return mOpponents.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView mText = new TextView(mContext);
		mText.setGravity(Gravity.CENTER);
		mText.setBackgroundColor(mOpponents.get(position).getSnakeColor());
		mText.setText(mOpponents.get(position).getName());
		mText.setTextSize(40);
		return mText;
	}
	
	public void addOpponent(Opponent o) {
		mOpponents.add(o);
	}

}
