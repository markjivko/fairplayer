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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.lang.StringBuilder;

/**
 * AdapterMedia provides an adapter backed by a MediaStore content provider.
 * It generates simple one- or two-line text views to display each media
 * element.
 *
 * Filtering is supported, as is a more specific type of filtering referred to
 * as limiting. Limiting is separate from filtering; a new filter will not
 * erase an active filter. Limiting is intended to allow only media belonging
 * to a specific group to be displayed, e.g. only songs from a certain artist.
 * See getLimiter and setLimiter for details.
 */
@SuppressLint("DefaultLocale") 
public class AdapterMedia extends BaseAdapter implements AdapterLibrary, View.OnClickListener, SectionIndexer {

    private static final Pattern SPACE_SPLIT = Pattern.compile("\\s+");

    private static final String SORT_MAGIC_PLAYCOUNT = "__PLAYCOUNT_SORT";

    /**
     * The string to use for length==0 db fields
     */
    private static final String DB_NULLSTRING_FALLBACK = "???";
    /**
     * A context to use.
     */
    private final Context mContext;
    /**
     * The library activity to use.
     */
    private final ActivityLibrary mActivity;
    /**
     * A LayoutInflater to use.
     */
    private final LayoutInflater mInflater;
    /**
     * The current data.
     */
    private Cursor mCursor;
    /**
     * The type of media represented by this adapter. Must be one of the
     * FpUtilsMedia.FIELD_* constants. Determines which content provider to query
     * for
     * media and what fields to display.
     */
    private final int mType;
    /**
     * The URI of the content provider backing this adapter.
     */
    private Uri mStore;
    /**
     * The fields to use from the content provider. The last field will be
     * displayed in the MediaView, as will the first field if there are
     * multiple fields. Other fields will be used for searching.
     */
    private String[] mFields;
    /**
     * The collation keys corresponding to each field. If provided, these are
     * used to speed up sorting and filtering.
     */
    private String[] mFieldKeys;
    /**
     * The columns to query from the content provider.
     */
    private String[] mProjection;
    /**
     * A limiter is used for filtering. The intention is to restrict items
     * displayed in the list to only those of a specific artist or album, as
     * selected through an expander arrow in a broader AdapterMedia list.
     */
    private FpSerializableLimiter mLimiter;
    /**
     * The constraint used for filtering, set by the search box.
     */
    private String mConstraint;
    /**
     * The sort order for use with buildSongQuery().
     */
    private String mSongSort;
    /**
     * The human-readable descriptions for each sort mode.
     */
    private int[] mSortEntries;
    /**
     * An array ORDER BY expressions for each sort mode. %1$s is replaced by
     * ASC or DESC as appropriate before being passed to the query.
     */
    private String[] mSortValues;
    /**
     * The index of the current of the current sort mode in mSortValues, or
     * the inverse of the index (in which case sort should be descending
     * instead of ascending).
     */
    private int mSortMode;
    /**
     * If true, show the expander button on each row.
     */
    private boolean mExpandable;
    /**
     * Defines the media type to use for this entry
     * Setting this to FpUtilsMedia.TYPE_INVALID disables cover artwork
     */
    private int mCoverCacheType;
    /**
     * Alphabet to be used for SectionIndexer. Populated in #buildAlphabet().
     */
    private List<SectionIndex> mAlphabet = new ArrayList<SectionIndex>(512);

    /**
     * Construct a AdapterMedia representing the given <code>type</code> of
     * media.
     *
     * @param context The Context used to access the content model.
     * @param type The type of media to represent. Must be one of the
     * FpTrack.TYPE_* constants. This determines which content provider to query
     * and what fields to display in the views.
     * @param limiter An initial limiter to use
     * @param activity The ActivityLibrary that will contain this adapter - may
     * be null
     *
     */
    public AdapterMedia(Context context, int type, FpSerializableLimiter limiter, ActivityLibrary activity) {
        mContext = context;
        mActivity = activity;
        mType = type;
        mLimiter = limiter;

        if (mActivity != null) {
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } else {
            mInflater = null; // not running inside an activity
        }

        // Use media type + base id as cache key combination
        mCoverCacheType = mType;
        String coverCacheKey = BaseColumns._ID;

        switch (type) {
            case FpUtilsMedia.TYPE_ARTIST:
                mStore = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Artists.ARTIST};
                mFieldKeys = new String[] {MediaStore.Audio.Artists.ARTIST_KEY};
                mSongSort = FpUtilsMedia.getDefaultSort();
                mSortEntries = new int[] {R.string.fp_media_sort_name, R.string.fp_media_sort_num_of_tracks};
                mSortValues = new String[] {MediaStore.Audio.Media.ARTIST_KEY + " %1$s", MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS + " %1$s," + MediaStore.Audio.Media.ARTIST_KEY + " %1$s"};
                break;
                
            case FpUtilsMedia.TYPE_ALBUM:
                mStore = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST};
                // Why is there no artist_key column constant in the album MediaStore? The column does seem to exist.
                mFieldKeys = new String[] {MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Media.ARTIST_KEY};
                mSongSort = FpUtilsMedia.getAlbumSort();
                mSortEntries = new int[] {R.string.fp_media_sort_name, R.string.fp_media_sort_artist_album, R.string.fp_media_sort_year, R.string.fp_media_sort_num_of_tracks, R.string.fp_media_sort_date_added};
                mSortValues = new String[] {MediaStore.Audio.Media.ALBUM_KEY + " %1$s", MediaStore.Audio.Media.ARTIST_KEY + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s", MediaStore.Audio.AlbumColumns.FIRST_YEAR + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s", MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s", MediaStore.MediaColumns._ID + " %1$s"};
                break;
                
            case FpUtilsMedia.TYPE_SONG:
                mStore = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST};
                mFieldKeys = new String[] {MediaStore.Audio.Media.TITLE_KEY, MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.ARTIST_KEY};
                mSortEntries = new int[] {R.string.fp_media_sort_name, R.string.fp_media_sort_artist_album_track, R.string.fp_media_sort_artist_album_title,
                    R.string.fp_media_sort_artist_year, R.string.fp_media_sort_album_track,
                    R.string.fp_media_sort_year, R.string.fp_media_sort_date_added, R.string.fp_media_sort_song_playcount};
                mSortValues = new String[] {MediaStore.Audio.Media.TITLE_KEY + " %1$s", MediaStore.Audio.Media.ARTIST_KEY + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s," + MediaStore.Audio.AudioColumns.TRACK, MediaStore.Audio.Media.ARTIST_KEY + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s," + MediaStore.Audio.Media.TITLE_KEY + " %1$s",
                    MediaStore.Audio.Media.ARTIST_KEY + " %1$s," + MediaStore.Audio.Media.YEAR + " %1$s," + MediaStore.Audio.Media.ALBUM_KEY + " %1$s, " + MediaStore.Audio.AudioColumns.TRACK, MediaStore.Audio.Media.ALBUM_KEY + " %1$s," + MediaStore.Audio.AudioColumns.TRACK,
                    MediaStore.Audio.Media.YEAR + " %1$s," + MediaStore.Audio.Media.TITLE_KEY + " %1$s", MediaStore.MediaColumns._ID + " %1$s", SORT_MAGIC_PLAYCOUNT};
                // Songs covers are cached per-album
                mCoverCacheType = FpUtilsMedia.TYPE_ALBUM;
                coverCacheKey = MediaStore.Audio.Albums.ALBUM_ID;
                break;
                
            case FpUtilsMedia.TYPE_PLAYLIST:
                mStore = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Playlists.NAME};
                mFieldKeys = null;
                mSortEntries = new int[] {R.string.fp_media_sort_name, R.string.fp_media_sort_date_added};
                mSortValues = new String[] {MediaStore.Audio.GenresColumns.NAME + " %1$s", MediaStore.Audio.Media.DATE_ADDED + " %1$s"};
                mExpandable = true;
                break;
                
            case FpUtilsMedia.TYPE_GENRE:
                mStore = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
                mFields = new String[] {MediaStore.Audio.Genres.NAME};
                mFieldKeys = null;
                mSortEntries = new int[] {R.string.fp_media_sort_name};
                mSortValues = new String[] {MediaStore.Audio.GenresColumns.NAME + " %1$s"};
                break;
                
            default:
                throw new IllegalArgumentException("Invalid value for type: " + type);
        }

        mProjection = new String[mFields.length + 2];
        mProjection[0] = BaseColumns._ID;
        mProjection[1] = coverCacheKey;
        for (int i = 0; i < mFields.length; i++) {
            mProjection[i + 2] = mFields[i];
        }
    }

    /**
     * Returns first sort column for this adapter. Ensure #mSortMode is
     * correctly set
     * prior to calling this.
     *
     * @return string representing sort column to be used in projection.
     * If the column is binary, returns its human-readable counterpart instead.
     */
    private String getFirstSortColumn() {
        int mode = mSortMode < 0 ? ~mSortMode : mSortMode; // get current sort mode
        String column = SPACE_SPLIT.split(mSortValues[mode])[0];
        if (column.endsWith("_key")) { // we want human-readable string, not machine-composed
            column = column.substring(0, column.length() - 4);
        }

        return column;
    }

    /**
     * Set whether or not the expander button should be shown in each row.
     * Defaults to true for playlist adapter and false for all others.
     *
     * @param expandable True to show expander, false to hide.
     */
    public void setExpandable(boolean expandable) {
        if (expandable != mExpandable) {
            mExpandable = expandable;
            notifyDataSetChanged();
        }
    }

    @Override
    public void setFilter(String filter) {
        mConstraint = filter;
    }

    /**
     * Build the query to be run with runQuery().
     *
     * @param projection The columns to query.
     * @param forceMusicCheck Force the is_music check to be added to the
     * selection.
     */
    private FpUtilsMedia.QueryTask buildQuery(String[] projection, boolean returnSongs) {
        String constraint = mConstraint;
        FpSerializableLimiter limiter = mLimiter;

        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = null;

        int mode = mSortMode;
        String sortDir;
        if (mode < 0) {
            mode = ~mode;
            sortDir = "DESC";
        } else {
            sortDir = "ASC";
        }

        String sortStringRaw = mSortValues[mode];

        String[] enrichedProjection;
        // Magic sort mode: sort by listen_count
        if (sortStringRaw == SORT_MAGIC_PLAYCOUNT) {
            // special case, no explicit column to sort
            enrichedProjection = projection;

            ArrayList<Long> topSongs = (new FpPlayCounter(mContext)).getTopSongs(4096);
            int sortWeight = -1 * topSongs.size(); // Sort mode is actually reversed (default: mostplayed -> leastplayed)

            StringBuilder sb = new StringBuilder("CASE WHEN " + MediaStore.MediaColumns._ID + "=0 THEN 0"); // include dummy statement in initial string -> topSongs may be empty
            for (Long id : topSongs) {
                sb.append(" WHEN " + MediaStore.MediaColumns._ID + "=" + id + " THEN " + sortWeight);
                sortWeight++;
            }
            sb.append(" ELSE 0 END %1s");
            sortStringRaw = sb.toString();
        } else {
            // enrich projection with sort column to build alphabet later
            enrichedProjection = Arrays.copyOf(projection, projection.length + 1);
            enrichedProjection[projection.length] = getFirstSortColumn();

            if (returnSongs && mType != FpUtilsMedia.TYPE_SONG) {
                // We are in a non-song adapter but requested to return songs - sorting
                // can only be done by using the adapters default sort mode :-(
                sortStringRaw = mSongSort;
            }
        }

        String sort = String.format(sortStringRaw, sortDir);

        if (returnSongs || mType == FpUtilsMedia.TYPE_SONG) {
            selection.append(MediaStore.Audio.Media.IS_MUSIC + " AND length(" + MediaStore.MediaColumns.DATA + ")");
        }

        if (constraint != null && constraint.length() != 0) {
            String[] needles;
            String[] keySource;

            // If we are using sorting keys, we need to change our constraint
            // into a list of collation keys. Otherwise, just split the
            // constraint with no modification.
            if (mFieldKeys != null) {
                String colKey = MediaStore.Audio.keyFor(constraint);
                String spaceColKey = DatabaseUtils.getCollationKey(" ");
                needles = colKey.split(spaceColKey);
                keySource = mFieldKeys;
            } else {
                needles = SPACE_SPLIT.split(constraint);
                keySource = mFields;
            }

            int size = needles.length;
            selectionArgs = new String[size];

            StringBuilder keys = new StringBuilder(20);
            keys.append(keySource[0]);
            for (int j = 1; j != keySource.length; ++j) {
                keys.append("||");
                keys.append(keySource[j]);
            }

            for (int j = 0; j != needles.length; ++j) {
                selectionArgs[j] = '%' + needles[j] + '%';

                // If we have something in the selection args (i.e. j > 0), we
                // must have something in the selection, so we can skip the more
                // costly direct check of the selection length.
                if (j != 0 || selection.length() != 0) {
                    selection.append(" AND ");
                }
                selection.append(keys);
                selection.append(" LIKE ?");
            }
        }

        FpUtilsMedia.QueryTask query;
        if (mType == FpUtilsMedia.TYPE_GENRE && !returnSongs) {
            query = FpUtilsMedia.buildGenreExcludeEmptyQuery(enrichedProjection, selection.toString(),
                    selectionArgs, sort);
        } else {
            if (limiter != null && limiter.type == FpUtilsMedia.TYPE_GENRE) {
                // Genre is not standard metadata for MediaStore.Audio.Media.
                // We have to query it through a separate provider. : /
                query = FpUtilsMedia.buildGenreQuery((Long) limiter.data, enrichedProjection, selection.toString(), selectionArgs, sort, mType, returnSongs);
            } else {
                if (limiter != null) {
                    if (selection.length() != 0) {
                        selection.append(" AND ");
                    }
                    selection.append(limiter.data);
                }
                query = new FpUtilsMedia.QueryTask(mStore, enrichedProjection, selection.toString(), selectionArgs, sort);
                if (returnSongs) // force query on song provider as we are requested to return songs
                {
                    query.uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
            }
        }
        return query;
    }

    @Override
    public Cursor query() {
        return buildQuery(mProjection, false).runQuery(mContext.getContentResolver());
    }

    @Override
    public void commitQuery(Object data) {
        changeCursor((Cursor) data);
    }

    /**
     * Build a query for all the songs represented by this adapter, for adding
     * to the timeline.
     *
     * @param projection The columns to query.
     */
    public FpUtilsMedia.QueryTask buildSongQuery(String[] projection) {
        FpUtilsMedia.QueryTask query = buildQuery(projection, true);
        query.type = mType;
        return query;
    }

    @Override
    public void clear() {
        changeCursor(null);
    }

    @Override
    public int getMediaType() {
        return mType;
    }

    @Override
    public void setLimiter(FpSerializableLimiter limiter) {
        mLimiter = limiter;
    }

    @Override
    public FpSerializableLimiter getLimiter() {
        return mLimiter;
    }

    @Override
    public FpSerializableLimiter buildLimiter(long id) {
        String[] fields;
        Object data;

        Cursor cursor = mCursor;
        if (cursor == null) {
            return null;
        }
        for (int i = 0, count = cursor.getCount(); i != count; ++i) {
            cursor.moveToPosition(i);
            if (cursor.getLong(0) == id) {
                break;
            }
        }

        switch (mType) {
            case FpUtilsMedia.TYPE_ARTIST:
                fields = new String[] {cursor.getString(2)};
                data = String.format(Locale.US, "%s=%d", MediaStore.Audio.Media.ARTIST_ID, id);
                break;
                
            case FpUtilsMedia.TYPE_ALBUM:
                fields = new String[] {cursor.getString(3), cursor.getString(2)};
                data = String.format(Locale.US, "%s=%d", MediaStore.Audio.Media.ALBUM_ID, id);
                break;
                
            case FpUtilsMedia.TYPE_GENRE:
                fields = new String[] {cursor.getString(2)};
                data = id;
                break;
                
            default:
                throw new IllegalStateException("Invalid media type: " + mType);
        }

        return new FpSerializableLimiter(mType, fields, data);
    }

    /**
     * Set a new cursor for this adapter. The old cursor will be closed.
     *
     * @param cursor The new cursor.
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = mCursor;
        mCursor = cursor;
        buildAlphabet();
        if (cursor == null) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }
        if (old != null) {
            old.close();
        }
    }

    @SuppressLint("InflateParams") 
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        FpElementRowHolder holder;

        if (view == null) {
            // We must create a new view if we're not given a recycle view or
            // if the recycle view has the wrong layout.

            view = mInflater.inflate(R.layout.fp_row_expandable, null);
            holder = new FpElementRowHolder();
            view.setTag(holder);

            holder.text = (TextView) view.findViewById(R.id.fp_text);

            // Set the text color
            holder.text.setTextColor(Theme.Resources.getColor(R.color.fp_color_row_title));
            
            // Get the divider
            holder.arrow = (ImageView) view.findViewById(R.id.fp_more);
            holder.arrow.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_row_more));
            holder.cover = (ElementSmallCover) view.findViewById(R.id.fp_cover);
            holder.arrow.setOnClickListener(this);

            holder.arrow.setVisibility(mExpandable ? View.VISIBLE : View.GONE);
            holder.cover.setVisibility(mCoverCacheType != FpUtilsMedia.TYPE_INVALID ? View.VISIBLE : View.GONE);
        } else {
            holder = (FpElementRowHolder) view.getTag();
        }

        Cursor cursor = mCursor;
        cursor.moveToPosition(position);
        holder.id = cursor.getLong(0);
        long cacheId = cursor.getLong(1);
        if (mProjection.length >= 5) {
            String line1 = cursor.getString(2);
            String line2 = cursor.getString(3);
            line1 = (line1 == null ? DB_NULLSTRING_FALLBACK : line1);
            line2 = (line2 == null ? DB_NULLSTRING_FALLBACK : line2 + ", " + cursor.getString(4));

            SpannableStringBuilder sb = new SpannableStringBuilder(line1);
            sb.append('\n');
            sb.append(line2);
            sb.setSpan(new ForegroundColorSpan(Theme.Resources.getColor(R.color.fp_color_row_subtitle)), line1.length() + 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.text.setText(sb);
            holder.text.setTextColor(Theme.Resources.getColor(R.color.fp_color_row_title));
            holder.title = line1;
        } else {
            String title = cursor.getString(2);
            if (title == null) {
                title = DB_NULLSTRING_FALLBACK;
            }
            holder.text.setText(title);
            holder.title = title;
        }

        holder.cover.setCover(mCoverCacheType, cacheId);

        return view;
    }

    /**
     * Returns the type of the current limiter.
     *
     * @return One of FpUtilsMedia.TYPE_, or FpUtilsMedia.TYPE_INVALID if there is
     * no limiter set.
     */
    public int getLimiterType() {
        FpSerializableLimiter limiter = mLimiter;
        if (limiter != null) {
            return limiter.type;
        }
        return FpUtilsMedia.TYPE_INVALID;
    }

    /**
     * Return the available sort modes for this adapter.
     *
     * @return An array containing the resource ids of the sort mode strings.
     */
    public int[] getSortEntries() {
        return mSortEntries;
    }

    /**
     * Set the sorting mode. The adapter should be re-queried after changing
     * this.
     *
     * @param i The index of the sort mode in the sort entries array. If this
     * is negative, the inverse of the index will be used and sort order will
     * be reversed.
     */
    public void setSortMode(int i) {
        mSortMode = i;
    }

    /**
     * Returns the sort mode that should be used if no preference is saved. This
     * may very based on the active limiter.
     */
    public int getDefaultSortMode() {
        int type = mType;
        if (type == FpUtilsMedia.TYPE_ALBUM || type == FpUtilsMedia.TYPE_SONG) {
            return 1; // aritst,album,track
        }
        return 0;
    }

    /**
     * Return the current sort mode set on this adapter.
     */
    public int getSortMode() {
        return mSortMode;
    }

    /**
     * Creates an intent to dispatch
     */
    @Override
    public Intent createData(View view) {
        FpElementRowHolder holder = (FpElementRowHolder) view.getTag();
        Intent intent = new Intent();
        intent.putExtra(AdapterLibrary.DATA_TYPE, mType);
        intent.putExtra(AdapterLibrary.DATA_ID, holder.id);
        intent.putExtra(AdapterLibrary.DATA_TITLE, holder.title);
        intent.putExtra(AdapterLibrary.DATA_EXPANDABLE, mExpandable);
        return intent;
    }

    /**
     * Callback of array clicks (item clicks are handled in AdapterPagerLibrary)
     */
    @Override
    public void onClick(View view) {
        view = (View) view.getParent(); // get view of linear layout, not the click consumer
        Intent intent = createData(view);
        mActivity.onItemExpanded(intent);
    }

    @Override
    public int getCount() {
        Cursor cursor = mCursor;
        if (cursor == null) {
            return 0;
        }
        return cursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor = mCursor;
        if (cursor == null || cursor.getCount() == 0) {
            return 0;
        }
        cursor.moveToPosition(position);
        return cursor.getLong(0);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Helper class for building an alphabet.
     * Contains a hint that is shown in fast scroll thumb popup and a position
     * where this hint
     * has appeared first.
     */
    private class SectionIndex {

        public SectionIndex(Object hint, int position) {
            this.hint = hint;
            this.position = position;
        }

        private Object hint;
        private int position;

        @Override
        public String toString() {
            return String.valueOf(hint);
        }
    }

    /**
     * Build alphabet for fast-scroller. Detects automatically whether we're
     * sorting
     * on string-type (e.g. title or album) or integer type (e.g. year).
     *
     * <p/>
     * Alphabet building is only performed if applicable, i.e. magic listen_count
     * sort
     * or sort by date added will yield no results as the section hints would
     * not be
     * human-readable.
     *
     * <p/>
     * Note: This clears alphabet in case current cursor is invalid.
     */
    private void buildAlphabet() {
        mAlphabet.clear();

        Cursor cursor = mCursor;
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }

        String columnName = getFirstSortColumn();
        int sortColumnIndex = cursor.getColumnIndex(columnName);
        if (sortColumnIndex <= 0) {
            // either projection doesn't contain this column
            // or the column is _id (e.g. sort by date added),
            // no point in building
            return;
        }

        cursor.moveToFirst();
        String lastString = null;
        Object lastKnown = null;
        Object next;
        do {
            if (cursor.isNull(sortColumnIndex)) {
                continue;
            }

            int type = cursor.getType(sortColumnIndex);
            switch (type) {
                case Cursor.FIELD_TYPE_INTEGER:
                    next = cursor.getInt(sortColumnIndex);
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    lastString = cursor.getString(sortColumnIndex);
                    lastString = lastString.trim().toUpperCase(); // normalize

                    // This is what AOSP's MediaStore.java:1337 does during indexing
                    if (lastString.startsWith("THE ")) {
                        lastString = lastString.substring(4);
                    }

                    if (lastString.startsWith("AN ")) {
                        lastString = lastString.substring(3);
                    }

                    if (lastString.startsWith("A ")) {
                        lastString = lastString.substring(2);
                    }

                    // Ensure that we got at least one char
                    if (lastString.length() < 1) {
                        lastString = DB_NULLSTRING_FALLBACK;
                    }

                    next = lastString.charAt(0);
                    break;
                default:
                    continue;
            }
            if (!next.equals(lastKnown)) { // new char
                mAlphabet.add(new SectionIndex(next, cursor.getPosition()));
                lastKnown = next;
            }
        } while (cursor.moveToNext());
    }

    @Override
    public Object[] getSections() {
        return mAlphabet.toArray(new SectionIndex[mAlphabet.size()]);
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        // clip to start
        if (sectionIndex < 0) {
            return 0;
        }

        // clip to end
        if (sectionIndex >= mAlphabet.size()) {
            return mCursor.getCount() - 1;
        }

        return mAlphabet.get(sectionIndex).position;
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < mAlphabet.size(); ++i) {
            if (mAlphabet.get(i).position > position) {
                return i - 1;
            }
        }
        return 0;
    }
}

/*EOF*/