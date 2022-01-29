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

public class CodecHeader extends CodecFile {

    // Sampling rate version -> field mapping
    private static int[][] sampleRates = {
        {11025, 12000, 8000}, // MPEG2.5 (idx = 0)
        {0, 0, 0}, // reserved (idx = 1)
        {22050, 24000, 16000}, // MPEG2 (idx = 2)
        {44100, 48000, 32000}, // MPEG1 (idx = 3)
    };

    // SamplesPerFrame layer -> version mapping
    private static int[][] samplesPerFrame = {
        // reserved, layer3, layer2, layer1
        {0, 576, 1152, 384}, // MPEG2.5
        {0, 0, 0, 0}, // RESERVED
        {0, 576, 1152, 384}, // MPEG2
        {0, 1152, 1152, 384}, // MPEG1
    };

    public CodecHeader() {
    }

    public HashMap getTags(RandomAccessFile s) throws IOException {
        return parseCodecHeader(s, 0);
    }

    public HashMap parseCodecHeader(RandomAccessFile s, long offset) throws IOException {
        HashMap tags = new HashMap();
        byte[] chunk = new byte[12];

        s.seek(offset + 0x24);
        s.read(chunk);

        String lameMark = new String(chunk, 0, 4, "ISO-8859-1");
        int flags = b2u(chunk[7]);

        if ((flags & 0x01) != 0) { // header indicates that totalFrames field is present
            int total_frames = b2be32(chunk, 8);
            s.seek(offset);
            s.read(chunk);

            int mpeg_hdr = b2be32(chunk, 0);
            int srate_idx = (mpeg_hdr >> 10) & 3; // sampling rate index at bit 10-11
            int layer_idx = (mpeg_hdr >> 17) & 3; // layer index value bit 17-18
            int ver_idx = (mpeg_hdr >> 19) & 3; // version index value bit 19-20

            // Try to calculate song duration if all indexes are sane
            if (ver_idx < sampleRates.length && srate_idx < sampleRates[ver_idx].length && layer_idx < samplesPerFrame[ver_idx].length) {
                int sample_rate = sampleRates[ver_idx][srate_idx];
                int sample_pfr = samplesPerFrame[ver_idx][layer_idx];
                if (sample_rate > 0 && sample_pfr > 0) {
                    double duration = ((double) sample_pfr / (double) sample_rate) * total_frames;
                    tags.put("duration", (int) duration);
                }
            }

        }

        if (lameMark.equals("Info") || lameMark.equals("Xing")) {
            s.seek(offset + 0xAB);
            s.read(chunk);
        }

        return tags;
    }

}

/*EOF*/