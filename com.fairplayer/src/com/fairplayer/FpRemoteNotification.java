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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

@SuppressWarnings("unused")
public class FpRemoteNotification {
    /**
     * Update available listener
     */
    public static abstract class UpdateAvailableListener {
        abstract void updateAvailable(Context context);
    }
    
    /**
     * Notification keys
     */
    public static final String ACCOUNT_NOTIF                    = "n";
    public static final String NOTIF_KEY_INTENT_TEXT            = "tx";
    public static final String NOTIF_KEY_INTENT_ACTIVITY        = "ac";
    public static final String NOTIF_KEY_INTENT_ACTIVITY_MORE   = "m";
    public static final String NOTIF_KEY_INTENT_ACTIVITY_INDEX  = "i";
    public static final String NOTIF_KEY_INTENT_ACTIVITY_UPDATE = "u";
    public static final String NOTIF_KEY_INTENT_STYLE           = "st";
    public static final String NOTIF_KEY_INTENT_STYLE_CUSTOM    = "c";
    public static final String NOTIF_KEY_INTENT_STYLE_DEFAULT   = "d";
    public static final String NOTIF_KEY_INTENT_COLOR           = "cl";
    public static final String NOTIF_KEY_INTENT_COLOR_RED       = "r";
    public static final String NOTIF_KEY_INTENT_COLOR_ORANGE    = "o";
    public static final String NOTIF_KEY_INTENT_COLOR_YELLOW    = "y";
    public static final String NOTIF_KEY_INTENT_COLOR_GREEN     = "g";
    public static final String NOTIF_KEY_INTENT_COLOR_BLUE      = "b";
    public static final String NOTIF_KEY_INTENT_THEME           = "th";
    public static final String NOTIF_KEY_INTENT_THEME_LIGHT     = "l";
    public static final String NOTIF_KEY_INTENT_THEME_DARK      = "d";
    public static final String NOTIF_KEY_INTENT_ICON            = "ic";
    public static final String NOTIF_KEY_INTENT_TOAST           = "ts";
    public static final String NOTIF_KEY_INTENT_EXPIRY          = "xp";
    public static final String NOTIF_KEY_INTENT_ID              = "id";
    
    // Shared preferences key
    public static final String SHARED_PREF_KEY = "fp_notif_id";
    
    // Intent variables
    public static String intentId       = "";
    public static String intentActivity = "";
    public static String intentStyle    = "";
    public static String intentColor    = "";
    public static String intentTheme    = "";
    public static String intentIcon     = "";
    public static String intentText     = "";
    public static String intentToast    = "";
    public static long   intentExpiry   = 0;
    
    /**
     * Check whether a translation was provided for the current locale and return it
     * 
     * @param keyName   Key
     * @param context  Context
     * @param dataJson JSON data
     * @return Key translation or null on error
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected static String _getByLocale(String keyName, Context context, JSONObject dataJson) {
        // Get the current locale
        try {
            // Prepare the current locale
            Locale currentLocale = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentLocale = context.getResources().getConfiguration().getLocales().get(0);
            } else {
                currentLocale = context.getResources().getConfiguration().locale;
            }
            
            // Valid locale found
            if (null != currentLocale) {
                // Get it as a lower case string
                String currentLocaleString = currentLocale.toString().toLowerCase(Locale.ENGLISH);
                
                // Part of the list
                if (currentLocaleString.matches("([a-z]{2})_.*")) {
                    // Get the language particle
                    String currentLocaleShort = currentLocaleString.replaceAll("([a-z]{2})_.*", "$1");
                    
                    // Check if the specific translation was provided
                    try {
                        // Get the string
                        return dataJson.getString(keyName + "_" + currentLocaleShort);
                    } catch (Exception excTranslation) {
                        // Nothing to do
                    }
                }
            }
        } catch (Exception excLocale) {
            // Nothing to do
        }
        
        // Try to get the default value
        try {
            return dataJson.getString(keyName);
        } catch (Exception excDefault) {
            // Nothing to do
        }
        
        // Nothing found
        return null;
    }
    
    /**
     * Get the class name for the current intent activity
     */
    public static String getIntentActivityLocal() {
        String intentActivityLocal = null;
        
        // Go through the keys
        switch (intentActivity) {
            case NOTIF_KEY_INTENT_ACTIVITY_INDEX:
            case NOTIF_KEY_INTENT_ACTIVITY_UPDATE:
                intentActivityLocal = "ActivityNowplaying";
                break;
            
            case NOTIF_KEY_INTENT_ACTIVITY_MORE:
                intentActivityLocal = "ActivitySettings";
                break;
        }
        
        // All done
        return intentActivityLocal;
    }

    /**
     * Parse a server-side JSON payload
     */
    public static void parse(String result, Context context) {
        parse(result, context, null);
    }

    /**
     * Parse a server-side JSON payload
     */
    public static void parse(String result, Context context, UpdateAvailableListener updateAvailableListener) {
        try {
            // Get the shared preferences
            SharedPreferences sharedPreferences = PreferenceUtils.getPreferences(context);
            
            // Get the data
            JSONObject fullPayload = new JSONObject(result);
            
            // Get the notification object
            JSONObject dataJson = fullPayload.getJSONObject(ACCOUNT_NOTIF);

            // Get the intent activity
            try {
                intentActivity = dataJson.getString(NOTIF_KEY_INTENT_ACTIVITY);

                // An empty value
                if (null == intentActivity) {
                    throw new Exception("Empty intentActivity");
                }

                // Not a URL
                if (!intentActivity.matches("^.*?\\:\\/\\/.*$")) {
                    try {
                        Class.forName(context.getPackageName() + "." + getIntentActivityLocal());
                    } catch (ClassNotFoundException e) {
                        // And not a local class, revert to the default
                        throw new Exception("Invalid intentActivity activity");
                    }
                }
            } catch (Exception exc) {
                // Set the default
                intentActivity = NOTIF_KEY_INTENT_ACTIVITY_MORE;
            }

            // Get the intent style
            try {
                intentStyle = dataJson.getString(NOTIF_KEY_INTENT_STYLE);

                // An empty value
                if (null == intentStyle) {
                    throw new Exception("Empty intentStyle");
                }

                // Not a valid style
                if (!intentStyle.matches("^(" + NOTIF_KEY_INTENT_STYLE_CUSTOM + "|" + NOTIF_KEY_INTENT_STYLE_DEFAULT + ")$")) {
                    throw new Exception("Invalid intentStyle");
                }
            } catch (Exception exc) {
                // Set the default style to "custom"
                intentStyle = NOTIF_KEY_INTENT_STYLE_CUSTOM;
            }
            
            // Get the intent color
            try {
                intentColor = dataJson.getString(NOTIF_KEY_INTENT_COLOR);

                // An empty value
                if (null == intentActivity) {
                    throw new Exception("Empty intentColor");
                }

                // Not a valid color
                if (!intentColor.matches("^(" + NOTIF_KEY_INTENT_COLOR_RED + "|" + NOTIF_KEY_INTENT_COLOR_GREEN + "|" + NOTIF_KEY_INTENT_COLOR_BLUE + "|" + NOTIF_KEY_INTENT_COLOR_ORANGE + "|" + NOTIF_KEY_INTENT_COLOR_YELLOW + ")$")) {
                    throw new Exception("Invalid intentColor");
                }
            } catch (Exception exc) {
                // Set the default
                if (intentActivity.matches("^.*?\\:\\/\\/.*$")) {
                    // URL
                    intentColor = NOTIF_KEY_INTENT_COLOR_ORANGE;
                } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_INDEX)) {
                    // Index
                    intentColor = NOTIF_KEY_INTENT_COLOR_BLUE;
                } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_MORE)) {
                    // More
                    intentColor = NOTIF_KEY_INTENT_COLOR_GREEN;
                } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_UPDATE)) {
                    // More
                    intentColor = NOTIF_KEY_INTENT_COLOR_RED;
                } else {
                    // Other
                    intentColor = NOTIF_KEY_INTENT_COLOR_RED;
                }
            }

            // Get the intent theme
            try {
                intentTheme = dataJson.getString(NOTIF_KEY_INTENT_THEME);

                // An empty value
                if (null == intentTheme) {
                    throw new Exception("Empty intentTheme");
                }

                // Not a valid theme
                if (!intentTheme.matches("^(" + NOTIF_KEY_INTENT_THEME_LIGHT + "|" + NOTIF_KEY_INTENT_THEME_DARK + ")$")) {
                    throw new Exception("Invalid intentTheme");
                }
            } catch (Exception exc) {
                // Set the default
                intentTheme = NOTIF_KEY_INTENT_THEME_DARK;
            }

            // Get the intent icon
            try {
                intentIcon = dataJson.getString(NOTIF_KEY_INTENT_ICON);

                // An empty value
                if (null == intentIcon) {
                    throw new Exception("Empty intentIcon");
                }
            } catch (Exception exc) {
                // Set the default
                if (intentActivity.matches("^.*?\\:\\/\\/.*$")) {
                    // URL
                    intentIcon = "url";
                } else {
                    // Other
                    intentIcon = intentActivity.toLowerCase(Locale.ENGLISH);
                }
            }

            // Get the intent text
            try {
                intentText = _getByLocale(NOTIF_KEY_INTENT_TEXT, context, dataJson);
                
                // An empty value
                if (null == intentText) {
                    throw new Exception("Empty intentText (i18n)");
                }
            } catch (Exception exc) {
                try {
                    // Sent as a plain text
                    intentText = dataJson.getString(NOTIF_KEY_INTENT_TEXT);
                    
                    // An empty value
                    if (null == intentText) {
                        throw new Exception("Empty intentText");
                    }
                } catch (Exception excGetFromAlert) {
                    // Set the default
                    if (intentActivity.matches("^.*?\\:\\/\\/.*$")) {
                        // URL
                        intentText = "Check this out";
                    } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_INDEX)) {
                        // Index
                        intentText = "We have missed you";
                    } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_MORE)) {
                        // More
                        intentText = "Get new themes";
                    } else if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_UPDATE)) {
                        // More
                        intentText = "Update available";
                    } else {
                        // Other
                        intentText = "Check this out";
                    }
                }
            }

            // Get the intent toast
            try {
                intentToast = _getByLocale(NOTIF_KEY_INTENT_TOAST, context, dataJson);

                // An empty value
                if (null == intentToast) {
                    throw new Exception("Empty intentToast (i18n)");
                }
            } catch (Exception exc) {
                try {
                    // Sent as a plain text
                    intentToast = dataJson.getString(NOTIF_KEY_INTENT_TOAST);
                    
                    // An empty value
                    if (null == intentToast) {
                        throw new Exception("Empty intentToast");
                    }
                } catch (Exception excGetFromAlert) {
                    // Keep it empty
                    intentToast = "";
                }
            }
            
            // Get the intent ID
            try {
                intentId = dataJson.getString(NOTIF_KEY_INTENT_ID);

                // An empty value
                if (null == intentId) {
                    throw new Exception("Empty intentId");
                }
            } catch (Exception exc) {
                // Keep it empty
                intentId = "";
            }
            
            // Get the intent expiry unix timestamp
            try {
                intentExpiry = dataJson.getLong(NOTIF_KEY_INTENT_EXPIRY);

                // An empty value
                if (0 == intentExpiry) {
                    throw new Exception("Empty intentExpiry");
                }
            } catch (Exception exc) {
                // Keep it empty
                intentExpiry = 0;
            }

            // Log the values
            Log.d(Ads.TAG, "Push message: "
                + "intentActivity = " + intentActivity 
                + ", intentStyle = " + intentStyle
                + ", intentColor = " + intentColor
                + ", intentTheme = " + intentTheme
                + ", intentIcon = " + intentIcon
                + ", intentText = " + intentText
                + ", intentToast = " + intentToast
                + ", intentExpiry = " + intentExpiry
                + ", intentId = " + intentId
            );
            
            do {
                // Get the stored notification ID
                String storedIntentId = sharedPreferences.getString(SHARED_PREF_KEY, "");
                
                // Already sent
                if (storedIntentId.equals(intentId)) {
                    // Custom update events should pass thru
                    if (!intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_UPDATE) || null == updateAvailableListener) {
                        Log.d(Ads.TAG, "Push message discarded: already sent [" + intentId + "]");
                        break;
                    }
                }
                
                // Get the preferences editor
                Editor sharedPreferencesEditor = sharedPreferences.edit();
                
                // Save the notification ID
                sharedPreferencesEditor.putString(SHARED_PREF_KEY, intentId);
                
                // Commit
                sharedPreferencesEditor.commit();
                
                // Expiration time reached
                if (System.currentTimeMillis() / 1000 >= intentExpiry) {
                    Log.d(Ads.TAG, "Push message discarded: expired");
                    break;
                }
                
                try {
                    if (intentActivity.equals(NOTIF_KEY_INTENT_ACTIVITY_UPDATE)) {
                        // Set the default value for the update listner
                        if (null == updateAvailableListener) {
                            updateAvailableListener = new UpdateAvailableListener() {

                                @Override
                                void updateAvailable(Context context) {
                                    // Send the notification anyway (maybe Google changed its source code again)
                                    new FpRemoteNotificationTask(context).execute();

                                    // Track the event
                                    Tracker.trackPushOpen(intentId);
                                }

                            };
                        }

                        // Update available
                        onUpdateAvailable(context, updateAvailableListener);
                    } else {
                        // Send the notification
                        new FpRemoteNotificationTask(context).execute();

                        // Track the event
                        Tracker.trackPushOpen(intentId);
                    }
                } catch (Exception excTask) {
                    // Nothing to do
                }
            } while (false);
        } catch (JSONException jsonException) {
            // Log this
            Log.e(Ads.TAG, jsonException.getMessage());
        }
    }

    /**
     * Check whether an update is available
     */
    public static void onUpdateAvailable(final Context context, final UpdateAvailableListener listener) {
        // On a different thread
        new Thread(new Runnable() {
            public void run() {
                // Prepare the page URL
                String dataUrl = "https://play.google.com/store/apps/details?hl=en&id=" + context.getPackageName();

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
                    do {
                        // Prepare the pattern
                        Pattern pattern = Pattern.compile("Current Version.*?>\\s*([^<>]*?)\\s*</span>");

                        // Prepare the matcher
                        Matcher matcher = pattern.matcher(result);

                        // Found a match
                        if(matcher.find()) {
                            // Get the store version
                            String versionLive = matcher.group(1);

                            // Prepare the package  manager
                            PackageManager packageManager = context.getPackageManager();
                            try {
                                // Get the info
                                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

                                // Get the current version
                                String versionCurrent = packageInfo.versionName;

                                // Same version
                                if(versionLive.equals(versionCurrent)) {
                                    // Nothing to do
                                    break;
                                }
                            } catch(Exception e) {}
                        }

                        // Callback
                        listener.updateAvailable(context);
                    } while(false);
                }
            }
        }).start();
    }
}

/*EOF*/