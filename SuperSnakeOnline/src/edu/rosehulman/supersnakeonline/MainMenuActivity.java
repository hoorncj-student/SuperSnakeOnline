package edu.rosehulman.supersnakeonline;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
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
    private boolean mIsBound = false;
    private BackgroundMusic mServ;
    protected static Intent bgMusic;
    private ServiceConnection conn = new ServiceConnection() {
    	@Override
	    public void onServiceConnected(ComponentName name, IBinder
	     binder) {
	    	mServ = ((BackgroundMusic.ServiceBinder)binder).getService();
	    }
	
    	@Override
	    public void onServiceDisconnected(ComponentName name) {
	        mServ = null;
	    }
    };    
    void doBindService(){
        bindService(new Intent(this,BackgroundMusic.class),
        		conn, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    void doUnbindService()
    {
        if(mIsBound)
        {
            unbindService(conn);
            mIsBound = false;
        }
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		
		// button sound
    	sounds = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);
    	buttonSound = sounds.load(this, R.raw.button, 1);

    	// music
    	bgMusic = new Intent();
    	bgMusic.setClass(this, BackgroundMusic.class);
    	startService(bgMusic);
    	
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
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		stopService(bgMusic);
	}
	
	@Override
	protected void onDestroy() {
		stopService(bgMusic);
		super.onDestroy();
	}
}
