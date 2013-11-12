package edu.rosehulman.supersnakeonline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SnakeGameView extends TileView {
	private static final String TAG = "SnakeGameView";
    /**
     * Current mode of application: READY to run, RUNNING, or you have already lost. static final
     * ints are used instead of an enum for performance reasons.
     */
    private int mMode = READY;
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;
    public static final int LOSE = 3;
    
    private long mPowerupDelay = 8000;
    private int level = 0;
    private int numPointsToLevel = 3;
    private boolean isMultiplayer;
	
    /**
     * Current direction the snake is headed.
     */
    private int mDirection = NORTH;
    private int mNextDirection = NORTH;
    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST = 3;
    public static final int WEST = 4;

    /**
     * Internal storage
     */
    public static final String FILENAME = "snake.highscores";
    
    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    private static final int RED_STAR = 1;
    private static final int YELLOW_STAR = 2;
    private static final int GREEN_STAR = 3;
    private static final int POINT_STAR = 4;
    private static final int APPLES_STAR = 5;
    private static final int FAST_STAR = 6;
    private static final int SLOW_STAR = 7;
    private static final int SHORTEN_STAR = 8;
    private static final int WALL_STAR = 9;
    private static final int PORTAL = 10;
    
    // location of portal
    private int x;
    private int y;
    
    // color to use for the snake
    private int snakeColor;
    
    // sounds
    private SoundPool sounds;
    private int powerupSound;
    private int portalSound;
    private int portalCreateSound;
    private int explosionSound;
    
    /**
     * mScore: Used to track the number of apples captured mMoveDelay: number of milliseconds
     * between snake movements. This will decrease as apples are captured.
     */
    private long mScore = 0;
    private long mMoveDelay = 200;
    private TextView mScoreView;
    private TextView mOppScoreView;
    private TextView mStatusText;
    
    /**
     * mLastMove: Tracks the absolute time when the snake last moved, and is used to determine if a
     * move should be made based on mMoveDelay.
     */
    private long mLastMove;
    private long mLastPowerTime;
    private long mPowerLife = 10000;
    
    private boolean mSendingWall = false;
    
    /**
     * mSnakeTrail: A list of Coordinates that make up the snake's body.
     */
    private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
    
    /**
     * mPowerupList
     */
    private Queue<Powerup> mPowerupList = new LinkedList<Powerup>();
    private Queue<Long> mPowerupSpawnList = new LinkedList<Long>();
    
    private ArrayList<Coordinate> mWallList = new ArrayList<Coordinate>();
    
    /**
     * mAppleList
     */
    private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>(); 
    
    /**
     * Create a simple handler that we can use to cause animation to happen. We set ourselves as a
     * target and we can use the sleep() function to cause an update/invalidate to occur at a later
     * date.
     */

    private RefreshHandler mRedrawHandler = new RefreshHandler();
	private boolean portalCreated = false;
    private static final Random RNG = new Random();
    
    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            SnakeGameView.this.update();
            SnakeGameView.this.invalidate();
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };
    
    public void setSendingWall(boolean wall){
    	mSendingWall = wall;
    }
    
    public boolean getSendingWall(){
    	return mSendingWall;
    }
    
    public int getMode() {
    	return this.mMode;
    }

    public int getDirection() {
    	return this.mDirection;
    }
    public void setNextDirection(int mNextDirection) {
    	this.mNextDirection = mNextDirection;
    }
    
    public void setSpeed(int mMoveDelay) {
    	this.mMoveDelay = mMoveDelay;
    }
    
    public SnakeGameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSnakeGameView(context);
	}
    
    public SnakeGameView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSnakeGameView(context);
	}
    
    private void initSnakeGameView(Context context) {
    	sounds = new SoundPool(30, AudioManager.STREAM_MUSIC, 0);
    	powerupSound = sounds.load(context, R.raw.powerup, 1);
    	portalSound = sounds.load(context, R.raw.portal, 1);
    	portalCreateSound = sounds.load(context, R.raw.portalcreate, 1);
    	explosionSound = sounds.load(context, R.raw.explosion, 1);

        setFocusable(true);

        Resources r = this.getContext().getResources();

        resetTiles(11);
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar)); 
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));
        loadTile(POINT_STAR, r.getDrawable(R.drawable.apple));
        loadTile(APPLES_STAR, r.getDrawable(R.drawable.apples));
        loadTile(FAST_STAR, r.getDrawable(R.drawable.speedup));
        loadTile(SLOW_STAR, r.getDrawable(R.drawable.slowdown));
        loadTile(SHORTEN_STAR, r.getDrawable(R.drawable.shorten));
        loadTile(WALL_STAR, r.getDrawable(R.drawable.wallstar));
        loadTile(PORTAL, r.getDrawable(R.drawable.portal));
    }
    
    public void setMultiplayer(boolean multiplayer){
    	isMultiplayer = multiplayer;
    }
 
    public void initNewGame(boolean alive) {
        mSnakeTrail.clear();
        mAppleList.clear();
        mPowerupList.clear();
        mPowerupSpawnList.clear();
        this.portalCreated = false;
        

        mSnakeTrail.add(new Coordinate(7, 8));
        //mSnakeTrail.add(new Coordinate(6, 8));
        //mSnakeTrail.add(new Coordinate(5, 8));
        //mSnakeTrail.add(new Coordinate(4, 8));
        //mSnakeTrail.add(new Coordinate(3, 8));
        //mSnakeTrail.add(new Coordinate(2, 8));
        mNextDirection = NORTH;
        
        if(!alive){
        	mScore = 0;
        	level = 0;
        }
        mScoreView.setText("Me - "+mScore);
        addRandomApple();
    }
    
    public void initTransitionGame() {
    	mSnakeTrail.clear();
    	mAppleList.clear();
    	mPowerupList.clear();
    	mPowerupSpawnList.clear();
    	
        mSnakeTrail.add(new Coordinate(this.x, this.y));
        mNextDirection = this.mDirection;
        
        this.portalCreated = false;
        
        addRandomApple();
    	
    }
    
    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array of ints before we can
     * stuff them into a map for flattening and storage.
     * 
     * @param cvec : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates as
     *         [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int[] rawArray = new int[cvec.size() * 2];

        int i = 0;
        for (Coordinate c : cvec) {
            rawArray[i++] = c.x;
            rawArray[i++] = c.y;
        }

        return rawArray;
    }
    
    /**
     * Save game state so that the user does not lose anything if the game process is killed while
     * we are in the background.
     * 
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();
        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
        map.putInt("mDirection", Integer.valueOf(mDirection));
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));
        map.putIntArray("mPowerupList", coordQueueToArray(mPowerupList, 0));
        return map;
    }

	private int[] coordQueueToArray(Queue<Powerup> cvec, int k) {
        int[] rawArray = new int[cvec.size() * 3];
        
        Iterator<Powerup> it = cvec.iterator();
        int i=0;
        while (it.hasNext()) {
        	Powerup nextPower = (Powerup) it.next();
        	Coordinate c = nextPower.getCoord();
        	PowerupType t = nextPower.getPowerup();
            rawArray[i++] = c.x;
            rawArray[i++] = c.y;
            rawArray[i++] = t.ordinal();
        	i++;
        }
        return rawArray;
	}

	/**
     * Given a flattened array of ordinate pairs, we re-constitute them into a ArrayList of
     * Coordinate objects
     * 
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }
    
    /**
     * Restore game state if our process is being re-launched
     * 
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
    	mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mDirection = icicle.getInt("mDirection");
        mNextDirection = icicle.getInt("mNextDirection");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
        mPowerupList = coordArrayToQueue(icicle.getIntArray("mPowerupList"));
    }
    
    private Queue<Powerup> coordArrayToQueue(int[] rawArray) {
        Queue<Powerup> powerQueue = new LinkedList<Powerup>();
        Powerup p = new Powerup();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 3) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            p.setCoord(c);
            p.setPowerup(PowerupType.values()[rawArray[index+2]]);
            powerQueue.add(p);
            
        }
        return powerQueue;
	}
    
    /**
     * Handles snake movement triggers from Snake Activity and moves the snake accordingly. Ignore
     * events that would cause the snake to immediately turn back on itself.
     *
     * @param direction The desired direction of movement
     */
    public void moveSnake(int direction) {

        if (direction == Snake.MOVE_UP) {
            if (mMode == READY | mMode == LOSE) {
                /*
                 * At the beginning of the game, or the end of a previous one,
                 * we should start a new game if UP key is clicked.
                 */
                initNewGame(mMode == LOSE && isMultiplayer);
                setMode(RUNNING);
                update();
                return;
            }

            if (mMode == PAUSE) {
                /*
                 * If the game is merely paused, we should just continue where we left off.
                 */
                setMode(RUNNING);
                update();
                return;
            }

            if (mDirection != SOUTH) {
                mNextDirection = NORTH;
            }
            return;
        }

        if (direction == Snake.MOVE_DOWN) {
            if (mDirection != NORTH) {
                mNextDirection = SOUTH;
            }
            return;
        }

        if (direction == Snake.MOVE_LEFT) {
            if (mDirection != EAST) {
                mNextDirection = WEST;
            }
            return;
        }

        if (direction == Snake.MOVE_RIGHT) {
            if (mDirection != WEST) {
                mNextDirection = EAST;
            }
            return;
        }

    }
    
    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the like) as well as sets
     * the visibility of textview for notification
     * 
     * @param newMode
     */
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;

        if (newMode == RUNNING && oldMode != RUNNING) {
            // hide the game instructions
            mStatusText.setVisibility(View.INVISIBLE);
            update();
            // make the background and arrows visible as soon the snake starts moving
           // mArrowsView.setVisibility(View.VISIBLE);
           // mBackgroundView.setVisibility(View.VISIBLE);
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";
        if (newMode == PAUSE) {
           // mArrowsView.setVisibility(View.GONE);
           // mBackgroundView.setVisibility(View.GONE);
           //str = res.getText(R.string.mode_pause);
        }
        if (newMode == READY) {
           // mArrowsView.setVisibility(View.GONE);
           // mBackgroundView.setVisibility(View.GONE);

          str = res.getText(R.string.mode_ready);
        }
        if (newMode == LOSE) {
          //  mArrowsView.setVisibility(View.GONE);
          //  mBackgroundView.setVisibility(View.GONE);
          str = res.getString(R.string.mode_lose, mScore);
          showAddDialog();
        }

        mStatusText.setText(str);
        mStatusText.setVisibility(View.VISIBLE);
    }
    
	private void showAddDialog() {
		final Dialog addDialog = new Dialog(getContext());
		addDialog.setContentView(R.layout.submit_dialog);
		addDialog.setTitle(getResources().getString(R.string.mode_lose,mScore));
		Button cancel = (Button) addDialog.findViewById(R.id.cancel_button);
		Button ok = (Button) addDialog.findViewById(R.id.ok_button);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addDialog.dismiss();
			}
		});
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = ((EditText) addDialog.findViewById(R.id.enter_name)).getText().toString();
				storeScore(name);
				addDialog.dismiss();
			}
		});
		addDialog.show();
	}
	
	private void storeScore(String name) {
		SharedPreferences sp = getContext().getSharedPreferences(MainMenuActivity.PREFERENCES_FILE, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("user:"+name, String.valueOf(mScore));
		editor.commit();
		
	}
    
    /**
     * @return the Game state as Running, Ready, Paused, Lose
     */
    public int getGameState() {
        return mMode;
    }
    
    /**
     * Handles the basic update loop, checking to see if we are in the running state, determining if
     * a move should be made, updating the snake's location.
     */
    public void update() {
    	if (mMode == RUNNING) {
	        long now = System.currentTimeMillis();
	
	        if (now - mLastMove > mMoveDelay) {
	            clearTiles();
	            updateWalls();
	            updateSnake();
	            updatePortal();
	            updatePowerups();
	            updateApples();
	            mLastMove = now;
	        }
	        if (now - mLastPowerTime > mPowerupDelay) {
	        	mPowerupSpawnList.add(now);
	        	addRandomPowerup();
	        	mLastPowerTime = now;
	        }
	        if (!mPowerupSpawnList.isEmpty()) {
	        	long firstSpawn = mPowerupSpawnList.peek();
		        if (now - firstSpawn > mPowerLife)  {
		        	if(mPowerupSpawnList.size() == mPowerupList.size()){
		        		mPowerupList.remove();
		        	}
		        	mPowerupSpawnList.remove();
		        }
	        }
	        mRedrawHandler.sleep(mMoveDelay);
    	}
    }
    
    private void addRandomPowerup() {
    	Powerup newPower = new Powerup();
    	Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 2+level + RNG.nextInt(mXTileCount - 3-level);
            int newY = 2+level + RNG.nextInt(mYTileCount - 3-level);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
            
            newPower.setCoord(newCoord);
        }
        //newPower.setPowerup(PowerupType.values()[RNG.nextInt(5)]);
        newPower.setPowerup(PowerupType.values()[4]);
        
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mPowerupList.add(newPower);
	}

	/**
     * Draws some apples.
     */
    private void updateApples() {
        for (Coordinate c : mAppleList) {
            setTile(POINT_STAR, c.x, c.y);
        }
    }


	private void updatePowerups() {
		for (Powerup p : mPowerupList) {
			Coordinate c = p.getCoord();
			switch(p.getPowerup().ordinal()) {
			case(0): // slower red
				setTile(SLOW_STAR, c.x, c.y);
				break;
			case(1): // apples yellow
				setTile(APPLES_STAR, c.x, c.y);
				break;
			case(2): // shrink green
				setTile(SHORTEN_STAR, c.x, c.y);
				break;
			case(3): //speed up
				setTile(FAST_STAR, c.x, c.y);
				break;
			case(4): //wall
				setTile(WALL_STAR, c.x, c.y);
				break;
			}
		}
	}

	/**
     * Draws some walls.
     */
    private void updateWalls() {
    	for (int x = 0+level; x < mXTileCount-level; x++) {
            setTile(GREEN_STAR, x, 0+level);
            setTile(GREEN_STAR, x, mYTileCount - 1 - level);
        }
        for (int y = 1+level; y < mYTileCount - 1 - level; y++) {
            setTile(GREEN_STAR, 0+level, y);
            setTile(GREEN_STAR, mXTileCount - 1 - level, y);
        }
        for(Coordinate c : mWallList){
        	setTile(GREEN_STAR, c.x, c.y);
        }
    }
    
 // if points reach a certain threshold, create portal so that snake starts in a new map at that location
    private void updatePortal() {
        if ((mScore % numPointsToLevel == 0 && mScore != 0) && !this.portalCreated) {
        	sounds.play(portalCreateSound, 1.0f, 1.0f, 0, 0, 1.5f);
            this.x = 2+level+RNG.nextInt(mXTileCount-3-level); 
        	this.y = 2+level+RNG.nextInt(mYTileCount-3-level);
        	setTile(PORTAL, this.x, this.y);
        	this.portalCreated  = true;
        } else if (this.portalCreated) {
        	setTile(PORTAL, this.x, this.y);
        }
    }
    
    public void makeLose(){
    	setMode(LOSE);
		if(isMultiplayer){
      	  mScore /= 2;
        }
    }
    
    /**
     * Figure out which way the snake is going, see if he's run into anything (the walls, himself,
     * or an apple). If he's not going to die, we then add to the front and subtract from the rear
     * in order to simulate motion. If we want to grow him, we don't subtract from the rear.
     */
    private void updateSnake() {
    	if (mSnakeTrail.size() == 0) {
    		makeLose();
    		return;
    	}
    	
        boolean growSnake = false;

        // Grab the snake by the head
        Coordinate head = mSnakeTrail.get(0);
        Coordinate newHead = new Coordinate(1, 1);

        mDirection = mNextDirection;

        switch (mDirection) {
            case EAST: {
                newHead = new Coordinate(head.x + 1, head.y);
                break;
            }
            case WEST: {
                newHead = new Coordinate(head.x - 1, head.y);
                break;
            }
            case NORTH: {
                newHead = new Coordinate(head.x, head.y - 1);
                break;
            }
            case SOUTH: {
                newHead = new Coordinate(head.x, head.y + 1);
                break;
            }
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        if ((newHead.x < 1+level) || (newHead.y < 1+level) || (newHead.x > mXTileCount - (2+level))
                || (newHead.y > mYTileCount - (2+level))) {
        		sounds.play(explosionSound, 1.0f, 1.0f, 0, 0, 1.5f);
                makeLose();
                return;
        }
        
        for(Coordinate c : mWallList){
        	if(c.equals(newHead)){
        		sounds.play(explosionSound, 1.0f, 1.0f, 0, 0, 1.5f);
                makeLose();
                return;
        	}
        }
        
        // portal collision
    	if (this.portalCreated && newHead.x==this.x && newHead.y==this.y) {
    		// go to new map
    		sounds.play(portalSound, 1.0f, 1.0f, 0, 0, 1.5f);
    		level++;
    		mScore++;
    		mScoreView.setText("Me - "+mScore);
    		initTransitionGame();
    		update();
    	} 

        // Look for collisions with itself
        int snakelength = mSnakeTrail.size();
        for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
            Coordinate c = mSnakeTrail.get(snakeindex);
            if (c.equals(newHead)) {
            	sounds.play(explosionSound, 1.0f, 1.0f, 0, 0, 1.5f);
                makeLose();
                return;
            }
        }

        // Look for powerups
        Iterator<Powerup> it = mPowerupList.iterator();
        while (it.hasNext()) {
        	Powerup p = it.next();
        	Coordinate c = p.getCoord();
        	PowerupType t = p.getPowerup();
        	if (c.equals(newHead)) {
        		sounds.play(powerupSound, 1.0f, 1.0f, 0, 0, 1.5f);
        		mPowerupList.remove(p);
        		switch(t.ordinal()) {
        		case(0): // slower
        			mMoveDelay *= 2;
        			break;
        		case(1): // apples
        			//addRandomApples();
        			mScore+=3*(level+1);
        			mScoreView.setText("Me - "+mScore);
        			break;
        		case(2): // shrink
        			shrinkSnake();
        			break;
        		case(3): // faster
        			mMoveDelay /= 2;
        			break;
        		case(4): //wall
        			mSendingWall = true;
        			break;
        		}
        		break;
        	}
        }
        
        int applecount = mAppleList.size();
        for (int appleindex = 0; appleindex < applecount; appleindex++) {
            Coordinate c = mAppleList.get(appleindex);
            if (c.equals(newHead)) {
            	sounds.play(powerupSound, 1.0f, 1.0f, 0, 0, 1.5f);
                mAppleList.remove(c);
                mScore+=1*(level+1);
                mScoreView.setText("Me - "+mScore);
                growSnake = true;
                addRandomApple();
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(0, newHead);
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.remove(mSnakeTrail.size() - 1);
        }

        
        // snake color
        int index = 0;
        for (Coordinate c : mSnakeTrail) {
        	setTile(snakeColor, c.x, c.y);
            /*if (index == 0) {
                setTile(YELLOW_STAR, c.x, c.y);
            } else {
                setTile(RED_STAR, c.x, c.y);
            }*/
            index++;
        }

    }

	private void shrinkSnake() {
		mSnakeTrail.remove(mSnakeTrail.size() - 1);
	}

	private void addRandomApple() {
        Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 2 + level + RNG.nextInt(mXTileCount - 3 - level);
            int newY = 2 + level + RNG.nextInt(mYTileCount - 3 - level);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mAppleList.add(newCoord);
	}

	public class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }

	public long getScore() {
		return this.mScore;
	}

	public void setSnakeColor(int color) {
		Log.d("COL",color+"");
		if(color == MainMenuActivity.COLOR_GREEN){
			snakeColor = GREEN_STAR;
		}else if(color == MainMenuActivity.COLOR_RED){
			snakeColor = RED_STAR;
		}else if(color == MainMenuActivity.COLOR_YELLOW){
			snakeColor = YELLOW_STAR;
		}
	}

	public void setDifficulty(int difficulty) {
		Log.d("DIFF",difficulty+"");
		mMoveDelay = (3-difficulty)*100;
	}

	public void setScoreView(View scoreView) {
		mScoreView = (TextView) scoreView;
	}
	
	public void setStatusView(View statusView) {
		mStatusText = (TextView) statusView;
	}
	
	public void setStatusText(String statusText){
		mStatusText.setText(statusText);
	}
	
	public void setOpponentScoreView(View oppScoreView){
		if(oppScoreView == null){
			Log.d("CRAP","setView");
		}
		Log.d("NORM","settingView");
		mOppScoreView = (TextView)oppScoreView;
	}
	
	public void setOpponentScore(String score){
		if(score == null){
			Log.d("CRAP","view");
		}else if(mOppScoreView == null){
			Log.d("CRAP","oppScore");
		}
		mOppScoreView.setText(score);
	}
	
	public void generateRandomWall(){
		Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 2 + level + RNG.nextInt(mXTileCount - 3 - level);
            int newY = 2 + level + RNG.nextInt(mYTileCount - 3 - level);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for a wall. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mWallList.add(newCoord);
	}
}
