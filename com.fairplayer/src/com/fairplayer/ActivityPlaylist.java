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
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import com.fairplayer.DragSortListView;

/**
 * The playlist activity where playlist songs can be viewed and reordered.
 */
public class ActivityPlaylist extends Activity
        implements View.OnClickListener, AbsListView.OnItemClickListener, DialogInterface.OnClickListener, DragSortListView.DropListener, DragSortListView.RemoveListener {

    /**
     * The FpTrackTimeline play mode corresponding to each
     * ActivityLibrary.ACTION_*
     */
    private static final int[] MODE_FOR_ACTION
            = {FpTrackTimeline.MODE_PLAY, FpTrackTimeline.MODE_ENQUEUE, -1,
                FpTrackTimeline.MODE_PLAY_POS_FIRST, FpTrackTimeline.MODE_ENQUEUE_POS_FIRST,
                -1, -1, -1, FpTrackTimeline.MODE_ENQUEUE_AS_NEXT};

    /**
     * An event loop running on a worker thread.
     */
    private Looper mLooper;
    private DragSortListView mListView;
    private AdapterPlaylist mAdapter;

    /**
     * The id of the playlist this activity is currently viewing.
     */
    private long mPlaylistId;
    /**
     * The name of the playlist this activity is currently viewing.
     */
    private String mPlaylistName;
    /**
     * If true, then playlists songs can be dragged to reorder.
     */
    private boolean mEditing;

    /**
     * The last action used from the context menu, used to implement
     * LAST_USED_ACTION action.
     */
    private int mLastAction = ActivityLibrary.ACTION_PLAY;

    private Button mEditButton;
    private Button mDeleteButton;

    @SuppressLint("InflateParams")
    @Override
    public void onCreate(Bundle state) {
        setTheme(R.style.StyleActionBar);
        super.onCreate(state);

        // Prepare the action bar
        ActionBar actionBar = getActionBar();
        
        // Set the background
        actionBar.setBackgroundDrawable(Theme.Resources.getDrawable(R.drawable.fp_bg_bar));
        
        // Start the new thread
        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();

        setContentView(R.layout.fp_activity_playlist);
        
        // Set the page background
        ((LinearLayout) findViewById(R.id.fp_content)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_page));
        
        DragSortListView view = (DragSortListView) findViewById(R.id.fp_list);
        view.setOnItemClickListener(this);
        view.setOnCreateContextMenuListener(this);
        view.setDropListener(this);
        view.setRemoveListener(this);
        mListView = view;

        View header = LayoutInflater.from(this).inflate(R.layout.fp_activity_playlist_buttons, null);
        
        // Prepare the edit button
        mEditButton = (Button) header.findViewById(R.id.fp_edit);
        mEditButton.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
        mEditButton.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
        mEditButton.setOnClickListener(this);
        
        // Prepare the delete button
        mDeleteButton = (Button) header.findViewById(R.id.fp_delete);
        mDeleteButton.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
        mDeleteButton.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
        mDeleteButton.setOnClickListener(this);
        
        // Add the header
        view.addHeaderView(header, null, false);
        mLooper = thread.getLooper();
        mAdapter = new AdapterPlaylist(this, mLooper);
        view.setAdapter(mAdapter);

        onNewIntent(getIntent());
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Track pageview
    	Tracker.trackPageview(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        mLooper.quit();
        super.onDestroy();
    }

    @Override
    public void onNewIntent(Intent intent) {
        long id = intent.getLongExtra(AdapterLibrary.DATA_PLAYLIST, 0);
        String title = intent.getStringExtra(AdapterLibrary.DATA_TITLE);
        mAdapter.setPlaylistId(id);
        mPlaylistId = id;
        mPlaylistName = title;
        
        // Prepare the action bar
        ActionBar actionBar = getActionBar();
        
        // Prepare the spannable string
        SpannableString spannableString = new SpannableString(title);
        
        // Prepare the span
        spannableString.setSpan(new ForegroundColorSpan(Theme.Resources.getColor(R.color.fp_color_bar_title)), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the colored title
        actionBar.setTitle(spannableString);
        
        // Set the title
        setTitle(title);
    }

    /**
     * Enable or disable edit mode, which allows songs to be reordered and
     * removed.
     *
     * @param editing True to enable edit mode.
     */
    public void setEditing(boolean editing) {
        mListView.setDragEnabled(editing);
        mAdapter.setEditable(editing);
        int visible = editing ? View.GONE : View.VISIBLE;
        mDeleteButton.setVisibility(visible);
        mEditButton.setText(editing ? R.string.fp_menu_done : R.string.fp_menu_edit);
        mEditing = editing;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fp_edit:
                setEditing(!mEditing);
                break;
                
            case R.id.fp_delete: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                String message = getResources().getString(R.string.fp_playlist_delete, mPlaylistName);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.fp_menu_delete, this);
                builder.setNegativeButton(android.R.string.cancel, this);
                builder.show();
                break;
            }
        }
    }

    private static final int MENU_PLAY = ActivityLibrary.ACTION_PLAY;
    private static final int MENU_PLAY_ALL = ActivityLibrary.ACTION_PLAY_ALL;
    private static final int MENU_ENQUEUE = ActivityLibrary.ACTION_ENQUEUE;
    private static final int MENU_ENQUEUE_ALL = ActivityLibrary.ACTION_ENQUEUE_ALL;
    private static final int MENU_ENQUEUE_AS_NEXT = ActivityLibrary.ACTION_ENQUEUE_AS_NEXT;
    private static final int MENU_REMOVE = -1;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View listView, ContextMenu.ContextMenuInfo absInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) absInfo;
        Intent intent = new Intent();
        intent.putExtra(AdapterLibrary.DATA_ID, info.id);
        intent.putExtra(AdapterLibrary.DATA_POSITION, info.position);
        intent.putExtra(AdapterLibrary.DATA_AUDIO_ID, (Long) info.targetView.findViewById(R.id.fp_text).getTag());

        menu.add(0, MENU_PLAY, 0, R.string.fp_menu_play).setIntent(intent);
        menu.add(0, MENU_PLAY_ALL, 0, R.string.fp_menu_play_all).setIntent(intent);
        menu.add(0, MENU_ENQUEUE_AS_NEXT, 0, R.string.fp_menu_play_next).setIntent(intent);
        menu.add(0, MENU_ENQUEUE, 0, R.string.fp_menu_queue_add).setIntent(intent);
        menu.add(0, MENU_ENQUEUE_ALL, 0, R.string.fp_menu_queue_add_all).setIntent(intent);
        menu.add(0, MENU_REMOVE, 0, R.string.fp_menu_remove).setIntent(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        Intent intent = item.getIntent();
        int pos = intent.getIntExtra(AdapterLibrary.DATA_POSITION, -1);

        if (itemId == MENU_REMOVE) {
            mAdapter.removeItem(pos - mListView.getHeaderViewsCount());
        } else {
            performAction(itemId, pos, intent.getLongExtra(AdapterLibrary.DATA_AUDIO_ID, -1));
        }

        return true;
    }

    /**
     * Perform the specified action on the adapter row with the given id and
     * position.
     *
     * @param action One of ActivityLibrary.ACTION_*.
     * @param position The position in the adapter.
     * @param audioId The id of the selected song, for PLAY/ENQUEUE.
     */
    private void performAction(int action, int position, long audioId) {
        if (action == ActivityLibrary.ACTION_PLAY_OR_ENQUEUE) {
            action = (FpServiceRendering.get(this).isPlaying() ? ActivityLibrary.ACTION_ENQUEUE : ActivityLibrary.ACTION_PLAY);
        }

        if (action == ActivityLibrary.ACTION_LAST_USED) {
            action = mLastAction;
        }

        switch (action) {
            case ActivityLibrary.ACTION_PLAY:
            case ActivityLibrary.ACTION_ENQUEUE:
            case ActivityLibrary.ACTION_ENQUEUE_AS_NEXT: {
                FpUtilsMedia.QueryTask query = FpUtilsMedia.buildQuery(FpUtilsMedia.TYPE_SONG, audioId, FpTrack.FILLED_PROJECTION, null);
                query.mode = MODE_FOR_ACTION[action];
                FpServiceRendering.get(this).addSongs(query);
                break;
            }
            
            case ActivityLibrary.ACTION_PLAY_ALL:
            case ActivityLibrary.ACTION_ENQUEUE_ALL: {
                FpUtilsMedia.QueryTask query = FpUtilsMedia.buildPlaylistQuery(mPlaylistId, FpTrack.FILLED_PLAYLIST_PROJECTION, null);
                query.mode = MODE_FOR_ACTION[action];
                query.data = position - mListView.getHeaderViewsCount();
                FpServiceRendering.get(this).addSongs(query);
                break;
            }
        }

        mLastAction = action;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!mEditing) {
            // fixme: this is butt ugly: the adapter should probably already set this on view (its parent)
            performAction(ActivityLibrary.ACTION_PLAY, position, (Long) view.findViewById(R.id.fp_text).getTag());
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            FpPlaylist.deletePlaylist(getContentResolver(), mPlaylistId);
            finish();
        }
        dialog.dismiss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fired from adapter fp_activity_library_list if user moved an item
     *
     * @param from the item index that was dragged
     * @param to the index where the item was dropped
     */
    @Override
    public void drop(int from, int to) {
        mAdapter.moveItem(from, to);
    }

    /**
     * Fired from adapter fp_activity_library_list if user fling-removed an item
     *
     * @param position The position of the removed item
     */
    @Override
    public void remove(int position) {
        mAdapter.removeItem(position);
    }
}

/*EOF*/