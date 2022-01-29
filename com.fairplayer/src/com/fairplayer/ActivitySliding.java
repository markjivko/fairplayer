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

import android.content.Intent;
import android.text.format.DateUtils;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import com.fairplayer.FpThemeOfTheDay;

public class ActivitySliding extends ActivityCommon implements ElementSlider.Callback, SeekBar.OnSeekBarChangeListener, FpPlaylistPopup.Callback {

    /**
     * Reference to the inflated menu
     */
    private Menu mMenu;
    /**
     * SeekBar widget
     */
    private SeekBar mSeekBar;
    /**
     * TextView indicating the elapsed playback time
     */
    private TextView mElapsedView;
    /**
     * TextView indicating the total duration of the song
     */
    private TextView mDurationView;
    /**
     * Current song duration in milliseconds.
     */
    private long mDuration;
    /**
     * True if user tracks/drags the seek bar
     */
    private boolean mSeekBarTracking;
    /**
     * True if the seek bar should not get periodic updates
     */
    private boolean mPaused;
    /**
     * Cached StringBuilder for formatting track position.
     */
    private final StringBuilder mTimeBuilder = new StringBuilder();
    /**
     * Instance of the sliding view
     */
    protected ElementSlider mElementSlider;

    /**
     * Open the playback activity and close any activities above it in the
     * stack.
     */
    public void openActivitySlidingCommon() {
        if (mElementSlider.isExpanded()) {
            mElementSlider.hideSlideDelayed();
        }
        startActivity(new Intent(this, ActivityNowplaying.class));
    }
    
    @Override
    protected void bindControlButtons() {
        super.bindControlButtons();

        mElementSlider = (ElementSlider) findViewById(R.id.fp_sliding_view);
        if (null != mElementSlider) {
            mElementSlider.setCallback(this);
        }
        
        mElapsedView = (TextView) findViewById(R.id.fp_elapsed);
        mDurationView = (TextView) findViewById(R.id.fp_duration);
        mSeekBar = (SeekBar) findViewById(R.id.fp_seek_bar);
        if (null != mSeekBar) {
            mSeekBar.setMax(1000);
            mSeekBar.setOnSeekBarChangeListener(this);
        }
        setDuration(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaused = false;
        updateElapsedTime();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    protected void onSongChange(FpTrack song) {
        setDuration(song == null ? 0 : song.duration);
        updateElapsedTime();
        super.onSongChange(song);
    }

    @Override
    protected void onStateChange(int state, int toggled) {
        updateElapsedTime();
        super.onStateChange(state, toggled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // ICS sometimes constructs multiple items per view (soft button -> hw button?)
        // we work around this by assuming that the first seen menu is the real one
        if (mMenu == null) {
            mMenu = menu;
        }

        menu.add(0, MENU_THEME_OF_THE_DAY, 20, R.string.fp_menu_theme_of_the_day);
        menu.add(0, MENU_SHOW_QUEUE, 20, R.string.fp_menu_queue_show);
        menu.add(0, MENU_HIDE_QUEUE, 20, R.string.fp_menu_queue_hide);
        menu.add(0, MENU_CLEAR_QUEUE, 20, R.string.fp_menu_queue_empty_rest);
        menu.add(0, MENU_EMPTY_QUEUE, 20, R.string.fp_menu_queue_empty);
        menu.add(0, MENU_SAVE_QUEUE_AS_PLAYLIST, 20, R.string.fp_menu_save_as_playlist);
        onSlideFullyExpanded(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_QUEUE:
                if (null != mElementSlider) {
                    mElementSlider.expandSlide();
                }
                break;
                
            case MENU_HIDE_QUEUE:
                if (null != mElementSlider) {
                    mElementSlider.hideSlide();
                }
                break;

            case MENU_THEME_OF_THE_DAY:
                FpThemeOfTheDay.getInstance(this).startIntent();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static final int CTX_MENU_ADD_TO_PLAYLIST = 300;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() != 0) {
            return super.onContextItemSelected(item);
        }

        final Intent intent = item.getIntent();
        switch (item.getItemId()) {
            case CTX_MENU_ADD_TO_PLAYLIST: {
                FpPlaylistPopup dialog = new FpPlaylistPopup(this, intent);
                dialog.show(getFragmentManager(), FpPlaylistPopup.class.getSimpleName());
                break;
            }
            
            default:
                throw new IllegalArgumentException("Invalid context item");
        }
        return true;
    }

    /**
     * Called by FpPlaylistPopup.Callback to prompt for the new
     * playlist name
     *
     * @param intent The intent holding the selected data
     */
    public void createNewPlaylistFromIntent(Intent intent) {
        FpPlaylistTask playlistTask = new FpPlaylistTask(-1, null);
        playlistTask.query = buildQueryFromIntent(intent, true, null);
        FpPlaylistPopupNew dialog = new FpPlaylistPopupNew(this, null, R.string.fp_playlist_create, playlistTask);
        dialog.setDismissMessage(mHandler.obtainMessage(MSG_NEW_PLAYLIST, dialog));
        dialog.show();
    }

    /**
     * Called by FpPlaylistPopup.Callback to append data to
     * a playlist
     *
     * @param intent The intent holding the selected data
     */
    public void appendToPlaylistFromIntent(Intent intent) {
        long playlistId = intent.getLongExtra(AdapterLibrary.DATA_PLAYLIST, -1);
        String playlistName = intent.getStringExtra(AdapterLibrary.DATA_PLAYLIST_NAME);
        FpPlaylistTask playlistTask = new FpPlaylistTask(playlistId, playlistName);
        playlistTask.query = buildQueryFromIntent(intent, true, null);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TO_PLAYLIST, playlistTask));
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_UPDATE_PROGRESS:
                updateElapsedTime();
                break;
                
            case MSG_SEEK_TO_PROGRESS:
                FpServiceRendering.get(this).seekToProgress(message.arg1);
                updateElapsedTime();
                break;
                
            default:
                return super.handleMessage(message);
        }
        return true;
    }

    /**
     * Builds a media query based off the data stored in the given intent.
     *
     * @param intent An intent created with
     * AdapterLibrary#createData(View).
     * @param empty If true, use the empty projection (only query id).
     * @param allSource use this mediaAdapter to queue all hold items
     */
    protected FpUtilsMedia.QueryTask buildQueryFromIntent(Intent intent, boolean empty, AdapterMedia allSource) {
        int type = intent.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);

        String[] projection;
        if (type == FpUtilsMedia.TYPE_PLAYLIST) {
            projection = empty ? FpTrack.EMPTY_PLAYLIST_PROJECTION : FpTrack.FILLED_PLAYLIST_PROJECTION;
        } else {
            projection = empty ? FpTrack.EMPTY_PROJECTION : FpTrack.FILLED_PROJECTION;
        }

        long id = intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID);
        FpUtilsMedia.QueryTask query;
        if (type == FpUtilsMedia.TYPE_FILE) {
            query = FpUtilsMedia.buildFileQuery(intent.getStringExtra(AdapterLibrary.DATA_FILE), projection);
        } else {
            if (allSource != null) {
                query = allSource.buildSongQuery(projection);
                query.data = id;
            } else {
                query = FpUtilsMedia.buildQuery(type, id, projection, null);
            }
        }

        return query;
    }

    /**
     * Update the current song duration fields.
     *
     * @param duration The new duration, in milliseconds.
     */
    private void setDuration(long duration) {
        mDuration = duration;
        if (null != mDurationView) {
            mDurationView.setText(DateUtils.formatElapsedTime(mTimeBuilder, duration / 1000));
        }
    }

    /**
     * Update seek bar progress and schedule another update in one second
     */
    private void updateElapsedTime() {
        long position = FpServiceRendering.hasInstance() ? FpServiceRendering.get(this).getPosition() : 0;

        if (!mSeekBarTracking) {
            long duration = mDuration;
            if (null != mSeekBar) {
                mSeekBar.setProgress(duration == 0 ? 0 : (int) (1000 * position / duration));
            }
        }

        if (null != mElapsedView) {
            mElapsedView.setText(DateUtils.formatElapsedTime(mTimeBuilder, position / 1000));
        }
        
        if (!mPaused && (mState & FpServiceRendering.FLAG_PLAYING) != 0) {
            // Try to update right after the duration increases by one second
            mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (null != mElapsedView) {
                mElapsedView.setText(DateUtils.formatElapsedTime(mTimeBuilder, progress * mDuration / 1000000));
            }
            mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mUiHandler.removeMessages(MSG_SEEK_TO_PROGRESS);
            mUiHandler.sendMessageDelayed(mUiHandler.obtainMessage(MSG_SEEK_TO_PROGRESS, progress, 0), 100);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mSeekBarTracking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mSeekBarTracking = false;
    }

    /**
     * Called by ElementSlider to signal a visibility change.
     * Toggles the visibility of menu items
     *
     * @param expanded true if slide fully expanded
     */
    @Override
    public void onSlideFullyExpanded(boolean expanded) {
        if (mMenu == null) {
            return; // not initialized yet
        }
        final int[] slide_visible = {MENU_HIDE_QUEUE, MENU_CLEAR_QUEUE, MENU_EMPTY_QUEUE, MENU_SAVE_QUEUE_AS_PLAYLIST};
        final int[] slide_hidden = {MENU_SHOW_QUEUE, MENU_SORT, MENU_DELETE, MENU_ENQUEUE_ALBUM, MENU_ENQUEUE_ARTIST, MENU_ENQUEUE_GENRE, MENU_ADD_TO_PLAYLIST};

        for (int id : slide_visible) {
            MenuItem item = mMenu.findItem(id);
            if (item != null) {
                item.setVisible(expanded);
            }
        }

        for (int id : slide_hidden) {
            MenuItem item = mMenu.findItem(id);
            if (item != null) {
                item.setVisible(!expanded);
            }
        }
    }

    @Override
    public void goPlaces() {
        // TODO Auto-generated method stub

    }
}

/*EOF*/