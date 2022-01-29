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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class CodecFileOpus extends CodecFileOgg {

    public CodecFileOpus() {
    }

    public HashMap getTags(RandomAccessFile s) throws IOException {

        // The opus specification is very strict: The first packet MUST
        // contain the OpusHeader while the 2nd MUST contain the
        // OggHeader payload: https://wiki.xiph.org/OggOpus
        long pos = 0;
        long offsets[] = parse_ogg_page(s, pos);

        HashMap tags = new HashMap();
        HashMap opus_head = parse_opus_head(s, pos + offsets[0], offsets[1]);
        pos += offsets[0] + offsets[1];

        // Check if we parsed a version number and ensure it doesn't have any
        // of the upper 4 bits set (eg: <= 15)
        if (opus_head.containsKey("version") && (Integer) opus_head.get("version") <= 0xF) {
            // Get next page: The spec requires this to be an OpusTags head
            offsets = parse_ogg_page(s, pos);
            tags = parse_opus_vorbis_comment(s, pos + offsets[0], offsets[1]);
            
            // Include the gain value found in the opus header
            int header_gain = (Integer) opus_head.get("header_gain");
            addTagEntry(tags, "R128_BASE_GAIN", "" + header_gain);
        }

        return tags;
    }

    /**
     * Attempts to parse an OpusHead block at given offset.
     * Returns an hash-map, will be empty on failure
     */
    private HashMap parse_opus_head(RandomAccessFile s, long offset, long pl_len) throws IOException {
        /*
         * Structure:
         * 8 bytes of 'OpusHead'
         * 1 byte version
         * 1 byte channel count
         * 2 bytes pre skip
         * 4 bytes input-sample-rate
         * 2 bytes outputGain as Q7.8
         * 1 byte channel map
         * --> 19 bytes
         */

        HashMap id_hash = new HashMap();
        byte[] buff = new byte[19];
        if (pl_len >= buff.length) {
            s.seek(offset);
            s.read(buff);
            if ((new String(buff, 0, 8)).equals("OpusHead")) {
                id_hash.put("version", b2u(buff[8]));
                id_hash.put("channels", b2u(buff[9]));
                id_hash.put("pre_skip", b2le16(buff, 10));
                id_hash.put("sampling_rate", b2le32(buff, 12));
                id_hash.put("header_gain", (int) ((short) b2le16(buff, 16)));
                id_hash.put("channel_map", b2u(buff[18]));
            }
        }

        return id_hash;
    }

    /**
     * Parses an OpusTags section
     * Returns a hash map of the found tags
     */
    private HashMap parse_opus_vorbis_comment(RandomAccessFile s, long offset, long pl_len) throws IOException {
        final int magic_len = 8; // OpusTags
        byte[] magic = new byte[magic_len];

        if (pl_len < magic_len) {
            xdie("invalid field");
        }

        // Read and check magic signature
        s.seek(offset);
        s.read(magic);

        if ((new String(magic, 0, magic_len)).equals("OpusTags") == false) {
            xdie("invalid file");
        }

        return parse_vorbis_comment(s, offset + magic_len, pl_len - magic_len);
    }

}

/*EOF*/