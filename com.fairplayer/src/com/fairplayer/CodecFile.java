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
import java.util.Vector;

public class CodecFile {

    private static final long MAX_PKT_SIZE = 524288;

    public void xdie(String reason) throws IOException {
        throw new IOException(reason);
    }

    /*
     ** Returns a 32bit int from given byte offset in LE
     */
    public int b2le32(byte[] b, int off) {
        int r = 0;
        for (int i = 0; i < 4; i++) {
            r |= (b2u(b[off + i]) << (8 * i));
        }
        return r;
    }

    public int b2be32(byte[] b, int off) {
        return swap32(b2le32(b, off));
    }

    public int swap32(int i) {
        return ((i & 0xff) << 24) + ((i & 0xff00) << 8) + ((i & 0xff0000) >> 8) + ((i >> 24) & 0xff);
    }

    /*
     ** Returns a 16bit int from given byte offset in LE
     */
    public int b2le16(byte[] b, int off) {
        return (b2u(b[off]) | b2u(b[off + 1]) << 8);
    }

    /*
     ** convert 'byte' value into unsigned int
     */
    public int b2u(byte x) {
        return (x & 0xFF);
    }

    /*
     ** Printout debug message to STDOUT
     */
    public void debug(String s) {
        System.out.println("DBUG " + s);
    }

    public HashMap parse_vorbis_comment(RandomAccessFile s, long offset, long payload_len) throws IOException {
        HashMap tags = new HashMap();
        int comments = 0;                // number of found comments 
        int xoff = 0;                // offset within 'scratch'
        int can_read = (int) (payload_len > MAX_PKT_SIZE ? MAX_PKT_SIZE : payload_len);
        byte[] scratch = new byte[can_read];

        // seek to given position and slurp in the payload
        s.seek(offset);
        s.read(scratch);
        // skip vendor string in format: [LEN][VENDOR_STRING] 
        xoff += 4 + b2le32(scratch, xoff); // 4 = LEN = 32bit int 
        comments = b2le32(scratch, xoff);
        xoff += 4;

        for (int i = 0; i < comments; i++) {

            int clen = (int) b2le32(scratch, xoff);
            xoff += 4 + clen;

            if (xoff > scratch.length) {
                xdie("string out of bounds");
            }

            String tag_raw = new String(scratch, xoff - clen, clen);
            String[] tag_vec = tag_raw.split("=", 2);
            String tag_key = tag_vec[0].toUpperCase();

            addTagEntry(tags, tag_key, tag_vec[1]);
        }
        return tags;
    }

    public void addTagEntry(HashMap tags, String key, String value) {
        if (tags.containsKey(key)) {
            ((Vector) tags.get(key)).add(value); // just add to existing vector
        } else {
            Vector vx = new Vector();
            vx.add(value);
            tags.put(key, vx);
        }
    }

}

/*EOF*/