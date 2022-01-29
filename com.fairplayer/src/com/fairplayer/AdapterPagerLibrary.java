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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.util.LruCache;
import android.widget.AdapterView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.util.Arrays;
import java.util.Locale;

/**
 * PagerAdapter that manages the library media ListViews.
 */
@SuppressLint("InflateParams") 
public class AdapterPagerLibrary extends PagerAdapter implements Handler.Callback, ViewPager.OnPageChangeListener, View.OnCreateContextMenuListener, AdapterView.OnItemClickListener {

    /**
     * The number of unique list types. The number of visible lists may be
     * smaller.
     */
    public static final int MAX_ADAPTER_COUNT = 6;
    
    /**
     * Limiters
     */
    public static final String LIMITER_SONGS = "fp_lim_song";
    public static final String LIMITER_ALBUMS = "fp_lim_album";
    public static final String LIMITER_ARTISTS = "fp_lim_artist";
    public static final String LIMITER_FILES = "fp_lim_file";
    
    /**
     * The human-readable title for each list. The positions correspond to the
     * FpUtilsMedia ids, so e.g. TITLES[FpUtilsMedia.TYPE_SONG] = R.string.fp_library_section_songs
     */
    public static final int[] TITLES = {R.string.fp_library_section_artists, R.string.fp_library_section_albums, R.string.fp_library_section_songs,
        R.string.fp_library_section_playlists, R.string.fp_library_section_genres, R.string.fp_library_section_files};
    
    /**
     * The corresponding icons
     */
    public static final int[] ICONS = {R.drawable.fp_library_artist, R.drawable.fp_library_album, R.drawable.fp_library_songs,
    	R.drawable.fp_library_playlist, R.drawable.fp_library_genre, R.drawable.fp_library_folder};
    
    /**
     * Default tab order.
     */
    public static final int[] DEFAULT_ORDER = {
        FpUtilsMedia.TYPE_SONG,
        FpUtilsMedia.TYPE_FILE,
        FpUtilsMedia.TYPE_GENRE, 
        FpUtilsMedia.TYPE_ALBUM, 
        FpUtilsMedia.TYPE_ARTIST, 
        FpUtilsMedia.TYPE_PLAYLIST, 
    };
    
    /**
     * The user-chosen tab order.
     */
    int[] mTabOrder;
    /**
     * The number of visible tabs.
     */
    private int mTabCount;
    
    /**
     * The ListView for each adapter. Each index corresponds to that list's
     * FpUtilsMedia id.
     */
    private final ListView[] mLists = new ListView[MAX_ADAPTER_COUNT];
    
    /**
     * The adapters. Each index corresponds to that adapter's FpUtilsMedia id.
     */
    public AdapterLibrary[] mAdapters = new AdapterLibrary[MAX_ADAPTER_COUNT];
    
    /**
     * Whether the adapter corresponding to each index has stale data.
     */
    private final boolean[] mRequeryNeeded = new boolean[MAX_ADAPTER_COUNT];
    
    /**
     * The artist adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_ARTIST].
     */
    private AdapterMedia mArtistAdapter;
    
    /**
     * The album adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_ALBUM].
     */
    private AdapterMedia mAlbumAdapter;
    
    /**
     * The song adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_SONG].
     */
    private AdapterMedia mSongAdapter;
    
    /**
     * The playlist adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_PLAYLIST].
     */
    AdapterMedia mPlaylistAdapter;
    
    /**
     * The genre adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_GENRE].
     */
    private AdapterMedia mGenreAdapter;
    
    /**
     * The file adapter instance, also stored at
     * mAdapters[FpUtilsMedia.TYPE_FILE].
     */
    private AdapterFileSystem mFilesAdapter;
    
    /**
     * LRU cache holding the last scrolling position of all adapter views
     */
    private static AdaperPositionLruCache sLruAdapterPos;
    /**
     * The adapter of the currently visible list.
     */
    private AdapterLibrary mCurrentAdapter;
    /**
     * The index of the current page.
     */
    private int mCurrentPage;
    /**
     * A limiter that should be set when the album adapter is created.
     */
    private FpSerializableLimiter mPendingArtistLimiter;
    /**
     * A limiter that should be set when the album adapter is created.
     */
    private FpSerializableLimiter mPendingAlbumLimiter;
    /**
     * A limiter that should be set when the song adapter is created.
     */
    private FpSerializableLimiter mPendingSongLimiter;
    /**
     * A limiter that should be set when the files adapter is created.
     */
    private FpSerializableLimiter mPendingFileLimiter;
    /**
     * The ActivityLibrary that owns this adapter. The adapter will be notified
     * of changes in the current page.
     */
    private final ActivityLibrary mActivity;
    /**
     * A Handler running on the UI thread.
     */
    private final Handler mUiHandler;
    /**
     * A Handler running on a worker thread.
     */
    private final Handler mWorkerHandler;
    /**
     * The text to be displayed in the first row of the artist, album, and
     * song limiters.
     */
    private String mHeaderText;
    private LinearLayout mSongHeader;
    /**
     * The current filter text, or null if none.
     */
    private String mFilter;
    /**
     * The position of the songs page, or -1 if it is hidden.
     */
    public int mSongsPosition = -1;
    /**
     * The position of the albums page, or -1 if it is hidden.
     */
    public int mAlbumsPosition = -1;
    /**
     * The position of the artists page, or -1 if it is hidden.
     */
    public int mArtistsPosition = -1;
    /**
     * The position of the genres page, or -1 if it is hidden.
     */
    public int mGenresPosition = -1;

    private final ContentObserver mPlaylistObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            if (mPlaylistAdapter != null) {
                postRequestRequery(mPlaylistAdapter);
            }
        }
    };

    /**
     * Create the LibraryPager.
     *
     * @param activity The ActivityLibrary that will own this adapter. The
     * activity
     * will receive callbacks from the ListViews.
     * @param workerLooper A Looper running on a worker thread.
     */
    public AdapterPagerLibrary(ActivityLibrary activity, Looper workerLooper) {
        if (sLruAdapterPos == null) {
            sLruAdapterPos = new AdaperPositionLruCache(32);
        }
        mActivity = activity;
        mUiHandler = new Handler(this);
        mWorkerHandler = new Handler(workerLooper, this);
        mCurrentPage = -1;
        activity.getContentResolver().registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mPlaylistObserver);
    }

    /**
     * Load the tab order from SharedPreferences.
     *
     * @return True if order has changed.
     */
    public boolean loadTabOrder() {
        int[] order = DEFAULT_ORDER;
        int count = MAX_ADAPTER_COUNT;
        if (count != mTabCount || !Arrays.equals(order, mTabOrder)) {
            mTabOrder = order;
            mTabCount = count;
            notifyDataSetChanged();
            computeExpansions();
            return true;
        }
        return false;
    }

    /**
     * Determines whether adapters should be expandable from the visibility of
     * the adapters each expands to. Also updates
     * mSongsPosition/mAlbumsPositions.
     */
    public void computeExpansions() {
        int[] order = mTabOrder;
        int songsPosition = -1;
        int albumsPosition = -1;
        int artistsPosition = -1;
        int genresPosition = -1;
        for (int i = mTabCount; --i != -1;) {
            switch (order[i]) {
                case FpUtilsMedia.TYPE_ALBUM:
                    albumsPosition = i;
                    break;

                case FpUtilsMedia.TYPE_SONG:
                    songsPosition = i;
                    break;

                case FpUtilsMedia.TYPE_ARTIST:
                    artistsPosition = i;
                    break;

                case FpUtilsMedia.TYPE_GENRE:
                    genresPosition = i;
                    break;
            }
        }

        if (mArtistAdapter != null) {
            mArtistAdapter.setExpandable(songsPosition != -1 || albumsPosition != -1);
        }
        if (mAlbumAdapter != null) {
            mAlbumAdapter.setExpandable(songsPosition != -1);
        }
        if (mGenreAdapter != null) {
            mGenreAdapter.setExpandable(songsPosition != -1);
        }

        mSongsPosition = songsPosition;
        mAlbumsPosition = albumsPosition;
        mArtistsPosition = artistsPosition;
        mGenresPosition = genresPosition;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int type = mTabOrder[position];
        
        // Invalid position
        if (type >= mLists.length) {
            return null;
        }
        ListView view = mLists[type];

        if (view == null) {
            ActivityLibrary activity = mActivity;
            LayoutInflater inflater = activity.getLayoutInflater();
            AdapterLibrary adapter;
            LinearLayout header = null;

            switch (type) {
                case FpUtilsMedia.TYPE_ARTIST:
                    adapter = mArtistAdapter = new AdapterMedia(activity, FpUtilsMedia.TYPE_ARTIST, mPendingArtistLimiter, activity);
                    mArtistAdapter.setExpandable(mSongsPosition != -1 || mAlbumsPosition != -1);
                    break;
                    
                case FpUtilsMedia.TYPE_ALBUM:
                    adapter = mAlbumAdapter = new AdapterMedia(activity, FpUtilsMedia.TYPE_ALBUM, mPendingAlbumLimiter, activity);
                    mAlbumAdapter.setExpandable(mSongsPosition != -1);
                    mPendingAlbumLimiter = null;
                    break;
                    
                case FpUtilsMedia.TYPE_SONG:
                    adapter = mSongAdapter = new AdapterMedia(activity, FpUtilsMedia.TYPE_SONG, mPendingSongLimiter, activity);
                    mPendingSongLimiter = null;
                    mSongHeader = header = (LinearLayout) inflater.inflate(R.layout.fp_row_expandable, null);
                    break;
                    
                case FpUtilsMedia.TYPE_PLAYLIST:
                    adapter = mPlaylistAdapter = new AdapterMedia(activity, FpUtilsMedia.TYPE_PLAYLIST, null, activity);
                    break;
                    
                case FpUtilsMedia.TYPE_GENRE:
                    adapter = mGenreAdapter = new AdapterMedia(activity, FpUtilsMedia.TYPE_GENRE, null, activity);
                    mGenreAdapter.setExpandable(mSongsPosition != -1);
                    break;
                    
                case FpUtilsMedia.TYPE_FILE:
                    adapter = mFilesAdapter = new AdapterFileSystem(activity, mPendingFileLimiter);
                    mPendingFileLimiter = null;
                    break;
                    
                default:
                    throw new IllegalArgumentException("Invalid media type: " + type);
            }

            view = (ListView) inflater.inflate(R.layout.fp_activity_library_list, null);
            view.setOnCreateContextMenuListener(this);
            view.setOnItemClickListener(this);

            view.setTag(type);
            if (header != null) {
                // Prepare the text
                TextView headerText = (TextView) header.findViewById(R.id.fp_text);
                headerText.setText(mHeaderText);

                // Set the background
                headerText.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));

                // Center
                header.setPadding(0, 20, 0, 20);
                LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                headerText.setLayoutParams(params);
                headerText.setGravity(Gravity.CENTER);

                // Colorize
                headerText.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));

                // Tag
                header.setTag(new FpElementRowHolder());
                view.addHeaderView(header);
            }
            view.setAdapter(adapter);
            if (type != FpUtilsMedia.TYPE_FILE) {
                loadSortOrder((AdapterMedia) adapter);
            }

            adapter.setFilter(mFilter);

            mAdapters[type] = adapter;
            mLists[type] = view;
            mRequeryNeeded[type] = true;
        }

        requeryIfNeeded(type);
        container.addView(view);
        return view;
    }

    @Override
    public int getItemPosition(Object item) {
        int type = (Integer) ((ListView) item).getTag();
        int[] order = mTabOrder;
        for (int i = mTabCount; --i != -1;) {
            if (order[i] == type) {
                return i;
            }
        }
        return POSITION_NONE;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mActivity.getResources().getText(TITLES[mTabOrder[position]]);
    }
    
    public static Drawable getPageIcon(int position) {
    	return Theme.Resources.getDrawable(ICONS[DEFAULT_ORDER[position]]);
    }

    @Override
    public int getCount() {
        return mTabCount;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        int type = mTabOrder[position];
        if (type >= mAdapters.length) {
            return;
        }
        
        AdapterLibrary adapter = mAdapters[type];
        if (position != mCurrentPage || adapter != mCurrentAdapter) {
            requeryIfNeeded(type);
            mCurrentAdapter = adapter;
            mCurrentPage = position;
            mActivity.onPageChanged(position, adapter);
        }
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle in = (Bundle) state;
        mPendingArtistLimiter = (FpSerializableLimiter) in.getSerializable(AdapterPagerLibrary.LIMITER_ARTISTS);
        mPendingAlbumLimiter = (FpSerializableLimiter) in.getSerializable(AdapterPagerLibrary.LIMITER_ALBUMS);
        mPendingSongLimiter = (FpSerializableLimiter) in.getSerializable(AdapterPagerLibrary.LIMITER_SONGS);
        mPendingFileLimiter = (FpSerializableLimiter) in.getSerializable(AdapterPagerLibrary.LIMITER_FILES);
    }

    @Override
    public Parcelable saveState() {
        Bundle out = new Bundle(10);
        if (mArtistAdapter != null) {
            out.putSerializable(AdapterPagerLibrary.LIMITER_ARTISTS, mArtistAdapter.getLimiter());
        }
        if (mAlbumAdapter != null) {
            out.putSerializable(AdapterPagerLibrary.LIMITER_ALBUMS, mAlbumAdapter.getLimiter());
        }
        if (mSongAdapter != null) {
            out.putSerializable(AdapterPagerLibrary.LIMITER_SONGS, mSongAdapter.getLimiter());
        }
        if (mFilesAdapter != null) {
            out.putSerializable(AdapterPagerLibrary.LIMITER_FILES, mFilesAdapter.getLimiter());
        }

        maintainPosition();
        return out;
    }

    /**
     * Sets the text to be displayed in the first row of the artist, album, and
     * song lists.
     */
    public void setHeaderText(String text) {
        if (mSongHeader != null) {
            ((TextView) mSongHeader.findViewById(R.id.fp_text)).setText(text);
        }
        mHeaderText = text;
    }

    /**
     * Clear a limiter.
     *
     * @param type Which type of limiter to clear.
     */
    public void clearLimiter(int type) {
        maintainPosition();

        if (type == FpUtilsMedia.TYPE_FILE) {
            if (mFilesAdapter == null) {
                mPendingFileLimiter = null;
            } else {
                mFilesAdapter.setLimiter(null);
                requestRequery(mFilesAdapter);
            }
        } else {
            if (mArtistAdapter == null) {
                mPendingArtistLimiter = null;
            } else {
                mArtistAdapter.setLimiter(null);
                loadSortOrder(mArtistAdapter);
                requestRequery(mArtistAdapter);
            }
            if (mAlbumAdapter == null) {
                mPendingAlbumLimiter = null;
            } else {
                mAlbumAdapter.setLimiter(null);
                loadSortOrder(mAlbumAdapter);
                requestRequery(mAlbumAdapter);
            }
            if (mSongAdapter == null) {
                mPendingSongLimiter = null;
            } else {
                mSongAdapter.setLimiter(null);
                loadSortOrder(mSongAdapter);
                requestRequery(mSongAdapter);
            }
        }
    }

    /**
     * Update the adapters with the given limiter.
     *
     * @param limiter The limiter to set.
     *
     * @return The tab type that should be switched to to expand the row.
     */
    public int setLimiter(FpSerializableLimiter limiter) {
        int tab;

        maintainPosition();

        switch (limiter.type) {
            case FpUtilsMedia.TYPE_ALBUM:
                if (mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                } else {
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mSongsPosition;
                break;

            case FpUtilsMedia.TYPE_ARTIST:
                if (mAlbumAdapter == null) {
                    mPendingAlbumLimiter = limiter;
                } else {
                    mAlbumAdapter.setLimiter(limiter);
                    loadSortOrder(mAlbumAdapter);
                    requestRequery(mAlbumAdapter);
                }
                if (mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                } else {
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mSongsPosition;
                break;

            case FpUtilsMedia.TYPE_GENRE:
                if (mArtistAdapter == null) {
                    mPendingArtistLimiter = limiter;
                } else {
                    mArtistAdapter.setLimiter(limiter);
                    loadSortOrder(mArtistAdapter);
                    requestRequery(mArtistAdapter);
                }
                if (mAlbumAdapter == null) {
                    mPendingAlbumLimiter = limiter;
                } else {
                    mAlbumAdapter.setLimiter(limiter);
                    loadSortOrder(mAlbumAdapter);
                    requestRequery(mAlbumAdapter);
                }
                if (mSongAdapter == null) {
                    mPendingSongLimiter = limiter;
                } else {
                    mSongAdapter.setLimiter(limiter);
                    loadSortOrder(mSongAdapter);
                    requestRequery(mSongAdapter);
                }
                tab = mSongsPosition;
                break;

            case FpUtilsMedia.TYPE_FILE:
                if (mFilesAdapter == null) {
                    mPendingFileLimiter = limiter;
                } else {
                    mFilesAdapter.setLimiter(limiter);
                    requestRequery(mFilesAdapter);
                }
                tab = -1;
                break;

            default:
                throw new IllegalArgumentException("Invalid limiter type: " + limiter.type);
        }

        return tab;
    }

    /**
     * Saves the scrolling position of every visible limiter
     */
    private void maintainPosition() {
        for (int i = MAX_ADAPTER_COUNT; --i != -1;) {
            if (mAdapters[i] != null) {
                sLruAdapterPos.storePosition(mAdapters[i], mLists[i].getFirstVisiblePosition());
            }
        }
    }

    /**
     * Returns the limiter set on the current adapter or null if there is none.
     */
    public FpSerializableLimiter getCurrentLimiter() {
        AdapterLibrary current = mCurrentAdapter;
        if (current == null) {
            return null;
        }
        return current.getLimiter();
    }

    /**
     * Run on query on the adapter passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_RUN_QUERY = 0;
    /**
     * Save the sort mode for the adapter passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_SAVE_SORT = 1;
    /**
     * Call AdapterPagerLibrary#requestRequery(AdapterLibrary) on the adapter
     * passed in obj.
     *
     * Runs on worker thread.
     */
    private static final int MSG_REQUEST_REQUERY = 2;
    /**
     * Commit the cursor passed in obj to the adapter at the index passed in
     * arg1.
     *
     * Runs on UI thread.
     */
    private static final int MSG_COMMIT_QUERY = 3;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_RUN_QUERY: {
                AdapterLibrary adapter = (AdapterLibrary) message.obj;
                int index = adapter.getMediaType();
                Handler handler = mUiHandler;
                handler.sendMessage(handler.obtainMessage(MSG_COMMIT_QUERY, index, 0, adapter.query()));
                break;
            }
            case MSG_COMMIT_QUERY: {
                int index = message.arg1;
                mAdapters[index].commitQuery(message.obj);

                // Restore scrolling position if present and valid
                Integer curPos = sLruAdapterPos.popPosition(mAdapters[index]);
                if (curPos != null && curPos < mLists[index].getCount()) {
                    mLists[index].setSelection(curPos);
                }

                break;
            }
            case MSG_SAVE_SORT: {
                AdapterMedia adapter = (AdapterMedia) message.obj;
                SharedPreferences.Editor editor = PreferenceUtils.edit();
                editor.putInt(String.format(Locale.US, "sort_%d_%d", adapter.getMediaType(), adapter.getLimiterType()), adapter.getSortMode());
                editor.apply();
                break;
            }
            case MSG_REQUEST_REQUERY:
                requestRequery((AdapterLibrary) message.obj);
                break;
            default:
                return false;
        }

        return true;
    }

    /**
     * Requery the given adapter. If it is the current adapter, requery
     * immediately. Otherwise, mark the adapter as needing a requery and requery
     * when its tab is selected.
     *
     * Must be called on the UI thread.
     */
    public void requestRequery(AdapterLibrary adapter) {
        if (adapter == mCurrentAdapter) {
            postRunQuery(adapter);
        } else {
            mRequeryNeeded[adapter.getMediaType()] = true;
            // Clear the data for non-visible adapters (so we don't show the old
            // data briefly when we later switch to that adapter)
            adapter.clear();
        }
    }

    /**
     * Call AdapterPagerLibrary#requestRequery(AdapterLibrary) on the UI
     * thread.
     *
     * @param adapter The adapter, passed to requestRequery.
     */
    public void postRequestRequery(AdapterLibrary adapter) {
        Handler handler = mUiHandler;
        handler.sendMessage(handler.obtainMessage(MSG_REQUEST_REQUERY, adapter));
    }

    /**
     * Schedule a query to be run for the given adapter on the worker thread.
     *
     * @param adapter The adapter to run the query for.
     */
    private void postRunQuery(AdapterLibrary adapter) {
        mRequeryNeeded[adapter.getMediaType()] = false;
        Handler handler = mWorkerHandler;
        handler.removeMessages(MSG_RUN_QUERY, adapter);
        handler.sendMessage(handler.obtainMessage(MSG_RUN_QUERY, adapter));
    }

    /**
     * Requery the adapter of the given type if it exists and needs a requery.
     *
     * @param type One of FpUtilsMedia.TYPE_*
     */
    private void requeryIfNeeded(int type) {
        AdapterLibrary adapter = mAdapters[type];
        if (adapter != null && mRequeryNeeded[type]) {
            postRunQuery(adapter);
        }
    }

    /**
     * Invalidate the data for all adapters.
     */
    public void invalidateData() {
        for (AdapterLibrary adapter : mAdapters) {
            if (adapter != null) {
                postRequestRequery(adapter);
            }
        }
    }

    /**
     * Set the saved sort mode for the given adapter. The adapter should
     * be re-queried after calling this.
     *
     * @param adapter The adapter to load for.
     */
    @SuppressLint("DefaultLocale") 
    public void loadSortOrder(AdapterMedia adapter) {
        String key = String.format(Locale.US, "sort_%d_%d", adapter.getMediaType(), adapter.getLimiterType());
        int def = adapter.getDefaultSortMode();
        int sort = PreferenceUtils.getPreferences(this.mActivity.getApplicationContext()).getInt(key, def);
        adapter.setSortMode(sort);
    }

    /**
     * Set the sort mode for the current adapter. Current adapter must be a
     * AdapterMedia. Saves this sort mode to preferences and updates the list
     * associated with the adapter to display the new sort mode.
     *
     * @param mode The sort mode. See AdapterMedia#setSortMode(int) for
     * details.
     */
    public void setSortMode(int mode) {
        AdapterMedia adapter = (AdapterMedia) mCurrentAdapter;
        if (mode == adapter.getSortMode()) {
            return;
        }

        adapter.setSortMode(mode);
        requestRequery(adapter);

        Handler handler = mWorkerHandler;
        handler.sendMessage(handler.obtainMessage(MSG_SAVE_SORT, adapter));
    }

    /**
     * Set a new filter on all the adapters.
     */
    public void setFilter(String text) {
        if (text.length() == 0) {
            text = null;
        }

        mFilter = text;
        for (AdapterLibrary adapter : mAdapters) {
            if (adapter != null) {
                adapter.setFilter(text);
                requestRequery(adapter);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        // onPageSelected and setPrimaryItem are called in similar cases, and it
        // would be nice to use just one of them, but each has caveats:
        // - onPageSelected isn't called when the ViewPager is first
        //   initialized if there is no scrolling to do
        // - setPrimaryItem isn't called until scrolling is complete, which
        //   makes tab bar and limiter updates look bad
        // So we use both.
        setPrimaryItem(null, position, null);
    }

    /**
     * Creates the row data used by ActivityLibrary.
     */
    private static Intent createHeaderIntent(View header) {
        header = (View) header.getParent(); // tag is set on parent view of header
        int type = (Integer) header.getTag();
        Intent intent = new Intent();
        intent.putExtra(AdapterLibrary.DATA_ID, AdapterLibrary.HEADER_ID);
        intent.putExtra(AdapterLibrary.DATA_TYPE, type);
        return intent;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View targetView = info.targetView;
        Intent intent = info.id == -1 ? createHeaderIntent(targetView) : mCurrentAdapter.createData(targetView);
        mActivity.onCreateContextMenu(menu, intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int type = (Integer) parent.getTag();
        if (type == FpUtilsMedia.TYPE_FILE) {
            mFilesAdapter.onHandleRowClick(view);
        } else {
            Intent intent = id == -1 ? createHeaderIntent(view) : mCurrentAdapter.createData(view);
            mActivity.onItemClicked(intent);
        }
    }

    /**
     * LRU implementation: saves the adapter position
     */
    private class AdaperPositionLruCache extends LruCache<String, Integer> {

        public AdaperPositionLruCache(int size) {
            super(size);
        }

        public void storePosition(AdapterLibrary adapter, Integer val) {
            this.put(_k(adapter), val);
        }

        public Integer popPosition(AdapterLibrary adapter) {
            return this.remove(_k(adapter));
        }

        /**
         * Assemble internal cache key from adapter
         */
        private String _k(AdapterLibrary adapter) {
            String result = adapter.getMediaType() + "://";
            FpSerializableLimiter limiter = adapter.getLimiter();

            if (limiter != null) {
                for (String entry : limiter.names) {
                    result = result + entry + "/";
                }
            }
            return result;
        }

    }
}

/*EOF*/