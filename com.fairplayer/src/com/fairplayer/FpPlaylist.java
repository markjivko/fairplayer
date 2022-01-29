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

import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Provides various playlist-related utility functions.
 */
public class FpPlaylist {

    /**
     * Queries all the playlists known to the MediaStore.
     *
     * @param resolver A ContentResolver to use.
     *
     * @return The queried cursor.
     */
    public static Cursor queryPlaylists(ContentResolver resolver) {
        Uri media = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME};
        String sort = MediaStore.Audio.Playlists.NAME;
        return FpUtilsMedia.queryResolver(resolver, media, projection, null, null, sort);
    }

    /**
     * Retrieves the id for a playlist with the given name.
     *
     * @param resolver A ContentResolver to use.
     * @param name The name of the playlist.
     *
     * @return The id of the playlist, or -1 if there is no playlist with the
     * given name.
     */
    public static long getPlaylist(ContentResolver resolver, String name) {
        long id = -1;

        Cursor cursor = FpUtilsMedia.queryResolver(resolver, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Playlists._ID},
                MediaStore.Audio.Playlists.NAME + "=?",
                new String[] {name}, null);

        if (cursor != null) {
            if (cursor.moveToNext()) {
                id = cursor.getLong(0);
            }
            cursor.close();
        }

        return id;
    }

    /**
     * Create a new playlist with the given name. If a playlist with the given
     * name already exists, it will be overwritten.
     *
     * @param resolver A ContentResolver to use.
     * @param name The name of the playlist.
     *
     * @return The id of the new playlist.
     */
    public static long createPlaylist(ContentResolver resolver, String name) {
        long id = getPlaylist(resolver, name);

        if (id == -1) {
            // We need to create a new playlist.
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Audio.Playlists.NAME, name);
            Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
            /*
             * Creating the playlist may fail due to race conditions or silly
             * android bugs (i am looking at you, kitkat!). In this case, id
             * will stay -1
             */
            if (uri != null) {
                id = Long.parseLong(uri.getLastPathSegment());
            }
        } else {
            // We are overwriting an existing playlist. Clear existing songs.
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);
            resolver.delete(uri, null, null);
        }

        return id;
    }

    /**
     * Run the given query and add the results to the given playlist. Should be
     * run on a background thread.
     *
     * @param resolver A ContentResolver to use.
     * @param playlistId The MediaStore.Audio.Playlist id of the playlist to
     * modify.
     * @param query The query to run. The audio id should be the first column.
     *
     * @return The number of songs that were added to the playlist.
     */
    public static int addToPlaylist(ContentResolver resolver, long playlistId, FpUtilsMedia.QueryTask query) {
        ArrayList<Long> result = new ArrayList<Long>();
        Cursor cursor = query.runQuery(resolver);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(0));
            }
        }
        return addToPlaylist(resolver, playlistId, result);
    }

    /**
     * Adds a set of audioIds to the given playlist. Should be
     * run on a background thread.
     *
     * @param resolver A ContentResolver to use.
     * @param playlistId The MediaStore.Audio.Playlist id of the playlist to
     * modify.
     * @param audioIds An ArrayList with all IDs to add
     *
     * @return The number of songs that were added to the playlist.
     */
    public static int addToPlaylist(ContentResolver resolver, long playlistId, ArrayList<Long> audioIds) {
        if (playlistId == -1) {
            return 0;
        }

        // Find the greatest PLAY_ORDER in the playlist
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        String[] projection = new String[] {MediaStore.Audio.Playlists.Members.PLAY_ORDER};
        Cursor cursor = FpUtilsMedia.queryResolver(resolver, uri, projection, null, null, null);
        int base = 0;
        if (cursor.moveToLast()) {
            base = cursor.getInt(0) + 1;
        }
        cursor.close();

        int count = audioIds.size();
        if (count > 0) {
            ContentValues[] values = new ContentValues[count];
            for (int i = 0; i != count; ++i) {
                ContentValues value = new ContentValues(2);
                value.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + i));
                value.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioIds.get(i));
                values[i] = value;
            }
            resolver.bulkInsert(uri, values);
        }

        return count;
    }

    /**
     * Removes a set of audioIds from the given playlist. Should be
     * run on a background thread.
     *
     * @param resolver A ContentResolver to use.
     * @param playlistId The MediaStore.Audio.Playlist id of the playlist to
     * modify.
     * @param audioIds An ArrayList with all IDs to add
     *
     * @return The number of songs that were added to the playlist.
     */
    public static int removeFromPlaylist(ContentResolver resolver, long playlistId, ArrayList<Long> audioIds) {
        if (playlistId == -1) {
            return 0;
        }

        int count = 0;
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        for (long id : audioIds) {
            String where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + id;
            count += resolver.delete(uri, where, null);
        }
        return count;
    }

    /**
     * Delete the playlist with the given id.
     *
     * @param resolver A ContentResolver to use.
     * @param id The Media.Audio.Playlists id of the playlist.
     */
    public static void deletePlaylist(ContentResolver resolver, long id) {
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id);
        resolver.delete(uri, null, null);
    }

    /**
     * Copy content from one playlist to another
     *
     * @param resolver A ContentResolver to use.
     * @param sourceId The Media.Audio.Playlists id of the source playlist
     * @param destinationId The Media.Audio.Playlists id of the destination
     * playlist
     */
    private static void _copyToPlaylist(ContentResolver resolver, long sourceId, long destinationId) {
        FpUtilsMedia.QueryTask query = FpUtilsMedia.buildPlaylistQuery(sourceId, FpTrack.FILLED_PLAYLIST_PROJECTION, null);
        addToPlaylist(resolver, destinationId, query);
    }

    /**
     * Rename the playlist with the given id.
     *
     * @param resolver A ContentResolver to use.
     * @param id The Media.Audio.Playlists id of the playlist.
     * @param newName The new name for the playlist.
     */
    public static void renamePlaylist(ContentResolver resolver, long id, String newName) {
        long newId = createPlaylist(resolver, newName);
        if (newId != -1) { // new playlist created -> move stuff over
            _copyToPlaylist(resolver, id, newId);
            deletePlaylist(resolver, id);
        }
    }

    /**
     * Returns the ID of the 'favorites' playlist.
     *
     * @param context The Context to use
     * @param create Create the playlist if it does not exist
     *
     * @return the id of the playlist, -1 on error
     */
    public static long getFavoritesId(Context context, boolean create) {
        String playlistName = context.getString(R.string.fp_playlist_favorites);
        long playlistId = getPlaylist(context.getContentResolver(), playlistName);

        if (playlistId == -1 && create == true) {
            playlistId = createPlaylist(context.getContentResolver(), playlistName);
        }

        return playlistId;
    }

    /**
     * Searches for given song in given playlist
     *
     * @param resolver A ContentResolver to use.
     * @param playlistId The ID of the Playlist to query
     * @param song The FpTrack to search in given playlistId
     *
     * @return true if `song' was found in `playlistId'
     */
    public static boolean isInPlaylist(ContentResolver resolver, long playlistId, FpTrack song) {
        if (playlistId == -1 || song == null) {
            return false;
        }

        boolean found = false;
        String where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.id;
        FpUtilsMedia.QueryTask query = FpUtilsMedia.buildPlaylistQuery(playlistId, FpTrack.EMPTY_PLAYLIST_PROJECTION, where);
        Cursor cursor = query.runQuery(resolver);
        if (cursor != null) {
            found = cursor.getCount() != 0;
            cursor.close();
        }
        return found;
    }
}