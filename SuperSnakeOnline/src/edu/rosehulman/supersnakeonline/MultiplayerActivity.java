/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.rosehulman.supersnakeonline;

import edu.rosehulman.supersnakeonline.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiplayerActivity extends BaseGameActivity
        implements View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    // Debug tag
    final static boolean ENABLE_DEBUG = true;
    final static String TAG = "SuperSnakeOnline";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Are we playing in multiplayer mode?
    boolean mMultiplayer = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;
    
    // The opponenet
    Participant mOpponent = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

    // Message buffer for sending messages
    byte[] mMsgBuf = new byte[2];

    // flag indicating whether we're dismissing the waiting room because the
    // game is starting
    boolean mWaitRoomDismissedFromCode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableDebugLog(ENABLE_DEBUG, TAG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
        mSnakeView = (SnakeGameView) findViewById(R.id.snake);
        mSnakeView.setMultiplayer(true);
        mSnakeView.setScoreView(findViewById(R.id.score_text));
        mSnakeView.setStatusView(findViewById(R.id.status_text));
        mSnakeView.setOpponentScoreView(findViewById(R.id.opponent_score_text));
    }

    /**
     * Called by the base class (BaseGameActivity) when sign-in has failed. For
     * example, because the user hasn't authenticated yet. We react to this by
     * showing the sign-in button.
     */
    @Override
    public void onSignInFailed() {
        Log.d(TAG, "Sign-in failed.");
        switchToScreen(R.id.screen_sign_in);
    }

    /**
     * Called by the base class (BaseGameActivity) when sign-in succeeded. We
     * react by going to our main screen.
     */
    @Override
    public void onSignInSucceeded() {
        Log.d(TAG, "Sign-in succeeded.");

        // install invitation listener so we get notified if we receive an
        // invitation to play
        // a game.
        getGamesClient().registerInvitationListener(this);

        // if we received an invite via notification, accept it; otherwise, go
        // to main screen
        if (getInvitationId() != null) {
            acceptInviteToRoom(getInvitationId());
            return;
        }
        switchToMainScreen();
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_sign_in:
                // user wants to sign in
                if (!verifyPlaceholderIdsReplaced()) {
                    showAlert("Error", "Sample not set up correctly. Please see README.");
                    return;
                }
                beginUserInitiatedSignIn();
                break;
            case R.id.button_sign_out:
                signOut();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                // show list of invitable players
                intent = getGamesClient().getSelectPlayersIntent(1, 1);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            case R.id.button_see_invitations:
                // show list of pending invitations
                intent = getGamesClient().getInvitationInboxIntent();
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
                // user wants to accept the invitation shown on the invitation
                // popup
                // (the one we got through the OnInvitationReceivedListener).
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;
            case R.id.chat_submit:
            	// user wants to send a chat message
            	sendChatItem();
            	break;
        }
    }

    void startQuickGame() {
        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        getGamesClient().createRoom(rtmConfigBuilder.build());
    }
    

    @Override
    public void onActivityResult(int requestCode, int responseCode,
            Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // ignore result if we dismissed the waiting room from code:
                if (mWaitRoomDismissedFromCode) break;

                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // player wants to start playing
                    Log.d(TAG, "Starting game because user requested via waiting room UI.");

                    // let other players know we're starting.
                    broadcastStart();

                    // start the game!
                    startGame(true);
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player actively indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    /* Dialog was cancelled (user pressed back key, for
                     * instance). In our game, this means leaving the room too. In more
                     * elaborate games,this could mean something else (like minimizing the
                     * waiting room UI but continue in the handshake process). */
                    leaveRoom();
                }

                break;
        }
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        getGamesClient().createRoom(rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(GamesClient.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        getGamesClient().joinRoom(roomConfigBuilder.build());
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        switchToScreen(R.id.screen_wait);
        super.onStop();
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        switchToScreen(R.id.screen_wait);
        super.onStart();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_end) {
            leaveRoom();
            return true;
        }
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
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        //mSecondsLeft = 0;
        stopKeepingScreenOn();
        if (mRoomId != null) {
            getGamesClient().leaveRoom(this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        mWaitRoomDismissedFromCode = false;

        // minimum number of players required for our game
        final int MIN_PLAYERS = 2;
        Intent i = getGamesClient().getRealTimeWaitingRoomIntent(room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Forcibly dismiss the waiting room UI (this is useful, for example, if we realize the
    // game needs to start because someone else is starting to play).
    void dismissWaitingRoom() {
        mWaitRoomDismissedFromCode = true;
        finishActivity(RC_WAITING_ROOM);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            mOpponent = p;
        }
        mMyId = room.getParticipantId(getGamesClient().getCurrentPlayerId());

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        showAlert(getString(R.string.error), getString(R.string.game_problem));
        switchToMainScreen();
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesClient.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesClient.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesClient.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    void updateRoom(Room room) {
        mParticipants = room.getParticipants();
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            mOpponent = p;
        }
        if(mOpponent == null){
        	Log.d("HI","crap");
        }
        updatePeerScoresDisplay();
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */
    
 // Current state of the game:
    int mSecondsLeft = -1; // how long until the game ends (seconds)
    final static int GAME_DURATION = 30; // game duration, seconds.
    String mChatText = "";

 // Reset game variables in preparation for a new game.
    void resetGameVars() {
        mParticipantScore = 0;
        participantFinished = false;
        mSecondsLeft = GAME_DURATION;
    }

    // Start the gameplay phase of the game.
    void startGame(boolean multiplayer) {
        mMultiplayer = multiplayer;
        broadcastScore(false);
        mSnakeView.setMultiplayer(multiplayer);
        createSnakeGame();

        // run the gameTick() method every second to update the game.
    }
    
    public void setupEndScreen(){
    	long myScore = mSnakeView.getScore();
    	if(myScore < mParticipantScore){
    		((TextView)findViewById(R.id.high_score_text)).setText(mOpponent.getDisplayName()+" - "+mParticipantScore);
    		((TextView)findViewById(R.id.low_score_text)).setText("Me - "+myScore);
    		((TextView)findViewById(R.id.end_message)).setText(getString(R.string.loser));
    	}else if(myScore > mParticipantScore){
    		((TextView)findViewById(R.id.low_score_text)).setText(mOpponent.getDisplayName()+" - "+mParticipantScore);
    		((TextView)findViewById(R.id.high_score_text)).setText("Me - "+myScore);
    		((TextView)findViewById(R.id.end_message)).setText(getString(R.string.winner));
    	}else{
    		((TextView)findViewById(R.id.low_score_text)).setText(mOpponent.getDisplayName()+" - "+mParticipantScore);
    		((TextView)findViewById(R.id.high_score_text)).setText("Me - "+myScore);
    		((TextView)findViewById(R.id.end_message)).setText(getString(R.string.draw));
    	}
    	mChatText = "";
    	updateChatWindow();
    	((EditText)findViewById(R.id.chat_entry)).setText("");
    }
    
    public void sendChatItem(){
    	String enteredText = ""+((EditText)findViewById(R.id.chat_entry)).getText();
    	mChatText += "me - "+enteredText+"\n";
    	
    	mMsgBuf[0] = (byte) 'B';
		mMsgBuf[1] = (byte) 'O';
		getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId, mOpponent.getParticipantId());
    	for(int letter=0;letter<enteredText.length();letter++){
    		mMsgBuf[0] = (byte) 'C';
    		mMsgBuf[1] = (byte) enteredText.charAt(letter);
    		getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId, mOpponent.getParticipantId());
    	}
    	mMsgBuf[0] = (byte) 'E';
		mMsgBuf[1] = (byte) 'O';
		getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId, mOpponent.getParticipantId());
    	updateChatWindow();
    	((EditText)findViewById(R.id.chat_entry)).setText("");
    }
    
    public void updateChatWindow(){
    	((TextView)findViewById(R.id.chat_box)).setText(mChatText);
    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Score of other participants. We update this as we receive their scores
    // from the network.
    int mParticipantScore;

    // Participants who sent us their final score.
    //Set<String> mFinishedParticipants = new HashSet<String>();
    boolean participantFinished;

    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);

        if (buf[0] == 'F' || buf[0] == 'U') {
            // score update.
        	int existingScore = mParticipantScore != 0 ? mParticipantScore : 0;
            int thisScore = (int) buf[1];
            /*if (thisScore > existingScore) {
                // this check is necessary because packets may arrive out of
                // order, so we
                // should only ever consider the highest score we received, as
                // we know in our
                // game there is no way to lose points. If there was a way to
                // lose points,
                // we'd have to add a "serial number" to the packet.
            	mParticipantScore = thisScore;
            }*/
            mParticipantScore = thisScore;

            // update the scores on the screen
            updatePeerScoresDisplay();

            // if it's a final score, mark this participant as having finished
            // the game
            if ((char) buf[0] == 'F') {
                //mFinishedParticipants.add(rtm.getSenderParticipantId());
            	participantFinished = true;
            }
        } else if (buf[0] == 'S') {
            // someone else started to play -- so dismiss the waiting room and
            // get right to it!
            Log.d(TAG, "Starting game because we got a start message.");
            dismissWaitingRoom();
            startGame(true);
        } else if (buf[0] == 'C'){
        	//Someone is sending a chat item
        	mChatText = mChatText+((char)buf[1]+"");
        } else if (buf[0] == 'E'){
        	mChatText += "\n";
        	updateChatWindow();
        } else if (buf[0] == 'B'){
        	mChatText += mOpponent.getDisplayName()+" - ";
        } else if (buf[0] == 'G'){
        	//Countdown is starting
        	startCountdown();
        } else if (buf[0] == 'W'){
        	//Opponent sent us a wall
        	mSnakeView.generateRandomWall();
        }
    }

    // Broadcast my score to everybody else.
    void broadcastScore(boolean finalScore) {
        if (!mMultiplayer)
            return; // playing single-player mode

        // First byte in message indicates whether it's a final score or not
        mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');

        // Second byte is the score.
        //mMsgBuf[1] = (byte) mScore;
        
        mMsgBuf[1] = (byte) mSnakeView.getScore();

        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (finalScore) {
                // final score notification must be sent via reliable message
                getGamesClient().sendReliableRealTimeMessage(null, mMsgBuf, mRoomId,
                        p.getParticipantId());
            } else {
                // it's an interim score notification, so we can use unreliable
                getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId,
                        p.getParticipantId());
            }
        }
    }
    
    void broadcastStart(){
    	if (!mMultiplayer)
            return; // playing single-player mode

        mMsgBuf[0] = 'S';
        mMsgBuf[1] = (byte) 0;
        getGamesClient().sendReliableRealTimeMessage(null, mMsgBuf, mRoomId, mOpponent.getParticipantId());
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.chat_submit
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_end, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait, R.id.screen_snake
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            // single-player: show on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }

    void switchToMainScreen() {
        switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
    }
    
    void updatePeerScoresDisplay() {
    	if(mOpponent == null){
    		Log.d("CRAP","Opponent");
    	}else if(mOpponent.getDisplayName() == null){
    		Log.d("CRAP","Name");
    	}
    	mSnakeView.setOpponentScore(mOpponent.getDisplayName()+" - "+mParticipantScore);
    }

    /*
     * MISC SECTION. Miscellaneous methods.
     */

    /**
     * Checks that the developer (that's you!) read the instructions. IMPORTANT:
     * a method like this SHOULD NOT EXIST in your production app! It merely
     * exists here to check that anyone running THIS PARTICULAR SAMPLE did what
     * they were supposed to in order for the sample to work.
     */
    boolean verifyPlaceholderIdsReplaced() {
        final boolean CHECK_PKGNAME = true; // set to false to disable check
                                             // (not recommended!)

        // Did the developer forget to change the package name?
        if (CHECK_PKGNAME && getPackageName().startsWith("com.google.example."))
            return false;

        // Did the developer forget to replace a placeholder ID?
        int res_ids[] = new int[] {
                R.string.app_id
        };
        for (int i : res_ids) {
            if (getString(i).equalsIgnoreCase("ReplaceMe"))
                return false;
        }
        return true;
    }

    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

	@Override
	public void onP2PConnected(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onP2PDisconnected(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public static int MOVE_LEFT = 0;
    public static int MOVE_UP = 1;
    public static int MOVE_DOWN = 2;
    public static int MOVE_RIGHT = 3;

    private static String ICICLE_KEY = "snake-view";

    private SnakeGameView mSnakeView;
    private GestureDetector mGestureDetector;
    private int COUNTDOWN_LENGTH = 3;
    private int countdownSeconds;
	
	
    public void createSnakeGame() {
    	switchToScreen(R.id.screen_snake);
        Log.d(ICICLE_KEY,"started game");

        mGestureDetector = new GestureDetector(this, new MyGestureDetector());
        
        SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFERENCES_FILE, 0);
		String username = settings.getString(MainMenuActivity.USERNAME_FIELD, "Player");
		if(!username.equals("Player")){
			settings = getSharedPreferences(MainMenuActivity.PREFERENCES_FILE+"_"+username, 0);
			username = settings.getString(MainMenuActivity.USERNAME_FIELD, "Player1");
		}
		int color = settings.getInt(MainMenuActivity.COLOR_FIELD, MainMenuActivity.COLOR_GREEN);
        
        mSnakeView.setSnakeColor(color);
        mSnakeView.setMode(SnakeGameView.READY);
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

                }else if (mSnakeView.getGameState() == SnakeGameView.READY) {
                    // If the game is not running then on touching any part of the screen
                    // we start the game by sending MOVE_UP signal to SnakeGameView
                    //mSnakeView.moveSnake(MOVE_UP);
                	signalStart();
                }else if (mSnakeView.getGameState() == SnakeGameView.LOSE){
                	mSnakeView.moveSnake(MOVE_UP);
                }
                return false;
            }
        });
    }
    
    public void signalStart(){
    	startCountdown();
    	mMsgBuf[0] = (byte) 'G';
		mMsgBuf[1] = (byte) 'O';
		getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId, mOpponent.getParticipantId());
    }
    
    public void startCountdown(){
    	countdownSeconds = COUNTDOWN_LENGTH;
    	final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdownSeconds <= 0){
                	go();
                	mSnakeView.moveSnake(MOVE_UP);
                    return;
                }
                count();
                h.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    public void count(){
    	mSnakeView.setStatusText(""+countdownSeconds);
    	countdownSeconds--;
    }
    
    public void go(){
    	final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSecondsLeft <= 0)
                    return;
                gameTick();
                h.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    void gameTick(){
    	if (mSecondsLeft > 0){
            --mSecondsLeft;
            if(mSnakeView.getSendingWall()){
            	mSnakeView.setSendingWall(false);
            	sendWallToOpponent();
            }
            broadcastScore(false);
    	}

        // update countdown
        ((TextView) findViewById(R.id.countdown)).setText((mSecondsLeft/60) + ":" +
                (mSecondsLeft%60 < 10 ? "0" : "") + String.valueOf(mSecondsLeft%60));

        if (mSecondsLeft <= 0) {
            broadcastScore(true);
            switchToScreen(R.id.screen_end);
            mSnakeView.setMode(SnakeGameView.LOSE);
            setupEndScreen();
        }
    }
    
    public void sendWallToOpponent(){
    	mMsgBuf[0] = (byte) 'W';
		mMsgBuf[1] = (byte) 'O';
		getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf, mRoomId, mOpponent.getParticipantId());
    }

    /*@Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mSnakeView.setMode(SnakeGameView.PAUSE);
    }*/

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the game state
        outState.putBundle(ICICLE_KEY, mSnakeView.saveState());
    }*/

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
