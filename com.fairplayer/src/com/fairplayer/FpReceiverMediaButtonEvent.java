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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Receives media button events and calls to FpServiceRendering to respond
 * appropriately.
 */
public class FpReceiverMediaButtonEvent extends BroadcastReceiver {

    /**
     * If another button event is received before this time in milliseconds
     * expires, the event with be considered a double click.
     */
    private static final int DOUBLE_CLICK_DELAY = 500;

    /**
     * Time of the last play/pause click. Used to detect double-clicks.
     */
    private static long sLastClickTime = 0;

    /**
     * Process a media button key press.
     *
     * @param context A context to use.
     * @param event The key press event.
     *
     * @return True if the event was handled and the broadcast should be
     * aborted.
     */
    public static boolean processKey(Context context, KeyEvent event) {
        if (event == null) {
            return false;
        }

        int action = event.getAction();
        String act = null;

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // single click: pause/resume.
                // double click: next track
                if (action == KeyEvent.ACTION_DOWN) {
                    long time = SystemClock.uptimeMillis();
                    if (time - sLastClickTime < DOUBLE_CLICK_DELAY) {
                        act = FpServiceRendering.ACTION_NEXT_SONG_AUTOPLAY;
                    } else {
                        act = FpServiceRendering.ACTION_TOGGLE_PLAYBACK;
                    }
                    sLastClickTime = time;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (action == KeyEvent.ACTION_DOWN) {
                    act = FpServiceRendering.ACTION_NEXT_SONG_AUTOPLAY;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (action == KeyEvent.ACTION_DOWN) {
                    act = FpServiceRendering.ACTION_PREVIOUS_SONG_AUTOPLAY;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (action == KeyEvent.ACTION_DOWN) {
                    act = FpServiceRendering.ACTION_PLAY;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (action == KeyEvent.ACTION_DOWN) {
                    act = FpServiceRendering.ACTION_PAUSE;
                }
                break;
            default:
                return false;
        }

        if (act != null) {
            Intent intent = new Intent(context, FpServiceRendering.class);
            intent.setAction(act);
            context.startService(intent);
        }

        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                boolean handled = processKey(context, event);
                if (handled && isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        } catch (Exception exc) {
            // IllegalStateException
        }
    }
}

/*EOF*/