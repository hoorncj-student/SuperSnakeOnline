/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.rosehulman.supersnakeonline;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

/**
 * Snake: a simple game that everyone can enjoy.
 * 
 * This is an implementation of the classic Game "Snake", in which you control a serpent roaming
 * around the garden looking for apples. Be careful, though, because when you catch one, not only
 * will you become longer, but you'll move faster. Running into yourself or the walls will end the
 * game.
 * 
 */
public class Snake extends Activity {

    /**
     * Constants for desired direction of moving the snake
     */
    public static int MOVE_LEFT = 0;
    public static int MOVE_UP = 1;
    public static int MOVE_DOWN = 2;
    public static int MOVE_RIGHT = 3;

    private static String ICICLE_KEY = "snake-view";

    private SnakeGameView mSnakeView;
    private GestureDetector mGestureDetector;

    
    /**
     * Called when Activity is first created. Turns off the title bar, sets up the content views,
     * and fires up the SnakeGameView.
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake_game);
        
        Log.d(ICICLE_KEY,"started game");

        mGestureDetector = new GestureDetector(this, new MyGestureDetector());
        
        SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFERENCES_FILE, 0);
		String username = settings.getString(MainMenuActivity.USERNAME_FIELD, "Player");
		if(!username.equals("Player")){
			settings = getSharedPreferences(MainMenuActivity.PREFERENCES_FILE+"_"+username, 0);
			username = settings.getString(MainMenuActivity.USERNAME_FIELD, "Player1");
		}
		int color = settings.getInt(MainMenuActivity.COLOR_FIELD, MainMenuActivity.COLOR_GREEN);
		int difficulty = settings.getInt(MainMenuActivity.DIFFICULTY_FIELD, MainMenuActivity.DIFFICULTY_NORMAL);
        mSnakeView = (SnakeGameView) findViewById(R.id.snake);
        mSnakeView.setMultiplayer(false);
        mSnakeView.setScoreView(findViewById(R.id.score_text));
        mSnakeView.setStatusText(findViewById(R.id.status_text));
        mSnakeView.setSnakeColor(color);
		mSnakeView.setDifficulty(difficulty);

        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            mSnakeView.setMode(SnakeGameView.READY);
        } else {
            // We are being restored
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                mSnakeView.restoreState(map);
            } else {
                mSnakeView.setMode(SnakeGameView.PAUSE);
            }
        }
        mSnakeView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	if (mSnakeView.getGameState() == SnakeGameView.RUNNING && mGestureDetector.onTouchEvent(event)) {
            		return true;
            	} 
            	
                if (mSnakeView.getGameState() == SnakeGameView.RUNNING) {
                    // Normalize x,y between 0 and 1
                    float x = event.getX() / v.getWidth();
                    float y = event.getY() / v.getHeight();

                    // Direction will be [0,1,2,3] depending on quadrant
                    int direction = 0;
                    direction = (x > y) ? 1 : 0;
                    direction |= (x > 1 - y) ? 2 : 0;

                    // Direction is same as the quadrant which was clicked
                    mSnakeView.moveSnake(direction);

                } else if (mSnakeView.getGameState() != SnakeGameView.RUNNING) {
                    // If the game is not running then on touching any part of the screen
                    // we start the game by sending MOVE_UP signal to SnakeGameView
                	
                    mSnakeView.moveSnake(MOVE_UP);
                }
                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mSnakeView.setMode(SnakeGameView.PAUSE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the game state
        outState.putBundle(ICICLE_KEY, mSnakeView.saveState());
    }

    /**
     * Handles key events in the game. Update the direction our snake is traveling based on the
     * DPAD.
     *
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                mSnakeView.moveSnake(MOVE_UP);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mSnakeView.moveSnake(MOVE_RIGHT);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mSnakeView.moveSnake(MOVE_DOWN);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mSnakeView.moveSnake(MOVE_LEFT);
                break;
        }

        return super.onKeyDown(keyCode, msg);
    }

    private class MyGestureDetector extends SimpleOnGestureListener {
        // PART E
    	@Override
    	public boolean onDown(MotionEvent e) {
    		return true;
    	}
    	
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    			float velocityY) {
    		
    		float dX = e2.getX() - e1.getX();
    		float dY = e1.getY() - e2.getY();
    		if (Math.abs(dY)<100 && Math.abs(velocityX)>=100 && Math.abs(dX)>=100) {
    			if (dX > 0) {
    				// fling right
    				Log.d("Fling", "Move right");
    				mSnakeView.moveSnake(MOVE_RIGHT);
    			} else {
    				// fling left
    				Log.d("Fling", "Move left");
    				mSnakeView.moveSnake(MOVE_LEFT);
    			}
    			return true;
    		} else if (Math.abs(dX)<100 && Math.abs(velocityY)>=100 && Math.abs(dY)>=100) {
    			if (dY > 0) {
    				// fling up
    				Log.d("Fling", "Move up");
    				mSnakeView.moveSnake(MOVE_UP);
    			} else {
    				// fling down
    				Log.d("Fling", "Move down");
    				mSnakeView.moveSnake(MOVE_DOWN);
    			}
    			return true;
    		}
    		return false;
    	}
    }
}
