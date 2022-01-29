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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.view.MenuItem;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.psdev.licensesdialog.LicensesDialogFragment;

import android.util.Base64;
import android.util.Log;

/**
 * The preferences activity in which one can change application preferences.
 */
public class ActivitySettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Initialize the activity, loading the preference specifications.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.StyleActionBar);
        PreferenceUtils.getPreferences(this).registerOnSharedPreferenceChangeListener(this);
        super.onCreate(savedInstanceState);
        
        // Request the permissions
        FpPermissions.request(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceUtils.getPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        ArrayList<Header> tmp = new ArrayList<Header>();
        loadHeadersFromResource(R.xml.fp_settings, tmp); 
        for (Header obj : tmp) {
            target.add(obj);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(Constants.LOG_TAG, "Shared preferences changed (activitypreferences) " + key);
    }
    
    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes, int shortTitleRes) {
    	// Track the pageview
    	Tracker.trackPageview(this, fragmentName.replaceFirst("^.*?ActivitySettings\\$", ""));
    	return super.onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
    }
    
    public static class SettingsFragmentAds extends PreferenceFragment {
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fp_settings_ads);
        }
    }
    
    public static class SettingsFragmentThemes extends PreferenceFragment {

        protected Context mContext;
        
        // Buttons
        public static final String BUTTON_DEFAULT = "default";
        public static final String BUTTON_MORE = "more";

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Prepare the context
            mContext = getActivity();

            // Prepare the screen
            PreferenceScreen preferencesScreen = getPreferenceManager().createPreferenceScreen(mContext);

            // Prepare the package names list
            List<String> themesList = Theme.Manager.getThemesList();

            // Default theme
            preferencesScreen.addPreference(setButton(BUTTON_DEFAULT));
            
            // Get the shared preferences
            SharedPreferences preferences = PreferenceUtils.getPreferences(this.getActivity().getApplicationContext());
            
            // Get the stored string
            String storedJsonString = preferences.getString(Constants.Keys.SETTINGS_REMOTE_INFO, Constants.Defaults.SETTINGS_REMOTE_INFO);
            
            // Get the store
            try {
            	// Get the object
                JSONObject adInfo = new JSONObject(storedJsonString);
                
                // Get the store
                JSONArray storeInfo = adInfo.getJSONArray(Ads.AdInfo.STORE);
                
                // Go through each element
                for (int i = 0; i < storeInfo.length(); i++) {
                    // Get the item entry
                    JSONObject storeInfoItem = storeInfo.getJSONObject(i);

                    // Get the package name
                    String packageName = storeInfoItem.getString("p");
                    
                    // App found on device
                    boolean alreadyInstalled = false;
                    
                    // Locally installed themes
                    if (themesList.size() > 0) {
                        // Check the list
                        for (String packageNameInstalled : themesList) {
                            // Found a match
                            if (packageNameInstalled.equals(packageName)) {
                                alreadyInstalled = true;
                                break;
                            }
                        }
                    }

                    // Get the icon
                    Drawable packageIcon = null;

                    // Get the base 64
                    byte[] packageIconBase64 = Base64.decode(storeInfoItem.getString("i"), Base64.DEFAULT);

                    // Convert it to a bitmap
                    Bitmap packageIconBitmap = BitmapFactory.decodeByteArray(packageIconBase64, 0, packageIconBase64.length); 

                    // Get the crop
                    Bitmap packageIconBitmapCropped = Bitmap.createBitmap(packageIconBitmap, 0, 0, packageIconBitmap.getHeight(), packageIconBitmap.getHeight());

                    // Resize it
                    Bitmap packageIconBitmapResized = Bitmap.createScaledBitmap(packageIconBitmapCropped, 400, 400, false);
                    
                    // Convert the bitmap to a drawable
                    packageIcon = new BitmapDrawable(packageIconBitmapResized);

                    // Get the theme name
                    String packageTitle = storeInfoItem.getString("t");

                    // Add the button
                    preferencesScreen.addPreference(setButton(packageName, !alreadyInstalled, packageTitle, packageIcon));
                }
            } catch (Exception exc) {}
            
            // Get more themes
            preferencesScreen.addPreference(setButton(BUTTON_MORE));
            
            // Locally installed themes
            if (themesList.size() > 0) {
                for (String packageName : themesList) {
                    preferencesScreen.addPreference(setButton(packageName));
                }
            }

            // Set the screen
            setPreferenceScreen(preferencesScreen);
            
            // Prepare the ads
            try {
                Ads ads = Ads.getInstance((ActivityCommon) ActivityCommon.getActivity());
                ads.place(Ads.AdPlacement.ENTER_EQUALIZER);
            } catch (Exception e) {}
        }
        
        protected Preference setButton(String packageName) {
            return setButton(packageName, false, null, null);
        }
        
        protected Preference setButton(String packageName, boolean download, String customTitle, Drawable customIcon) {
            // Prepare the theme preference
            final Preference themePreference = new Preference(mContext);

            // Store the download flag
            final boolean needsDownload = download;
            
            // Set persistence
            themePreference.setPersistent(false);
            
            // Set the on-click listener
            themePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    do {
                        // Set the default theme
                    	if (themePreference.getKey().equals(BUTTON_DEFAULT)) {
                            Theme.Manager.setCurrentTheme("");
                            
                            // Inform the user
                            Context context = ActivityCommon.getContext();
                            if (null != context) {
                                Toast.makeText(context, context.getString(R.string.settings_themes_applying_default), Toast.LENGTH_LONG).show();
                            }
                            break;
                    	}
                    	
                    	// Get more themes
                    	if (themePreference.getKey().equals(BUTTON_MORE)) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            
                            // Get the shared preferences
                            SharedPreferences preferences = PreferenceUtils.getPreferences(mContext);

                            // Get the stored string
                            String storedJsonString = preferences.getString(Constants.Keys.SETTINGS_REMOTE_INFO, Constants.Defaults.SETTINGS_REMOTE_INFO);
            
                            // Prepare the author
                            String author = "Mark Jivko";
                            
                            // Get the store
                            try {
                                // Get the object
                                JSONObject adInfo = new JSONObject(storedJsonString);

                                // Get the publisher
                                String storeInfoPublisher = adInfo.getString(Ads.AdInfo.PUBLISHER);

                                // Valid publisher
                                if (null != storeInfoPublisher && storeInfoPublisher.length() > 0) {
                                    author = storeInfoPublisher;
                                }
                            } catch (Exception exc) {}
                            
                            // All done
                            marketIntent.setData(Uri.parse("market://search?q=pub:" + author));
                            
                            // Start the activity
                            startActivity(marketIntent);
                            
                            // Track this event
                            Tracker.trackMoreAuthor(author);
                            break;
                    	}
                    	
                    	// Download from Google Play
                    	if (needsDownload) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + themePreference.getKey()));
                            startActivity(marketIntent);
                            
                            // Track the event
                            Tracker.trackMoreApp(themePreference.getKey());
                            break;
                        }
                    	
                        // Apply the theme
                        Theme.Manager.setCurrentTheme(themePreference.getKey());
                        
                        // Inform the user
                        Context context = ActivityCommon.getContext();
                        if (null != context) {
                            Toast.makeText(context, context.getString(R.string.settings_themes_applying), Toast.LENGTH_LONG).show();
                        }
                    } while (false);
	                	
                    // All done
                    return true;
                }
            });

            // Prepare the title
            String title = null;
            String summary = null;
            Drawable icon = null;
            do {
            	if (packageName.equals(BUTTON_DEFAULT)) {
                    title = mContext.getString(R.string.settings_themes_default_title);
                    summary = mContext.getString(R.string.settings_themes_default_info);
                    
                    // Default theme
                    if (null == Theme.Manager.getCurrentTheme() || Theme.Manager.getCurrentTheme().length() == 0) {                    	
                    	// Mark as disabled
                        themePreference.setEnabled(false);
                    }
                    break;
            	}
            	
            	if (packageName.equals(BUTTON_MORE)) {
                    title = mContext.getString(R.string.settings_themes_more_title);
                    summary = mContext.getString(R.string.settings_themes_more_info);
                    break;
            	}
            	
            	if (needsDownload) {
                    title = mContext.getString(R.string.settings_themes_install_title) + " " + customTitle;
                    summary = mContext.getString(R.string.settings_themes_install_info);
                    icon = customIcon;
                    break;
            	}
            	
            	// Prepare the theme information
            	Theme.Manager.Info themeInfo = Theme.Manager.getThemeInfo(packageName);
            	
            	// Current theme
            	if (packageName.equals(Theme.Manager.getCurrentTheme())) {
                    // Set the title
                    title = themeInfo.themeName();

                    // Mark as disabled
                    themePreference.setEnabled(false);
            	} else {
                    title = mContext.getString(R.string.settings_themes_apply_title) + " " + themeInfo.themeName();
            	}
                
            	// Set the summary
            	summary = mContext.getString(R.string.settings_themes_apply_created_by) + " " + themeInfo.themeAuthor();
            	
                // Animated
                if (themeInfo.isAnimated()) {
                    summary = "(" + mContext.getString(R.string.settings_themes_animated) + ") " + summary;
                }
                
            	// Set the icon
            	icon = themeInfo.themeIcon();
            	
            } while (false);
            
            // Set the title
            themePreference.setTitle(title);
            
            // Set the summary
            themePreference.setSummary(summary);
            
            // Set the package name
            themePreference.setKey(packageName);
            
            // Set the icon
            themePreference.setIcon(icon);
            
            // All done
            return themePreference;
        }
    }
    
    public static class SettingsFragmentSoundControl extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fp_settings_sound_control);
        }
    }

    public static class SettingsFragmentTimeControl extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fp_settings_time_control);
        }
    }
    
    public static class SettingsFragmentScanner extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fp_settings_scanner);
        }
    }
    
    public static class SettingsFragmentAbout extends PreferenceFragment {
        
        protected Context mContext;
        
        // Buttons
        public static final String BUTTON_VERSION = "version";
        public static final String BUTTON_PRIVACY = "privacy";
        public static final String BUTTON_LICENSES = "licenses";
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Prepare the context
            mContext = getActivity();

            // Prepare the screen
            PreferenceScreen preferencesScreen = getPreferenceManager().createPreferenceScreen(mContext);
            
            // Prepare the author preference
            final Preference authorPreference = new Preference(mContext);

            // Set persistence
            authorPreference.setPersistent(false);
            
            // Set the title
            authorPreference.setTitle(mContext.getResources().getString(R.string.appName) + " v. " + mContext.getResources().getString(R.string.projectVersion));
            
            // Set the summary
            authorPreference.setSummary(mContext.getResources().getString(R.string.fp_notification_rate_message));
            
            // Set the package name
            authorPreference.setKey(BUTTON_VERSION);
            
            // Set the activity
            authorPreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mContext.startService(new Intent(mContext, FpNotificationHelper.class));
                    return false;
                }
            });
            
            // Get more themes
            preferencesScreen.addPreference(authorPreference);
            
            // Prepare the privacy policy preference
            final Preference privacyPolicyPreference = new Preference(mContext);

            // Set persistence
            privacyPolicyPreference.setPersistent(false);
            
            // Set the title
            privacyPolicyPreference.setTitle(mContext.getResources().getString(R.string.settings_section_about_privacy));
            
            // Set the package name
            privacyPolicyPreference.setKey(BUTTON_PRIVACY);
            
            // Set the activity
            privacyPolicyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://fairplayerteam.github.io/FairPlayer-SDK/PrivacyPolicy"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return false;
                }
            });
            
            // Get more themes
            preferencesScreen.addPreference(privacyPolicyPreference);

            // Prepare the licenses preference
            final Preference licensePreference = new Preference(mContext);
            
            // Set persistence
            licensePreference.setPersistent(false);
            
            // Set the title
            licensePreference.setTitle(mContext.getResources().getString(R.string.settings_licenses_title));

            // Create the license dialog
            final LicensesDialogFragment licenseFragment = new LicensesDialogFragment
                .Builder(mContext)
                .setNotices(R.raw.licenses)
                .setShowFullLicenseText(true)
                .build();
            
            // Set the dialog 
            licensePreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!licenseFragment.isVisible()) {
                        licenseFragment.show(ActivitySettings.SettingsFragmentAbout.this.getFragmentManager(), null);
                    }
                    return false;
                }
            });
            
            // Set the package name
            licensePreference.setKey(BUTTON_LICENSES);
            
            // Get more themes
            preferencesScreen.addPreference(licensePreference);
            
            // Set the screen
            setPreferenceScreen(preferencesScreen);
        }
    }

    public static class SettingsFragmentStatusBar extends PreferenceFragment {

        CheckBoxPreference cbStatusBarEnabled;
        CheckBoxPreference cbStatusBarNowPlaying;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fp_settings_status_bar);
        
            // Prepare the checkboxes
            cbStatusBarEnabled = (CheckBoxPreference) findPreference(Constants.Keys.SETTINGS_STATUS_BAR_ENABLED);
            cbStatusBarNowPlaying = (CheckBoxPreference) findPreference(Constants.Keys.SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED);
            
            // Listen to the main notification
            cbStatusBarEnabled.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    updateConfigWidgets();
                    return false;
                }
            });
            updateConfigWidgets();
        }

        private void updateConfigWidgets() {
            // Extra information is conditional on the status bar
            cbStatusBarNowPlaying.setEnabled(cbStatusBarEnabled.isChecked());
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}

/*EOF*/