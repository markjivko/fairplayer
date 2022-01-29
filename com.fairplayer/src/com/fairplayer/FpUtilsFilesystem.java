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

import java.io.File;
import java.net.URI;
import java.net.URLConnection;
import java.net.URISyntaxException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * Provides some static File-related utility functions.
 */
public class FpUtilsFilesystem {

    /**
     * Checks if dispatching this intent to an external application makes sense
     *
     * @param intent The intent to examine
     *
     * @return bool true if the intent could be dispatched
     */
    public static boolean canDispatchIntent(Intent intent) {
        boolean canDispatch = false;

        int type = intent.getIntExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_INVALID);
        boolean isFolder = intent.getBooleanExtra(AdapterLibrary.DATA_EXPANDABLE, false);
        String path = intent.getStringExtra(AdapterLibrary.DATA_FILE);
        if (type == FpUtilsMedia.TYPE_FILE && isFolder == false) {
            try {
                URI uri = new URI("file", path, null);
                String mimeGuess = URLConnection.guessContentTypeFromName(uri.toString());
                if (mimeGuess != null && mimeGuess.matches("^(image|text)/.+")) {
                    canDispatch = true;
                }
            } catch (URISyntaxException e) {
            }
        }
        return canDispatch;
    }

    /**
     * Opens an intent in an external application
     *
     * @param activity The library activity to use
     * @param intent The intent to examine and launch
     *
     * @return bool true if the intent was dispatched
     */
    public static boolean dispatchIntent(ActivityLibrary activity, Intent intent) {
        boolean handled = true;

        String path = intent.getStringExtra(AdapterLibrary.DATA_FILE);
        String mimeGuess = URLConnection.guessContentTypeFromName(path);
        File file = new File(path);
        Uri uri = Uri.fromFile(file);

        Intent extView = new Intent(Intent.ACTION_VIEW);
        extView.setDataAndType(uri, mimeGuess);
        try {
            activity.startActivity(extView);
        } catch (Exception ActivityNotFoundException) {
            handled = false;
        }
        return handled;
    }

    /**
     * Called by FileSystem adapter to get the start folder
     * for browsing directories
     */
    public static File getFilesystemBrowseStart(Context context) {
        SharedPreferences prefs = PreferenceUtils.getPreferences(context);
        String folder = prefs.getString(Constants.Keys.SETTINGS_ROOT, Constants.Defaults.SETTINGS_ROOT);
        return new File(folder.equals("") ? Environment.getExternalStorageDirectory().getAbsolutePath() : folder);
    }
}