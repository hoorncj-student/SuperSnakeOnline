package edu.rosehulman.supersnakeonline;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainMenuActivity extends Activity implements OnClickListener {

	public static String PREFERENCES_FILE = "SnakePreferences";
	public static String USERNAME_FIELD = "username";
	public static String COLOR_FIELD = "snakeColor";
	public static String DIFFICULTY_FIELD = "difficulty";
	public static int DIFFICULTY_EASY = 0;
	public static int DIFFICULTY_NORMAL = 1;
	public static int DIFFICULTY_HARD = 2;
	public static int COLOR_GREEN = 0;
	public static int COLOR_YELLOW = 1;
	public static int COLOR_RED = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		
		((Button)findViewById(R.id.one_player_button)).setOnClickListener(this);
		((Button)findViewById(R.id.two_player_button)).setOnClickListener(this);
		((Button)findViewById(R.id.settings_button)).setOnClickListener(this);
		((Button)findViewById(R.id.high_scores_button)).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.one_player_button:
			Intent onePlayerIntent = new Intent(this, Snake.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(onePlayerIntent);
			break;
		case R.id.two_player_button:
			Intent twoPlayerIntent = new Intent(this, OnlineLobbyActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(twoPlayerIntent);
			break;
		case R.id.settings_button:
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(settingsIntent);
			break;
		case R.id.high_scores_button:
			Intent highScoresIntent = new Intent(this, HighScoresActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(highScoresIntent);
			break;
		default:
			//do nothing
			break;
		}
	}

}
