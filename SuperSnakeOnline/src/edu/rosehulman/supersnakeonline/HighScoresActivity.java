package edu.rosehulman.supersnakeonline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HighScoresActivity extends Activity {
	
	private ListView mScoresList;
	private ArrayList<String> rows;
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_high_scores);
		
		rows = new ArrayList<String>();
		mScoresList = (ListView)findViewById(R.id.high_scores_list);
		SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFERENCES_FILE, 0);
		HashMap<String, String> map = (HashMap<String, String>) settings.getAll();
		for (String s : map.keySet()) {
			if (s.substring(0, 5).equals("user:")) {
				rows.add(s.substring(5) + " (" + map.get(s) + ")");
			}
		}
		String[] sortedArray = sortRows();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sortedArray);
		mScoresList.setAdapter(adapter);
	}

	private String[] sortRows() {
		String[] array = rows.toArray(new String[0]);
		int index = 0;
		for (int i=1; i<array.length; i++) {
			if (Long.parseLong(array[i].substring(array[i].indexOf("(")+1, array[i].indexOf(")"))) > Long.parseLong(rows.get(index).substring(rows.get(index).indexOf("(")+1, rows.get(index).indexOf(")")))) {
				String temp = array[index];
				array[index] = array[i];
				array[i] = temp;
				index++;
			}
		}
		return array;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.high_scores, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
	}

}
