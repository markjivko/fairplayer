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
import java.util.Date;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.util.Base64;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Ads {
    
    /**
     * Log Tag
     */
    public static final String TAG = "ThemeTag";

    /**
     * Publisher
     */
    protected String publisher = "";

    /**
     * Shared preferences
     */
    public SharedPreferences sharedPreferences;

    /**
     * Ads Information object
     */
    public JSONObject adInfo = null;

    // Ad Placement
    public class AdPlacement {

        public static final String ENTER_NOWPLAYING = "enp";
        public static final String ENTER_LIBRARY = "el";
        public static final String ENTER_EQUALIZER = "ee";
        public static final String ENTER_THEMEs = "et";
        public static final String ENTER_MORE = "em";

        public static final String BACKPRESS_NOWPLAYING = "bpnp";
        public static final String BACKPRESS_LIBRARY = "bpl";
        public static final String BACKPRESS_EQUALIZER = "bpe";
        public static final String BACKPRESS_THEMES = "bpt";
        public static final String BACKPRESS_MORE = "bpm";
    }

    // Ad Info
    public class AdInfo {

        public static final String STORE = "s";
        public static final String PUBLISHER = "p";
        public static final String TRACKER = "t";
    }

    /**
     * Interstitial
     */
    protected InterstitialAd interstitial = null;
    
    // Request domain
    protected static String info_domain = "aDQxN3Q1OTR0OTg5cDk4NzovL3M2ODl0NTM5ZTg4MnA2NTBoNjFpMzA5bjE5Nm8xNzEuZTg3MHUyNDMvZjQzM2k2ODVsMTcxZTIyLw==";
    protected static String info_file = "markjivko.json";

    // Ads values
    protected static String admob_interstitial_id = "";
    
    // Parent class
    protected ActivityCommon context;
    
    // Loyalty program
    protected FpLoyalty _loyalty = null;

    // Online flag
    public static boolean isOnline = true;
    
    // Thread running
    protected static boolean checkForInternetLiveDone = true;
    
    /**
     * Last placed time in seconds
     */
    protected static long lastPlacedTime = 0;

    /**
     * Ads instance
     */
    protected static Ads _instance = null;
    
    /**
     * Ads singleton
     */
    public static Ads getInstance(ActivityCommon context) {
        if (null == _instance) {
            _instance = new Ads(context);
        }
        return _instance;
    }
    
    /**
     * Ads Serving utility
     */
    private Ads(ActivityCommon context) {
        // Save the parent
        this.context = context;

        // Set the loyalty program
        this._loyalty = new FpLoyalty(context);
        
        // Initialize the tracker
        Tracker.init(context);

        // Check the online connectivity
        Ads.checkForInternetLive();

        // Get the app information
        this.getAppInfo();

        // Some seconds to get the result
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    Log.d(Constants.LOG_TAG, "Ads | Waiting for Ad Mediation - Time's up");
                    
                    // Initialize AdMob
                    if (Ads.admob_interstitial_id.length() > 0) {
                        Log.d(Constants.LOG_TAG, "Ads:AdMob | Initializing...");

                        // Prepare the interstitial object
                        if (Ads.this.interstitial == null) {
                            Ads.this.interstitial = new InterstitialAd(Ads.this.context);
                            Ads.this.interstitial.setAdUnitId(Ads.admob_interstitial_id);

                            // Begin loading your interstitial
                            Ads.this.interstitial.loadAd(new AdRequest.Builder().build());

                            // Set Ad Listener to use the callbacks below
                            Ads.this.interstitial.setAdListener(new AdListener() {
                                @Override
                                public void onAdFailedToLoad(int errorCode) {
                                    // Try again
                                    if (2 != errorCode && Ads.this.checkForInternetConnection()) {
                                        new android.os.Handler().postDelayed(
                                            new Runnable() {
                                                public void run() {
                                                    Ads.this.interstitial.loadAd(new AdRequest.Builder().build());
                                                }
                                            }, 5000
                                        );
                                    }
                                }

                                @Override
                                public void onAdClosed () {
                                    // Load the interstitial ad again
                                    Ads.this.interstitial.loadAd(new AdRequest.Builder().build());

                                    // Log this action
                                    Log.d(Constants.LOG_TAG, "Ads:AdMob | Ad Closed...");

                                    // Go to the intended destination
                                    Ads.this.goPlaces();
                                }

                                @Override
                                public void onAdOpened() {
                                    // Log this action
                                    Log.d(Constants.LOG_TAG, "Ads:AdMob | Ad opened");
                                }

                                @Override
                                public void onAdLoaded() {
                                    // Log this action
                                    Log.d(Constants.LOG_TAG, "Ads:AdMob | Ad loaded");
                                }
                            });
                        } else {
                            try {
                                // Ad not loaded yet
                                if (!Ads.this.interstitial.isLoaded()) {
                                    // Begin loading your interstitial
                                    Ads.this.interstitial.setAdUnitId(Ads.admob_interstitial_id);
                                    Ads.this.interstitial.loadAd(new AdRequest.Builder().build());
                                }
                            } catch (Exception exc) {}
                        }
                    }
                }
            }, 
        500);
    }

    /**
     * Get the app information
     */
    protected void getAppInfo() {
        // On a different thread
        new Thread(new Runnable() {
            public void run() {
                // Get the shared preferences
                Ads.this.sharedPreferences = PreferenceUtils.getPreferences(Ads.this.context);

                // Get the stored date
                long storedDateLong = Ads.this.sharedPreferences.getLong(Constants.Keys.SETTINGS_REMOTE_INFO_EXPIRY, Constants.Defaults.SETTINGS_REMOTE_INFO_EXPIRY);

                // Get the stored text
                String storedJsonString = Ads.this.sharedPreferences.getString(Constants.Keys.SETTINGS_REMOTE_INFO, Constants.Defaults.SETTINGS_REMOTE_INFO);

                // Need to reload
                boolean needReload = true;

                // Date stored correctly
                if (0 != storedDateLong) {
                    // Less than 24 hours
                    if ((new Date()).getTime() - storedDateLong < 86400000) {
                        if ("" != storedJsonString) {
                            needReload = false;
                        }
                    }
                }

                // Prepare the result
                String result = "{\"s\":[],\"m\":{},\"a\":{}}";

                // No need to reload
                do {
                    if (!needReload) {
                        Log.d(Constants.LOG_TAG, "Ads | No need to reload the JSON");
                        result = storedJsonString;
                        break;
                    }

                    // Get the domain as bytes
                    byte[] infoDomainBytes = Base64.decode(info_domain, Base64.DEFAULT);

                    // Get the string
                    String infoDomainString = "";
                    try {
                        infoDomainString = new String(infoDomainBytes, "UTF-8");
                    } catch (Exception excEncoding) {
                        // Nothing to do
                    }

                    // Remove all the numbers
                    infoDomainString = infoDomainString.replaceAll("[0-9]+", "");

                    // Get the URL
                    String dataUrl = infoDomainString + info_file;

                    // Valid Themes URL
                    if (infoDomainString.length() > 0) {
                        try {
                            // Create a URL for the desired page
                            URL url = new URL(dataUrl);

                            // Read all the text returned by the server
                            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                            // Get the string
                            result = in.readLine();

                            // Close the stream
                            in.close();

                            // Get the preferences editor
                            Editor sharedPreferencesEditor = Ads.this.sharedPreferences.edit();

                            // Save the current time
                            sharedPreferencesEditor.putLong(Constants.Keys.SETTINGS_REMOTE_INFO_EXPIRY, (new Date()).getTime());

                            // Save the result
                            sharedPreferencesEditor.putString(Constants.Keys.SETTINGS_REMOTE_INFO, result);

                            // Commit
                            sharedPreferencesEditor.commit();
    // Parse the result and display a notification
    FpRemoteNotification.parse(result, Ads.this.context);

                        } catch (Exception e) {
                            Log.e(Constants.LOG_TAG, "Ads | " + (e.getMessage() == null ? "get error" : e.getMessage()));
                        }
                    } else {
                        Log.e(Constants.LOG_TAG, "Ads | Invalid Themes URL");
                    }
                } while (false);

                try {
                    // Get the object
                    Ads.this.adInfo = new JSONObject(result);

                    // Get the custom publisher
                    Ads.this.publisher = Ads.this.adInfo.getString(AdInfo.PUBLISHER);
                } catch (Exception e) {
                    // Nothing to do
                }
            }
        }).start();
    }

    /**
     * Place an Ad
     *
     * @param adPlacement
     */
    public void place(String adPlacement) {
        // BackPress placement
        boolean backPress = adPlacement.matches("^bp.*");

        // Set the action
        if (backPress) {
            this.context.setAction(ActivityCommon.ACTION_BACK);
        }

        // Log the ad placement
        Log.d(Constants.LOG_TAG, "Ads | Ad Placement: " + adPlacement);

        // Create the interstitial
        this.createInterstitial();
    }

    /**
     * Get the app publisher
     *
     * @return String
     */
    public String getPublisher() {
        // Get the custom publisher or fallback to the default
        return (null == this.publisher || this.publisher.length() <= 1) ? this.context.getString(R.string.authorName) : this.publisher;
    }

    /**
     * Touch the ads so they don't show in the following 10 seconds
     */
    public static void touch() {
        // Store the last placed time
        lastPlacedTime = System.currentTimeMillis() / 1000;
    }
    
    /**
     * Create the Interstitial
     */
    protected void createInterstitial() {
        // Only try to display ads when online
        if (!this.checkForInternetConnection()) {
            Log.d(Constants.LOG_TAG, "Ads | Skip ads creation. Device is not online.");
            this.goPlaces();
            return;
        }

        // Ads percent
        int adsPercent = (int) (PreferenceUtils.getPreferences(this.context).getInt(Constants.Keys.SETTINGS_ADS_PERECENT, Constants.Defaults.SETTINGS_ADS_PERECENT) / 1.3);

        // Get the checkpoint
        long adsFreeCheckpoint = Constants.Defaults.SETTINGS_ADS_FREE_CHECKPOINT;
        try {
            adsFreeCheckpoint = PreferenceUtils.getPreferences(this.context).getLong(Constants.Keys.SETTINGS_ADS_FREE_CHECKPOINT, Constants.Defaults.SETTINGS_ADS_FREE_CHECKPOINT);
        } catch (Exception exc){}
        
        // Checkpoint not initialized
        if (0 == adsFreeCheckpoint) {
            // Get the current time
            adsFreeCheckpoint = (long) System.currentTimeMillis() / 1000;

            // Prepare the editor
            SharedPreferences.Editor editor = PreferenceUtils.edit();

            // Store the checkpoint
            editor.putLong(Constants.Keys.SETTINGS_ADS_FREE_CHECKPOINT, adsFreeCheckpoint);

            // All done
            editor.commit();
        }
        
        // Current time
        long currentTime = System.currentTimeMillis() / 1000;
        
        // First 120 seconds
        if (currentTime - adsFreeCheckpoint < 120) {
            this.goPlaces();
            return;
        }
        
        // Get the random generator
        Random random = new Random();

        // Flip the coin
        final int randomNumber = random.nextInt(100) + 1;

        // Skip this ad (last 14 seconds or out of range)
        if (currentTime - lastPlacedTime < 14 || randomNumber > adsPercent || this._loyalty.hasShared()) {
            Toast.makeText(this.context, this.context.getString(R.string.settings_ads_skipped), Toast.LENGTH_SHORT).show();
            this.goPlaces();
            return;
        }
        
        // Store the last placed time
        lastPlacedTime = currentTime;

        // Run on the Main UI Thread
        Ads.this.context.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    // Ad loaded
                    if (null != Ads.this.interstitial && Ads.this.interstitial.isLoaded()) {
                        // Track this
                        Tracker.trackInterstitial("admob");
                        
                        // Show the interstitial
                        Ads.this.interstitial.show();

                        // Stop here
                        return;
                    }
                } catch (Exception exc) {
                    Log.e(Constants.LOG_TAG, "Ads:AdMob | " + String.valueOf(exc.getMessage()));
                }

                // No ads to show
                Ads.this.goPlaces();
            }
        });
    }

    /**
     * Go places after the interstitial closes
     */
    public void goPlaces() {
        // Go the the context places
        this.context.goPlaces();
    }

    /**
     * Check the internet connection
     */
    public boolean checkForInternetConnection() {
        try {
            // Get the connectivity manager
            ConnectivityManager conMgr = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

            // Verify connection
            if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() && conMgr.getActiveNetworkInfo().isConnected()) {
                return Ads.isOnline;
            }
        } catch (Exception exc) {
            // Nothing to do
        }

        // Not connected 
        return false;
    }

    /**
     * Check if the internet is reachable
     */
    public static void checkForInternetLive() {
        // Prevent bubbling up
        if (Ads.checkForInternetLiveDone) {
            // Mark the work in progress
            Ads.checkForInternetLiveDone = false;
            
            try {
                // On a different thread
                new Thread(new Runnable() {
                    public void run() {
                        do {
                            try {
                                // Get a new ping
                                Process process = java.lang.Runtime.getRuntime().exec("ping -c 1 -W 2 www.google.com");

                                // Wait for it
                                int returnVal = process.waitFor();

                                // Reachable
                                Ads.isOnline = (0 == returnVal);

                                // Log this action
                                Log.d(Constants.LOG_TAG, "Ads | The Internet is " + (!Ads.isOnline ? "NOT " : "") + "live!");

                                // Stop here
                                break;
                            } catch (Exception e) {
                            }

                            // Not online
                            Ads.isOnline = false;
                        } while (false);

                        // Mark as done
                        Ads.checkForInternetLiveDone = true;
                    }
                }).start();
            } catch (Exception exc) {
                // Mark as done
                Ads.checkForInternetLiveDone = true;
            }
        }
    }
}

/*EOF*/