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
import java.util.Enumeration;

public class CodecFileFlac extends CodecFile {

    private static final int FLAC_TYPE_STREAMINFO = 0; // Basic stream info
    private static final int FLAC_TYPE_COMMENT = 4;   // ID of 'VorbisComment's

    public CodecFileFlac() {
    }

    public HashMap getTags(RandomAccessFile s) throws IOException {
        int xoff = 4;  // skip file magic
        int retry = 64;
        int r[];
        boolean need_infos = true;
        boolean need_tags = true;
        HashMap infos = new HashMap();
        HashMap tags = new HashMap();

        for (; retry > 0; retry--) {
            r = parse_metadata_block(s, xoff);
            if (r[2] == FLAC_TYPE_STREAMINFO) {
                infos = parse_streaminfo_block(s, xoff + r[0], r[1]);
                need_infos = false;
            }
            if (r[2] == FLAC_TYPE_COMMENT) {
                tags = parse_vorbis_comment(s, xoff + r[0], r[1]);
                need_tags = false;
            }

            if (r[3] != 0 || (need_tags == false && need_infos == false)) {
                break; // eof reached
            }
            // else: calculate next offset
            xoff += r[0] + r[1];
        }

        // Copy duration to final hashmap if found in infoblock
        if (infos.containsKey("duration")) {
            tags.put("duration", infos.get("duration"));
        }

        return tags;
    }

    /*
     * Parses the metadata block at 'offset' and returns
     ** [header_size, payload_size, type, stop_after]
     */
    private int[] parse_metadata_block(RandomAccessFile s, long offset) throws IOException {
        int[] result = new int[4];
        byte[] mb_head = new byte[4];
        int stop_after = 0;
        int block_type = 0;
        int block_size = 0;

        s.seek(offset);

        if (s.read(mb_head) != 4) {
            xdie("block header error");
        }

        block_size = b2be32(mb_head, 0);                         // read whole header as 32 big endian
        block_type = (block_size >> 24) & 127;                  // BIT 1-7 are the type
        stop_after = (((block_size >> 24) & 128) > 0 ? 1 : 0); // BIT 0 indicates the last-block flag
        block_size = (block_size & 0x00FFFFFF);                 // byte 1-7 are the size

        // debug("size="+block_size+", type="+block_type+", is_last="+stop_after);
        result[0] = 4; // hardcoded - only returned to be consistent with OGG parser
        result[1] = block_size;
        result[2] = block_type;
        result[3] = stop_after;

        return result;
    }

    /*
     ** Returns a hashma with parsed vorbis identification header data
     *
     */
    private HashMap parse_streaminfo_block(RandomAccessFile s, long offset, long pl_len) throws IOException {
        HashMap id_hash = new HashMap();
        byte[] buff = new byte[18];

        if (pl_len >= buff.length) {
            s.seek(offset);
            s.read(buff);
            id_hash.put("blocksize_minimal", (b2be32(buff, 0) >> 16));
            id_hash.put("blocksize_maximal", (b2be32(buff, 0) & 0x0000FFFF));
            id_hash.put("framesize_minimal", (b2be32(buff, 4) >> 8));
            id_hash.put("framesize_maximal", (b2be32(buff, 7) >> 8));
            id_hash.put("sampling_rate", (b2be32(buff, 10) >> 12));
            id_hash.put("channels", ((b2be32(buff, 10) >> 9) & 7) + 1); // 3 bits
            id_hash.put("num_samples", b2be32(buff, 14)); // fixme: this is actually 36 bit: the 4 hi bits are discarded due to java
            if ((Integer) id_hash.get("sampling_rate") > 0) {
                int duration = (Integer) id_hash.get("num_samples") / (Integer) id_hash.get("sampling_rate");
                id_hash.put("duration", (int) duration);
            }
        }
        return id_hash;
    }

}

/*EOF*/