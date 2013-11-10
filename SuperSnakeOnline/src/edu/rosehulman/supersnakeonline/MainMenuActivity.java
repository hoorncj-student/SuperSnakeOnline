package edu.rosehulman.supersnakeonline;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
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
	public static String MUSIC_ON = "true";
	public static int DIFFICULTY_EASY = 0;
	public static int DIFFICULTY_NORMAL = 1;
	public static int DIFFICULTY_HARD = 2;
	public static int COLOR_GREEN = 0;
	public static int COLOR_YELLOW = 1;
	public static int COLOR_RED = 2;
	
    // sounds
    private SoundPool sounds;
    private int buttonSound;
    //private BackgroundSound mBSound; TODO
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		
		// button sound
    	sounds = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);
    	buttonSound = sounds.load(this, R.raw.button, 1);
    	//mBSound = new BackgroundSound();
    	
		// button handler
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
			sounds.play(buttonSound, 1.0f, 1.0f, 0, 0, 1.5f);
			Intent onePlayerIntent = new Intent(this, Snake.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(onePlayerIntent);
			break;
		case R.id.two_player_button:
			sounds.play(buttonSound, 1.0f, 1.0f, 0, 0, 1.5f);
			Intent twoPlayerIntent = new Intent(this, MultiplayerActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(twoPlayerIntent);
			break;
		case R.id.settings_button:
			sounds.play(buttonSound, 1.0f, 1.0f, 0, 0, 1.5f);
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(settingsIntent);
			break;
		case R.id.high_scores_button:
			sounds.play(buttonSound, 1.0f, 1.0f, 0, 0, 1.5f);
			Intent highScoresIntent = new Intent(this, HighScoresActivity.class);
			//onPlayerIntent.putExtra(KEY_NUM_BUTTONS, mNumButtons);
			startActivity(highScoresIntent);
			break;
		default:
			// nothing
			break;
		}
	}
	
	/*
	@Override
	protected void onStop() {
		super.onStop();
		mBSound.cancel(true);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		mBSound.execute();
	}
	
	class BackgroundSound extends AsyncTask<Void, Void, Void> {
	    @Override
	    protected Void doInBackground(Void... params) {
	    	if (!SettingsActivity.musicOff) {
		        MediaPlayer player = MediaPlayer.create(MainMenuActivity.this, R.raw.arab); 
		        player.setLooping(true); // Set looping 
		        player.setVolume(100,100); 
		        player.start(); 
	    	} 

	        return null;
	    }
	}*/
}
