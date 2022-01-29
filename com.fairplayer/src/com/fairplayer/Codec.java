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

import com.fairplayer.CodecFileOgg;
import com.fairplayer.CodecFileFlac;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.HashMap;

public class Codec {

    public Codec() {
    }

    public HashMap getTags(String fname) {
        HashMap tags = new HashMap();
        try {
            RandomAccessFile ra = new RandomAccessFile(fname, "r");
            tags = getTags(ra);
            ra.close();
        } catch (Exception e) {
            /*
             * we dont' care much: SOMETHING went wrong. d'oh!
             */
        }

        return tags;
    }

    public HashMap getTags(RandomAccessFile s) {
        HashMap tags = new HashMap();
        byte[] file_ff = new byte[4];

        try {
            s.read(file_ff);
            String magic = new String(file_ff);
            if (magic.equals("fLaC")) {
                tags = (new CodecFileFlac()).getTags(s);
                tags.put("type", "FLAC");
            } else {
                if (magic.equals("OggS")) {
                    // This may be an Opus OR an Ogg Vorbis file
                    tags = (new CodecFileOpus()).getTags(s);
                    if (tags.size() > 0) {
                        tags.put("type", "OPUS");
                    } else {
                        tags = (new CodecFileOgg()).getTags(s);
                        tags.put("type", "OGG");
                    }
                } else {
                    if (file_ff[0] == -1 && file_ff[1] == -5) { /*
                         * aka 0xfffb in real languages
                         */

                        tags = (new CodecHeader()).getTags(s);
                        tags.put("type", "MP3/Lame");
                    } else {
                        if (magic.substring(0, 3).equals("ID3")) {
                            tags = (new CodecFileId3v2()).getTags(s);
                            if (tags.containsKey("_hdrlen")) {
                                Long hlen = Long.parseLong(tags.get("_hdrlen").toString(), 10);
                                HashMap lameInfo = (new CodecHeader()).parseCodecHeader(s, hlen);
                                /*
                                 * add tags from lame header if not already
                                 * present
                                 */
                                inheritTag("duration", lameInfo, tags);
                            }
                            tags.put("type", "MP3/ID3v2");
                        }
                    }
                }
            }

        } catch (IOException e) {
        }
        return tags;
    }

    private void inheritTag(String key, HashMap from, HashMap to) {
        if (!to.containsKey(key) && from.containsKey(key)) {
            to.put(key, from.get(key));
        }
    }
}

/*EOF*/