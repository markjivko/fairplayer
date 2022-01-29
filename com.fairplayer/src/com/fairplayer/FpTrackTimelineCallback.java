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

public interface FpTrackTimelineCallback {

    /**
     * Called when the song timeline position/size has changed
     */
    void onPositionInfoChanged();

    /**
     * The library contents changed and should be invalidated
     */
    void onMediaChange();

    /**
     * Notification about a change in the timeline
     */
    void onTimelineChanged();

    /**
     * Updates song at 'delta'
     */
    void replaceSong(int delta, FpTrack song);

    /**
     * Sets the currently active song
     */
    void setSong(long uptime, FpTrack song);

    /**
     * Sets the current playback state
     */
    void setState(long uptime, int state);

    /**
     * The view/activity should re-create itself due to a theme change
     */
    void recreate();
}