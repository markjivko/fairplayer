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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.fairplayer.DragSortListView;

public class FragmentShowQueue extends Fragment
        implements FpTrackTimelineCallback,
        AdapterView.OnItemClickListener,
        DragSortListView.DropListener,
        DragSortListView.RemoveListener,
        MenuItem.OnMenuItemClickListener {

    private DragSortListView mListView;
    private AdapterShowQueue mListAdapter;
    private FpServiceRendering mService;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fp_queue, container, false);
        Context context = getActivity();

        // Get the parent
        mListView = (DragSortListView) view.findViewById(R.id.fp_list);
        mListAdapter = new AdapterShowQueue(context, R.layout.fp_row);
        mListView.setAdapter(mListAdapter);
        mListView.setDropListener(this);
        mListView.setRemoveListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnCreateContextMenuListener(this);

        FpServiceRendering.addTimelineCallback(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        FpServiceRendering.removeTimelineCallback(this);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get playback service if we can and must
        // This happens eg. during a rotate where the view
        // was destroyed
        if (mService == null && FpServiceRendering.hasInstance()) {
            mService = FpServiceRendering.get(getActivity());
        }

        if (mService != null) {
            refreshSongQueueList(true);
        }
    }

    private final static int CTX_MENU_PLAY = 100;
    private final static int CTX_MENU_ENQUEUE_ALBUM = 101;
    private final static int CTX_MENU_ENQUEUE_ARTIST = 102;
    private final static int CTX_MENU_ENQUEUE_GENRE = 103;
    private final static int CTX_MENU_REMOVE = 104;

    /**
     * Called by Android on long press. Builds the long press context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View listView, ContextMenu.ContextMenuInfo absInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) absInfo;
        FpTrack song = mService.getSongByQueuePosition(info.position);

        Intent intent = new Intent();
        intent.putExtra(AdapterLibrary.DATA_ID, song.id);
        intent.putExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_SONG);
        intent.putExtra(AdapterLibrary.DATA_POSITION, info.position);
        menu.setHeaderTitle(song.title);
        menu.add(0, CTX_MENU_PLAY, 0, R.string.fp_menu_play).setIntent(intent).setOnMenuItemClickListener(this);
//        menu.add(0, CTX_MENU_ENQUEUE_ALBUM, 0, R.string.fp_menu_queue_add_album).setIntent(intent).setOnMenuItemClickListener(this);
//        menu.add(0, CTX_MENU_ENQUEUE_ARTIST, 0, R.string.fp_menu_queue_add_artist).setIntent(intent).setOnMenuItemClickListener(this);
//        menu.add(0, CTX_MENU_ENQUEUE_GENRE, 0, R.string.fp_menu_queue_add_genre).setIntent(intent).setOnMenuItemClickListener(this);
        menu.addSubMenu(0, ActivitySliding.CTX_MENU_ADD_TO_PLAYLIST, 0, R.string.fp_menu_add_to_playlist).getItem().setIntent(intent); // handled by fragment parent
        menu.add(0, CTX_MENU_REMOVE, 0, R.string.fp_menu_remove).setIntent(intent).setOnMenuItemClickListener(this);
    }

    /**
     * Called by Android after the User selected a MenuItem.
     *
     * @param item The selected menu item.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent = item.getIntent();
        int itemId = item.getItemId();
        int pos = intent.getIntExtra(AdapterLibrary.DATA_POSITION, -1);

        FpTrack song = mService.getSongByQueuePosition(pos);
        switch (itemId) {
            case CTX_MENU_PLAY:
                onItemClick(null, null, pos, -1);
                break;

            case CTX_MENU_ENQUEUE_ALBUM:
                mService.enqueueFromSong(song, FpUtilsMedia.TYPE_ALBUM);
                break;

            case CTX_MENU_ENQUEUE_ARTIST:
                mService.enqueueFromSong(song, FpUtilsMedia.TYPE_ARTIST);
                break;

            case CTX_MENU_ENQUEUE_GENRE:
                mService.enqueueFromSong(song, FpUtilsMedia.TYPE_GENRE);
                break;

            case CTX_MENU_REMOVE:
                remove(pos);
                break;

            default:
                throw new IllegalArgumentException("Bad context");
            // we could actually dispatch this to the hosting activity, but we do not need this for now.
        }
        return true;
    }

    /**
     * Fired from adapter fp_activity_library_list if user moved an item
     *
     * @param from the item index that was dragged
     * @param to the index where the item was dropped
     */
    @Override
    public void drop(int from, int to) {
        if (from != to) {
            mService.moveSongPosition(from, to);
        }
    }

    /**
     * Fired from adapter fp_activity_library_list after user removed a song
     *
     * @param which index to remove from queue
     */
    @Override
    public void remove(int which) {
        mService.removeSongPosition(which);
    }

    /**
     * Called when an item in the fp_activity_library_list gets clicked
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mService.jumpToQueuePosition(position);
    }

    /**
     * Triggers a refresh of the queueview
     *
     * @param scroll enable or disable jumping to the currently playing item
     */
    public void refreshSongQueueList(final boolean scroll) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                int i, stotal, spos;
                stotal = mService.getTimelineLength();   /*
                 * Total number of songs in queue
                 */

                spos = mService.getTimelinePosition(); /*
                 * Current position in queue
                 */

                mListAdapter.clear();                    /*
                 * Flush all existing entries...
                 */

                mListAdapter.highlightRow(spos);         /*
                 * and highlight current position
                 */

                for (i = 0; i < stotal; i++) {
                    mListAdapter.add(mService.getSongByQueuePosition(i));
                }

                if (scroll) {
                    scrollToCurrentSong(spos);
                }
            }
        });
    }

    /**
     * Scrolls to the current song<br/>
     * We suppress the new api lint check as lint thinks
     * android.widget.AbsListView#setSelectionFromTop(int, int) was only added
     * in
     * Build.VERSION_CODES#JELLY_BEAN, but it was actually added in API
     * level 1<br/>
     * Android reference: AbsListView.setSelectionFromTop()</a>
     *
     * @param currentSongPosition The position in #mListView of the current song
     */
    @SuppressLint("NewApi")
    private void scrollToCurrentSong(int currentSongPosition) {
        mListView.setSelectionFromTop(currentSongPosition, 0); /*
         * scroll to currently playing song
         */

    }

    /**
     * Called after a song has been set.
     * We are only interested in this call if mService is null
     * as this signals that the playback service just became ready
     * (and wasn't during onResume())
     */
    public void setSong(long uptime, FpTrack song) {
        if (mService == null) {
            mService = FpServiceRendering.get(getActivity());
            onTimelineChanged();
        }
    }

    /**
     * Called after the timeline changed
     */
    public void onTimelineChanged() {
        if (mService != null) {
            refreshSongQueueList(false);
        }
    }

    // Unused Callbacks of FpTrackTimelineCallback
    public void onPositionInfoChanged() {
    }

    public void onMediaChange() {
    }

    public void recreate() {
    }

    public void replaceSong(int delta, FpTrack song) {
    }

    public void setState(long uptime, int state) {
    }
}

/*EOF*/