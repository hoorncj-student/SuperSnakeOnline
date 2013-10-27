package edu.rosehulman.supersnakeonline;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.view.Menu;
import android.widget.ListView;

public class OnlineLobbyActivity extends Activity {
	
	private ListView mOpponentList;
	private OpponentAdapter mOpponentAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_online_lobby);
		
		mOpponentList = (ListView)findViewById(R.id.online_players);
		
		mOpponentAdapter = new OpponentAdapter(this);
		Opponent joe = new Opponent("Joe Lee",Color.RED);
		mOpponentAdapter.addOpponent(joe);
		mOpponentList.setAdapter(mOpponentAdapter);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.online_lobby, menu);
		return true;
	}

}
