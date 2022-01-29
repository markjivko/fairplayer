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

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.util.Log;

/**
 * The primary playback screen with playback controls and large cover display.
 */
@SuppressLint("NewApi")
public class ActivityNowplaying extends ActivitySliding implements View.OnLongClickListener {
    
    public static final int DISPLAY_INFO_OVERLAP = 0;
    public static final int DISPLAY_INFO_BELOW = 1;
    public static final int DISPLAY_INFO_WIDGETS = 2;

    protected boolean mShouldAnimate = true;
    private TextView mQueuePosView;

    private TextView mTitle;
    private TextView mArtist;
    
    private ControlbarTop mControlbarTop;

    /**
     * The currently playing song.
     */
    private FpTrack mCurrentSong;

    private String mFormat;
    private TextView mFormatView;
    private ElementImageButton mFavorites;
    private ElementImageButton mThemes;
    private ElementImageButton mEqualizer;
    private ElementImageButton mLibrary;
    protected FpAnimation mAnimation = null;
    protected ImageView mCoverFrame = null;
    protected FpLoyalty mLoyalty = null;
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the loyalty class
        mLoyalty = new FpLoyalty(this);
        
        // Start the notification manager
        this.startNotificationManager();
        
        // Request the permissions
        FpPermissions.request(this);
        
        setTitle(R.string.fp_menu_now_playing);
        setContentView(R.layout.fp_playback);

        FpCoverView coverView = (FpCoverView) findViewById(R.id.fp_cover_view);
        coverView.setup(mLooper, this);
        coverView.setOnClickListener(this);
        coverView.setOnLongClickListener(this);
        mCoverView = coverView;

        mTitle = (TextView) findViewById(R.id.fp_title);
        mArtist = (TextView) findViewById(R.id.fp_artist);
        mFormatView = (TextView) findViewById(R.id.fp_format);
        
        // Prepare the favorites button
        mFavorites = (ElementImageButton) findViewById(R.id.fp_favorites);
        if (null != mFavorites) {
            mFavorites.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    long playlistId = FpPlaylist.getFavoritesId(ActivityNowplaying.this, true);
                    if (mCurrentSong != null) {
                        FpPlaylistTask playlistTask = new FpPlaylistTask(playlistId, getString(R.string.fp_playlist_favorites));
                        playlistTask.audioIds = new ArrayList<Long>();
                        playlistTask.audioIds.add(mCurrentSong.id);
                        int action = FpPlaylist.isInPlaylist(getContentResolver(), playlistId, mCurrentSong) ? MSG_REMOVE_FROM_PLAYLIST : MSG_ADD_TO_PLAYLIST;
                        
                        // Add to favorites
                        if (action == MSG_ADD_TO_PLAYLIST) {
                            mLoyalty.sharingDialog(mCurrentSong);
                        }
                        
                        // Send the message
                        mHandler.sendMessage(mHandler.obtainMessage(action, playlistTask));
                    }
                }
            });
        }
        
        // Prepare playback info header
        LinearLayout linearLayoutInfoHeader = (LinearLayout) findViewById(R.id.fp_playback_info_header);
        if (null != linearLayoutInfoHeader) {
            linearLayoutInfoHeader.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (null != mElementSlider && !mElementSlider.isExpanded()) {
                        mElementSlider.expandSlide();
                    }
                }
            });
        }
        
        // Prepare the themes button
        mThemes = (ElementImageButton) findViewById(R.id.fp_themes);
        if (null != mThemes) {
        	// Set the image resource
        	mThemes.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_panel_themes));
        	
        	// Set the on-click action
        	mThemes.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(ActivityNowplaying.this, ActivitySettings.class );
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ActivitySettings.SettingsFragmentThemes.class.getName() );
                    intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                    ActivityNowplaying.this.startActivity(intent);
                }
            });
        }
        
        // Prepare the equalizer button
        mEqualizer = (ElementImageButton) findViewById(R.id.fp_equalizer);
        if (null != mEqualizer) {
        	// Set the image resource
        	mEqualizer.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_panel_equalizer));
        	
        	// Set the on-click action
        	mEqualizer.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(ActivityNowplaying.this, ActivityEqualizer.class);
                    ActivityNowplaying.this.startActivity(intent);
                }
            });
        }
        
        // Prepare the library button
        mLibrary = (ElementImageButton) findViewById(R.id.fp_library);
        if (null != mLibrary) {
        	// Set the image resource
        	mLibrary.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_panel_library));
        	
        	// Set the on-click action
        	mLibrary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(ActivityNowplaying.this, ActivityLibrary.class);
                    ActivityNowplaying.this.startActivity(intent);
                }
            });
        }
        
        // Ensure that mFavorites is updated
        mHandler.sendEmptyMessage(MSG_LOAD_FAVOURITE_INFO);
        
        mQueuePosView = (TextView) findViewById(R.id.fp_queue_pos);
        bindControlButtons();

        // Create the control bar
        mControlbarTop = (ControlbarTop) findViewById(R.id.fp_controlbar_top);
        mControlbarTop.enableOptionsMenu(this);
        
        // Set the overlay
        mCoverFrame = (ImageView) findViewById(R.id.fp_cover_view_over);
        mCoverFrame.setBackground(Theme.Resources.getDrawable(R.drawable.fp_cover_frame));
        
        // Set the page background
        ((LinearLayout) findViewById(R.id.fp_content)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_page));
        
        // Set the background for the playback info, controls and seekbar
        ((LinearLayout) findViewById(R.id.fp_playback_info)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_controls));
        ((LinearLayout) findViewById(R.id.fp_playback_controls)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_controls));
        ((LinearLayout) findViewById(R.id.fp_playback_seekbar)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_controls));
        
        // Set the queue color
        ((TextView) findViewById(R.id.fp_queue_pos)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_title));
        
        // Set the title color
        ((TextView) findViewById(R.id.fp_title)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_title));
        
        // Set the subtitle color
        ((TextView) findViewById(R.id.fp_artist)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_subtitle));
        
        // Customize the seek bar
        ((SeekBar) findViewById(R.id.fp_seek_bar)).setProgressDrawable(Theme.Resources.getDrawable(R.drawable.fp_progress));
        
        // Set the seekbar colors
        ((TextView) findViewById(R.id.fp_elapsed)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_seekbar));
        ((TextView) findViewById(R.id.fp_format)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_seekbar));
        ((TextView) findViewById(R.id.fp_duration)).setTextColor(Theme.Resources.getColor(R.color.fp_color_playback_seekbar));
        
        // Prepare the tutorial items
        int[][] tutorialItems = {
            {R.id.fp_cover_frame, R.string.fp_tutorial_cover_title, R.string.fp_tutorial_cover_info},
            {R.id.fp_playback_controls, R.string.fp_tutorial_controls_title, R.string.fp_tutorial_controls_info},
            {R.id.fp_themes, R.string.fp_tutorial_themes_title, R.string.fp_tutorial_themes_info},
            {R.id.fp_equalizer, R.string.fp_tutorial_equalizer_title, R.string.fp_tutorial_equalizer_info},
        };
        
        // Show the tutorial
        FpTutorial.show((Activity) this, tutorialItems, Constants.Keys.SETTINGS_TUTORIAL_NOWPLAYING);
    }

    /**
     * Start the notification
     */
    protected void startNotificationManager() {
        // Notification already setn
        if (PreferenceUtils.getPreferences(this).getBoolean(Constants.Notification.SENT, false)) {
            Log.d(Constants.LOG_TAG, "Notification already sent.");
            return;
        }
        
        // Set the default notification alarm
        this.setNotificationAlarm(Constants.Notification.Default.TIME, Constants.Notification.Default.ID);
        this.setNotificationAlarm(Constants.Notification.Loyalty.TIME, Constants.Notification.Loyalty.ID);

        // Get the shared preferences editor
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceUtils.edit();

        // Store the flag
        sharedPreferencesEditor.putBoolean(Constants.Notification.SENT, true);
        sharedPreferencesEditor.commit();
    }

    /**
     * Set an actual alarm for the provided intent in x seconds
     * 
     * @param intent
     * @param time
     */
    protected void setNotificationAlarm(int time, int requestCode) {
        // Log the preferences
        Log.d(Constants.LOG_TAG, "Setting alarm #" + requestCode + " for " + time + " seconds...");

        // New intent
        Intent intent = new Intent(this, FpNotification.class);

        // Store the request code
        intent.putExtra("id", requestCode);
        
        // Prepare the intent sender
        PendingIntent intentSender = PendingIntent.getBroadcast(this, requestCode, intent, 0);

        // Get the alarm manager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Set the alarm
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time * 1000, intentSender);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Get the pending theme
        final String pendingTheme = getIntent().getStringExtra(Theme.Manager.THEME_INTENT_KEY);
        getIntent().removeExtra(Theme.Manager.THEME_INTENT_KEY);
        
        // Valid pending theme
        if (null != pendingTheme && pendingTheme.length() > 0) {
            // Prepare the editor
            SharedPreferences.Editor editor = PreferenceUtils.edit();

            // Ad-free zone for a while
            editor.putLong(Constants.Keys.SETTINGS_ADS_FREE_CHECKPOINT, Constants.Defaults.SETTINGS_ADS_FREE_CHECKPOINT);

            // All done
            editor.commit();
            
            // Theme is installed
            if (Theme.Manager.isThemeInstalled(pendingTheme)) {
            	// Get the handler
                final Handler handler = new Handler();

                // Run after 1 second
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    	Theme.Manager.setCurrentTheme(pendingTheme);
                    }
                }, 250);
            }
        }

        // Extra info on resume
        if (!mHandler.hasMessages(MSG_LOAD_EXTRA_INFO)) {
            mHandler.sendEmptyMessageDelayed(MSG_LOAD_EXTRA_INFO, 200);
        }

        // Update notifier
        UpdateChecker.getInstance(this).check(true);
    }

    @Override
    protected void onStateChange(int state, int toggled) {
        super.onStateChange(state, toggled);
        
        // Empty queue, start the Library activity
        if ((toggled & (FpServiceRendering.FLAG_NO_MEDIA | FpServiceRendering.FLAG_EMPTY_QUEUE)) != 0) {
            startActivity(new Intent(this, ActivityLibrary.class));
        }

        // Resume the animation
        this._resumeAlbumartAnimation();

        // Update queue position
        if (mQueuePosView != null) {
            updateQueuePosition();
        }
        
        // Ads
        if (this.mShouldAnimate && FpTutorial.isShown(Constants.Keys.SETTINGS_TUTORIAL_NOWPLAYING)) {
            this.setAction(ACTION_NONE);

            // Applying a theme
            String pendingTheme = getIntent().getStringExtra(Theme.Manager.THEME_INTENT_KEY);
            
            // Skip ads if applying a theme
            if (null == pendingTheme) {
                this.ads.place(Ads.AdPlacement.ENTER_NOWPLAYING);
            }
        }
    }

    @Override
    public void onPause() {
        // Animation visible
        this.mShouldAnimate = false;

        // Stop the current animation
        if (null != mAnimation) {
            mAnimation.stop();
            mAnimation = null;
        }
        // Parent
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus) {
            // Animation visible
            this.mShouldAnimate = true;

            // Stop the current animation
            if (null != mAnimation) {
                mAnimation.stop();
                mAnimation = null;
            }

            // Clear the animation holder
            ((ImageView) findViewById(R.id.fp_cover_view_animation)).setImageDrawable(null);            

            // Prepare the animation
            mAnimation = new FpAnimation(
                (ImageView) findViewById(R.id.fp_cover_view_animation), 
                "fp_frame_animation",
                33
            );

            // Resume the animation
            this._resumeAlbumartAnimation();
        }   
    }

    /**
     * Resume albumart animation
     */
    protected void _resumeAlbumartAnimation() {
        // Animation is set
        if (null != mAnimation) {
            // Stop the current animation
            mAnimation.stop();

            // Show the cover frame
            if(View.VISIBLE != mCoverFrame.getVisibility()) {
                mCoverFrame.setVisibility(View.VISIBLE);
            }
                    
            // Start
            if(this.mShouldAnimate && (mState & FpServiceRendering.FLAG_PLAYING) != 0) {
                // Start the animation
                if (mAnimation.getNumberOfFrames() > 0) {
                    // Start the animation
                    mAnimation.start();

                    // Not the default theme
                    if (!Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(Theme.Manager.getCurrentTheme())) {
                        // Get the handler
                        final Handler handler = new Handler();

                        // Run after 1 second
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mCoverFrame.setVisibility(View.INVISIBLE);
                            }
                        }, 200);
                    }
                }
            }
        }
    }

    @Override
    protected void onSongChange(FpTrack song) {
        if (mTitle != null) {
            if (song == null) {
                mTitle.setText(null);
                mArtist.setText(null);
            } else {
                mTitle.setText(song.title);
                mTitle.setSelected(true);
                mArtist.setText(song.artist);
                mArtist.setSelected(true);
            }
            updateQueuePosition();
        }
        mCurrentSong = song;

        mHandler.sendEmptyMessage(MSG_LOAD_FAVOURITE_INFO);
        mHandler.sendEmptyMessage(MSG_LOAD_EXTRA_INFO);
        super.onSongChange(song);
    }

    /**
     * Update the queue position display. mQueuePos must not be null.
     */
    private void updateQueuePosition() {
        if (FpServiceRendering.finishAction(mState) == FpTrackTimeline.FINISH_RANDOM) {
            // Not very useful in random mode; it will always show something
            // like 11/13 since the timeline is trimmed to 10 previous songs.
            // So just hide it.
            mQueuePosView.setText(null);
        } else {
            FpServiceRendering service = FpServiceRendering.get(this);
            mQueuePosView.setText((service.getTimelinePosition() + 1) + "/" + service.getTimelineLength());
        }
        mQueuePosView.requestLayout(); // ensure queue pos column has enough room
    }

    @Override
    public void onPositionInfoChanged() {
        if (mQueuePosView != null) {
            mUiHandler.sendEmptyMessage(MSG_UPDATE_POSITION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_DELETE, 30, R.string.fp_menu_delete);
//        menu.add(0, MENU_ENQUEUE_ALBUM, 30, R.string.fp_menu_queue_add_album);
//        menu.add(0, MENU_ENQUEUE_ARTIST, 30, R.string.fp_menu_queue_add_artist);
//        menu.add(0, MENU_ENQUEUE_GENRE, 30, R.string.fp_menu_queue_add_genre);
        menu.add(0, MENU_LIBRARY, 30, R.string.fp_menu_library);
        menu.add(0, MENU_SHARE, 30, R.string.fp_menu_share);
        menu.add(0, MENU_ADD_TO_PLAYLIST, 30, R.string.fp_menu_add_to_playlist);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final FpTrack song = mCurrentSong;

        switch (item.getItemId()) {
            case MENU_LIBRARY:
                openLibrary(null);
                break;

            case MENU_SHARE:
                mLoyalty.sharingDialog(mCurrentSong);
                break;
                
            case MENU_ENQUEUE_ALBUM:
                FpServiceRendering.get(this).enqueueFromSong(song, FpUtilsMedia.TYPE_ALBUM);
                break;
                
            case MENU_ENQUEUE_ARTIST:
                FpServiceRendering.get(this).enqueueFromSong(song, FpUtilsMedia.TYPE_ARTIST);
                break;
                
            case MENU_ENQUEUE_GENRE:
                FpServiceRendering.get(this).enqueueFromSong(song, FpUtilsMedia.TYPE_GENRE);
                break;
                
            case MENU_ADD_TO_PLAYLIST:
                if (song != null) {
                    Intent intent = new Intent();
                    intent.putExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_SONG);
                    intent.putExtra(AdapterLibrary.DATA_ID, song.id);
                    FpPlaylistPopup dialog = new FpPlaylistPopup(this, intent);
                    dialog.show(getFragmentManager(), FpPlaylistPopup.class.getSimpleName());
                }
                break;
                
            case MENU_DELETE:
                if (song != null) {
                    String delete_message = getString(R.string.fp_menu_notif_delete_confirm_file, song.title);
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setTitle(R.string.fp_menu_delete);
                    dialog
                            .setMessage(delete_message)
                            .setPositiveButton(R.string.fp_menu_delete, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // MSG_DELETE expects an intent (usually called from fp_activity_library_list)
                                    Intent intent = new Intent();
                                    intent.putExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_SONG);
                                    intent.putExtra(AdapterLibrary.DATA_ID, song.id);
                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_DELETE, intent));
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    dialog.create().show();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean onSearchRequested() {
        openLibrary(null);
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                shiftCurrentSong(FpTrackTimeline.SHIFT_NEXT_SONG);
                findViewById(R.id.fp_next).requestFocus();
                return true;
                
            case KeyEvent.KEYCODE_DPAD_LEFT:
                shiftCurrentSong(FpTrackTimeline.SHIFT_PREVIOUS_SONG);
                findViewById(R.id.fp_previous).requestFocus();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (null != mElementSlider && !mElementSlider.isHidden()) {
                    mElementSlider.hideSlide();
                    return false;
                }
                
                finish();
                break;
                
            case KeyEvent.KEYCODE_MENU:
                // We intercept these to avoid showing the activity-default menu
                mControlbarTop.openMenu();
                break;
        }

        // Stop here
        return true;
    }

    /**
     * Retrieve the extra metadata for the current song.
     */
    private void loadExtraInfo() {
        // Hide the queue
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mElementSlider && mElementSlider.isExpanded()) {
                    mElementSlider.hideSlide();
                }
           }
        });

        mFormat = null;
        if (mCurrentSong != null) {
            MediaMetadataRetriever data = new MediaMetadataRetriever();

            try {
                data.setDataSource(mCurrentSong.path);
            } catch (Exception e) {}

            StringBuilder sb = new StringBuilder(12);
            sb.append(decodeMimeType(data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)));
            String bitrate = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null && bitrate.length() > 3) {
                sb.append(' ');
                sb.append(bitrate.substring(0, bitrate.length() - 3));
                sb.append("Kbps");
            }
            mFormat = sb.toString();
            
            data.release();
        }

        // Send the message
        mUiHandler.sendEmptyMessage(MSG_COMMIT_INFO);

        // Prepare the service
        FpServiceRendering service = FpServiceRendering.get(this);
        
        // Empty queue
        if (service.getTimelineLength() <= 0) {
            openLibrary(null);
        }
    }

    /**
     * Decode the given mime type into a more human-friendly description.
     */
    private static String decodeMimeType(String mime) {
        if ("audio/mpeg".equals(mime)) {
            return "Mp3";
        } 
        if ("audio/mp4".equals(mime)) {
            return "Aac";
        } 
        if ("audio/vorbis".equals(mime)) {
            return "Ogg Vorbis";
        }
        if ("application/ogg".equals(mime)) {
            return "Ogg Vorbis";
        }
        if ("audio/flac".equals(mime)) {
            return "Flac";
        }
        return mime;
    }
    
    /**
     * Call #loadExtraInfo().
     */
    private static final int MSG_LOAD_EXTRA_INFO = 11;
    
    /**
     * Pass obj to mExtraInfo.setText()
     */
    private static final int MSG_COMMIT_INFO = 12;
    
    /**
     * Calls #updateQueuePosition().
     */
    private static final int MSG_UPDATE_POSITION = 13;
    
    /**
     * Check if passed song is a favorite
     */
    private static final int MSG_LOAD_FAVOURITE_INFO = 14;
    
    /**
     * Updates the favorites state
     */
    private static final int MSG_COMMIT_FAVOURITE_INFO = 15;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_LOAD_EXTRA_INFO:
                loadExtraInfo();
                break;
                
            case MSG_COMMIT_INFO: {
                mFormatView.setText(mFormat);
                break;
            }
            
            case MSG_UPDATE_POSITION:
                updateQueuePosition();
                break;
                
            case MSG_NOTIFY_PLAYLIST_CHANGED: // triggers a fav-refresh
            case MSG_LOAD_FAVOURITE_INFO:
                if (mCurrentSong != null) {
                    boolean found = FpPlaylist.isInPlaylist(getContentResolver(), FpPlaylist.getFavoritesId(this, false), mCurrentSong);
                    mUiHandler.sendMessage(mUiHandler.obtainMessage(MSG_COMMIT_FAVOURITE_INFO, found));
                }
                break;
                
            case MSG_COMMIT_FAVOURITE_INFO:
                if (mFavorites != null) {
                    mFavorites.setImageResource(message.obj != null && (Boolean) message.obj ? R.drawable.fp_rating_full : R.drawable.fp_rating_empty);
                }
                break;
                
            default:
                return super.handleMessage(message);
        }

        return true;
    }

    @Override
    protected void performAction(FpAction action) {
        super.performAction(action);
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }
}

/*EOF*/