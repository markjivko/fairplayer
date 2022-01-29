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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;
import java.io.File;
import java.util.Locale;

import junit.framework.Assert;

import android.util.Log;

/**
 * The library activity where songs to play can be selected from the library.
 */
@SuppressLint({ "DefaultLocale", "NewApi" }) 
public class ActivityLibrary extends ActivitySliding implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, SearchView.OnQueryTextListener {

    public static final String INTENT_ALBUM_ID = "albumId";
    public static final String INTENT_ALBUM = "album";
    public static final String INTENT_ARTIST = "artist";
    public static final String INTENT_SONG_ID = "songId";
    public static final String INTENT_SONG_TITLE = "track";
    public static final String INTENT_PLAYING = "playing";
    
    /**
     * FpAction for row click: play the row.
     */
    public static final int ACTION_PLAY = 0;
    /**
     * FpAction for row click: enqueue the row.
     */
    public static final int ACTION_ENQUEUE = 1;
    /**
     * FpAction for row click: perform the last used action.
     */
    public static final int ACTION_LAST_USED = 2;
    /**
     * FpAction for row click: play all the songs in the adapter, starting with
     * the current row.
     */
    public static final int ACTION_PLAY_ALL = 3;
    /**
     * FpAction for row click: enqueue all the songs in the adapter, starting with
     * the current row.
     */
    public static final int ACTION_ENQUEUE_ALL = 4;
    /**
     * FpAction for row click: do nothing.
     */
    public static final int ACTION_DO_NOTHING = 5;
    /**
     * FpAction for row click: expand the row.
     */
    public static final int ACTION_EXPAND = 6;
    /**
     * FpAction for row click: play if paused or enqueue if playing.
     */
    public static final int ACTION_PLAY_OR_ENQUEUE = 7;
    /**
     * FpAction for row click: queue selection as next item
     */
    public static final int ACTION_ENQUEUE_AS_NEXT = 8;
    /**
     * The FpTrackTimeline add song modes corresponding to each relevant action.
     */
    private static final int[] modeForAction = {
        FpTrackTimeline.MODE_PLAY, FpTrackTimeline.MODE_ENQUEUE, -1,
        FpTrackTimeline.MODE_PLAY_ID_FIRST, FpTrackTimeline.MODE_ENQUEUE_ID_FIRST,
        -1, -1, -1, FpTrackTimeline.MODE_ENQUEUE_AS_NEXT
    };
    public ViewPager mViewPager;
    private ControlbarBottom mControlbarBottom;
    private HorizontalScrollView mLimiterScroller;
    private ViewGroup mLimiterViews;
    private LayoutTab mLayoutTab;
    
    /**
     * The id of the media that was last pressed in the current adapter. Used to
     * open the playback activity when an item is pressed twice.
     */
    private long mLastActedId;
    
    /**
     * The pager adapter that manages each media ListView.
     */
    public AdapterPagerLibrary mPagerAdapter;
    /**
     * The adapter for the currently visible list.
     */
    private AdapterLibrary mCurrentAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        setContentView(R.layout.fp_activity_library);
        
        // Set the background
        ((RelativeLayout) findViewById(R.id.fp_content_wrapper)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_page));
        
        mLimiterScroller = (HorizontalScrollView) findViewById(R.id.fp_limiter_scroller);
        mLimiterViews = (ViewGroup) findViewById(R.id.fp_limiter_layout);
        AdapterPagerLibrary pagerAdapter = new AdapterPagerLibrary(this, mLooper);
        mPagerAdapter = pagerAdapter;
        ViewPager pager = (ViewPager) findViewById(R.id.fp_pager);
        pager.setAdapter(pagerAdapter);
        mViewPager = pager;
        mControlbarBottom = (ControlbarBottom) findViewById(R.id.fp_bottombar_controls);
        mControlbarBottom.setOnClickListener(this);
        mControlbarBottom.setOnQueryTextListener(this);
        mControlbarBottom.enableOptionsMenu(this);
        mLayoutTab = (LayoutTab) findViewById(R.id.fp_sliding_tabs);
        mLayoutTab.setOnPageChangeListener(pagerAdapter);
        loadTabOrder();
        loadAlbumIntent(getIntent());
        bindControlButtons();
        
        // Prepare the tutorial items
        int[][] tutorialItems = {
            {R.id.fp_limiter_scroller, R.string.fp_tutorial_welcome_title, R.string.fp_tutorial_welcome_info},
            {R.id.fp_sliding_tabs, R.string.fp_tutorial_scroller_title, R.string.fp_tutorial_scroller_info},
            {R.id.fp_bottombar_controls, R.string.fp_tutorial_bottombar_title, R.string.fp_tutorial_bottombar_info},
            {R.id.fp_text, R.string.fp_tutorial_playall_title, R.string.fp_tutorial_playall_info},
        };
        
        // Show the tutorial
        FpTutorial.show((Activity) this, tutorialItems, Constants.Keys.SETTINGS_TUTORIAL_LIBRARY);
        
        // Set the listener
        FpTutorial.setOnShowListener(new FpTutorial.ShowListener() {
			
            @Override
            public void onItemEnd(int tutorialItemId) {
                // Request the permissions
                FpPermissions.request(ActivityLibrary.this);

                // Reload
                mPagerAdapter.invalidateData();
            }
        });

        // Ads 
        if (FpTutorial.isShown(Constants.Keys.SETTINGS_TUTORIAL_LIBRARY)) {
            this.setAction(this.ACTION_NONE);
            this.ads.place(Ads.AdPlacement.ENTER_LIBRARY);
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        loadTabOrder();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLastActedId = AdapterLibrary.INVALID_ID;
        updateHeaders();
    }

    /**
     * Load the tab order and update the tab bars if needed.
     */
    private void loadTabOrder() {
        if (mPagerAdapter.loadTabOrder()) {
            // Reinitializes all tabs
            mLayoutTab.setViewPager(mViewPager);
        }
    }

    /** 
     * If the given intent has album data, set a limiter built from that data.
     */
    private void loadAlbumIntent(Intent intent) {
        long albumId = intent.getLongExtra(ActivityLibrary.INTENT_ALBUM_ID, -1);
        if (albumId != -1) {
            String[] fields = {intent.getStringExtra(ActivityLibrary.INTENT_ARTIST), intent.getStringExtra(ActivityLibrary.INTENT_ALBUM)};
            String data = String.format(Locale.US, "%s=%d", MediaStore.Audio.AudioColumns.ALBUM_ID, albumId);
            FpSerializableLimiter limiter = new FpSerializableLimiter(FpUtilsMedia.TYPE_ALBUM, fields, data);
            int tab = mPagerAdapter.setLimiter(limiter);
            if (tab == -1 || tab == mViewPager.getCurrentItem()) {
                updateLimiterViews();
            } else {
                mViewPager.setCurrentItem(tab);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        loadAlbumIntent(intent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                FpSerializableLimiter limiter = mPagerAdapter.getCurrentLimiter();
                if (mElementSlider.isHidden() == false) {
                    mElementSlider.hideSlide();
                    break;
                }
                if (mControlbarBottom.showSearch(false)) {
                    break;
                }
                if (limiter != null) {
                    int pos = -1;
                    switch (limiter.type) {
                        case FpUtilsMedia.TYPE_ALBUM:
                            setLimiter(FpUtilsMedia.TYPE_ARTIST, limiter.data.toString());
                            pos = mPagerAdapter.mAlbumsPosition;
                            break;
                            
                        case FpUtilsMedia.TYPE_ARTIST:
                            mPagerAdapter.clearLimiter(FpUtilsMedia.TYPE_ARTIST);
                            pos = mPagerAdapter.mArtistsPosition;
                            break;
                            
                        case FpUtilsMedia.TYPE_GENRE:
                            mPagerAdapter.clearLimiter(FpUtilsMedia.TYPE_GENRE);
                            pos = mPagerAdapter.mGenresPosition;
                            break;
                            
                        case FpUtilsMedia.TYPE_FILE:
                            if (limiter.names.length > 1) {
                                File parentFile = ((File) limiter.data).getParentFile();
                                mPagerAdapter.setLimiter(AdapterFileSystem.buildLimiter(parentFile));
                            } else {
                                mPagerAdapter.clearLimiter(FpUtilsMedia.TYPE_FILE);
                            }
                            break;
                    }
                    if (pos == -1) {
                        updateLimiterViews();
                    } else {
                        mViewPager.setCurrentItem(pos);
                    }
                } else {
                    finish();
                }
                break;
                
            case KeyEvent.KEYCODE_MENU:
                // We intercept these to avoid showing the activity-default menu
                mControlbarBottom.openMenu();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL) // On ICS, EditText reports backspace events as unhandled despite
        // actually handling them. To workaround, just assume the event was
        // handled if we get here.
        {
            return true;
        }
        if (super.onKeyDown(keyCode, event)) {
            return true;
        }
        return false;
    }

    /**
     * Update the first row of the lists with the appropriate action (play all
     * or enqueue all).
     */
    private void updateHeaders() {
        mPagerAdapter.setHeaderText(getString(R.string.fp_menu_play_all));
    }

    /**
     * Adds songs matching the data from the given intent to the song timelime.
     *
     * @param intent An intent created with AdapterLibrary#createData(View).
     * @param action One of ActivityLibrary.ACTION_*
     */
    private void pickSongs(Intent intent, int action) {
        long id = intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID);
        boolean all = false;
        int mode = action;
        if (action == ACTION_PLAY_ALL || action == ACTION_ENQUEUE_ALL) {
            int type = mCurrentAdapter.getMediaType();
            boolean notPlayAllAdapter = type > FpUtilsMedia.TYPE_SONG || id == AdapterLibrary.HEADER_ID;
            if (mode == ACTION_ENQUEUE_ALL && notPlayAllAdapter) {
                mode = ACTION_ENQUEUE;
            } else {
                if (mode == ACTION_PLAY_ALL && notPlayAllAdapter) {
                    mode = ACTION_PLAY;
                } else {
                    all = true;
                }
            }
        }
        if (id == AdapterLibrary.HEADER_ID) {
            all = true; // page header was clicked -> force all mode
        }
        FpUtilsMedia.QueryTask query = buildQueryFromIntent(intent, false, (all ? (AdapterMedia) mCurrentAdapter : null));
        query.mode = modeForAction[mode];
        FpServiceRendering.get(this).addSongs(query);
        mLastActedId = id;
        
        // Open the player
        if (all) {
            // Get the handler
            final Handler handler = new Handler();
            
            // Run after 1 second
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openActivitySlidingCommon();
                }
            }, 1000);
        }
    }

    /**
     * "Expand" the view represented by the given intent by setting the limiter
     * from the view and switching to the appropriate tab.
     *
     * @param intent An intent created with AdapterLibrary#createData(View).
     */
    private void expand(Intent intent) {
        mControlbarBottom.showSearch(false);
        int type = intent.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);
        long id = intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID);
        int tab = mPagerAdapter.setLimiter(mPagerAdapter.mAdapters[type].buildLimiter(id));
        if (tab == -1 || tab == mViewPager.getCurrentItem()) {
            updateLimiterViews();
        } else {
            mViewPager.setCurrentItem(tab);
        }
    }

    /**
     * Called by LibraryAdapters when a row has been clicked.
     *
     * @param rowData The data for the row that was clicked.
     */
    public void onItemClicked(Intent rowData) {
        if (rowData.getBooleanExtra(AdapterLibrary.DATA_EXPANDABLE, false)) {
            onItemExpanded(rowData);
        } else {
            if (rowData.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID) == mLastActedId) {
                openActivitySlidingCommon();
            } else {
                pickSongs(rowData, ACTION_PLAY);
            }
        }
    }

    /**
     * Called by LibraryAdapters when a row's expand arrow has been clicked.
     *
     * @param rowData The data for the row that was clicked.
     */
    public void onItemExpanded(Intent rowData) {
        int type = rowData.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);
        if (type == FpUtilsMedia.TYPE_PLAYLIST) {
            editPlaylist(rowData);
        } else {
            expand(rowData);
        }
    }

    /**
     * Create or recreate the limiter breadcrumbs.
     */
    @SuppressLint("InflateParams") 
    public void updateLimiterViews() {
        mLimiterViews.removeAllViews();
        FpSerializableLimiter limiterData = mPagerAdapter.getCurrentLimiter();
        if (limiterData != null) {
            // Prepare the limiters
            String[] limiter = limiterData.names;
            
            // Go throuhg the list
            for (int i = 0; i < limiter.length; i++) {
                // Prepare the prefix
                String prefix = "";
                
                // Dependent on media type
                switch (limiterData.type) {
                    case FpUtilsMedia.TYPE_FILE:
                        prefix = "/";
                        break;
                    
                    case FpUtilsMedia.TYPE_ARTIST:
                        switch (i) {
                            case 0:
                                prefix = this.getText(R.string.fp_library_limiter_artist) + ": ";
                                break;
                        }
                        break;
                        
                    case FpUtilsMedia.TYPE_GENRE:
                        switch (i) {
                            case 0:
                                prefix = this.getText(R.string.fp_library_limiter_genre) + ": ";
                                break;
                        }
                        break;
                    
                    case FpUtilsMedia.TYPE_ALBUM:
                        switch (i) {
                            case 0:
                                prefix = this.getText(R.string.fp_library_limiter_artist) + ": ";
                                break;
                                
                            case 1:
                                prefix = this.getText(R.string.fp_library_limiter_album) + ": ";
                                break;
                        }
                        break;
                }
                
            	// Get the limiter layout
                LinearLayout limiterLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.fp_activity_library_limiter, null);
                
                // Set the background
                limiterLayout.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
                
                // Set the tag
                limiterLayout.setTag(i);
                
                // Set the text
                ((TextView) limiterLayout.findViewById(R.id.fp_text_view)).setText(prefix + limiter[i]);
                ((TextView) limiterLayout.findViewById(R.id.fp_text_view)).setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
                
                // Set the close icon
                ((ImageView) limiterLayout.findViewById(R.id.fp_image_view)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_row_close));
                
                // Set the on-click listener
                limiterLayout.setOnClickListener(this);
                
                // Add the view
                mLimiterViews.addView(limiterLayout);
            }
            
            // Make visible
            mLimiterScroller.setVisibility(View.VISIBLE);
        } else {
            mLimiterScroller.setVisibility(View.GONE);
        }
    }

    /**
     * Updates mCover with the new bitmap, running in the UI thread
     *
     * @param cover the cover to set, will use a fallback drawable if null
     */
    private void updateCover(final Bitmap cover) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlbarBottom.setCover(cover);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == mControlbarBottom) {
            // Queue not empty
            if(FpServiceRendering.get(this).getTimelineLength() > 0) {
                openActivitySlidingCommon();
            } else {
            	Toast.makeText(this, getString(R.string.fp_menu_queue_empty_warn), Toast.LENGTH_LONG).show();
            }
            
            // Stop here
            return;
        }
        
        if (view.getTag() != null) {
            // a limiter view was clicked
            int i = (Integer) view.getTag();
            FpSerializableLimiter limiter = mPagerAdapter.getCurrentLimiter();
            int type = limiter.type;
            if (i == 1 && type == FpUtilsMedia.TYPE_ALBUM) {
                setLimiter(FpUtilsMedia.TYPE_ARTIST, limiter.data.toString());
            } else {
                if (i > 0) {
                    Assert.assertEquals(FpUtilsMedia.TYPE_FILE, limiter.type);
                    File file = (File) limiter.data;
                    int diff = limiter.names.length - i;
                    while (--diff != -1) {
                        file = file.getParentFile();
                    }
                    mPagerAdapter.setLimiter(AdapterFileSystem.buildLimiter(file));
                } else {
                    mPagerAdapter.clearLimiter(type);
                }
            }
            updateLimiterViews();
            
            // Stop here
            return;
        }
        
        // Call the parent
        super.onClick(view);
    }

    /**
     * Set a new limiter of the given type built from the first
     * MediaStore.Audio.Media row that matches the selection.
     *
     * @param limiterType The type of limiter to create. Must be either
     * FpUtilsMedia.TYPE_ARTIST or FpUtilsMedia.TYPE_ALBUM.
     * @param selection Selection to pass to the query.
     */
    private void setLimiter(int limiterType, String selection) {
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] {MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};
        Cursor cursor = FpUtilsMedia.queryResolver(resolver, uri, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                String[] fields;
                String data;
                switch (limiterType) {
                    case FpUtilsMedia.TYPE_ARTIST:
                        fields = new String[] {cursor.getString(2)};
                        data = String.format(Locale.US, "%s=%d", MediaStore.Audio.AudioColumns.ARTIST_ID, cursor.getLong(0));
                        break;
                        
                    case FpUtilsMedia.TYPE_ALBUM:
                        fields = new String[] {cursor.getString(2), cursor.getString(3)};
                        data = String.format(Locale.US, "%s=%d", MediaStore.Audio.AudioColumns.ALBUM_ID, cursor.getLong(1));
                        break;
                        
                    default:
                        throw new IllegalArgumentException("Invalid type " + limiterType);
                }
                mPagerAdapter.setLimiter(new FpSerializableLimiter(limiterType, fields, data));
            }
            cursor.close();
        }
    }
    private static final int CTX_MENU_PLAY = 0;
    private static final int CTX_MENU_ENQUEUE = 1;
    private static final int CTX_MENU_EXPAND = 2;
    private static final int CTX_MENU_ENQUEUE_AS_NEXT = 3;
    private static final int CTX_MENU_DELETE = 4;
    private static final int CTX_MENU_RENAME_PLAYLIST = 5;
    private static final int CTX_MENU_PLAY_ALL = 6;
    private static final int CTX_MENU_ENQUEUE_ALL = 7;
    private static final int CTX_MENU_MORE_FROM_ALBUM = 8;
    private static final int CTX_MENU_MORE_FROM_ARTIST = 9;
    private static final int CTX_MENU_OPEN_EXTERNAL = 10;

    /**
     * Creates a context menu for an adapter row.
     *
     * @param menu The menu to create.
     * @param rowData Data for the adapter row.
     */
    public void onCreateContextMenu(ContextMenu menu, Intent rowData) {
        if (rowData.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID) == AdapterLibrary.HEADER_ID) {
            menu.setHeaderTitle(getString(R.string.fp_library_header_songs));
            menu.add(0, CTX_MENU_PLAY_ALL, 0, R.string.fp_menu_play_all).setIntent(rowData);
            menu.add(0, CTX_MENU_ENQUEUE_ALL, 0, R.string.fp_menu_queue_add_all).setIntent(rowData);
            menu.addSubMenu(0, CTX_MENU_ADD_TO_PLAYLIST, 0, R.string.fp_menu_add_to_playlist).getItem().setIntent(rowData);
        } else {
            int type = rowData.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);
            menu.setHeaderTitle(rowData.getStringExtra(AdapterLibrary.DATA_TITLE));
            if (FpUtilsFilesystem.canDispatchIntent(rowData)) {
                menu.add(0, CTX_MENU_OPEN_EXTERNAL, 0, R.string.fp_menu_open).setIntent(rowData);
            }
            menu.add(0, CTX_MENU_PLAY, 0, R.string.fp_menu_play).setIntent(rowData);
            if (type <= FpUtilsMedia.TYPE_SONG) {
                menu.add(0, CTX_MENU_PLAY_ALL, 0, R.string.fp_menu_play_all).setIntent(rowData);
            }
            menu.add(0, CTX_MENU_ENQUEUE_AS_NEXT, 0, R.string.fp_menu_play_next).setIntent(rowData);
            menu.add(0, CTX_MENU_ENQUEUE, 0, R.string.fp_menu_queue_add).setIntent(rowData);
            if (type == FpUtilsMedia.TYPE_PLAYLIST) {
                menu.add(0, CTX_MENU_RENAME_PLAYLIST, 0, R.string.fp_menu_rename).setIntent(rowData);
            } else {
                if (rowData.getBooleanExtra(AdapterLibrary.DATA_EXPANDABLE, false)) {
                    menu.add(0, CTX_MENU_EXPAND, 0, R.string.fp_menu_expand).setIntent(rowData);
                }
            }
            if (type == FpUtilsMedia.TYPE_ALBUM || type == FpUtilsMedia.TYPE_SONG) {
                menu.add(0, CTX_MENU_MORE_FROM_ARTIST, 0, R.string.fp_menu_more_artist).setIntent(rowData);
            }
            if (type == FpUtilsMedia.TYPE_SONG) {
                menu.add(0, CTX_MENU_MORE_FROM_ALBUM, 0, R.string.fp_menu_more_album).setIntent(rowData);
            }
            menu.addSubMenu(0, CTX_MENU_ADD_TO_PLAYLIST, 0, R.string.fp_menu_add_to_playlist).getItem().setIntent(rowData);
            menu.add(0, CTX_MENU_DELETE, 0, R.string.fp_menu_delete).setIntent(rowData);
        }
    }

    /**
     * Open the playlist editor for the playlist with the given id.
     */
    private void editPlaylist(Intent rowData) {
        Intent launch = new Intent(this, ActivityPlaylist.class);
        launch.putExtra(AdapterLibrary.DATA_PLAYLIST, rowData.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID));
        launch.putExtra(AdapterLibrary.DATA_TITLE, rowData.getStringExtra(AdapterLibrary.DATA_TITLE));
        startActivity(launch);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() != 0) {
            return super.onContextItemSelected(item);
        }
        final Intent intent = item.getIntent();
        switch (item.getItemId()) {
            case CTX_MENU_EXPAND:
                expand(intent);
                break;
            case CTX_MENU_ENQUEUE:
                pickSongs(intent, ACTION_ENQUEUE);
                break;
            case CTX_MENU_PLAY:
                pickSongs(intent, ACTION_PLAY);
                break;
            case CTX_MENU_PLAY_ALL:
                pickSongs(intent, ACTION_PLAY_ALL);
                break;
            case CTX_MENU_ENQUEUE_ALL:
                pickSongs(intent, ACTION_ENQUEUE_ALL);
                break;
            case CTX_MENU_ENQUEUE_AS_NEXT:
                pickSongs(intent, ACTION_ENQUEUE_AS_NEXT);
                break;
            case CTX_MENU_RENAME_PLAYLIST: {
                FpPlaylistTask playlistTask = new FpPlaylistTask(intent.getLongExtra(AdapterLibrary.DATA_ID, -1), intent.getStringExtra(AdapterLibrary.DATA_TITLE));
                FpPlaylistPopupNew dialog = new FpPlaylistPopupNew(this, intent.getStringExtra(AdapterLibrary.DATA_TITLE), R.string.fp_menu_rename, playlistTask);
                dialog.setDismissMessage(mHandler.obtainMessage(MSG_RENAME_PLAYLIST, dialog));
                dialog.show();
                break;
            }
            case CTX_MENU_DELETE:
                String delete_message = getString(R.string.fp_menu_notif_delete_confirm_item, intent.getStringExtra(AdapterLibrary.DATA_TITLE));
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.fp_menu_delete);
                dialog
                        .setMessage(delete_message)
                        .setPositiveButton(R.string.fp_menu_delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_DELETE, intent));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                dialog.create().show();
                break;
            case CTX_MENU_OPEN_EXTERNAL: {
                FpUtilsFilesystem.dispatchIntent(this, intent);
                break;
            }
            case CTX_MENU_MORE_FROM_ARTIST: {
                String selection;
                if (intent.getIntExtra(AdapterLibrary.DATA_TYPE, -1) == FpUtilsMedia.TYPE_ALBUM) {
                    selection = MediaStore.Audio.AudioColumns.ALBUM_ID + "=";
                } else {
                    selection = MediaStore.MediaColumns._ID + "=";
                }
                selection += intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID);
                setLimiter(FpUtilsMedia.TYPE_ARTIST, selection);
                updateLimiterViews();
                break;
            }
            case CTX_MENU_MORE_FROM_ALBUM:
                setLimiter(FpUtilsMedia.TYPE_ALBUM, MediaStore.MediaColumns._ID + "=" + intent.getLongExtra(AdapterLibrary.DATA_ID, AdapterLibrary.INVALID_ID));
                updateLimiterViews();
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SEARCH, 0, R.string.fp_menu_search).setIcon(R.drawable.fp_panel_search).setVisible(false);
        menu.add(0, MENU_SORT, 30, R.string.fp_menu_sort_by);
        menu.add(0, MENU_NOW_PLAYING, 30, R.string.fp_menu_now_playing);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        AdapterLibrary adapter = mCurrentAdapter;
        menu.findItem(MENU_SORT).setEnabled(adapter != null && adapter.getMediaType() != FpUtilsMedia.TYPE_FILE);
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("InflateParams") 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SEARCH:
                mControlbarBottom.showSearch(true);
                return true;
                
            case android.R.id.home:
            case MENU_NOW_PLAYING:
                openActivitySlidingCommon();
                return true;
                
            case MENU_SORT: {
                AdapterMedia adapter = (AdapterMedia) mCurrentAdapter;
                LinearLayout header = (LinearLayout) getLayoutInflater().inflate(R.layout.fp_dialog_sort, null);
                CheckBox reverseSort = (CheckBox) header.findViewById(R.id.fp_reverse_sort);
                int[] itemIds = adapter.getSortEntries();
                String[] items = new String[itemIds.length];
                Resources res = getResources();
                for (int i = itemIds.length; --i != -1;) {
                    items[i] = res.getString(itemIds[i]);
                }
                int mode = adapter.getSortMode();
                if (mode < 0) {
                    mode = ~mode;
                    reverseSort.setChecked(true);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.fp_menu_sort_by);
                builder.setSingleChoiceItems(items, mode + 1, this); // add 1 for header
                builder.setNeutralButton(R.string.fp_menu_done, null);
                AlertDialog dialog = builder.create();
                dialog.getListView().addHeaderView(header);
                dialog.setOnDismissListener(this);
                dialog.show();
                return true;
            }
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Callback of mSearchView while user types in text
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        mPagerAdapter.setFilter(newText);
        return true;
    }

    /**
     * Callback of mSearchViews submit action
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        mPagerAdapter.setFilter(query);
        return true;
    }
    /**
     * Save the current page, passed in arg1, to SharedPreferences.
     */
    private static final int MSG_SAVE_PAGE = 40;
    /**
     * Updates mCover using a background thread
     */
    private static final int MSG_UPDATE_COVER = 41;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_SAVE_PAGE: {
                SharedPreferences.Editor editor = PreferenceUtils.edit();
                editor.putInt(Constants.Keys.SETTINGS_LIBRARY_PAGE, message.arg1);
                editor.apply();
                break;
            }
            
            case MSG_UPDATE_COVER: {
                Bitmap cover = null;
                FpTrack song = (FpTrack) message.obj;
                if (song != null) {
                    cover = song.getSmallCover(this);
                }
                // Dispatch view update to UI thread
                updateCover(cover);
                break;
            }
            default:
                return super.handleMessage(message);
        }
        return true;
    }

    @Override
    public void onMediaChange() {
        mPagerAdapter.invalidateData();
    }

    @Override
    protected void onStateChange(int state, int toggled) {
        super.onStateChange(state, toggled);
        if ((state & FpServiceRendering.FLAG_EMPTY_QUEUE) != 0) {
            mControlbarBottom.setSong(null);
        }
    }

    @Override
    protected void onSongChange(FpTrack song) {
        super.onSongChange(song);
        mControlbarBottom.setSong(song);
        if (song != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_COVER, song));
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ListView list = ((AlertDialog) dialog).getListView();
        // subtract 1 for header
        int which = list.getCheckedItemPosition() - 1;
        CheckBox reverseSort = (CheckBox) list.findViewById(R.id.fp_reverse_sort);
        if (reverseSort.isChecked()) {
            which = ~which;
        }
        mPagerAdapter.setSortMode(which);
    }

    /**
     * Called when a new page becomes visible.
     *
     * @param position The position of the new page.
     * @param adapter The new visible adapter.
     */
    public void onPageChanged(int position, AdapterLibrary adapter) {
        mCurrentAdapter = adapter;
        mLastActedId = AdapterLibrary.INVALID_ID;
        updateLimiterViews();
        if (adapter != null && (adapter.getLimiter() == null || adapter.getMediaType() == FpUtilsMedia.TYPE_FILE)) {
            // Save current page so it is opened on next startup. Don't save if
            // the page was expanded to, as the expanded page isn't the starting
            // point. This limitation does not affect the files tab as the limiter
            // (the files almost always have a limiter)
            Handler handler = mHandler;
            handler.sendMessage(mHandler.obtainMessage(MSG_SAVE_PAGE, position, 0));
        }
    }
}

/*EOF*/