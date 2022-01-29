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

import android.content.SharedPreferences;

/**
 * Various actions that can be passed to FpServiceRendering#performAction(FpAction,
 * ActivityCommon).
 */
enum FpAction {

    /**
     * Dummy action: do nothing.
     */
    Nothing,
    /**
     * Open the library activity.
     */
    Library,
    /**
     * If playing music, pause. Otherwise, start playing.
     */
    PlayPause,
    /**
     * Skip to the next song.
     */
    NextSong,
    /**
     * Go back to the previous song.
     */
    PreviousSong,
    /**
     * Skip to the first song from the next album.
     */
    NextAlbum,
    /**
     * Skip to the last song from the previous album.
     */
    PreviousAlbum,
    /**
     * Cycle the repeat mode.
     */
    Repeat,
    /**
     * Cycle the shuffle mode.
     */
    Shuffle,
    /**
     * Enqueue the rest of the current album.
     */
    EnqueueAlbum,
    /**
     * Enqueue the rest of the songs by the current artist.
     */
    EnqueueArtist,
    /**
     * Enqueue the rest of the songs in the current genre.
     */
    EnqueueGenre,
    /**
     * Clear the queue of all remaining songs.
     */
    ClearQueue,
    /**
     * Displays the queue
     */
    ShowQueue,
    /**
     * Seek 10 seconds forward
     */
    SeekForward,
    /**
     * Seek 10 seconds back
     */
    SeekBackward;

    /**
     * Retrieve an action from the given SharedPreferences.
     *
     * @param prefs The SharedPreferences instance to load from.
     * @param key The preference key to load.
     * @param def The value to return if the key is not found or cannot be
     * loaded.
     *
     * @return The loaded action or def if no action could be loaded.
     */
    public static FpAction getAction(SharedPreferences prefs, String key, FpAction def) {
        try {
            String pref = prefs.getString(key, null);
            if (pref == null) {
                return def;
            }
            return Enum.valueOf(FpAction.class, pref);
        } catch (Exception e) {
            return def;
        }
    }
}