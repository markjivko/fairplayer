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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;

public class Tracker {

    // Tracking ID 
    private static String _trackingId = null;

    // Device ID
    private static String _deviceID = null;

    // Current app context
    private static Context _context = null;
    
    // Current activity (optional)
    private static Activity _activity = null;
    
    // Current subpage (optional)
    private static String _subpage = null;

    // Set the context
    public static void init(Context context) {
        // Set the context
        _context = context;
    }

    /**
     * Set the activity
     */
    public static void setActivity(Activity activity) {
    	_activity = activity;
    }
    
    /**
     * Track the pageview for a specific activity and sub-page
     */
    public static void trackPageview(Activity activity, String subpage) {
    	_subpage = subpage;
    	trackPageview(activity);
    }
    
    /**
     * Track the pageview for a specific activity
     * @param activity
     */
    public static void trackPageview(Activity activity) {
    	_activity = activity;
    	trackPageview();
    }
    
    /**
     * Track the pageview
     */
    public static void trackPageview() {
        _send();
    }

    /**
     * Track push events
     *
     * @param pushId
     * @param actionPush
     */
    private static void _trackPush(String pushId, String actionPush) {
        _sendEvent("push", actionPush, pushId);
    }

    /**
     * Track push:receive
     *
     * @param pushId
     */
    public static void trackPushReceive(String pushId) {
        _trackPush(pushId, "receive");
    }

    /**
     * Track push:open
     *
     * @param pushId
     */
    public static void trackPushOpen(String pushId) {
        _trackPush(pushId, "open");
    }

    /**
     * Track push:dismiss
     *
     * @param pushId
     */
    public static void trackPushDismiss(String pushId) {
        _trackPush(pushId, "dismiss");
    }

    /**
     * Track more events
     *
     * @param moreDimension
     * @param moreType
     */
    private static void _trackMore(String moreDimension, String moreType) {
        _sendEvent("more-" + moreType, moreDimension);
    }

    /**
     * Track more:app
     *
     * @param packageName
     */
    public static void trackMoreApp(String packageName) {
        _trackMore(_getAppName(packageName), "app");
    }

    /**
     * Track more:author
     *
     * @param authorName
     */
    public static void trackMoreAuthor(String authorName) {
        _trackMore(authorName, "author");
    }

    /**
     * Track download
     *
     * @param packageName
     */
    public static void trackDownload(String packageName) {
        _sendEvent("download", packageName);
    }

    /**
     * Track tap
     *
     * @param buttonName
     */
    public static void trackTap(String buttonName) {
        _sendEvent("tap", buttonName);
    }
    
    /**
     * Track sharing
     * 
     * @param packageName
     */
    public static void trackShare(String packageName) {
    	_sendEvent("share", packageName);
    }

    /**
     * Track interstitial
     *
     * @param adProvider
     */
    public static void trackInterstitial(String adProvider) {
        _sendEvent("interstitial", adProvider);
    }

    /**
     * Send an event
     *
     * @param category
     * @param action
     */
    private static void _sendEvent(String category, String action) {
        _sendEvent(category, action, _getAccountName(), null);
    }

    /**
     * Send an event
     *
     * @param category
     * @param action
     * @param label
     */
    private static void _sendEvent(String category, String action, String label) {
        _sendEvent(category, action, label, null);
    }

    /**
     * Send an event
     *
     * @param category
     * @param action
     * @param label
     * @param value
     */
    private static void _sendEvent(String category, String action, String label, String value) {
        // Inform the user
        Log.d(Constants.LOG_TAG, "Track: Event(" + String.valueOf(category) + ", " + String.valueOf(action) + ", " + String.valueOf(label) + ", " + String.valueOf(value) + ")");

        // Prepare the payload
        Map<String, String> payload = new HashMap<String, String>();

        // Set the category
        payload.put("ec", String.valueOf(category));

        // Set the action
        payload.put("ea", String.valueOf(action));

        // Set the label
        payload.put("el", String.valueOf(label));

        // Set the value
        if (null != value) {
            payload.put("ev", String.valueOf(value));
        }

        // Send the event
        try {
            _send(payload, "event");
        } catch (Exception exc) {
            Log.e(Constants.LOG_TAG, String.valueOf(exc));
        }
    }

    /**
     * Send the Google Analytics request
     */
    private static void _send() {
        // Prepare the payload
        Map<String, String> payload = new HashMap<String, String>();

        // Send the screnview
        try {
            _send(payload, "screenview");
        } catch (Exception exc) {
            Log.e(Constants.LOG_TAG, String.valueOf(exc));
        }
    }

    /**
     * Get the tracking ID
     * 
     * @return 
     */
    private static String _getTrackingId() {
        // Tracking ID not initialized
        if (null == _trackingId || _trackingId.length() == 0) {
            // Get the shared preferences
            SharedPreferences sharedPreferences = PreferenceUtils.getPreferences();
            
            // Get the stored text
            String storedJsonString = sharedPreferences.getString(Constants.Keys.SETTINGS_REMOTE_INFO, Constants.Defaults.SETTINGS_REMOTE_INFO);

            try {
                // Get the object
                JSONObject adInfo = new JSONObject(storedJsonString);

                // Get the custom tracker ID
                try {
                    // Get the custom tracker ID
                    String trackingId = adInfo.getString(Ads.AdInfo.TRACKER);

                    // Valid tracker ID
                    if (null != trackingId && trackingId.length() > 0) {
                        _trackingId = trackingId;
                    }
                } catch (Exception exc) {}
            } catch (Exception exc) {}
        }
        
        // All done
        return _trackingId;
    }
    
    /**
     * Send the Google Analytics request
     */
    private static void _send(Map<String, String> payload, String hitType) {
        // Context not set
        if (null == _context) {
            Log.e(Constants.LOG_TAG, "Track: Context not set, cannot continue");
            return;
        }
        
        // Get the tracking id
        String trackingId = _getTrackingId();
        
        // Tracking ID not set
        if (null == trackingId || trackingId.length() == 0) {
            Log.e(Constants.LOG_TAG, "Track: Invalid tracking ID");
            return;
        }

        // Prepare the parameter list
        final List<NameValuePair> arguments = new ArrayList<NameValuePair>();

        // Add the payload
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            // Add the argument
            arguments.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        // Set the protocol version
        arguments.add(new BasicNameValuePair("v", "1"));

        // Set the tracking ID
        arguments.add(new BasicNameValuePair("tid", trackingId));

        // Get the device ID
        if (null == _getDeviceId()) {
            Log.e(Constants.LOG_TAG, "Track: Could not compute UUID");
            return;
        }

        // Set the client ID
        arguments.add(new BasicNameValuePair("cid", _getDeviceId()));

        // Set the hit type
        arguments.add(new BasicNameValuePair("t", hitType));

        // Set the data source
        arguments.add(new BasicNameValuePair("ds", "web"));

        // Set the current page
        arguments.add(new BasicNameValuePair("cd", _getScreenName()));

        // Set the app name
        arguments.add(new BasicNameValuePair("an", _getAppName()));

        // Set the app version
        arguments.add(new BasicNameValuePair("av", _getAppName() + ", " + _getAppVersion()));

        // Set the device resolution
        String screenResolution = _getScreenResolution();
        if (null != screenResolution) {
	        arguments.add(new BasicNameValuePair("sr", _getScreenResolution()));
	        arguments.add(new BasicNameValuePair("vp", _getScreenResolution()));
        }
        
        // Set the language
        arguments.add(new BasicNameValuePair("ul", _getUserLanguage()));

        // Set the cache buster
        arguments.add(new BasicNameValuePair("z", _getCacheBuster()));

        // Inform the user
        Log.i(Constants.LOG_TAG, "Track: Sending \"" + hitType + "\" for \"" + trackingId + "\"...");
        Log.d(Constants.LOG_TAG, "Track: " + String.valueOf(arguments));

        // Perform the request
        (new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    // Get the tracking URL
                    URL url = new URL("https://www.google-analytics.com/collect");

                    // Open the connection
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // Set the user agent
                    connection.setRequestProperty("User-Agent", _getUserAgent());

                    // Set the connection details
                    connection.setAllowUserInteraction(false);
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    // Prepare the output stream
                    OutputStream outoutStream = connection.getOutputStream();

                    // Prepare the arguments string
                    StringBuilder argumentsString = new StringBuilder();

                    // First argument flag
                    boolean argumentsFirst = true;

                    // Go through the elements
                    for (NameValuePair pair : arguments) {
                        // First time
                        if (!argumentsFirst) {
                            argumentsString.append("&");
                        }

                        // No longer the first argument
                        argumentsFirst = false;

                        // Add the parameter
                        argumentsString.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                        argumentsString.append("=");
                        argumentsString.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
                    }

                    // Write the argumetns
                    outoutStream.write(argumentsString.toString().getBytes());

                    // Close writer
                    outoutStream.close();

                    // Connect
                    connection.connect();

                    // Get the response code
                    int status = connection.getResponseCode();

                    // Track the status
                    Log.d(Constants.LOG_TAG, "Track: Status " + status);
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, "Track: " + String.valueOf(e));
                }

                // All done
                return "";
            }
        }).execute();
    }

    /**
     * Get the user agent
     *
     * @return Current device user agent
     */
    private static String _getUserAgent() {
        // Get random browser vrsion
        String browserVersion = "5.0";

        // Get local Android version
        String androidVersion = android.os.Build.VERSION.RELEASE;

        // Get local locale
        String androidLocale = _getUserLanguage();

        // Get the device name
        String androidDeviceName = android.os.Build.MODEL.startsWith(android.os.Build.MANUFACTURER) ? android.os.Build.MODEL : (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

        // Capitalize
        androidDeviceName = Character.toUpperCase(androidDeviceName.charAt(0)) + androidDeviceName.substring(1);

        // All done
        return String.format(
            "Mozilla/%s (Linux; U; Android %s; %s; %s) AppleWebKit/999+ (KHTML, like Gecko) Safari/999.9",
            browserVersion,
            androidVersion,
            androidLocale,
            androidDeviceName
        );
    }

    /**
     * Get the current user language
     *
     * @return Current language; ex.: "en-us"
     */
    @SuppressLint("DefaultLocale") 
    private static String _getUserLanguage() {
        return Locale.getDefault().toString().replace("_", "-").toLowerCase();
    }

    /**
     * Get the current account name
     *
     * @return Internal account name
     */
    private static String _getAccountName() {
        return "markjivko";
    }

    /**
     * Get the app name for the provided package
     *
     * @param packageName
     * @return Obfuscated app name
     */
    private static String _getAppName(String packageName) {
        // Remove the "com." prefix
        String appName = packageName.replaceAll("^com\\.", "").replace(".", " ").replace("_", "-");

        // All done
        return appName;
    }

    /**
     * Get the app name for the current package
     *
     * @return Obfuscated app name
     */
    private static String _getAppName() {
        return _getAppName(_context.getPackageName());
    }

    /**
     * Get the app version
     *
     * @return Returns the numeric app version
     */
    private static String _getAppVersion() {
        try {
            // Get the package info
            PackageInfo pInfo = _context.getPackageManager().getPackageInfo(_context.getPackageName(), 0);

            // Return the version
            return String.valueOf(pInfo.versionCode);
        } catch (Exception exc) {}

        // Version not found
        return "1";
    }

    /**
     * Get the current screen name
     *
     * @return Screen name based on activity class name
     */
    private static String _getScreenName() {
    	// Prepare the screen name
    	String screenName = _context.getClass().getSimpleName();
    	
    	// Valid activity
    	if (null != _activity) {
            try {
                screenName = _activity.getClass().getSimpleName();
            } catch (Exception e){}
    	}
    	
    	// Sub-page
    	if (null != _subpage) {
            // Append the sub-page
            screenName += "/" + _subpage;

            // Reset the argument
            _subpage = null;
    	}
    	
    	// All done
        return screenName;
    }

    /**
     * Get the screen resolution in pixels
     *
     * @return Screen resoltion in "width x height" format
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi") 
    private static String _getScreenResolution() {
    	try {
            // Get the display
            Display display = ((Activity) _context).getWindowManager().getDefaultDisplay();

            // Get the point size
            Point size = new Point();

            // Get the window width and height
            if (android.os.Build.VERSION.SDK_INT >= 13) {
                display.getSize(size);

                // All done
                return size.x + "x" + size.y;
            }

            // Older version
            return String.valueOf(display.getWidth()) + "x" + String.valueOf(display.getHeight());
    	} catch (Exception exc) {}
    	
    	// Not found
    	return null;
    }

    /**
     * Get the cache buster
     *
     * @return Random number from 0 to 9999999
     */
    private static String _getCacheBuster() {
        // Prepare the random generator
        Random random = new Random();

        // Get a random number
        int randomNumber = random.nextInt(9999999);

        // Return the string representation
        return String.valueOf(randomNumber);
    }

    /**
     * Get the device ID in UUIDv4 format
     *
     * @return UUID device ID
     */
    private static String _getDeviceId() {
        // Not calculated yet
        if (null == _deviceID && null != _context) {
            // Get the shared preferences
            SharedPreferences sharedPreferences = _context.getSharedPreferences(_context.getPackageName(), _context.MODE_PRIVATE);

            // Get the stored text
            String storedUUID = sharedPreferences.getString("UUID", "");
            
            // No value defined
            if (0 == storedUUID.length()) {
            	// Get the new UUID
            	storedUUID = UUID.randomUUID().toString();
            	
                // Prepare the editor
                android.content.SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

                // Store the current UUID
                sharedPreferencesEditor.putString("UUID", storedUUID);
                sharedPreferencesEditor.commit();
            }
        	
            // Inform the user
            Log.i(Constants.LOG_TAG, "Track: New UUID = " + storedUUID);
            
            // Get the string value of the UUID
            _deviceID = storedUUID;
        }

        // All done
        return _deviceID;
    }
}

/*EOF*/