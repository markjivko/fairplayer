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
import android.view.View;
import android.widget.ListAdapter;

/**
 * Provides support for limiters and a few other methods ActivityLibrary uses
 * for its adapters.
 */
public interface AdapterLibrary extends ListAdapter {

    /**
     * Return the type of media represented by this adapter. One of
     * FpUtilsMedia.TYPE_*.
     */
    int getMediaType();

    /**
     * Set the limiter for the adapter.
     *
     * A limiter is intended to restrict displayed media to only those that are
     * children of a given parent media item.
     *
     * @param limiter The limiter, created by
     * AdapterLibrary#buildLimiter(long).
     */
    void setLimiter(FpSerializableLimiter limiter);

    /**
     * Returns the limiter currently active on this adapter or null if none are
     * active.
     */
    FpSerializableLimiter getLimiter();

    /**
     * Builds a limiter based off of the media represented by the given row.
     *
     * @param id The id of the row.
     *
     * @see AdapterLibrary#getLimiter()
     * @see AdapterLibrary#setLimiter(FpSerializableLimiter)
     */
    FpSerializableLimiter buildLimiter(long id);

    /**
     * Set a new filter.
     *
     * The data should be requeried after calling this.
     *
     * @param filter The terms to filter on, separated by spaces. Only
     * media that contain all of the terms (in any order) will be displayed
     * after filtering is complete.
     */
    void setFilter(String filter);

    /**
     * Retrieve the data for this adapter. The data must be set with
     * AdapterLibrary#commitQuery(Object) before it takes effect.
     *
     * This should be called on a worker thread.
     *
     * @return The data. Contents depend on the sub-class.
     */
    Object query();

    /**
     * Update the adapter with the given data.
     *
     * Must be called on the UI thread.
     *
     * @param data Data from AdapterLibrary#query().
     */
    void commitQuery(Object data);

    /**
     * Clear the data for this adapter.
     *
     * Must be called on the UI thread.
     */
    void clear();

    /**
     * Creates the row data used by ActivityLibrary.
     */
    Intent createData(View row);

    /**
     * Special id for #DATA_ID: the row represented is a header view.
     */
    long HEADER_ID = -1;
    
    /**
     * Special id for #DATA_ID: invalid id.
     */
    long INVALID_ID = -2;
    
    /**
     * Extra for row data: media id. type: long.
     */
    String DATA_ID = "id";
    
    /**
     * Extra for row data: media title. type: String.
     */
    String DATA_TITLE = "title";
    
    /**
     * Extra for row data: media type. type: int. One of FpUtilsMedia.TYPE_*.
     */
    String DATA_TYPE = "type";
    
    /**
     * Extra for row data: canonical file path. type: String. Only present if
     * type is FpUtilsMedia#TYPE_FILE.
     */
    String DATA_FILE = "file";
    
    /**
     * Extra for row data: if true, row has expander arrow. type: boolean.
     */
    String DATA_EXPANDABLE = "expandable";
    
    /**
     * Extra for playlist
     */
    String DATA_PLAYLIST = "playlist";
    
    /**
     * Extra position
     */
    String DATA_PLAYLIST_NAME = "playlistName";
    
    /**
     * Extra position
     */
    String DATA_POSITION = "position";
    
    /**
     * Extra position
     */
    String DATA_AUDIO_ID = "audioId";
}

/*EOF*/