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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.backup.BackupManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.videolan.libvlc.MediaPlayer;

/**
 * Handles music playback and pretty much all the other work.
 */
public final class FpServiceRendering extends Service implements Handler.Callback, FpMediaPlayer.Listener, SharedPreferences.OnSharedPreferenceChangeListener, FpTrackTimeline.Callback, AudioManager.OnAudioFocusChangeListener {

    /**
     * Name of the state file.
     */
    protected static final String STATUS_FILE = "fp_status";
    
    /**
     * Notification ID
     */
    protected static final int NOTIFICATION_ID = 890711;

    /**
     * Rewind song if we already played more than 5 sec
     */
    protected static final int REWIND_AFTER_PLAYED_MS = 5000;

    /**
     * FpAction for startService: toggle playback on/off.
     */
    public static final String ACTION_TOGGLE_PLAYBACK = "com.fairplayer.fp.TOGGLE_PLAYBACK";
   
    /**
     * FpAction for startService: start playback if paused.
     */
    public static final String ACTION_PLAY = "com.fairplayer.fp.PLAY";
    
    /**
     * FpAction for startService: pause playback if playing.
     */
    public static final String ACTION_PAUSE = "com.fairplayer.fp.PAUSE";

    /**
     * FpAction for startService: toggle playback on/off.
     *
     * Unlike FpServiceRendering#ACTION_TOGGLE_PLAYBACK, the toggle does
     * not occur immediately. Instead, it is delayed so that if two of these
     * actions are received within 400 ms, the playback activity is opened
     * instead.
     */
    public static final String ACTION_TOGGLE_PLAYBACK_DELAYED = "com.fairplayer.fp.TOGGLE_PLAYBACK_DELAYED";
    
    /**
     * FpAction for startService: toggle playback on/off.
     *
     * This works the same way as ACTION_PLAY_PAUSE but prevents the
     * notification
     * from being hidden regardless of notification visibility settings.
     */
    public static final String ACTION_TOGGLE_PLAYBACK_NOTIFICATION = "com.fairplayer.fp.TOGGLE_PLAYBACK_NOTIFICATION";
    
    /**
     * FpAction for startService: advance to the next song.
     */
    public static final String ACTION_NEXT_SONG = "com.fairplayer.fp.NEXT_SONG";
    
    /**
     * FpAction for startService: advance to the next song.
     *
     * Unlike FpServiceRendering#ACTION_NEXT_SONG, the toggle does
     * not occur immediately. Instead, it is delayed so that if two of these
     * actions are received within 400 ms, the playback activity is opened
     * instead.
     */
    public static final String ACTION_NEXT_SONG_DELAYED = "com.fairplayer.fp.NEXT_SONG_DELAYED";
    
    /**
     * FpAction for startService: advance to the next song.
     *
     * Like ACTION_NEXT_SONG, but starts playing automatically if paused
     * when this is called.
     */
    public static final String ACTION_NEXT_SONG_AUTOPLAY = "com.fairplayer.fp.NEXT_SONG_AUTOPLAY";
    
    /**
     * FpAction for startService: go back to the previous song.
     */
    public static final String ACTION_PREVIOUS_SONG = "com.fairplayer.fp.PREVIOUS_SONG";
    
    /**
     * FpAction for startService: go back to the previous song.
     *
     * Like ACTION_PREVIOUS_SONG, but starts playing automatically if paused
     * when this is called.
     */
    public static final String ACTION_PREVIOUS_SONG_AUTOPLAY = "com.fairplayer.fp.PREVIOUS_SONG_AUTOPLAY";
    
    /**
     * Change the shuffle mode.
     */
    public static final String ACTION_CYCLE_SHUFFLE = "com.fairplayer.CYCLE_SHUFFLE";
    
    /**
     * Change the repeat mode.
     */
    public static final String ACTION_CYCLE_REPEAT = "com.fairplayer.CYCLE_REPEAT";
    
    /**
     * Pause music and hide the notifcation.
     */
    public static final String ACTION_CLOSE_NOTIFICATION = "com.fairplayer.CLOSE_NOTIFICATION";

    public static final int NEVER = 0;
    public static final int WHEN_PLAYING = 1;
    public static final int ALWAYS = 2;

    /**
     * If a user action is triggered within this time (in ms) after the
     * idle time fade-out occurs, playback will be resumed.
     */
    protected static final long IDLE_GRACE_PERIOD = 60000;
    
    /**
     * Defer entering deep sleep for this time (in ms).
     */
    protected static final int SLEEP_STATE_DELAY = 60000;
    
    /**
     * Save the current playlist state on queue changes after this time (in ms).
     */
    protected static final int SAVE_STATE_DELAY = 5000;
    
    /**
     * If set, music will play.
     */
    public static final int FLAG_PLAYING = 0x1;
    
    /**
     * Set when there is no media available on the device.
     */
    public static final int FLAG_NO_MEDIA = 0x2;
    
    /**
     * Set when the current song is unplayable.
     */
    public static final int FLAG_ERROR = 0x4;
    
    /**
     * Set when the user needs to select songs to play.
     */
    public static final int FLAG_EMPTY_QUEUE = 0x8;
    public static final int SHIFT_FINISH = 4;
    
    /**
     * These three bits will be one of FpTrackTimeline.FINISH_*.
     */
    public static final int MASK_FINISH = 0x7 << SHIFT_FINISH;
    public static final int SHIFT_SHUFFLE = 7;
    
    /**
     * These three bits will be one of FpTrackTimeline.SHUFFLE_*.
     */
    public static final int MASK_SHUFFLE = 0x7 << SHIFT_SHUFFLE;
    public static final int SHIFT_DUCKING = 10;
    
    /**
     * Whether we're 'ducking' (lowering the playback volume temporarily due to
     * a transient system
     * sound, such as a notification) or not
     */
    public static final int FLAG_DUCKING = 0x1 << SHIFT_DUCKING;

    /**
     * The FpServiceRendering state, indicating if the service is playing,
     * repeating, etc.
     *
     * The format of this is 0b00000000_00000000_000000gff_feeedcba,
     * where each bit is:
     * a: FpServiceRendering#FLAG_PLAYING
     * b: FpServiceRendering#FLAG_NO_MEDIA
     * c: FpServiceRendering#FLAG_ERROR
     * d: FpServiceRendering#FLAG_EMPTY_QUEUE
     * eee: FpServiceRendering#MASK_FINISH
     * fff: FpServiceRendering#MASK_SHUFFLE
     * g: FpServiceRendering#FLAG_DUCKING
     */
    int mState;

    /**
     * How many broken songs we did already skip
     */
    int mSkipBroken;

    /**
     * Object used for state-related locking.
     */
    final Object[] mStateLock = new Object[0];
    
    /**
     * Object used for FpServiceRendering startup waiting.
     */
    protected static final Object[] sWait = new Object[0];
    
    /**
     * The appplication-wide instance of the FpServiceRendering.
     */
    public static FpServiceRendering sInstance;
    
    /**
     * Static referenced-array to PlaybackActivities, used for callbacks
     */
    protected static final ArrayList<FpTrackTimelineCallback> sCallbacks = new ArrayList<FpTrackTimelineCallback>(5);
    
    /**
     * Cached app-wide SharedPreferences instance.
     */
    protected static SharedPreferences sSettings;
    
    /**
     * Behaviour of the notification
     */
    protected boolean mNotificationMode;
    
    /**
     * If true, create a notification with ticker text or heads up display
     */
    protected boolean mNotificationNag;
    
    /**
     * The time to wait before considering the player idle.
     */
    protected int mIdleTimeout;
    
    /**
     * The intent for the notification to execute, created by
     * FpServiceRendering#createNotificationAction(SharedPreferences).
     */
    protected PendingIntent mNotificationAction;

    protected Looper mLooper;
    protected Handler mHandler;
    FpMediaPlayer mMediaPlayer;
    FpMediaPlayer mPreparedMediaPlayer;
    protected boolean mMediaPlayerInitialized;
    protected boolean mMediaPlayerAudioFxActive;
    protected PowerManager.WakeLock mWakeLock;
    protected NotificationManager mNotificationManager;
    protected AudioManager mAudioManager;
    
    /**
     * A remote control client implementation
     */
    protected FpRemoteControl.Client mRemoteControlClient;

    FpTrackTimeline mTimeline;
    protected FpTrack mCurrentSong;

    /**
     * Stores the saved position in the current song from saved state. Should
     * be seeked to when the song is loaded into MediaPlayer. Used only during
     * initialization. The song that the saved position is for is stored in
     * #mPendingSeekSong.
     */
    protected int mPendingSeek;
    
    /**
     * The id of the song that the mPendingSeek position is for. -1 indicates
     * an invalid song. Value is undefined when mPendingSeek is 0.
     */
    protected long mPendingSeekSong;
    public Receiver mReceiver;
    protected String mErrorMessage;
    
    /**
     * Current fade-out progress. 1.0f if we are not fading out
     */
    protected float mFadeOut = 1.0f;
    
    /**
     * Elapsed realtime at which playback was paused by idle timeout. -1
     * indicates that no timeout has occurred.
     */
    protected long mIdleStart = -1;
    
    /**
     * True if we encountered a transient audio loss
     */
    protected boolean mTransientAudioLoss;
    
    /**
     * Seek step (seconds)
     */
    protected int mSeekStep;
    
    /**
     * If true, the notification should not be hidden when pausing regardless
     * of user settings.
     */
    protected boolean mForceNotificationVisible;
    
    /**
     * Percentage to set the volume as while a notification is playing (aka
     * ducking)
     */
    protected int mBlurKeepPlayingVolume;
    
    /**
     *
     */
    protected boolean mBlurKeepPlaying;
    
    /**
     * Reference to precreated ReadAhead thread
     */
    protected FpThreadPreloader mReadahead;
    
    /**
     * Reference to Playcounts helper class
     */
    protected FpPlayCounter mPlayCounts;

    @Override
    public void onCreate() {
        // Set the context
        ActivityCommon.setContext(this.getApplicationContext());
        
        // Prepare the thread
        HandlerThread thread = new HandlerThread(FpServiceRendering.class.getSimpleName(), Process.THREAD_PRIORITY_DEFAULT);
        thread.start();
        
        mTimeline = new FpTrackTimeline(this);
        mTimeline.setCallback(this);
        int state = loadState();

        mPlayCounts = new FpPlayCounter(this);

        // Prepare the media player
        mMediaPlayer = new FpMediaPlayer(this);
        mPreparedMediaPlayer = new FpMediaPlayer(this);
        
        // Update the player's equalizer
        EqualizerUtil.updatePlayer(mMediaPlayer.getPlayer());
        
        // We only have a single audio session
        mPreparedMediaPlayer.setAudioSessionId(mMediaPlayer.getAudioSessionId());
        
        mReadahead = new FpThreadPreloader();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        SharedPreferences settings = PreferenceUtils.getPreferences(this.getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(this);
        mSeekStep = settings.getInt(Constants.Keys.SETTINGS_SEEK_STEP, Constants.Defaults.SETTINGS_SEEK_STEP);
        mNotificationMode = settings.getBoolean(Constants.Keys.SETTINGS_STATUS_BAR_ENABLED, Constants.Defaults.SETTINGS_STATUS_BAR_ENABLED);
        mNotificationNag = settings.getBoolean(Constants.Keys.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED, Constants.Defaults.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED);
        mIdleTimeout = settings.getBoolean(Constants.Keys.SETTINGS_TIMEOUT_ENABLED, Constants.Defaults.SETTINGS_TIMEOUT_ENABLED) ? settings.getInt(Constants.Keys.SETTINGS_TIMEOUT_VALUE, Constants.Defaults.SETTINGS_TIMEOUT_VALUE) : 0;

        // Set the cover mode
        FpCoverStore.mCoverLoadMode = FpCoverStore.mCoverLoadMode | FpCoverStore.COVER_MODE_ANDROID | FpCoverStore.COVER_MODE_CUSTOM | FpCoverStore.COVER_MODE_SHADOW;

        mNotificationAction = createNotificationAction(settings);
        mBlurKeepPlayingVolume = settings.getInt(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_VOLUME, Constants.Defaults.SETTINGS_BLUR_KEEP_PLAYING_VOLUME);
        mBlurKeepPlaying = settings.getBoolean(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_ENABLED, Constants.Defaults.SETTINGS_BLUR_KEEP_PLAYING_ENABLED);
        refreshDuckingValues();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "a1 Music Lock");

        mReceiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);

        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mObserver);

        mRemoteControlClient = new FpRemoteControl().getClient(this);
        mRemoteControlClient.initializeRemote();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

        updateState(state);
        setCurrentSong(0);

        sInstance = this;
        synchronized (sWait) {
            sWait.notifyAll();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            // Get the action
            String action = intent.getAction();
            
            do {
                // No action
                if (null == action || action.length() == 0) {
                    break;
                }
            
            	// Play/pause
            	if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
                    playPause();
                    break;
                }
            	
            	// Toggle playback notification
            	if (action.equals(ACTION_TOGGLE_PLAYBACK_NOTIFICATION)) {
                    mForceNotificationVisible = true;
                    synchronized (mStateLock) {
                        if ((mState & FLAG_PLAYING) != 0) {
                            pause();
                        } else {
                            play();
                        }
                    }
                    
                    break;
                }
            	
            	// Toggle playback delayed
            	if (action.equals(ACTION_TOGGLE_PLAYBACK_DELAYED)) {
                    if (mHandler.hasMessages(MSG_CALL_GO, Integer.valueOf(0))) {
                        mHandler.removeMessages(MSG_CALL_GO, Integer.valueOf(0));
                        Intent launch = new Intent(this, ActivityLibrary.class);
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        launch.setAction(Intent.ACTION_MAIN);
                        startActivity(launch);
                    } else {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CALL_GO, 0, 0, Integer.valueOf(0)), 400);
                    }
                    
                    break;
                }
            	
            	// Next song
            	if (action.equals(ACTION_NEXT_SONG)) {
                    shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_SONG);
                    break;
                }
            	
            	// Next song (autoplay)
            	if (action.equals(ACTION_NEXT_SONG_AUTOPLAY)) {
                    shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_SONG);
                    play();
                    break;
                }
            	
            	// Next song (delayed)
            	if (action.equals(ACTION_NEXT_SONG_DELAYED)) {
                    if (mHandler.hasMessages(MSG_CALL_GO, Integer.valueOf(1))) {
                        mHandler.removeMessages(MSG_CALL_GO, Integer.valueOf(1));
                        Intent launch = new Intent(this, ActivityLibrary.class);
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        launch.setAction(Intent.ACTION_MAIN);
                        startActivity(launch);
                    } else {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CALL_GO, 1, 0, Integer.valueOf(1)), 400);
                    }
                    
                    break;
                }
            	
            	// Previous song
            	if (action.equals(ACTION_PREVIOUS_SONG)) {
                    rewindCurrentSong();
                    
                    break;
                }
            	
            	// Previous song (autoplay)
            	if (action.equals(ACTION_PREVIOUS_SONG_AUTOPLAY)) {
                    rewindCurrentSong();
                    play();
                    
                    break;
                }
            	
            	// Play
            	if (action.equals(ACTION_PLAY)) {
                    play();
                    
                    break;
                }

            	// Pause
            	if (action.equals(ACTION_PAUSE)) {
                    pause();
                    
                    break;
                }
            	
            	// Repeat
            	if (action.equals(ACTION_CYCLE_REPEAT)) {
                    cycleFinishAction();
                    
                    break;
                }
            	
            	// Shuffle
            	if (action.equals(ACTION_CYCLE_SHUFFLE)) {
                    cycleShuffle();
                    
                    break;
                }
            	
            	// Close notification
            	if (action.equals(ACTION_CLOSE_NOTIFICATION)) {
                    mForceNotificationVisible = false;
                    pause();
                    stopForeground(true);
                    updateNotification();
                    
                    break;
                }
            	
            } while (false);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        mLooper.quit();

        // clear the notification
        stopForeground(true);

        // defer wakelock and close audioFX
        enterSleepState();

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mPreparedMediaPlayer != null) {
            mPreparedMediaPlayer.release();
            mPreparedMediaPlayer = null;
        }

        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // we haven't registered the receiver yet
        }

        if (mRemoteControlClient != null) {
            mRemoteControlClient.unregisterRemote();
        }

        super.onDestroy();
    }

    public FpMediaPlayer getFpMediaPlayer() {
        return mMediaPlayer;
    }
    
    public void prepareMediaPlayer(FpMediaPlayer mp, String path) throws IOException {
        mp.setDataSource(path);
        mp.updateVolume();
    }

    /**
     * Refresh the media player's volume
     */
    protected void refreshVolume() {
        mMediaPlayer.updateVolume();
        mPreparedMediaPlayer.updateVolume();
    }

    protected void refreshDuckingValues() {
        float duckingFactor = ((float) mBlurKeepPlayingVolume) / 100f;
        mMediaPlayer.setDuckingFactor(duckingFactor);
        mPreparedMediaPlayer.setDuckingFactor(duckingFactor);
    }

    /**
     * Prepares FpServiceRendering to sleep / shutdown
     * Closes any open AudioFX session and releases
     * our wakelock if held
     */
    protected void enterSleepState() {
        if (mMediaPlayer != null) {
            if (mMediaPlayerAudioFxActive) {
                mMediaPlayer.closeAudioFx();
                mMediaPlayerAudioFxActive = false;
            }
            saveState(mMediaPlayer.getCurrentPosition());
        }

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**
     * Destroys any currently prepared MediaPlayer and
     * re-creates a newone if needed.
     */
    protected void triggerGaplessUpdate() {
        if (mMediaPlayerInitialized != true) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return; 
        }

        boolean doGapless = false;
        int fa = finishAction(mState);
        FpTrack nextSong = getSong(1);

        if (nextSong != null
                && fa != FpTrackTimeline.FINISH_REPEAT_CURRENT
                && !mTimeline.isEndOfQueue()) {
            doGapless = true;
        } else {
            Log.d(Constants.LOG_TAG, "Must not create new media player object");
        }

        if (doGapless == true) {
            try {
                if (nextSong.path.equals(mPreparedMediaPlayer.getDataSource()) == false) {
                    // Prepared MP has a different data source: We need to re-initalize
                    // it and set it as the next MP for the active media player
                    mPreparedMediaPlayer.reset();
                    prepareMediaPlayer(mPreparedMediaPlayer, nextSong.path);
                }
            } catch (IOException e) {
                mPreparedMediaPlayer.reset();
            }
        }
    }

    /**
     * Stops or starts the readahead thread
     */
    protected void triggerReadAhead() {
        FpTrack song = mCurrentSong;
        if ((mState & FLAG_PLAYING) != 0 && song != null) {
            mReadahead.setSource(song.path);
        } else {
            mReadahead.pause();
        }
    }

    protected void loadPreference(String key) {
    	SharedPreferences settings = PreferenceUtils.getPreferences(this.getApplicationContext());
    	do {
    	    // Invalid key
    	    if (null == key) {
    	        break;
    	    }

    	    // Status bar enabled
    	    if (key.equals(Constants.Keys.SETTINGS_STATUS_BAR_ENABLED)) {
    	        mNotificationMode = settings.getBoolean(Constants.Keys.SETTINGS_STATUS_BAR_ENABLED, Constants.Defaults.SETTINGS_STATUS_BAR_ENABLED);
    	        // The only way to remove the notification
    	        stopForeground(true);
    	        updateNotification();
    	        break;
    	    }

            if (key.equals(Constants.Keys.SETTINGS_SEEK_STEP)) {
    	        mSeekStep = settings.getInt(Constants.Keys.SETTINGS_SEEK_STEP, Constants.Defaults.SETTINGS_SEEK_STEP);
    	        updateNotification();
    	        break;
    	    }

    	    if (key.equals(Constants.Keys.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED)) {
    	        mNotificationNag = settings.getBoolean(Constants.Keys.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED, Constants.Defaults.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED);
    	        updateNotification();
    	        break;
    	    }

    	    if (key.equals(Constants.Keys.SETTINGS_TIMEOUT_ENABLED) || key.equals(Constants.Keys.SETTINGS_TIMEOUT_VALUE)) {
    	        mIdleTimeout = settings.getBoolean(Constants.Keys.SETTINGS_TIMEOUT_ENABLED, Constants.Defaults.SETTINGS_TIMEOUT_ENABLED) ? settings.getInt(Constants.Keys.SETTINGS_TIMEOUT_VALUE, Constants.Defaults.SETTINGS_TIMEOUT_VALUE) : 0;
    	        userActionTriggered();
    	        break;
    	    }

    	    if (key.equals(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_VOLUME)) {
    	        mBlurKeepPlayingVolume = settings.getInt(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_VOLUME, Constants.Defaults.SETTINGS_BLUR_KEEP_PLAYING_VOLUME);
    	        refreshDuckingValues();
    	        break;
    	    }

    	    if (key.equals(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_ENABLED)) {
    	        mBlurKeepPlaying = settings.getBoolean(Constants.Keys.SETTINGS_BLUR_KEEP_PLAYING_ENABLED, Constants.Defaults.SETTINGS_BLUR_KEEP_PLAYING_ENABLED);
    	        break;
    	    }

    	} while (false);
    	
        /*
         * Tell androids cloud-backup manager that we just changed our
         * preferences
         */
        (new BackupManager(this)).dataChanged();
    }

    /**
     * Set a state flag.
     */
    public void setFlag(int flag) {
        synchronized (mStateLock) {
            updateState(mState | flag);
        }
    }

    /**
     * Unset a state flag.
     */
    public void unsetFlag(int flag) {
        synchronized (mStateLock) {
            updateState(mState & ~flag);
        }
    }

    /**
     * Return true if audio would play through the speaker.
     */
    @SuppressWarnings("deprecation")
    protected boolean isSpeakerOn() {
        // Android seems very intent on making this difficult to detect. In
        // Android 1.5, this worked great with AudioManager.getRouting(),
        // which definitively answered if audio would play through the speakers.
        // Android 2.0 deprecated this method and made it no longer function.
        // So this hacky alternative was created. But with Android 4.0,
        // isWiredHeadsetOn() was deprecated, though it still works. But for
        // how much longer?
        //
        // I'd like to remove this feature so I can avoid fighting Android to
        // keep it working, but some users seem to really like it. I think the
        // best solution to this problem is for Android to have separate media
        // volumes for speaker, headphones, etc. That way the speakers can be
        // muted system-wide. There is not much I can do about that here,
        // though.
        return !mAudioManager.isWiredHeadsetOn() && !mAudioManager.isBluetoothA2dpOn() && !mAudioManager.isBluetoothScoOn();
    }

    /**
     * Modify the service state.
     *
     * @param state Union of FpServiceRendering.STATE_* flags
     *
     * @return The new state
     */
    protected int updateState(int state) {
        if ((state & (FLAG_NO_MEDIA | FLAG_ERROR | FLAG_EMPTY_QUEUE)) != 0) {
            state &= ~FLAG_PLAYING;
        }

        int oldState = mState;
        mState = state;

        if (state != oldState) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_STATE, oldState, state));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_CHANGE, state, 0, new FpTimestamp(null)));
        }

        return state;
    }

    protected void processNewState(int oldState, int state) {
        int toggled = oldState ^ state;

        if (((toggled & FLAG_PLAYING) != 0) && mCurrentSong != null) { // user requested to start playback AND we have a song selected
            if ((state & FLAG_PLAYING) != 0) {

                // We get noisy: Acquire a new AudioFX session if required
                if (mMediaPlayerAudioFxActive == false) {
                    mMediaPlayer.openAudioFx();
                    mMediaPlayerAudioFxActive = true;
                }

                if (mMediaPlayerInitialized) {
                    mMediaPlayer.start();
                }

                if (mNotificationMode) {
                    startForeground(NOTIFICATION_ID, createNotification(mCurrentSong, mState, mNotificationMode));
                }

                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                mHandler.removeMessages(MSG_ENTER_SLEEP_STATE);
                try {
                    if (mWakeLock != null && mWakeLock.isHeld() == false) {
                        mWakeLock.acquire();
                    }
                } catch (SecurityException e) {
                    // Don't have WAKE_LOCK permission
                }
            } else {
                if (mMediaPlayerInitialized) {
                    mMediaPlayer.pause();
                }

                // We are switching into background mode. The notification will be removed
                // unless we forcefully show it (or the user selected to always show it)
                // In both cases we will update the notification to reflect the
                // actual playback state (or to hit cancel() as this is required to
                // get rid of it if it was created via notify())
                boolean removeNotification = (mForceNotificationVisible == false && !mNotificationMode);
                stopForeground(removeNotification);
                updateNotification();

                // Delay entering deep sleep. This allows the headset
                // button to continue to function for a short period after
                // pausing and keeps the AudioFX session open
                mHandler.sendEmptyMessageDelayed(MSG_ENTER_SLEEP_STATE, SLEEP_STATE_DELAY);
            }
        }

        if ((toggled & FLAG_NO_MEDIA) != 0 && (state & FLAG_NO_MEDIA) != 0) {
            FpTrack song = mCurrentSong;
            if (song != null && mMediaPlayerInitialized) {
                mPendingSeek = mMediaPlayer.getCurrentPosition();
                mPendingSeekSong = song.id;
            }
        }

        if ((toggled & MASK_SHUFFLE) != 0) {
            mTimeline.setShuffleMode(shuffleMode(state));
        }
        if ((toggled & MASK_FINISH) != 0) {
            mTimeline.setFinishAction(finishAction(state));
        }

        if ((toggled & FLAG_DUCKING) != 0) {
            boolean isDucking = (state & FLAG_DUCKING) != 0;
            mMediaPlayer.setIsDucking(isDucking);
            mPreparedMediaPlayer.setIsDucking(isDucking);
        }
    }

    protected void broadcastChange(int state, FpTrack song, long uptime) {
        if (state != -1) {
            ArrayList<FpTrackTimelineCallback> list = sCallbacks;
            for (int i = list.size(); --i != -1;) {
                list.get(i).setState(uptime, state);
            }
        }

        if (song != null) {
            ArrayList<FpTrackTimelineCallback> list = sCallbacks;
            for (int i = list.size(); --i != -1;) {
                list.get(i).setSong(uptime, song);
            }
        }

        updateWidgets();
        triggerReadAhead();

        mRemoteControlClient.updateRemote(mCurrentSong, mState, mForceNotificationVisible);
        stockMusicBroadcast();
    }

    /**
     * Update the widgets with the current song and state.
     */
    public void updateWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        WidgetBar.updateWidget(this, manager, mCurrentSong, mState);
        WidgetSquare.updateWidget(this, manager, mCurrentSong, mState);
    }

    /**
     * Send a broadcast emulating that of the stock music player.
     */
    protected void stockMusicBroadcast() {
        FpTrack song = mCurrentSong;
        Intent intent = new Intent(Constants.ANDROID_LOCKSCREEN_LISTENER);
        intent.putExtra(ActivityLibrary.INTENT_PLAYING, (mState & FLAG_PLAYING) != 0);
        if (song != null) {
            intent.putExtra(ActivityLibrary.INTENT_SONG_TITLE, song.title);
            intent.putExtra(ActivityLibrary.INTENT_SONG_ID, song.id);
            intent.putExtra(ActivityLibrary.INTENT_ALBUM, song.album);
            intent.putExtra(ActivityLibrary.INTENT_ALBUM_ID, song.albumId);
            intent.putExtra(ActivityLibrary.INTENT_ARTIST, song.artist);
        }
        sendBroadcast(intent);
    }

    public void updateNotification() {
        if ((mForceNotificationVisible || mNotificationMode && (mState & FLAG_PLAYING) != 0) && mCurrentSong != null) {
            mNotificationManager.notify(NOTIFICATION_ID, createNotification(mCurrentSong, mState, mNotificationMode));
        } else {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    /**
     * When playing through MirrorLink(tm) don't interact
     * with the User directly as this is considered distracting
     * while driving
     */
    protected void showMirrorLinkSafeToast(int resId, int duration) {
        Toast.makeText(this, resId, duration).show();
    }

    protected void showMirrorLinkSafeToast(CharSequence text, int duration) {
        Toast.makeText(this, text, duration).show();
    }

    /**
     * Start playing if currently paused.
     *
     * @return The new state after this is called.
     */
    public int play() {
        synchronized (mStateLock) {
            if ((mState & FLAG_EMPTY_QUEUE) != 0) {
                setFinishAction(FpTrackTimeline.FINISH_RANDOM);
                setCurrentSong(0);
                showMirrorLinkSafeToast(R.string.fp_menu_notif_random_enabled, Toast.LENGTH_SHORT);
            }

            int state = updateState(mState | FLAG_PLAYING);
            userActionTriggered();
            return state;
        }
    }

    /**
     * Pause if currently playing.
     *
     * @return The new state after this is called.
     */
    public int pause() {
        synchronized (mStateLock) {
            mTransientAudioLoss = false; // do not resume playback as this pause was user initiated
            int state = updateState(mState & ~FLAG_PLAYING & ~FLAG_DUCKING);
            userActionTriggered();
            return state;
        }
    }

    /**
     * If playing, pause. If paused, play.
     *
     * @return The new state after this is called.
     */
    public int playPause() {
        mForceNotificationVisible = false;
        synchronized (mStateLock) {
            if ((mState & FLAG_PLAYING) != 0) {
                return pause();
            } else {
                return play();
            }
        }
    }

    /**
     * Change the end action (e.g. repeat, random).
     *
     * @param action The new action. One of FpTrackTimeline.FINISH_*.
     *
     * @return The new state after this is called.
     */
    public int setFinishAction(int action) {
        synchronized (mStateLock) {
            return updateState(mState & ~MASK_FINISH | action << SHIFT_FINISH);
        }
    }

    /**
     * Cycle repeat mode. Disables random mode.
     *
     * @return The new state after this is called.
     */
    public int cycleFinishAction() {
        synchronized (mStateLock) {
            int mode = finishAction(mState) + 1;
            if (mode > FpTrackTimeline.FINISH_RANDOM) {
                mode = FpTrackTimeline.FINISH_STOP;
            }
            return setFinishAction(mode);
        }
    }

    /**
     * Change the shuffle mode.
     *
     * @param mode The new mode. One of FpTrackTimeline.SHUFFLE_*.
     *
     * @return The new state after this is called.
     */
    public int setShuffleMode(int mode) {
        synchronized (mStateLock) {
            return updateState(mState & ~MASK_SHUFFLE | mode << SHIFT_SHUFFLE);
        }
    }

    /**
     * Cycle shuffle mode.
     *
     * @return The new state after this is called.
     */
    public int cycleShuffle() {
        synchronized (mStateLock) {
            int mode = shuffleMode(mState) + 1;
            if (mode > FpTrackTimeline.SHUFFLE_ALBUMS) {
                mode = FpTrackTimeline.SHUFFLE_NONE; // end reached: switch to none
            }
            return setShuffleMode(mode);
        }
    }

    /**
     * Move to the next or previous song or album in the timeline.
     *
     * @param delta One of FpTrackTimeline.SHIFT_*. 0 can also be passed to
     * initialize the current song with media player, notification,
     * broadcasts, etc.
     *
     * @return The new current song
     */
    protected FpTrack setCurrentSong(int delta) {
        if (mMediaPlayer == null) {
            return null;
        }

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }

        FpTrack song = mTimeline.shiftCurrentSong(delta);
        mCurrentSong = song;
        if (song == null) {
            if (FpUtilsMedia.isSongAvailable(getContentResolver())) {
                int flag = finishAction(mState) == FpTrackTimeline.FINISH_RANDOM ? FLAG_ERROR : FLAG_EMPTY_QUEUE;
                synchronized (mStateLock) {
                    updateState((mState | flag) & ~FLAG_NO_MEDIA);
                }
                return null;
            } 

            // we don't have any songs : /
            synchronized (mStateLock) {
                updateState((mState | FLAG_NO_MEDIA) & ~FLAG_EMPTY_QUEUE);
            }
            return null;
        }

        if ((mState & (FLAG_NO_MEDIA | FLAG_EMPTY_QUEUE)) != 0) {
            synchronized (mStateLock) {
                updateState(mState & ~(FLAG_EMPTY_QUEUE | FLAG_NO_MEDIA));
            }
        }

        mHandler.removeMessages(MSG_PROCESS_SONG);
        mMediaPlayerInitialized = false;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_SONG, song));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_CHANGE, -1, 0, new FpTimestamp(song)));
        return song;
    }

    protected void processSong(FpTrack song) {
        /*
         * Save our 'current' state as the try block may set the ERROR flag
         * (which clears the PLAYING flag
         */
        boolean playing = (mState & FLAG_PLAYING) != 0;

        try {
            mMediaPlayerInitialized = false;
            mMediaPlayer.reset();

            if (mPreparedMediaPlayer.isPlaying()) {
                // The prepared media player is playing as the previous song
                // reched its end 'naturally' (-> gapless)
                // We can now swap mPreparedMediaPlayer and mMediaPlayer
                FpMediaPlayer tmpPlayer = mMediaPlayer;
                mMediaPlayer = mPreparedMediaPlayer;
                mPreparedMediaPlayer = tmpPlayer; // this was mMediaPlayer and is in reset() state
            } else {
                prepareMediaPlayer(mMediaPlayer, song.path);
            }

            mMediaPlayerInitialized = true;
            // Cancel any pending gapless updates and re-send them
            mHandler.removeMessages(MSG_GAPLESS_UPDATE);
            mHandler.sendEmptyMessage(MSG_GAPLESS_UPDATE);

            if (mPendingSeek != 0 && mPendingSeekSong == song.id) {
                mMediaPlayer.seekTo(mPendingSeek);
                mPendingSeek = 0;
            }

            if ((mState & FLAG_PLAYING) != 0) {
                mMediaPlayer.start();
            }

            if ((mState & FLAG_ERROR) != 0) {
                mErrorMessage = null;
                updateState(mState & ~FLAG_ERROR);
            }
            mSkipBroken = 0; /*
             * File not broken, reset skip counter
             */

        } catch (IOException e) {
            mErrorMessage = getResources().getString(R.string.fp_menu_notif_track_error, song.path);
            updateState(mState | FLAG_ERROR);
            showMirrorLinkSafeToast(mErrorMessage, Toast.LENGTH_LONG);
            Log.e(Constants.LOG_TAG, "IOException", e);

            /*
             * Automatically advance to next song IF we are currently playing or
             * already did skip something
             * This will stop after skipping 10 songs to avoid endless loops
             * (queue full of broken stuff
             */
            if (mTimeline.isEndOfQueue() == false && getSong(1) != null && (playing || (mSkipBroken > 0 && mSkipBroken < 10))) {
                mSkipBroken++;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SKIP_BROKEN_SONG, getTimelinePosition(), 0), 1000);
            }

        }

        updateNotification();

    }

    @Override
    public void onCompletion(MediaPlayer player) {
        // Count this song as played
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_PLAYCOUNTS, mCurrentSong), 2500);
        if (finishAction(mState) == FpTrackTimeline.FINISH_REPEAT_CURRENT) {
            setCurrentSong(0);
        } else {
            if (mTimeline.isEndOfQueue()) {
                unsetFlag(FLAG_PLAYING);
            } else {
                setCurrentSong(+1);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Log.e(Constants.LOG_TAG, "MediaPlayer error: " + what + ' ' + extra);
        return true;
    }

    /**
     * Returns the song <code>delta</code> places away from the current
     * position.
     *
     * @see FpTrackTimeline#getSong(int)
     */
    public FpTrack getSong(int delta) {
        if (mTimeline == null) {
            return null;
        }
        if (delta == 0) {
            return mCurrentSong;
        }
        return mTimeline.getSong(delta);
    }

    protected class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();

            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                pause();
            } else {
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    userActionTriggered();
                }
            }
        }
    }

    public void onMediaChange() {
        if (FpUtilsMedia.isSongAvailable(getContentResolver())) {
            if ((mState & FLAG_NO_MEDIA) != 0) {
                setCurrentSong(0);
            }
        } else {
            setFlag(FLAG_NO_MEDIA);
        }

        ArrayList<FpTrackTimelineCallback> list = sCallbacks;
        for (int i = list.size(); --i != -1;) {
            list.get(i).onMediaChange();
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        Log.i(Constants.LOG_TAG, "Shared preferences changed " + key);
        loadPreference(key);
    }

    /**
     * Releases mWakeLock and closes any open AudioFx sessions
     */
    protected static final int MSG_ENTER_SLEEP_STATE = 1;
    /**
     * Run the given query and add the results to the timeline.
     *
     * obj is the FpUtilsMedia.QueryTask. arg1 is the add mode (one of FpTrackTimeline.MODE_*)
     */
    protected static final int MSG_QUERY = 2;
    /**
     * This message is sent with a delay specified by a user preference. After
     * this delay, assuming no new SETTINGS_TIMEOUT_VALUE messages cancel it, playback
     * will be stopped.
     */
    protected static final int MSG_IDLE_TIMEOUT = 4;
    /**
     * Decrease the volume gradually over five seconds, pausing when 0 is
     * reached.
     *
     * arg1 should be the progress in the fade as a percentage, 1-100.
     */
    protected static final int MSG_FADE_OUT = 7;
    /**
     * If arg1 is 0, calls FpServiceRendering#playPause().
     * Otherwise, calls FpServiceRendering#setCurrentSong(int) with arg1.
     */
    protected static final int MSG_CALL_GO = 8;
    protected static final int MSG_BROADCAST_CHANGE = 10;
    protected static final int MSG_SAVE_STATE = 12;
    protected static final int MSG_PROCESS_SONG = 13;
    protected static final int MSG_PROCESS_STATE = 14;
    protected static final int MSG_SKIP_BROKEN_SONG = 15;
    protected static final int MSG_GAPLESS_UPDATE = 16;
    protected static final int MSG_UPDATE_PLAYCOUNTS = 17;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_CALL_GO:
                if (message.arg1 == 0) {
                    playPause();
                } else {
                    setCurrentSong(message.arg1);
                }
                break;

            case MSG_SAVE_STATE:
                // For unexpected terminations: crashes, task killers, etc.
                // In most cases onDestroy will handle this
                saveState(0);
                break;

            case MSG_PROCESS_SONG:
                processSong((FpTrack) message.obj);
                break;

            case MSG_QUERY:
                runQuery((FpUtilsMedia.QueryTask) message.obj);
                break;

            case MSG_IDLE_TIMEOUT:
                if ((mState & FLAG_PLAYING) != 0) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_FADE_OUT, 0));
                }
                break;
            case MSG_FADE_OUT:
                if (mFadeOut <= 0.0f) {
                    mIdleStart = SystemClock.elapsedRealtime();
                    unsetFlag(FLAG_PLAYING);
                } else {
                    mFadeOut -= 0.01f;
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_FADE_OUT, 0), 50);
                }
                refreshVolume();

                break;
            case MSG_PROCESS_STATE:
                processNewState(message.arg1, message.arg2);
                break;
            case MSG_BROADCAST_CHANGE:
                FpTimestamp tso = (FpTimestamp) message.obj;
                broadcastChange(message.arg1, (FpTrack) tso.object, tso.uptime);
                break;
            case MSG_ENTER_SLEEP_STATE:
                enterSleepState();
                break;
            case MSG_SKIP_BROKEN_SONG:
                /*
                 * Advance to next song if the user didn't already change.
                 * But we are restoring the Playing state in ANY case as we are
                 * most
                 * likely still stopped due to the error
                 * Note: This is somewhat racy with user input but also is the -
                 * by far - simplest
                 * solution
                 */
                if (getTimelinePosition() == message.arg1) {
                    setCurrentSong(1);
                }
                // Optimistically claim to have recovered from this error
                mErrorMessage = null;
                unsetFlag(FLAG_ERROR);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CALL_GO, 0, 0));
                break;
            case MSG_GAPLESS_UPDATE:
                triggerGaplessUpdate();
                break;
            case MSG_UPDATE_PLAYCOUNTS:
                FpTrack song = (FpTrack) message.obj;
                mPlayCounts.countSong(song);
                break;
            default:
                return false;
        }

        return true;
    }

    /**
     * Returns the current service state. The state comprises several individual
     * flags.
     */
    public int getState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    /**
     * Returns the current position in current song in milliseconds.
     */
    public int getPosition() {
        if (!mMediaPlayerInitialized) {
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    /**
     * Returns the song duration in milliseconds.
     */
    public int getDuration() {
        if (!mMediaPlayerInitialized) {
            return 0;
        }
        return mMediaPlayer.getDuration();
    }

    /**
     * Returns the global audio session
     */
    public int getAudioSession() {
        // Must not be 'ready' or initialized: the audio session
        // is set on object creation
        return mMediaPlayer.getAudioSessionId();
    }

    /**
     * Seek to a position in the current song.
     *
     * @param progress Proportion of song completed (where 1000 is the end of
     * the song)
     */
    public void seekToProgress(int progress) {
        if (!mMediaPlayerInitialized) {
            return;
        }
        int duration = mMediaPlayer.getDuration();
        
        long position = (long) duration * progress / 1000;
        Log.i(Constants.LOG_TAG, "Seek to " + String.valueOf(progress) + ", duration = " + String.valueOf(duration) + ", position = " + position);
        
        mMediaPlayer.seekTo((int) position);
    }

    @Override
    public IBinder onBind(Intent intents) {
        return null;
    }

    @Override
    public void activeSongReplaced(int delta, FpTrack song) {
        ArrayList<FpTrackTimelineCallback> list = sCallbacks;
        for (int i = list.size(); --i != -1;) {
            list.get(i).replaceSong(delta, song);
        }

        if (delta == 0) {
            setCurrentSong(0);
        }
    }

    /**
     * Delete all the songs in the given media set. Should be run on a
     * background thread.
     *
     * @param type One of the TYPE_* constants, excluding playlists.
     * @param id The MediaStore id of the media to delete.
     *
     * @return The number of songs deleted.
     */
    public int deleteMedia(int type, long id) {
        int count = 0;

        ContentResolver resolver = getContentResolver();
        String[] projection = new String[] {MediaStore.MediaColumns._ID, MediaStore.Audio.Media.DATA};
        Cursor cursor = FpUtilsMedia.buildQuery(type, id, projection, null).runQuery(resolver);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (new File(cursor.getString(1)).delete()) {
                    long songId = cursor.getLong(0);
                    String where = MediaStore.MediaColumns._ID + '=' + songId;
                    resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, null);
                    mTimeline.removeSong(songId);
                    ++count;
                }
            }

            cursor.close();
        }

        return count;
    }

    /**
     * Move to next or previous song or album in the queue.
     *
     * @param delta One of FpTrackTimeline.SHIFT_*.
     *
     * @return The new current song.
     */
    public FpTrack shiftCurrentSong(int delta) {
        FpTrack song = setCurrentSong(delta);
        userActionTriggered();
        return song;
    }

    /**
     * Skips to the previous song OR rewinds the currently playing track
     *
     * @return The new current song
     */
    public FpTrack rewindCurrentSong() {
        int delta = FpTrackTimeline.SHIFT_PREVIOUS_SONG;
        if (isPlaying() && getPosition() > REWIND_AFTER_PLAYED_MS && getDuration() > REWIND_AFTER_PLAYED_MS * 2) {
            delta = FpTrackTimeline.SHIFT_KEEP_SONG;
            // Count song as played if >= 80% were done
            double pctPlayed = (double) getPosition() / getDuration();
            if (pctPlayed >= 0.8) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_PLAYCOUNTS, mCurrentSong), 2500);
            }
        }
        return shiftCurrentSong(delta);
    }

    /**
     * Resets the idle timeout countdown. Should be called by a user action
     * has been triggered (new song chosen or playback toggled).
     *
     * If an idle fade out is actually in progress, aborts it and resets the
     * volume.
     */
    public void userActionTriggered() {
        mHandler.removeMessages(MSG_FADE_OUT);
        mHandler.removeMessages(MSG_IDLE_TIMEOUT);
        if (mIdleTimeout != 0) {
            mHandler.sendEmptyMessageDelayed(MSG_IDLE_TIMEOUT, mIdleTimeout * 1000);
        }

        if (mFadeOut != 1.0f) {
            mFadeOut = 1.0f;
            refreshVolume();
        }

        long idleStart = mIdleStart;
        if (idleStart != -1 && SystemClock.elapsedRealtime() - idleStart < IDLE_GRACE_PERIOD) {
            mIdleStart = -1;
            setFlag(FLAG_PLAYING);
        }
    }

    /**
     * Run the query and add the results to the timeline. Should be called in
     * the
     * worker thread.
     *
     * @param query The query to run.
     */
    public void runQuery(FpUtilsMedia.QueryTask query) {
        int count = mTimeline.addSongs(this, query);

        int text;

        switch (query.mode) {
            case FpTrackTimeline.MODE_PLAY:
            case FpTrackTimeline.MODE_PLAY_POS_FIRST:
            case FpTrackTimeline.MODE_PLAY_ID_FIRST:
                text = R.plurals.fp_plurals_song_playing;
                if (count != 0 && (mState & FLAG_PLAYING) == 0) {
                    setFlag(FLAG_PLAYING);
                }
                break;

            case FpTrackTimeline.MODE_FLUSH_AND_PLAY_NEXT:
            case FpTrackTimeline.MODE_ENQUEUE:
            case FpTrackTimeline.MODE_ENQUEUE_ID_FIRST:
            case FpTrackTimeline.MODE_ENQUEUE_POS_FIRST:
            case FpTrackTimeline.MODE_ENQUEUE_AS_NEXT:
                text = R.plurals.fp_plurals_song_enqueued;
                break;

            default:
                throw new IllegalArgumentException("Add mode: " + query.mode);
        }
        showMirrorLinkSafeToast(getResources().getQuantityString(text, count, count), Toast.LENGTH_SHORT);
    }

    /**
     * Run the query in the background and add the results to the timeline.
     *
     * @param query The query.
     */
    public void addSongs(FpUtilsMedia.QueryTask query) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUERY, query));
    }

    /**
     * Enqueues all the songs with the same album/artist/genre as the passed
     * song.
     *
     * This will clear the queue and place the first song from the group after
     * the playing song.
     *
     * @param song The song to base the query on
     * @param type The media type, one of FpUtilsMedia.TYPE_ALBUM, TYPE_ARTIST,
     * or TYPE_GENRE
     */
    public void enqueueFromSong(FpTrack song, int type) {
        if (song == null) {
            return;
        }

        long id;
        switch (type) {
            case FpUtilsMedia.TYPE_ARTIST:
                id = song.artistId;
                break;

            case FpUtilsMedia.TYPE_ALBUM:
                id = song.albumId;
                break;

            case FpUtilsMedia.TYPE_GENRE:
                id = FpUtilsMedia.queryGenreForSong(getContentResolver(), song.id);
                break;

            default:
                throw new IllegalArgumentException("Media type: " + type);
        }

        String selection = MediaStore.MediaColumns._ID + "!=" + song.id;
        FpUtilsMedia.QueryTask query = FpUtilsMedia.buildQuery(type, id, FpTrack.FILLED_PROJECTION, selection);
        query.mode = FpTrackTimeline.MODE_FLUSH_AND_PLAY_NEXT;
        addSongs(query);
    }

    /**
     * Clear the song queue.
     */
    public void clearQueue() {
        mTimeline.clearQueue();
    }

    /**
     * Empty the song queue.
     */
    public void emptyQueue() {
        pause();
        setFlag(FLAG_EMPTY_QUEUE);
        mTimeline.emptyQueue();
    }

    /**
     * Return the error message set when FLAG_ERROR is set.
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public void timelineChanged() {
        mHandler.removeMessages(MSG_SAVE_STATE);
        mHandler.sendEmptyMessageDelayed(MSG_SAVE_STATE, SAVE_STATE_DELAY);

        // Trigger a gappless update for the new timeline
        // This might get canceled if setCurrentSong() also fired a call
        // to processSong();
        mHandler.removeMessages(MSG_GAPLESS_UPDATE);
        mHandler.sendEmptyMessageDelayed(MSG_GAPLESS_UPDATE, 100);

        ArrayList<FpTrackTimelineCallback> list = sCallbacks;
        for (int i = list.size(); --i != -1;) {
            list.get(i).onTimelineChanged();
        }

    }

    @Override
    public void positionInfoChanged() {
        ArrayList<FpTrackTimelineCallback> list = sCallbacks;
        for (int i = list.size(); --i != -1;) {
            list.get(i).onPositionInfoChanged();
        }
    }

    protected final ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            FpUtilsMedia.onMediaChange();
            onMediaChange();
        }
    };

    /**
     * Return the FpServiceRendering instance, creating one if needed.
     */
    public static FpServiceRendering get(Context context) {
        if (sInstance == null) {
            context.startService(new Intent(context, FpServiceRendering.class));

            while (sInstance == null) {
                try {
                    synchronized (sWait) {
                        sWait.wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }

        return sInstance;
    }

    /**
     * Returns true if a FpServiceRendering instance is active.
     */
    public static boolean hasInstance() {
        return sInstance != null;
    }

    /**
     * Add an Activity to the registered PlaybackActivities.
     *
     * @param activity The Activity to be added
     */
    public static void addTimelineCallback(FpTrackTimelineCallback consumer) {
        sCallbacks.add(consumer);
    }

    /**
     * Remove an Activity from the registered PlaybackActivities
     *
     * @param activity The Activity to be removed
     */
    public static void removeTimelineCallback(FpTrackTimelineCallback consumer) {
        sCallbacks.remove(consumer);
    }

    /**
     * Initializes the service state, loading songs saved from the disk into the
     * song timeline.
     *
     * @return The loaded value for mState.
     */
    public int loadState() {
        int state = 0;

        try {
            DataInputStream in = new DataInputStream(openFileInput(STATUS_FILE));
            mPendingSeek = in.readInt();
            mPendingSeekSong = in.readLong();
            mTimeline.readState(in);
            state |= mTimeline.getShuffleMode() << SHIFT_SHUFFLE;
            state |= mTimeline.getFinishAction() << SHIFT_FINISH;
            in.close();
        } catch (Exception e) {}

        return state;
    }

    /**
     * Save the service state to disk.
     *
     * @param pendingSeek The pendingSeek to store. Should be the current
     * MediaPlayer position or 0.
     */
    public void saveState(int pendingSeek) {
        try {
            DataOutputStream out = new DataOutputStream(openFileOutput(STATUS_FILE, 0));
            FpTrack song = mCurrentSong;
            out.writeInt(pendingSeek);
            out.writeLong(song == null ? -1 : song.id);
            mTimeline.writeState(out);
            out.close();
        } catch (IOException e) {
            Log.w(Constants.LOG_TAG, "Failed to save state", e);
        }
    }

    /**
     * Returns the shuffle mode for the given state.
     *
     * @param state The FpServiceRendering state to process.
     *
     * @return The shuffle mode. One of FpTrackTimeline.SHUFFLE_*.
     */
    public static int shuffleMode(int state) {
        return (state & MASK_SHUFFLE) >> SHIFT_SHUFFLE;
    }

    /**
     * Returns the finish action for the given state.
     *
     * @param state The FpServiceRendering state to process.
     *
     * @return The finish action. One of FpTrackTimeline.FINISH_*.
     */
    public static int finishAction(int state) {
        return (state & MASK_FINISH) >> SHIFT_FINISH;
    }

    /**
     * Create a PendingIntent for use with the notification.
     *
     * @param prefs Where to load the action preference from.
     */
    public PendingIntent createNotificationAction(SharedPreferences prefs) {
        Intent intent = new Intent(this, ActivityNowplaying.class);
        intent.setAction(Intent.ACTION_MAIN);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    /**
     * Create a song notification. Call through the NotificationManager to
     * display it.
     *
     * @param song The FpTrack to display information about.
     * @param state The state. Determines whether to show paused or playing
     * icon.
     */
    @SuppressLint("NewApi")
    public Notification createNotification(FpTrack song, int state, boolean mode) {
        // Get the playing flag
        boolean playing = (state & FLAG_PLAYING) != 0;

        // Prepare the remote views
        RemoteViews remoteViewMinified = new RemoteViews(getPackageName(), R.layout.fp_notif_minified);
        RemoteViews remoteViewExpanded = new RemoteViews(getPackageName(), R.layout.fp_notif_expanded);

        // Prepare the cover
        Bitmap cover = song.getCover(this);

        // Get the default cover
        do {
            // Empty cover
            if (cover == null) {
                // Get a new one
                if (null != ActivityCommon.getContext()) {
                    cover = FpCoverBitmap.generateDefaultCover(ActivityCommon.getContext(), FpCoverStore.SIZE_LARGE, FpCoverStore.SIZE_LARGE);
                } else {
                    // Apply the local resources
                    remoteViewMinified.setImageViewResource(R.id.fp_cover, R.drawable.fp_albumart);
                    remoteViewExpanded.setImageViewResource(R.id.fp_cover, R.drawable.fp_albumart);
                    break;
                }
            }
            
            // Set the cover bitmap
            remoteViewMinified.setImageViewBitmap(R.id.fp_cover, cover);
            remoteViewExpanded.setImageViewBitmap(R.id.fp_cover, cover);

        } while (false);

        // Set the background
        remoteViewMinified.setImageViewBitmap(R.id.fp_widget_background, FpServiceRendering.getThemedBitmap(R.drawable.fp_bg_widget_bar));
        remoteViewExpanded.setImageViewBitmap(R.id.fp_widget_background, FpServiceRendering.getThemedBitmap(R.drawable.fp_bg_widget_bar));

        // Prepare the play button
        int playButton = playing ? R.drawable.fp_control_pause : R.drawable.fp_control_play;

        // Play/Pause drawable
        remoteViewMinified.setImageViewBitmap(R.id.fp_play_pause, FpServiceRendering.getThemedBitmap(playButton));
        remoteViewExpanded.setImageViewBitmap(R.id.fp_play_pause, FpServiceRendering.getThemedBitmap(playButton));

        // Next drawable
        remoteViewMinified.setImageViewBitmap(R.id.fp_next, FpServiceRendering.getThemedBitmap(R.drawable.fp_control_next));
        remoteViewExpanded.setImageViewBitmap(R.id.fp_next, FpServiceRendering.getThemedBitmap(R.drawable.fp_control_next));
        
        // Previous drawable
        remoteViewExpanded.setImageViewBitmap(R.id.fp_previous, FpServiceRendering.getThemedBitmap(R.drawable.fp_control_prev));
        
        // Close drawable
        remoteViewExpanded.setImageViewBitmap(R.id.fp_close, FpServiceRendering.getThemedBitmap(R.drawable.fp_notif_close));

        // Title text color
        remoteViewMinified.setTextColor(R.id.fp_title, Theme.Resources.getColor(R.color.fp_color_widget_title));
        remoteViewExpanded.setTextColor(R.id.fp_title, Theme.Resources.getColor(R.color.fp_color_widget_title));

        // Artist text color
        remoteViewMinified.setTextColor(R.id.fp_artist, Theme.Resources.getColor(R.color.fp_color_widget_subtitle));
        remoteViewExpanded.setTextColor(R.id.fp_artist, Theme.Resources.getColor(R.color.fp_color_widget_subtitle));

        // Prepare the service
        ComponentName service = new ComponentName(this, FpServiceRendering.class);
        
        // Previous
        Intent previous = new Intent(FpServiceRendering.ACTION_PREVIOUS_SONG);
        previous.setComponent(service);
        remoteViewExpanded.setOnClickPendingIntent(R.id.fp_previous, PendingIntent.getService(this, 0, previous, 0));

        // Play/pause
        Intent playPause = new Intent(FpServiceRendering.ACTION_TOGGLE_PLAYBACK_NOTIFICATION);
        playPause.setComponent(service);
        remoteViewMinified.setOnClickPendingIntent(R.id.fp_play_pause, PendingIntent.getService(this, 0, playPause, 0));
        remoteViewExpanded.setOnClickPendingIntent(R.id.fp_play_pause, PendingIntent.getService(this, 0, playPause, 0));

        // Next
        Intent next = new Intent(FpServiceRendering.ACTION_NEXT_SONG);
        next.setComponent(service);
        remoteViewMinified.setOnClickPendingIntent(R.id.fp_next, PendingIntent.getService(this, 0, next, 0));
        remoteViewExpanded.setOnClickPendingIntent(R.id.fp_next, PendingIntent.getService(this, 0, next, 0));

        // Close
        int closeButtonVisibility = mode ? View.VISIBLE : View.INVISIBLE;
        Intent close = new Intent(FpServiceRendering.ACTION_CLOSE_NOTIFICATION);
        close.setComponent(service);
        remoteViewMinified.setOnClickPendingIntent(R.id.fp_close, PendingIntent.getService(this, 0, close, 0));
        remoteViewMinified.setViewVisibility(R.id.fp_close, closeButtonVisibility);
        remoteViewExpanded.setOnClickPendingIntent(R.id.fp_close, PendingIntent.getService(this, 0, close, 0));
        remoteViewExpanded.setViewVisibility(R.id.fp_close, closeButtonVisibility);

        // Set the title
        remoteViewMinified.setTextViewText(R.id.fp_title, song.title);
        remoteViewExpanded.setTextViewText(R.id.fp_title, song.title);
        
        // Set the artist
        remoteViewMinified.setTextViewText(R.id.fp_artist, song.artist);
        remoteViewExpanded.setTextViewText(R.id.fp_artist, song.artist);

        // Prepare the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getApplicationContext(), FpNotification.createChannel(this.getApplicationContext(), "FpServiceRendering"));
        Notification notification = mBuilder
            .setSmallIcon(R.drawable.icon)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();

        // Set the details
        notification.contentView = remoteViewMinified;
        notification.icon = R.drawable.fp_notif_icon;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.contentIntent = mNotificationAction;
        
        // Add the minified layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.bigContentView = remoteViewExpanded;
        }

        // Add the ticker
        if (mNotificationNag) {
            notification.tickerText = song.title + " (" + song.artist + ")";
        }

        return notification;
    }

    /**
     * Get the bitmap (theme-compatible) of a drawable by resource ID
     */
    public static Bitmap getThemedBitmap(int drawableId) {
        return getThemedBitmap(drawableId, 0, 0);
    }
    
    /**
     * Get the bitmap (theme-compatible) of a drawable by resource ID
     */
    public static Bitmap getThemedBitmap(int drawableId, int customWidth, int customHeight) {
    	// Prepare the drawable
    	Drawable icon = Theme.Resources.getDrawable(drawableId);
    	
        // Prepare the width and height
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        
        // Custom dimensions
        if (0 != customWidth) {
            width = customWidth;
        }
        if (0 != customHeight) {
            height = customHeight;
        }
        
    	// Prepare the bitmap
    	Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        
    	// Prepare the canvas
    	Canvas canvas = new Canvas(bitmap); 
    	
    	// Set the canvas bounds
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw on the canvas
        icon.draw(canvas);
        
        // All done
        return bitmap;
    }

    public void onAudioFocusChange(int type) {
        Log.d(Constants.LOG_TAG, "audio focus change: " + type);

        // Rewrite permanent focus loss into can_duck
        if (mBlurKeepPlaying) {
            switch (type) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    type = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            }
        }

        switch (type) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                synchronized (mStateLock) {
                    mTransientAudioLoss = (mState & FLAG_PLAYING) != 0;
                    if (mTransientAudioLoss) {
                        if (type == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            setFlag(FLAG_DUCKING);
                        } else {
                            mForceNotificationVisible = true;
                            unsetFlag(FLAG_PLAYING);
                        }
                    }
                    break;
                }
            
            case AudioManager.AUDIOFOCUS_LOSS:
                mTransientAudioLoss = false;
                mForceNotificationVisible = true;
                unsetFlag(FLAG_PLAYING);
                break;
                
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mTransientAudioLoss) {
                    mTransientAudioLoss = false;
                    // Restore all flags possibly changed by AUDIOFOCUS_LOSS_TRANSIENT_*
                    unsetFlag(FLAG_DUCKING);
                    setFlag(FLAG_PLAYING);
                }
                break;
        }
    }

    /**
     * Execute the given action.
     *
     * @param action The action to execute.
     * @param receiver Optional. If non-null, update the ActivityCommon with
     * new song or state from the executed action. The activity will still be
     * updated by the broadcast if not passed here; passing it just makes the
     * update immediate.
     */
    public void performAction(FpAction action, ActivityCommon receiver) {
        switch (action) {
            case Nothing:
                break;

            case Library:
                Intent intent = new Intent(this, ActivityLibrary.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
                
            case PlayPause: {
                int state = playPause();
                if (receiver != null) {
                    receiver.setState(state);
                }
                break;
            }
            
            case NextSong: {
                FpTrack song = shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_SONG);
                if (receiver != null) {
                    receiver.setSong(song);
                }
                break;
            }
            
            case PreviousSong: {
                FpTrack song = shiftCurrentSong(FpTrackTimeline.SHIFT_PREVIOUS_SONG);
                if (receiver != null) {
                    receiver.setSong(song);
                }
                break;
            }
            
            case NextAlbum: {
                FpTrack song = shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_ALBUM);
                if (receiver != null) {
                    receiver.setSong(song);
                }
                break;
            }
            
            case PreviousAlbum: {
                FpTrack song = shiftCurrentSong(FpTrackTimeline.SHIFT_PREVIOUS_ALBUM);
                if (receiver != null) {
                    receiver.setSong(song);
                }
                break;
            }
            
            case Repeat: {
                int state = cycleFinishAction();
                if (receiver != null) {
                    receiver.setState(state);
                }
                break;
            }
            
            case Shuffle: {
                int state = cycleShuffle();
                if (receiver != null) {
                    receiver.setState(state);
                }
                break;
            }
            
            case EnqueueAlbum:
                enqueueFromSong(mCurrentSong, FpUtilsMedia.TYPE_ALBUM);
                break;
                
            case EnqueueArtist:
                enqueueFromSong(mCurrentSong, FpUtilsMedia.TYPE_ARTIST);
                break;
                
            case EnqueueGenre:
                enqueueFromSong(mCurrentSong, FpUtilsMedia.TYPE_GENRE);
                break;
                
            case ClearQueue:
                clearQueue();
                showMirrorLinkSafeToast(R.string.fp_menu_notif_queue_empty, Toast.LENGTH_SHORT);
                break;
                
            case ShowQueue:
                // These are NOOPs here and should be handled in ActivityNowplaying
                break;
            
            case SeekForward:
                Log.i(Constants.LOG_TAG, "Current song: " + (null == mCurrentSong ? "null" : String.valueOf(mCurrentSong.id)));
                if (mCurrentSong != null) {
                    mPendingSeekSong = mCurrentSong.id;
                    mPendingSeek = getPosition() + (1000 * mSeekStep);
                    mMediaPlayer.seekTo(mPendingSeek);
                }
                break;
                
            case SeekBackward:
                if (mCurrentSong != null) {
                    mPendingSeekSong = mCurrentSong.id;
                    mPendingSeek = getPosition() - (1000 * mSeekStep);
                    if (mPendingSeek < 1) {
                        mPendingSeek = 1;
                    }
                    mMediaPlayer.seekTo(mPendingSeek);
                }
                break;
                
            default:
                throw new IllegalArgumentException("FpAction: " + action);
        }
    }

    /**
     * Returns the playing status of the current song
     */
    public boolean isPlaying() {
        return (mState & FLAG_PLAYING) != 0;
    }

    /**
     * Returns the position of the current song in the song timeline.
     */
    public int getTimelinePosition() {
        return mTimeline.getPosition();
    }

    /**
     * Returns the number of songs in the song timeline.
     */
    public int getTimelineLength() {
        return mTimeline.getLength();
    }

    /**
     * Returns 'FpTrack' with given id from timeline
     */
    public FpTrack getSongByQueuePosition(int id) {
        return mTimeline.getSongByQueuePosition(id);
    }

    /**
     * Do a 'hard' jump to given queue position
     */
    public void jumpToQueuePosition(int id) {
        mTimeline.setCurrentQueuePosition(id);
        play();   
    }

    /**
     * Moves a songs position in the queue
     *
     * @param from the index of the song to be moved
     * @param to the new index position of the song
     */
    public void moveSongPosition(int from, int to) {
        mTimeline.moveSongPosition(from, to);
    }

    /**
     * Removes a song from the queue
     *
     * @param which index to remove
     */
    public void removeSongPosition(int which) {
        mTimeline.removeSongPosition(which);
    }

}

/*EOF*/