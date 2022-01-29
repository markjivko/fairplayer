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

import java.io.Serializable;

/**
 * FpSerializableLimiter is a constraint for AdapterMedia and AdapterFileSystem used when
 * a row is "expanded".
 */
public class FpSerializableLimiter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The type of the limiter. One of FpUtilsMedia.TYPE_ARTIST, TYPE_ALBUM,
     * TYPE_GENRE, or TYPE_FILE.
     */
    public final int type;
    /**
     * Each element will be given a separate view each representing a higher
     * different limiters. The first element is the broadest limiter, the last
     * the most specific. For example, an album limiter would look like:
     * { "Some Artist", "Some Album" }
     * Or a file limiter:
     * { "sdcard", "Music", "folder" }
     */
    public final String[] names;
    /**
     * The data for the limiter. This varies according to the type of the
     * limiter.
     */
    public final Object data;

    /**
     * Create a limiter with the given data. All parameters initialize their
     * corresponding fields in the class.
     */
    public FpSerializableLimiter(int type, String[] names, Object data) {
        this.type = type;
        this.names = names;
        this.data = data;
    }
}

/*EOF*/