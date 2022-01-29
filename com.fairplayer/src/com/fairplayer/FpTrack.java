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

import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

/**
 * Represents a FpTrack backed by the MediaStore. Includes basic metadata and
 * utilities to retrieve songs from the MediaStore.
 */
public class FpTrack implements Comparable<FpTrack> {

    /**
     * Indicates that this song was randomly selected from all songs.
     */
    public static final int FLAG_RANDOM = 0x1;
    /**
     * If set, this song has no cover art. If not set, this song may or may not
     * have cover art.
     */
    public static final int FLAG_NO_COVER = 0x2;
    /**
     * The number of flags.
     */
    public static final int FLAG_COUNT = 2;

    public static final String[] EMPTY_PROJECTION = {
        MediaStore.MediaColumns._ID,};

    public static final String[] FILLED_PROJECTION = {
        MediaStore.MediaColumns._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,};

    public static final String[] EMPTY_PLAYLIST_PROJECTION = {
        MediaStore.Audio.Playlists.Members.AUDIO_ID,};

    public static final String[] FILLED_PLAYLIST_PROJECTION = {
        MediaStore.Audio.Playlists.Members.AUDIO_ID,
        MediaStore.Audio.Playlists.Members.DATA,
        MediaStore.Audio.Playlists.Members.TITLE,
        MediaStore.Audio.Playlists.Members.ALBUM,
        MediaStore.Audio.Playlists.Members.ARTIST,
        MediaStore.Audio.Playlists.Members.ALBUM_ID,
        MediaStore.Audio.Playlists.Members.ARTIST_ID,
        MediaStore.Audio.Playlists.Members.DURATION,
        MediaStore.Audio.Playlists.Members.TRACK,};

    /**
     * The cache instance.
     */
    private static FpCoverStore sCoverCache = null;

    /**
     * Id of this song in the MediaStore
     */
    public long id;
    /**
     * Id of this song's album in the MediaStore
     */
    public long albumId;
    /**
     * Id of this song's artist in the MediaStore
     */
    public long artistId;

    /**
     * Path to the data for this song
     */
    public String path;

    /**
     * FpTrack title
     */
    public String title;
    /**
     * Album name
     */
    public String album;
    /**
     * Artist name
     */
    public String artist;

    /**
     * Length of the song in milliseconds.
     */
    public long duration;
    /**
     * The position of the song in its album.
     */
    public int trackNumber;

    /**
     * FpTrack flags. Currently #FLAG_RANDOM or #FLAG_NO_COVER.
     */
    public int flags;

    /**
     * Initialize the song with the specified id. Call populate to fill fields
     * in the song.
     */
    public FpTrack(long id) {
        this.id = id;
    }

    /**
     * Initialize the song with the specified id and flags. Call populate to
     * fill fields in the song.
     */
    public FpTrack(long id, int flags) {
        this.id = id;
        this.flags = flags;
    }

    /**
     * Return true if this song was retrieved from randomSong().
     */
    public boolean isRandom() {
        return (flags & FLAG_RANDOM) != 0;
    }

    /**
     * Returns true if the song is filled
     */
    public boolean isFilled() {
        return (id != -1 && path != null);
    }

    /**
     * Populate fields with data from the supplied cursor.
     *
     * @param cursor Cursor queried with FILLED_PROJECTION projection
     */
    public void populate(Cursor cursor) {
        id = cursor.getLong(0);
        path = cursor.getString(1);
        title = cursor.getString(2);
        album = cursor.getString(3);
        artist = cursor.getString(4);
        albumId = cursor.getLong(5);
        artistId = cursor.getLong(6);
        duration = cursor.getLong(7);
        trackNumber = cursor.getInt(8);
    }

    /**
     * Get the id of the given song.
     *
     * @param song The FpTrack to get the id from.
     *
     * @return The id, or 0 if the given song is null.
     */
    public static long getId(FpTrack song) {
        if (song == null) {
            return 0;
        }
        return song.id;
    }

    /**
     * Query the large album art for this song.
     *
     * @param context A context to use.
     *
     * @return The album art or null if no album art could be found
     */
    public Bitmap getCover(Context context) {
        return getCoverInternal(context, FpCoverStore.SIZE_LARGE);
    }

    /**
     * Query the small album art for this song.
     *
     * @param context A context to use.
     *
     * @return The album art or null if no album art could be found
     */
    public Bitmap getSmallCover(Context context) {
        return getCoverInternal(context, FpCoverStore.SIZE_SMALL);
    }

    /**
     * Internal implementation of getCover
     *
     * @param context A context to use.
     * @param size The desired cover size
     *
     * @return The album art or null if no album art could be found
     */
    private Bitmap getCoverInternal(Context context, int size) {
        if (FpCoverStore.mCoverLoadMode == 0 || id == -1 || (flags & FLAG_NO_COVER) != 0) {
            return null;
        }

        if (sCoverCache == null) {
            sCoverCache = new FpCoverStore(context.getApplicationContext());
        }

        Bitmap cover = sCoverCache.getCoverFromSong(this, size);

        if (cover == null) {
            flags |= FLAG_NO_COVER;
        }
        return cover;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%d %d %s", id, albumId, path);
    }

    /**
     * Compares the album ids of the two songs; if equal, compares track order.
     */
    @Override
    public int compareTo(FpTrack other) {
        if (albumId == other.albumId) {
            return trackNumber - other.trackNumber;
        }
        if (albumId > other.albumId) {
            return 1;
        }
        return -1;
    }
}