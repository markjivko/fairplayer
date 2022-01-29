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

// Application constants
public class Constants {
    
    // Log Tag
    public static final String LOG_TAG = "ThemeTag";
    
    // Lockscreen listener
    public static final String ANDROID_LOCKSCREEN_LISTENER = "com.android.music.playstatechanged";
    
    // Notification parameters
    public static class Notification {
        public static final String SENT = "notification_sent_x";
        public static class Default {
            public static final int ID = 1;
            public static final int TIME = 10800;
        }
        
        public static class Loyalty {
            public static final int ID = 2;
            public static final int TIME = 0;
        }
        
        public static class UpdateCheck {
            public static final int ID = 3;
            public static final int TIME = 0; 
        }
    }
    
    // Settings keys
    public static class Keys {
        // Status bar
        public static final String SETTINGS_STATUS_BAR_ENABLED = "settings_status_bar_enabled";
        public static final String SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED = "settings_status_bar_now_playing_enabled";

        // Time control
        public static final String SETTINGS_SEEK_STEP = "settings_seek_step";
        public static final String SETTINGS_TIMEOUT_ENABLED = "settings_timeout_enabled";
        public static final String SETTINGS_TIMEOUT_VALUE = "settings_timeout_value";

        // Sound control
        public static final String SETTINGS_EQUALIZER_ENABLED = "settings_equalizer_enabled";
        public static final String SETTINGS_EQUALIZER_VALUES = "settings_equalizer_values";
        public static final String SETTINGS_EQUALIZER_PRESET = "settings_equalizer_preset";
        public static final String SETTINGS_EQUALIZER_CUSTOM_PRESETS = "settings_equalizer_custom_presets";
        public static final String SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS = "settings_equalizer_custom_presets_bands";
        public static final String SETTINGS_BLUR_KEEP_PLAYING_ENABLED = "settings_blur_keep_playing_enabled";
        public static final String SETTINGS_BLUR_KEEP_PLAYING_VOLUME = "settings_blur_keep_playing_volume";
        
        // Themes
        public static final String SETTINGS_THEME_PACKAGE_NAME = "settings_theme_package_name";

        // System root
        public static final String SETTINGS_ROOT = "settings_root";
        
        // Library page
        public static final String SETTINGS_LIBRARY_PAGE = "settings_library_page";

        // Ads
        public static final String SETTINGS_ADS_CURRENT_LEVEL = "settings_ads_current_level";
        public static final String SETTINGS_ADS_PERECENT = "settings_ads_percent";
        public static final String SETTINGS_ADS_FREE_CHECKPOINT = "settings_ads_free_checkpoint";
        
        // Tutorial
        public static final String SETTINGS_TUTORIAL_LIBRARY = "settings_tutorial_library";
        public static final String SETTINGS_TUTORIAL_NOWPLAYING = "settings_tutorial_nowplaying";
        
        // Remote information
        public static final String SETTINGS_REMOTE_INFO = "settings_remote_info";
        public static final String SETTINGS_REMOTE_INFO_EXPIRY = "settings_remote_info_expiry";
        
        // Update Checker
        public static final String SETTINGS_UPDATE_CHECKER_VERSION = "settings_update_checker_version";
        public static final String SETTINGS_UPDATE_CHECKER_LAST_CHECK = "settings_update_checker_last_check";
        
        // Sharing
        public static final String SETTINGS_SHARING_REWARD_TIME = "settings_sharing_reward_time";
    }
    
    // Settings defaults
    public class Defaults {
        // Status bar
        public static final boolean SETTINGS_STATUS_BAR_ENABLED = true;
        public static final boolean SETTINGS_STATUS_BAR_NOW_PLAYING_ENABLED = true;

        // Time control
        public static final int SETTINGS_SEEK_STEP = 10;
        public static final boolean SETTINGS_TIMEOUT_ENABLED = false;
        public static final int SETTINGS_TIMEOUT_VALUE = 3600;
        
        // Sound control
        public static final boolean SETTINGS_EQUALIZER_ENABLED = true;
        public static final int SETTINGS_EQUALIZER_PRESET = 0;
        public static final String SETTINGS_EQUALIZER_CUSTOM_PRESETS = "{}";
        public static final String SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS = "{}";
        public static final boolean SETTINGS_BLUR_KEEP_PLAYING_ENABLED = false;
        public static final int SETTINGS_BLUR_KEEP_PLAYING_VOLUME = 50;

        // Themes
        public static final String SETTINGS_THEME_PACKAGE_NAME = "";
        
        // System root
        public static final String SETTINGS_ROOT = "";
        
        // Tutorial
        public static final boolean SETTINGS_TUTORIAL_SHOWN = false;
        
        // Ads
        public static final int SETTINGS_ADS_CURRENT_LEVEL = -1;
        public static final int SETTINGS_ADS_PERECENT = 100;
        public static final long SETTINGS_ADS_FREE_CHECKPOINT = 0;
        
        // Remote information
        public static final String SETTINGS_REMOTE_INFO = "";
        public static final int SETTINGS_REMOTE_INFO_EXPIRY = 0;
        
        // Update checker
        public static final String SETTINGS_UPDATE_CHECKER_VERSION = "";
        public static final long SETTINGS_UPDATE_CHECKER_LAST_CHECK = 0;
        
        // Sharing
        public static final long SETTINGS_SHARING_REWARD_TIME = 0l;
    }
}

/*EOF*/