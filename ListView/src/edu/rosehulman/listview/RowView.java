package edu.rosehulman.listview;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RowView extends LinearLayout {
	
	private Context mContext;
	private TextView mLeftTextView;
	private TextView mRightTextView;
	
	public RowView(Context context) {
		super(context);
		mContext = context;
		/*mLeftTextView = new TextView(mContext);
		mRightTextView = new TextView(mContext);
		this.addView(mLeftTextView);
		this.addView(mRightTextView);*/
		LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
		inflater.inflate(R.layout.row_view, this);
		mLeftTextView = (TextView)findViewById(R.id.left_text_view);
		mRightTextView = (TextView)findViewById(R.id.right_text_view);
	}

	public void setLeftText(String string) {
		mLeftTextView.setText(string);
	}
	
	public void setRightText(String string) {
		mRightTextView.setText(string);
	}

}
