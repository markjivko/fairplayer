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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class UpdateChecker {

    /**
     * UpdateChecker instance
     */
    protected static UpdateChecker _instance = null;

    /**
     * Current context
     */
    protected Context _mContext = null;

    /**
     * Hidden constructor
     */
    protected UpdateChecker(Context context) {
        this._mContext = context;
    }

    /**
     * UpdateChecker
     */
    public static UpdateChecker getInstance(Context context) {
        if(null == _instance) {
            _instance = new UpdateChecker(context);
        }
        return _instance;
    }

    /**
     * Check whether an update is available
     */
    public void check(boolean sendNotification) {
        // On a different thread
        new Thread(new Runnable() {
            public void run() {
                // Get the preferences
                SharedPreferences preferences = PreferenceUtils.getPreferences(_mContext);

                // Get the current time
                long currentTime = (long) System.currentTimeMillis() / 1000;

                // Get the last check time
                long lastCheckTime = preferences.getLong(Constants.Keys.SETTINGS_UPDATE_CHECKER_LAST_CHECK, Constants.Defaults.SETTINGS_UPDATE_CHECKER_LAST_CHECK);

                // Less than 1 hour
                if(currentTime - lastCheckTime <= 3600) {
                    return;
                }

                // Prepare the page URL
                String dataUrl = "https://play.google.com/store/apps/details?hl=en&id=" + _mContext.getPackageName();

                // Prepare the result
                String result = "";

                try {
                    // Create a URL for the desired page
                    URL url = new URL(dataUrl);

                    // Read all the text returned by the server
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                    // Get the string
                    String line = null;
                    while(true) {
                        // Read the line
                        line = in.readLine();

                        // Failed to read
                        if(null == line) {
                            break;
                        }

                        // Add the line
                        result = result + line;
                    }

                    // Close the stream
                    in.close();
                } catch(Exception exc) {
                }

                // Found the page
                if(result.length() > 0) {
                    // Prepare the pattern
                    Pattern pattern = Pattern.compile("Current Version.*?>\\s*([^<>]*?)\\s*</span>");

                    // Prepare the matcher
                    Matcher matcher = pattern.matcher(result);

                    // Found a match
                    if(matcher.find()) {
                        // Get the store version
                        String versionLive = matcher.group(1);

                        // Prepare the package  manager
                        PackageManager packageManager = _mContext.getPackageManager();
                        try {
                            // Get the info
                            PackageInfo packageInfo = packageManager.getPackageInfo(_mContext.getPackageName(), 0);

                            // Get the current version
                            String versionCurrent = packageInfo.versionName;

                            // Get the stored version
                            String versionStored = preferences.getString(Constants.Keys.SETTINGS_UPDATE_CHECKER_VERSION, Constants.Defaults.SETTINGS_UPDATE_CHECKER_VERSION);

                            // New version online
                            if(!versionLive.equals(versionCurrent)) {
                                // And we did not check it yet
                                if(!versionLive.equals(versionStored)) {
                                    // Get the editor
                                    SharedPreferences.Editor editor = preferences.edit();

                                    // Store the value
                                    editor.putString(Constants.Keys.SETTINGS_UPDATE_CHECKER_VERSION, versionLive);

                                    // Store this check
                                    editor.putLong(Constants.Keys.SETTINGS_UPDATE_CHECKER_LAST_CHECK, currentTime);

                                    // Commit
                                    editor.commit();

                                    // Inform the user
                                    FpNotification.setNotificationAlarm(_mContext, Constants.Notification.UpdateCheck.TIME, Constants.Notification.UpdateCheck.ID);
                                }
                            }
                        } catch(NameNotFoundException e) {
                        }
                    }
                }
            }
        }).start();
    }
}

/*EOF*/