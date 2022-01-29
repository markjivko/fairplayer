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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class FpThemeOfTheDay {

    // Context
    protected Context mContext = null;

    // Theme installed
    protected boolean _themeInstalled = false;

    // Theme package name
    protected String _themePackageName = null;

    // Theme title
    protected String _themeTitle = null;

    // Instance
    protected static FpThemeOfTheDay _instance = null;

    /**
     * Singleton constructor
     */
    protected FpThemeOfTheDay(Context context) {
        try {
            mContext = context.getApplicationContext();
        } catch (Exception exc) {}
    }

    /**
     * Get a "Theme of the day" instance
     */
    public static FpThemeOfTheDay getInstance(Context context) {
        if(null == _instance) {
            _instance = new FpThemeOfTheDay(context);
        }

        // Get the infor
        _instance._getInfo();

        // All done
        return _instance;
    }

    /**
     * Check theme is available
     */
    public boolean isAvailable() {
        return null != this._themePackageName;
    }

    /**
     * Check theme is installed
     */
    public boolean isInstalled() {
        return this._themeInstalled;
    }

    /**
     * Get theme title
     */
    public String getTitle() {
        return this._themeTitle;
    }

    /**
     * Get the theme package name
     */
    public String getPackageName() {
        return this._themePackageName;
    }

    /**
     * Download action; direct to Google Play or to the Themes list if no theme defined
     */
    public void startIntent() {
        // No context provided
        if(null == mContext) {
            return;
        }

        // We have a theme
        if(this.isAvailable()) {
            // Prepare the message
            String message = mContext.getResources().getString(R.string.fp_menu_theme_of_the_day_check);

            // Valid theme name provided
            if(null != this.getTitle() && this.getTitle().length() > 0) {
                message = mContext.getResources().getString(R.string.fp_menu_theme_of_the_day_check_name, this.getTitle());
            }

            // Already installed
            if(this.isInstalled()) {
                // Apply the theme of the day
                if(!this.getPackageName().equals(Theme.Manager.getCurrentTheme())) {
                    Theme.Manager.setCurrentTheme(this.getPackageName());

                    // Inform the user
                    Toast.makeText(mContext, R.string.settings_themes_applying, Toast.LENGTH_LONG).show();
                    return;
                }

                // Prepare the rating string
                message = mContext.getResources().getString(R.string.fp_menu_theme_of_the_day_rate);

                // Valid theme name provided
                if(null != this.getTitle() && this.getTitle().length() > 0) {
                    message = mContext.getResources().getString(R.string.fp_menu_theme_of_the_day_rate_name, this.getTitle());
                }
            }

            // Prepare the dialog
            try {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityCommon.getActivity());
                dialog.setTitle(R.string.fp_menu_theme_of_the_day);
                dialog
                    .setMessage(message)
                    .setPositiveButton(this.isInstalled() ? R.string.fp_menu_theme_of_the_day_rate_button : R.string.fp_menu_theme_of_the_day_check_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Prepare Play Store download
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            marketIntent.setData(Uri.parse("market://details?id=" + FpThemeOfTheDay.this._themePackageName));
                            mContext.startActivity(marketIntent);

                            // Track the event
                            Tracker.trackMoreApp(FpThemeOfTheDay.this._themePackageName);
                        }
                    })
                    .setNegativeButton(R.string.fp_menu_theme_of_the_day_no, null);

                dialog.create().show();
            } catch(Exception exc) {
            }

            // Stop here
            return;
        }

        // Go to the themes directory
        try {
            Intent intent = new Intent(ActivityCommon.getActivity(), ActivitySettings.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ActivitySettings.SettingsFragmentThemes.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            ActivityCommon.getActivity().startActivity(intent);
        } catch(Exception exc) {
        }
    }

    /**
     * Get first item information
     */
    protected void _getInfo() {
        // No context provided
        if(null == mContext) {
            return;
        }

        // Initialize the values
        this._themePackageName = null;
        this._themeTitle = null;
        this._themeInstalled = false;

        // Get the store
        try {
            // Prepare the package names list
            List<String> themesList = Theme.Manager.getThemesList();

            // Get the shared preferences
            SharedPreferences preferences = PreferenceUtils.getPreferences(mContext);

            // Get the stored string
            String storedJsonString = preferences.getString(Constants.Keys.SETTINGS_REMOTE_INFO, Constants.Defaults.SETTINGS_REMOTE_INFO);

            // Get the object
            JSONObject adInfo = new JSONObject(storedJsonString);

            // Get the store
            JSONArray storeInfo = adInfo.getJSONArray(Ads.AdInfo.STORE);

            // Get the item entry
            JSONObject storeInfoItem = storeInfo.getJSONObject(0);

            // Get the package name
            this._themePackageName = storeInfoItem.getString("p");

            // Locally installed themes
            if(themesList.size() > 0) {
                // Check the list
                for(String packageNameInstalled : themesList) {
                    // Found a match
                    if(packageNameInstalled.equals(this._themePackageName)) {
                        this._themeInstalled = true;
                        break;
                    }
                }
            }

            // Get the theme name
            this._themeTitle = storeInfoItem.getString("t");
        } catch(Exception exc) {
        }
    }

}

/*EOF*/