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

public class CodecFileId3v2 extends CodecFile {

    private static int ID3_ENC_LATIN = 0x00;
    private static int ID3_ENC_UTF16LE = 0x01;
    private static int ID3_ENC_UTF16BE = 0x02;
    private static int ID3_ENC_UTF8 = 0x03;

    public CodecFileId3v2() {
    }

    public HashMap getTags(RandomAccessFile s) throws IOException {
        HashMap tags = new HashMap();

        final int v2hdr_len = 10;
        byte[] v2hdr = new byte[v2hdr_len];

        // read the whole 10 byte header into memory
        s.seek(0);
        s.read(v2hdr);

        int v3minor = ((b2be32(v2hdr, 0))) & 0xFF;   // swapped ID3\04 -> ver. ist the first byte
        int v3len = ((b2be32(v2hdr, 6)));          // total size EXCLUDING the this 10 byte header
        v3len = unsyncsafe(v3len);

		// debug(">> tag version ID3v2."+v3minor);
        // debug(">> LEN= "+v3len+" // "+v3len);
        // we should already be at the first frame
        // so we can start the parsing right now
        tags = parse_v3_frames(s, v3len, v3minor);
        tags.put("_hdrlen", v3len + v2hdr_len);
        return tags;
    }

    /*
     ** converts syncsafe integer to Java integer
     */
    private int unsyncsafe(int x) {
        x = ((x & 0x7f000000) >> 3)
                | ((x & 0x007f0000) >> 2)
                | ((x & 0x00007f00) >> 1)
                | ((x & 0x0000007f) >> 0);
        return x;
    }

    /*
     * Parses all ID3v2 frames at the current position up until payload_len
     ** bytes were read
     */
    public HashMap parse_v3_frames(RandomAccessFile s, long payload_len, int v3minor) throws IOException {
        HashMap tags = new HashMap();
        byte[] frame = new byte[10]; // a frame header is always 10 bytes
        long bread = 0;            // total amount of read bytes

        while (bread < payload_len) {
            bread += s.read(frame);
            String framename = new String(frame, 0, 4);
            int rawlen = b2be32(frame, 4);
            // Encoders prior ID3v2.4 did not encode the frame length
            int slen = (v3minor >= 4 ? unsyncsafe(rawlen) : rawlen);

            /*
             * Abort on silly sizes
             */
            long bytesRemaining = payload_len - bread;
            if (slen < 1 || slen > (bytesRemaining)) {
                break;
            }

            byte[] xpl = new byte[slen];
            bread += s.read(xpl);

            if (framename.substring(0, 1).equals("T")) {
                String[] nmzInfo = normalizeTaginfo(framename, xpl);
                String oggKey = nmzInfo[0];
                String decPld = nmzInfo[1];

                if (oggKey.length() > 0 && !tags.containsKey(oggKey)) {
                    addTagEntry(tags, oggKey, decPld);
                }
            } else {
                if (framename.equals("RVA2")) {
                    //
                }
            }

        }
        return tags;
    }

    /*
     * Converts ID3v2 sillyframes to OggNames
     */
    private String[] normalizeTaginfo(String k, byte[] v) {
        String[] rv = new String[] {"", ""};
        HashMap lu = new HashMap<String, String>();
        lu.put("TIT2", "TITLE");
        lu.put("TALB", "ALBUM");
        lu.put("TPE1", "ARTIST");

        if (lu.containsKey(k)) {
            /*
             * A normal, known key: translate into Ogg-Frame name
             */
            rv[0] = (String) lu.get(k);
            rv[1] = getDecodedString(v);
        } else {
            if (k.equals("TXXX")) {
                /*
                 * A freestyle field, ieks!
                 */
                String txData[] = getDecodedString(v).split(Character.toString('\0'), 2);
                /*
                 * Other values
                 */
                if (txData.length == 2) {
                    rv[0] = txData[0].toUpperCase();
                    rv[1] = txData[1];
                }
            }
        }

        return rv;
    }

    /*
     * Converts a raw byte-stream text into a java String
     */
    private String getDecodedString(byte[] raw) {
        int encid = raw[0] & 0xFF;
        int len = raw.length;
        String v = "";
        try {
            if (encid == ID3_ENC_LATIN) {
                v = new String(raw, 1, len - 1, "ISO-8859-1");
            } else {
                if (encid == ID3_ENC_UTF8) {
                    v = new String(raw, 1, len - 1, "UTF-8");
                } else {
                    if (encid == ID3_ENC_UTF16LE) {
                        v = new String(raw, 3, len - 3, "UTF-16LE");
                    } else {
                        if (encid == ID3_ENC_UTF16BE) {
                            v = new String(raw, 3, len - 3, "UTF-16BE");
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return v;
    }

}

/*EOF*/