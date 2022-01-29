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

public class CodecFileOgg extends CodecFile {

    private static final int OGG_PAGE_SIZE = 27;  // Static size of an OGG Page
    private static final int OGG_TYPE_IDENTIFICATION = 1;   // Identification header
    private static final int OGG_TYPE_COMMENT = 3;   // ID of 'VorbisComment's

    public CodecFileOgg() {
    }

    public HashMap getTags(RandomAccessFile s) throws IOException {
        long offset = 0;
        int retry = 64;
        boolean need_tags = true;
        boolean need_id = true;

        HashMap tags = new HashMap();
        HashMap identification = new HashMap();

        for (; retry > 0; retry--) {
            long res[] = parse_ogg_page(s, offset);
            if (res[2] == OGG_TYPE_IDENTIFICATION) {
                identification = parse_ogg_vorbis_identification(s, offset + res[0], res[1]);
                need_id = false;
            } else {
                if (res[2] == OGG_TYPE_COMMENT) {
                    tags = parse_ogg_vorbis_comment(s, offset + res[0], res[1]);
                    need_tags = false;
                }
            }
            offset += res[0] + res[1];
            if (need_tags == false && need_id == false) {
                break;
            }
        }

        // Calculate duration in seconds
        // Note that this calculation is WRONG: We would have to get the last
        // packet to calculate the real length - but this is goot enough.
        if (identification.containsKey("bitrate_nominal")) {
            int br_nom = (Integer) identification.get("bitrate_nominal") / 8;
            long file_length = s.length();
            if (file_length > 0 && br_nom > 0) {
                tags.put("duration", (int) (file_length / br_nom));
            }
        }

        return tags;
    }

    /*
     * Parses the ogg page at offset 'offset' and returns
     ** [header_size, payload_size, type]
     */
    protected long[] parse_ogg_page(RandomAccessFile s, long offset) throws IOException {
        long[] result = new long[3];               // [header_size, payload_size]
        byte[] p_header = new byte[OGG_PAGE_SIZE];   // buffer for the page header 
        byte[] scratch;
        int bread = 0;                         // number of bytes read
        int psize = 0;                         // payload-size
        int nsegs = 0;                         // Number of segments

        s.seek(offset);
        bread = s.read(p_header);
        if (bread != OGG_PAGE_SIZE) {
            xdie("cannot read header size");
        }
        if ((new String(p_header, 0, 5)).equals("OggS\0") != true) {
            xdie("not ogg");
        }

        nsegs = b2u(p_header[26]);
        // debug("> file seg: "+nsegs);
        if (nsegs > 0) {
            scratch = new byte[nsegs];
            bread = s.read(scratch);
            if (bread != nsegs) {
                xdie("read failed");
            }

            for (int i = 0; i < nsegs; i++) {
                psize += b2u(scratch[i]);
            }
        }

        // populate result array
        result[0] = (s.getFilePointer() - offset);
        result[1] = psize;
        result[2] = -1;

        /*
         * next byte is most likely the type -> pre-read
         */
        if (psize >= 1 && s.read(p_header, 0, 1) == 1) {
            result[2] = b2u(p_header[0]);
        }

        return result;
    }

    /*
     * In 'vorbiscomment' field is prefixed with \3vorbis in OGG files
     ** we check that this marker is present and call the generic comment
     ** parset with the correct offset (+7)
     */
    private HashMap parse_ogg_vorbis_comment(RandomAccessFile s, long offset, long pl_len) throws IOException {
        final int pfx_len = 7;
        byte[] pfx = new byte[pfx_len];

        if (pl_len < pfx_len) {
            xdie("ogg vorbis comment field");
        }

        s.seek(offset);
        s.read(pfx);

        if ((new String(pfx, 0, pfx_len)).equals("\3vorbis") == false) {
            xdie("invalid file");
        }

        return parse_vorbis_comment(s, offset + pfx_len, pl_len - pfx_len);
    }

    /*
     ** Returns a hashma with parsed vorbis identification header data
     *
     */
    private HashMap parse_ogg_vorbis_identification(RandomAccessFile s, long offset, long pl_len) throws IOException {
        /*
         * Structure:
         * 7 bytes of \1vorbis
         * 4 bytes version
         * 1 byte channels
         * 4 bytes sampling rate
         * 4 bytes bitrate max
         * 4 bytes bitrate nominal
         * 4 bytes bitrate min
         *
         */
        HashMap id_hash = new HashMap();
        byte[] buff = new byte[28];

        if (pl_len >= buff.length) {
            s.seek(offset);
            s.read(buff);
            id_hash.put("version", b2le32(buff, 7));
            id_hash.put("channels", b2u(buff[11]));
            id_hash.put("sampling_rate", b2le32(buff, 12));
            id_hash.put("bitrate_minimal", b2le32(buff, 16));
            id_hash.put("bitrate_nominal", b2le32(buff, 20));
            id_hash.put("bitrate_maximal", b2le32(buff, 24));
        }

        return id_hash;
    }

};

/*EOF*/