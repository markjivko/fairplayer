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

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.widget.Toast;

public class FpLoyalty {
    
    protected final int[] _levels = {0,600,1800,3600,43200,86400,259200,864000,2592000,5184000,7776000};
    protected final int[] _chances = {90,85,80,75,70,65,60,55,50,40,25};
    protected final int _shareMinutes = 20;

    protected Context mContext;
    protected SharedPreferences mPreferences = null;
    protected long mAge;

    public FpLoyalty(Context context) {
        mContext = context;
        
        // Store the preferences
        mPreferences = PreferenceUtils.getPreferences(mContext);
    }

    /**
     * Get the detailed levels - chances table
     */
    public Map<String,String> getTable() {
        // Prepare the result
        Map<String,String> result = new LinkedHashMap<String,String>();

        // Go through the levels
        for (int i = 0; i < _levels.length; i++) {
            // Get the time in seconds
            int seconds = _levels[i];

            // Prepare the time string
            String time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_day, (int) TimeUnit.SECONDS.toDays(seconds), (int) TimeUnit.SECONDS.toDays(seconds));
            if (seconds < 86400) {
                time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_hour, (int) TimeUnit.SECONDS.toHours(seconds), (int) TimeUnit.SECONDS.toHours(seconds));
            }
            if (seconds < 3600) {
                time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_minute, (int) TimeUnit.SECONDS.toMinutes(seconds), (int) TimeUnit.SECONDS.toMinutes(seconds));
            }

            // Prepare the chance string
            String chance = String.valueOf(_chances[i]) + "%";

            // Append
            result.put(time, chance);
        }

        // All done
        return result;
    }

    /**
     * Get the time remaining until the next level
     */
    public String getNextString() {
        // Get the current level
        int currentLevel = getCurrentLevel();

        // Could not get the time
        if (currentLevel < 0) {
            return null;
        }

        // Top level
        if (currentLevel >= _chances.length - 1) {
            return null;
        }

        // Get the next level age
        int nextLevelAge = _levels[currentLevel + 1];

        // Get the time remaining
        int seconds = nextLevelAge - (int) mAge;

        // Prepare the time string
        String time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_day, (int) TimeUnit.SECONDS.toDays(seconds), (int) TimeUnit.SECONDS.toDays(seconds));
        if (seconds < 86400) {
            time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_hour, (int) TimeUnit.SECONDS.toHours(seconds), (int) TimeUnit.SECONDS.toHours(seconds));
        }
        if (seconds < 3600) {
            time = mContext.getResources().getQuantityString(R.plurals.fp_plurals_minute, (int) TimeUnit.SECONDS.toMinutes(seconds), (int) TimeUnit.SECONDS.toMinutes(seconds));
        }

        // All done
        return time;
    }

    /**
     * Get the current loyalty level
     */
    public int getCurrentLevel() {
        try {
            // Calculate the age in seconds
            long installedMs = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).firstInstallTime;
            long ageSeconds = (System.currentTimeMillis() - installedMs) / 1000;
            mAge = ageSeconds;

            // Prepare the level
            int level = 0;
            for (;level < _levels.length; level++) {
                if (ageSeconds < _levels[level]) {
                    break;
                }
            }

            // All done
            return level - 1;
        } catch (NameNotFoundException e) {}

        // Package name not found
        return -1;
    }

    /**
     * Get the ads minimum probability
     */
    public int getAdsChance() {
        int currentLevel = getCurrentLevel();

        // Could not get the time
        if (currentLevel < 0) {
            return _chances[0];
        }

        // Invalid level
        if (currentLevel > _chances.length - 1) {
            return _chances[10];
        }

        // All done
        return _chances[currentLevel];
    }

    /**
     * Get the next check time in seconds
     */
    public int getNextCheck() {
        int currentLevel = getCurrentLevel();

        // Could not get the time
        if (currentLevel < 0) {
            return -1;
        }

        // No extra check needed
        if (currentLevel >= _chances.length - 1) {
            return -1;
        }

        // Get the next check time in seconds
        return currentLevel >= 3 ? 3601 : 31;
    }
    
    /**
     * Setter
     */
    public void hasShared(boolean flag) {
        // Get the reward time (no ads up until that point)
        long rewardTime = System.currentTimeMillis() / 1000 + (long) (FpLoyalty.this._shareMinutes * this.getCurrentLevel() * 60);
        
        // Get the editor
        SharedPreferences.Editor editor = mPreferences.edit();
        
        // Store the time
        editor.putLong(Constants.Keys.SETTINGS_SHARING_REWARD_TIME, rewardTime);
        
        // All done
        editor.commit();
    }
    
    /**
     * Getter
     */
    public boolean hasShared() {
        // Get the reward time
        long rewardTime = mPreferences.getLong(Constants.Keys.SETTINGS_SHARING_REWARD_TIME, Constants.Defaults.SETTINGS_SHARING_REWARD_TIME);

        // Inside the interval
        return System.currentTimeMillis() / 1000 <= rewardTime;
    }
    
    /**
     * Sharing dialog
     * @param mCurrentSong 
     */
    public void sharingDialog(FpTrack currentSong) {
        // First phase
        sharingDialog(currentSong, false);
    }
    
    /**
     * Sharing dialog
     */
    public void sharingDialog(FpTrack currentSong, boolean secondPhase) {
        // Store the final values
        final boolean fSecondPhase = secondPhase;
        final FpTrack fCurrentSong = currentSong;
        
        // Prepare the task
        class RetrieveThemeTask extends AsyncTask<String, Void, String> {

            protected String doInBackground(String... currentThemes) {
                // Get the current theme
                String currentTheme = currentThemes[0];
                
                // A standalone theme
                if (!Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(currentTheme)) {
                    // Prepare the result
                    boolean result = false;
                    try {
                        // Open the stream
                        (new URL("https://play.google.com/store/apps/details?id=" + currentTheme)).openStream();

                        // All went well
                        result = true;
                    } catch (Exception ex) {}
                    
                    // Page not found
                    if(!result) {
                        currentTheme = Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME;
                    }
                }

                // All done
                return currentTheme;
            }

            protected void onPostExecute(String currentTheme) {
                try {
                    // Get the builder
                    AlertDialog.Builder builder = new AlertDialog.Builder(FpLoyalty.this.mContext);

                    // Get the resources
                    final Resources fResources = mContext.getResources();

                    do {
                        // Second phase
                        if (fSecondPhase) {
                            // Safe to say the user shared
                            Tracker.trackTap("share-done");

                            // Prepare the message
                            builder.setMessage(fResources.getString(R.string.fp_sharing_thank_you, FpLoyalty.this._shareMinutes * FpLoyalty.this.getCurrentLevel()));
                            builder.setPositiveButton(R.string.fp_sharing_button_rate, new OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Tracker.trackTap("share-rate");
                                    mContext.startService(new Intent(mContext, FpNotificationHelper.class));
                                }

                            });
                            break;
                        }

                        // Check the internet
                        Ads.checkForInternetLive();

                        // Offline mode
                        if (!Ads.isOnline) {
                            builder.setMessage(R.string.fp_sharing_internet);
                            builder.setPositiveButton(R.string.fp_sharing_button_ok, null);
                            break;
                        }

                        // Prepare the quote
                        String quote = fResources.getString(R.string.fp_sharing_quote_default);

                        // No song
                        if (null == fCurrentSong || null == fCurrentSong.title || null == fCurrentSong.artist || fCurrentSong.title.length() == 0 || fCurrentSong.artist.length() == 0) {
                            // Custom theme
                            if (!Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(currentTheme)) {
                                quote = fResources.getString(
                                    R.string.fp_sharing_quote_theme,
                                    Theme.Manager.getThemeInfo().themeName()
                                );
                            }
                        } else {
                            // Default theme
                            if (Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(currentTheme)) {
                                quote = fResources.getString(
                                    R.string.fp_sharing_quote_song,
                                    fCurrentSong.title,
                                    fCurrentSong.artist
                                );
                            } else {
                                quote = fResources.getString(
                                    R.string.fp_sharing_quote_theme_song,
                                    fCurrentSong.title,
                                    fCurrentSong.artist,
                                    Theme.Manager.getThemeInfo().themeName()
                                );
                            }
                        }

                        // Store the final values
                        final String fQuote = quote;
                        final String fPackageName = !Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(currentTheme) ? currentTheme : mContext.getPackageName();
                        final String fUrl = "https://play.google.com/store/apps/details?id=" + fPackageName + "&utm_source=app&utm_medium=share";

                        // Inform the user
                        builder.setMessage(R.string.fp_sharing_call_to_action);

                        // Add the button
                        builder.setPositiveButton(R.string.fp_sharing_button_share, new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    // Track this (potential) share event
                                    Tracker.trackShare(fPackageName);
                                    
                                    // Start the share intent
                                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                                    sharingIntent.setType("text/plain");
                                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mContext.getString(R.string.appName));
                                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fQuote + " " + fUrl);
                                    mContext.startActivity(Intent.createChooser(sharingIntent, "Share via"));

                                    // Out of the free zone
                                    if (!FpLoyalty.this.hasShared()) {
                                        // Mark this moment
                                        FpLoyalty.this.hasShared(true);

                                        // Thank the user
                                        FpLoyalty.this.sharingDialog(null, true);
                                    }
                                } catch (Exception e) {}
                            }

                        });

                    } while (false);

                    // Show the dialog
                    builder.show();
                } catch (Exception exc){}
            }
        }
        
        // Inform the user
        Toast.makeText(this.mContext, R.string.fp_sharing_loading, Toast.LENGTH_SHORT).show();
        
        // Execute the task
        new RetrieveThemeTask().execute(Theme.Manager.getCurrentTheme());
    }
}

/*EOF*/