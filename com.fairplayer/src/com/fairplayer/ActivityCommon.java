/**
 * Copyright 2016 Mark Jivko https://markjivko.com
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://gnu.org/licenses/gpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Redistributions of files must retain the above copyright notice.
 */
package com.fairplayer;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Base activity for activities that contain playback controls. Handles
 * communication with the FpServiceRendering and response to state and song
 * changes.
 */
public abstract class ActivityCommon extends Activity implements FpTrackTimelineCallback, Handler.Callback, View.OnClickListener, FpCoverView.Callback {
	
	// NO action
    protected static final int ACTION_NONE = 0; 
    protected static final int ACTION_BACK = -1;
	
    /**
     * Update the seekbar progress with the current song progress. This must be
     * called on the UI Handler.
     */
    protected static final int MSG_UPDATE_PROGRESS = 20;

    /**
     * Calls FpServiceRendering#seekToProgress(int).
     */
    protected static final int MSG_SEEK_TO_PROGRESS = 21;

    /**
     * A Handler running on the UI thread, in contrast with mHandler which runs
     * on a worker thread.
     */
    protected final Handler mUiHandler = new Handler(this);
    /**
     * A Handler running on a worker thread.
     */
    protected Handler mHandler;
    /**
     * The looper for the worker thread.
     */
    protected Looper mLooper;

    protected FpCoverView mCoverView;
    protected ElementImageButton mPrevButton;
    protected ElementImageButton mNextButton;
    protected ElementImageButton mPlayPauseButton;
    protected ElementImageButton mShuffleButton;
    protected ElementImageButton mRepeatButton;

    protected int mState;
    private long mLastStateEvent;
    private long mLastSongEvent;

    /**
     * Store the current app's context
     */
    protected static Context _mContext = null;
    private static Activity _mActivity = null;
    
    /**
     * Set the current app's context
     */
    public static void setContext (Context context) {
    	_mContext = context;
    }
    
    /**
     * Get the current app's context
     */
    public static Context getContext() {
        return _mContext;
    }
    
    /**
     * Get the current app's activity
     */
    public static Activity getActivity() {
    	return _mActivity;
    }
    
    // Store the current action
    protected int action = ACTION_NONE;
    
    // Ads
    protected Ads ads = null;
    
    /**
     * Go Places after the interstitial is closed
     */
    public abstract void goPlaces();
    
    /**
     * Get the current action
     */
    public int getAction() {
        return this.action;
    }

    /**
     * Set the current action
     */
    public void setAction(int action) {
        this.action = action;
    }
    
    @Override
    public void onCreate(Bundle state) {
        // Store the context
    	_mContext = getApplicationContext();
        
        // Store the activity
        _mActivity = this;
        
        // Store the ads
        this.ads = Ads.getInstance(this);

        // Call the parent
        super.onCreate(state);

        FpServiceRendering.addTimelineCallback(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

    }

    @Override
    public void onDestroy() {
        FpServiceRendering.removeTimelineCallback(this);
        mLooper.quit();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (FpServiceRendering.hasInstance()) {
            onServiceReady();
        } else {
            startService(new Intent(this, FpServiceRendering.class));
        }

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Store the context
    	_mContext = getApplicationContext();
        
        // Store the activity
        _mActivity = this;
        
        // Ads set
        if (null != this.ads) {
            // Log
            Log.d(Constants.LOG_TAG, "Resumed - show ads");
            
            // Check the online connection
            Ads.checkForInternetLive();    
        }
        
        // Track pageview
    	Tracker.trackPageview(this);
        
        if (FpServiceRendering.hasInstance()) {
            FpServiceRendering service = FpServiceRendering.get(this);
            service.userActionTriggered();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return FpReceiverMediaButtonEvent.processKey(this, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                return FpReceiverMediaButtonEvent.processKey(this, event);
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void shiftCurrentSong(int delta) {
        setSong(FpServiceRendering.get(this).shiftCurrentSong(delta));
    }

    public void playPause() {
        FpServiceRendering service = FpServiceRendering.get(this);
        int state = service.playPause();
        if ((state & FpServiceRendering.FLAG_ERROR) != 0) {
            showToast(service.getErrorMessage(), Toast.LENGTH_LONG);
        }
        setState(state);
    }

    private void rewindCurrentSong() {
        setSong(FpServiceRendering.get(this).rewindCurrentSong());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fp_next:
                shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_SONG);
                break;
            case R.id.fp_play_pause:
                playPause();
                break;
            case R.id.fp_previous:
                rewindCurrentSong();
                break;
            case R.id.fp_repeat:
                cycleFinishAction();
                break;
            case R.id.fp_shuffle:
                cycleShuffle();
                break;
        }
    }

    /**
     * Called when the FpServiceRendering state has changed.
     *
     * @param state FpServiceRendering state
     * @param toggled The flags that have changed from the previous state
     */
    protected void onStateChange(int state, int toggled) {
        if ((toggled & FpServiceRendering.FLAG_PLAYING) != 0 && mPlayPauseButton != null) {
            mPlayPauseButton.setImageResource((state & FpServiceRendering.FLAG_PLAYING) == 0 ? R.drawable.fp_control_play : R.drawable.fp_control_pause);
        }
        if ((toggled & FpServiceRendering.MASK_FINISH) != 0 && mRepeatButton != null) {
            mRepeatButton.setImageResource(FpTrackTimeline.REPEAT_ICONS[FpServiceRendering.finishAction(state)]);
        }
        if ((toggled & FpServiceRendering.MASK_SHUFFLE) != 0 && mShuffleButton != null) {
            mShuffleButton.setImageResource(FpTrackTimeline.SHUFFLE_ICONS[FpServiceRendering.shuffleMode(state)]);
        }
    }

    protected void setState(final int state) {
        mLastStateEvent = System.nanoTime();

        if (mState != state) {
            final int toggled = mState ^ state;
            mState = state;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onStateChange(state, toggled);
                }
            });
        }
    }

    /**
     * Called by FpServiceRendering to update the state.
     */
    public void setState(long uptime, int state) {
        if (uptime > mLastStateEvent) {
            setState(state);
            mLastStateEvent = uptime;
        }
    }

    /**
     * Sets up components when the FpServiceRendering is initialized and available
     * to
     * interact with. Override to implement further post-initialization
     * behavior.
     */
    protected void onServiceReady() {
        FpServiceRendering service = FpServiceRendering.get(this);
        setSong(service.getSong(0));
        setState(service.getState());
    }

    /**
     * Called when the current song changes.
     *
     * @param song The new song
     */
    protected void onSongChange(FpTrack song) {
        if (mCoverView != null) {
            mCoverView.querySongs(FpServiceRendering.get(this));
        }
    }

    protected void setSong(final FpTrack song) {
        mLastSongEvent = System.nanoTime();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onSongChange(song);
            }
        });
    }

    /**
     * Sets up onClick listeners for our common control buttons bar
     */
    protected void bindControlButtons() {
        // Previous
        mPrevButton = (ElementImageButton) findViewById(R.id.fp_previous);
        if (null != mPrevButton) {
            mPrevButton.setImageResource(R.drawable.fp_control_prev);
            mPrevButton.setOnClickListener(this);
        }
        
        // Play/Pause
        mPlayPauseButton = (ElementImageButton) findViewById(R.id.fp_play_pause);
        if (null != mPlayPauseButton) {
            mPlayPauseButton.setImageResource(R.drawable.fp_control_play);
            mPlayPauseButton.setOnClickListener(this);
        }
        
        // Next
        mNextButton = (ElementImageButton) findViewById(R.id.fp_next);
        if (null != mNextButton) {
            mNextButton.setImageResource(R.drawable.fp_control_next);
            mNextButton.setOnClickListener(this);
        }
        
        // Shuffle
        mShuffleButton = (ElementImageButton) findViewById(R.id.fp_shuffle);
        if (null != mShuffleButton) {
            mShuffleButton.setImageResource(R.drawable.fp_shuffle_none);
            mShuffleButton.setOnClickListener(this);
            registerForContextMenu(mShuffleButton);
        }
        
        // Repeat
        mRepeatButton = (ElementImageButton) findViewById(R.id.fp_repeat);
        if (null != mRepeatButton) {
            mRepeatButton.setImageResource(R.drawable.fp_repeat_none);
            mRepeatButton.setOnClickListener(this);
            registerForContextMenu(mRepeatButton);
        }
    }

    /**
     * Called by FpServiceRendering to update the current song.
     */
    public void setSong(long uptime, FpTrack song) {
        if (uptime > mLastSongEvent) {
            setSong(song);
            mLastSongEvent = uptime;
        }
    }

    /**
     * Called by FpServiceRendering to update an active song (next, previous, or
     * current).
     */
    public void replaceSong(int delta, FpTrack song) {
        if (mCoverView != null) {
            mCoverView.setSong(delta + 1, song);
        }
    }

    /**
     * Called when the song timeline position/size has changed.
     */
    public void onPositionInfoChanged() {
    }

    /**
     * Called when the content of the media store has changed.
     */
    public void onMediaChange() {
    }

    /**
     * Called when the timeline change has changed.
     */
    public void onTimelineChanged() {
    }

    static final int MENU_SORT = 1;
    static final int MENU_PREFS = 2;
    static final int MENU_LIBRARY = 3;
    static final int MENU_NOW_PLAYING = 5;
    static final int MENU_SEARCH = 7;
    static final int MENU_ENQUEUE_ALBUM = 8;
    static final int MENU_ENQUEUE_ARTIST = 9;
    static final int MENU_ENQUEUE_GENRE = 10;
    static final int MENU_CLEAR_QUEUE = 11;
    static final int MENU_SONG_FAVORITE = 12;
    static final int MENU_SHOW_QUEUE = 13;
    static final int MENU_HIDE_QUEUE = 14;
    static final int MENU_SAVE_QUEUE_AS_PLAYLIST = 15;
    static final int MENU_DELETE = 16;
    static final int MENU_EMPTY_QUEUE = 17;
    static final int MENU_ADD_TO_PLAYLIST = 18;
    static final int MENU_SHARE = 19;
    static final int MENU_THEME_OF_THE_DAY = 20;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PREFS, 10, R.string.fp_menu_settings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PREFS:
                startActivity(new Intent(this, ActivitySettings.class));
                break;
            case MENU_CLEAR_QUEUE:
                FpServiceRendering.get(this).clearQueue();
                break;
            case MENU_EMPTY_QUEUE:
                FpServiceRendering.get(this).emptyQueue();
                break;
            case MENU_SAVE_QUEUE_AS_PLAYLIST:
                FpPlaylistPopupNew dialog = new FpPlaylistPopupNew(this, null, R.string.fp_playlist_create, null);
                dialog.setOnDismissListener(new SaveAsPlaylistDismiss());
                dialog.show();
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Call addToPlaylist with the results from a FpPlaylistPopupNew stored in
     * obj.
     */
    protected static final int MSG_NEW_PLAYLIST = 0;
    /**
     * Call renamePlaylist with the results from a FpPlaylistPopupNew stored in
     * obj.
     */
    protected static final int MSG_RENAME_PLAYLIST = 1;
    /**
     * Call addToPlaylist with data from the playlisttask object.
     */
    protected static final int MSG_ADD_TO_PLAYLIST = 2;
    /**
     * Call removeFromPlaylist with data from the playlisttask object.
     */
    protected static final int MSG_REMOVE_FROM_PLAYLIST = 3;
    /**
     * Removes a media object
     */
    protected static final int MSG_DELETE = 4;
    /**
     * Saves the current queue as a playlist
     */
    protected static final int MSG_SAVE_QUEUE_AS_PLAYLIST = 5;
    /**
     * Notification that we changed some playlist members
     */
    protected static final int MSG_NOTIFY_PLAYLIST_CHANGED = 6;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_NEW_PLAYLIST: {
                FpPlaylistPopupNew dialog = (FpPlaylistPopupNew) message.obj;
                if (dialog.isAccepted()) {
                    String name = dialog.getText();
                    long playlistId = FpPlaylist.createPlaylist(getContentResolver(), name);
                    FpPlaylistTask playlistTask = dialog.getPlaylistTask();
                    playlistTask.name = name;
                    playlistTask.playlistId = playlistId;
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TO_PLAYLIST, playlistTask));
                }
                break;
            }
            case MSG_ADD_TO_PLAYLIST: {
                FpPlaylistTask playlistTask = (FpPlaylistTask) message.obj;
                addToPlaylist(playlistTask);
                break;
            }
            case MSG_SAVE_QUEUE_AS_PLAYLIST: {
                String playlistName = (String) message.obj;
                long playlistId = FpPlaylist.createPlaylist(getContentResolver(), playlistName);
                FpPlaylistTask playlistTask = new FpPlaylistTask(playlistId, playlistName);
                playlistTask.audioIds = new ArrayList<Long>();

                FpTrack song;
                FpServiceRendering service = FpServiceRendering.get(this);
                for (int i = 0;; i++) {
                    song = service.getSongByQueuePosition(i);
                    if (song == null) {
                        break;
                    }
                    playlistTask.audioIds.add(song.id);
                }

                addToPlaylist(playlistTask);
                break;
            }
            case MSG_REMOVE_FROM_PLAYLIST: {
                FpPlaylistTask playlistTask = (FpPlaylistTask) message.obj;
                removeFromPlaylist(playlistTask);
                break;
            }
            case MSG_RENAME_PLAYLIST: {
                FpPlaylistPopupNew dialog = (FpPlaylistPopupNew) message.obj;
                if (dialog.isAccepted()) {
                    long playlistId = dialog.getPlaylistTask().playlistId;
                    FpPlaylist.renamePlaylist(getContentResolver(), playlistId, dialog.getText());
                }
                break;
            }
            case MSG_DELETE: {
                delete((Intent) message.obj);
                break;
            }
            case MSG_NOTIFY_PLAYLIST_CHANGED: {
                // this is a NOOP here: super classes might implement this.
                break;
            }
            default:
                return false;
        }
        return true;
    }

    /**
     * Add a set of songs represented by the playlistTask to a playlist.
     * Displays a
     * Toast notifying of success.
     *
     * @param playlistTask The pending FpPlaylistTask to execute
     */
    protected void addToPlaylist(FpPlaylistTask playlistTask) {
        int count = 0;

        if (playlistTask.query != null) {
            count += FpPlaylist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.query);
        }

        if (playlistTask.audioIds != null) {
            count += FpPlaylist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.audioIds);
        }

        String message = getResources().getQuantityString(R.plurals.fp_plurals_song_playlist_added, count, count, playlistTask.name);
        showToast(message, Toast.LENGTH_SHORT);
        mHandler.sendEmptyMessage(MSG_NOTIFY_PLAYLIST_CHANGED);
    }

    /**
     * Removes a set of songs represented by the playlistTask from a playlist.
     * Displays a
     * Toast notifying of success.
     *
     * @param playlistTask The pending FpPlaylistTask to execute
     */
    private void removeFromPlaylist(FpPlaylistTask playlistTask) {
        int count = 0;

        if (playlistTask.query != null) {
            playlistTask.query = null;
        }

        if (playlistTask.audioIds != null) {
            count += FpPlaylist.removeFromPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.audioIds);
        }

        String message = getResources().getQuantityString(R.plurals.fp_plurals_song_playlist_removed, count, count, playlistTask.name);
        showToast(message, Toast.LENGTH_SHORT);
        mHandler.sendEmptyMessage(MSG_NOTIFY_PLAYLIST_CHANGED);
    }

    /**
     * Delete the media represented by the given intent and show a Toast
     * informing the user of this.
     *
     * @param intent An intent created with
     * AdapterLibrary#createData(View).
     */
    private void delete(Intent intent) {
        int type = intent.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);
        long id = intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID);
        String message = null;
        Resources res = getResources();

        if (type == FpUtilsMedia.TYPE_FILE) {
            String file = intent.getStringExtra(AdapterLibrary.DATA_FILE);
            boolean success = FpUtilsMedia.deleteFile(new File(file));
            if (!success) {
                message = res.getString(R.string.fp_menu_notif_delete_error, file);
            }
        } else {
            if (type == FpUtilsMedia.TYPE_PLAYLIST) {
                FpPlaylist.deletePlaylist(getContentResolver(), id);
            } else {
                int count = FpServiceRendering.get(this).deleteMedia(type, id);
                message = res.getQuantityString(R.plurals.fp_plurals_song_deleted, count, count);
            }
        }

        if (message == null) {
            message = res.getString(R.string.fp_menu_notif_delete_success, intent.getStringExtra(AdapterLibrary.DATA_TITLE));
        }

        showToast(message, Toast.LENGTH_SHORT);
    }

    /**
     * Creates and displays a new toast message
     */
    private void showToast(final String message, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, duration).show();
            }
        });
    }

    /**
     * Cycle shuffle mode.
     */
    public void cycleShuffle() {
        setState(FpServiceRendering.get(this).cycleShuffle());
    }

    /**
     * Cycle the finish action.
     */
    public void cycleFinishAction() {
        setState(FpServiceRendering.get(this).cycleFinishAction());
    }

    /**
     * Open the library activity.
     *
     * @param song If non-null, will open the library focused on this song.
     */
    public void openLibrary(FpTrack song) {
        Intent intent = new Intent(this, ActivityLibrary.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (song != null) {
            intent.putExtra(ActivityLibrary.INTENT_ALBUM_ID, song.albumId);
            intent.putExtra(ActivityLibrary.INTENT_ALBUM, song.album);
            intent.putExtra(ActivityLibrary.INTENT_ARTIST, song.artist);
        }
        startActivity(intent);
    }

    @Override
    public void upSwipe() {
        performAction(FpAction.SeekForward);

        // Update the view
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 500);
    }

    @Override
    public void downSwipe() {
        performAction(FpAction.SeekBackward);

        // Update the view
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 500);
    }

    protected void performAction(FpAction action) {
        FpServiceRendering.get(this).performAction(action, this);
    }

    private static final int CTX_MENU_GRP_SHUFFLE = 200;
    private static final int CTX_MENU_GRP_FINISH = 201;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        if (view == mShuffleButton) {
            menu.add(CTX_MENU_GRP_SHUFFLE, FpTrackTimeline.SHUFFLE_NONE, 0, R.string.fp_menu_shuffle_none);
            menu.add(CTX_MENU_GRP_SHUFFLE, FpTrackTimeline.SHUFFLE_SONGS, 0, R.string.fp_menu_shuffle_all);
            menu.add(CTX_MENU_GRP_SHUFFLE, FpTrackTimeline.SHUFFLE_ALBUMS, 0, R.string.fp_menu_shuffle_albums);
        } else {
            if (view == mRepeatButton) {
                menu.add(CTX_MENU_GRP_FINISH, FpTrackTimeline.FINISH_STOP, 0, R.string.fp_menu_repeat_none);
                menu.add(CTX_MENU_GRP_FINISH, FpTrackTimeline.FINISH_REPEAT, 0, R.string.fp_menu_repeat_all);
                menu.add(CTX_MENU_GRP_FINISH, FpTrackTimeline.FINISH_REPEAT_CURRENT, 0, R.string.fp_menu_repeat_song);
                menu.add(CTX_MENU_GRP_FINISH, FpTrackTimeline.FINISH_RANDOM, 0, R.string.fp_menu_repeat_random);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int group = item.getGroupId();
        int id = item.getItemId();
        if (group == CTX_MENU_GRP_SHUFFLE) {
            setState(FpServiceRendering.get(this).setShuffleMode(id));
        } else {
            if (group == CTX_MENU_GRP_FINISH) {
                setState(FpServiceRendering.get(this).setFinishAction(id));
            }
        }
        return true;
    }

    /**
     * Fired if user dismisses the create-playlist dialog
     *
     * @param dialogInterface the dismissed interface dialog
     */
    class SaveAsPlaylistDismiss implements DialogInterface.OnDismissListener {

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            FpPlaylistPopupNew dialog = (FpPlaylistPopupNew) dialogInterface;
            if (dialog.isAccepted()) {
                String playlistName = dialog.getText();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SAVE_QUEUE_AS_PLAYLIST, playlistName));
            }
        }
    }
}