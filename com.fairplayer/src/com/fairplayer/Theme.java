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

import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Theme handler
 */
public class Theme {
    
    /**
     * Theme resources
     */
    public static class Resources {
        /**
         * Class initialized flag
         */
        protected static boolean _initialized = false;

        /**
         * Local context
         */
        protected static Context _localContext = null;

        /**
         * Local resources
         */
        protected static android.content.res.Resources _locaResources = null;

        /**
         * Remote package name
         */
        protected static String _themePackageName = null;

        /**
         * Remote resources
         */
        protected static android.content.res.Resources _themeResources = null;

        /**
         * Remote context
         */
        protected static Context _themeContext = null;

        /**
         * Invalidate the current theme
         */
        public static void invalidate() {
            _initialized = false;
        }
        
        /**
         * Theme constructor
         */
        protected static void _init() {
            // Alread initialized
            if (_initialized) {
                return;
            }

            // Set the theme package name
            _themePackageName = Theme.Manager.getCurrentTheme();

            // Set the local context
            _localContext = ActivityCommon.getContext();

            // Set the local resources
            _locaResources = _localContext.getResources();

            // Set the theme context
            _themeContext = null;
            
            // Set the theme resources
            _themeResources = null;
            
            // Valid package name
            if (null != _themePackageName && _themePackageName.length() > 0) {
                    // Set the theme context
                    try {
                        _themeContext = _localContext.createPackageContext(
                            _themePackageName,
                            Context.CONTEXT_INCLUDE_CODE + Context.CONTEXT_IGNORE_SECURITY
                        );
                    } catch (Exception e) {
                        Log.i(Constants.LOG_TAG, "Error (theme context): " + String.valueOf(e));
                    }

                    // Set the theme resources
                    try {
                        _themeResources = _localContext.getPackageManager().getResourcesForApplication(_themePackageName);
                    } catch (Exception e) {
                        Log.i(Constants.LOG_TAG, "Error (theme resources): " + String.valueOf(e));
                    }
            }

            // All done
            _initialized = true;
        }

        /**
         * Get a value from the remote theme or revert to the default local one
         */
        protected static Object _getValue(int localId, String methodName) {
            // Initialize
            _init();

            // Prepare the result
            Object result = null;

            // Prepare the remote ID
            int remoteId = _getRemoteId(localId);

            do {
                // Valid remote ID
                if (0 != remoteId && null != _themeResources) {
                    // Get the remote value
                    try {
                        // Prepare the method
                        Method remoteMethod = _themeResources.getClass().getMethod(methodName, int.class);

                        // Set the result
                        result = remoteMethod.invoke(_themeResources, remoteId);

                        // Prevent reverting to the default local value
                        break;
                    } catch (Exception excRemote) {
                        Log.i(Constants.LOG_TAG, "Error (Theme:" + methodName + " remote) " + String.valueOf(excRemote));
                    }
                }

                // Get the local value
                try {
                    // Prepare the method
                    Method localMethod = _localContext.getResources().getClass().getMethod(methodName, int.class);

                    // Set the result
                    result = localMethod.invoke(_localContext.getResources(), localId);
                } catch (Exception excLocal) {
                    Log.i(Constants.LOG_TAG, "Error (Theme:" + methodName + " local) " + String.valueOf(excLocal));
                }
            } while(false);

            // Prepare the default result string
            String resultString = (null != result ? result.toString() : "null");

            // Color
            if (methodName.equals("getColor")) {
                resultString = (null != result ? String.format(Locale.US, "#%08X", 0xFFFFFFFF & Integer.class.cast(result)) : "null");
            }

            // Log the information
            Log.d(Constants.LOG_TAG, "Theme:" + methodName + "(" + (0 != remoteId && null != _themeResources ? "R" + String.valueOf(remoteId) : "L" + String.valueOf(localId)) + ") = " + resultString);

            // All done
            return result;
        }

        /**
         * Get the resource ID from the remote package
         */
        protected static int _getRemoteId(int localId) {
            // Prepare the result
            int result = 0;

            // Get the remote ID
            if (null != _themeResources) {
                try {
                    result = _themeResources.getIdentifier(
                        _localContext.getResources().getResourceEntryName(localId),
                        _localContext.getResources().getResourceTypeName(localId),
                        _themePackageName
                    );
                } catch (Exception e) {
                    Log.i(Constants.LOG_TAG, "Error (theme _getRemoteId): " + String.valueOf(e));
                }
            }

            // Log the data
            if (null != _themePackageName && _themePackageName.length() > 0) {
                Log.d(Constants.LOG_TAG, "Theme:getRemoteId(" + _themePackageName + ":" + _localContext.getResources().getResourceTypeName(localId) + "/" + _localContext.getResources().getResourceEntryName(localId) + ") = " + String.valueOf(result));
            }

            // All done
            return result;
        }

        /**
         * Theme context
         */
        public static Context getContext() {
            _init();
            return _themeContext;
        }

        /**
         * Theme resources
         */
        public static android.content.res.Resources get() {
            _init();
            return _themeResources;
        }

        /**
         * Theme package name
         */
        public static String getPackageName() {
            _init();
            return _themePackageName;
        }

        /**
         * Get the animation frames
         */
        public static List<Integer> getAnimationFrames(String framePrefix) {
            // Prepare the result
        	List<Integer> result = new ArrayList<Integer>();
            
            // Prepare the index
            int index = 0;
        	
            // Remote mode
            if (null != _themePackageName && _themePackageName.length() > 0 && null != _themeResources) {
            	do {
                    try {
                        // Found a remote ID
                        int remoteId = _themeResources.getIdentifier(
                            framePrefix + "_" + index,
                            "drawable",
                            _themePackageName
                        );
                        
                        // Invalid id
                        if (0 == remoteId) {
                        	break;
                        }
                        
                        // Store the remote ID
                        result.add(remoteId);

                        // Increment
                        index++;
                    } catch (Exception e) {
                        // End of the line
                        break;
                    }
            	} while (true);
            } else {
            	do {
                    try {
                        // Found a local ID
                        int localId = _locaResources.getIdentifier(
                            framePrefix + "_" + index,
                            "drawable",
                            _localContext.getPackageName()
                        );

                        // Invalid id
                        if (0 == localId) {
                        	break;
                        }
                        
                        // Store the local ID
                        result.add(localId);
                        
                        // Increment
                        index++;
                    } catch (Exception e) {
                        // End of the line
                        break;
                    }
            	} while (true);
            }

            // All done
            return result;
        }
        
        /**
         * Get a color
         */
        public static int getColor(int localId) {
            return Integer.class.cast(_getValue(localId, "getColor"));
        }

        /**
         * Get a boolean
         */
        public static boolean getBoolean(int localId) {
            return Boolean.class.cast(_getValue(localId, "getBoolean"));
        }

        /**
         * Get a dimension
         */
        public static float getDimension(int localId) {
            return Float.class.cast(_getValue(localId, "getDimension"));
        }

        /**
         * Get a drawable
         */
        public static Drawable getDrawable(int localId) {
            return Drawable.class.cast(_getValue(localId, "getDrawable"));
        }

        /**
         * Get an integer
         */
        public static int getInteger(int localId) {
            return Integer.class.cast(_getValue(localId, "getInteger"));
        }

        /**
         * Get an integer array
         */
        public static int[] getIntArray(int localId) {
            return int[].class.cast(_getValue(localId, "getIntArray"));
        }

        /**
         * Get a string
         */
        public static String getString(int localId) {
            return String.class.cast(_getValue(localId, "getString"));
        }

        /**
         * Get a string array
         */
        public static String[] getStringArray(int localId) {
            return String[].class.cast(_getValue(localId, "getStringArray"));
        }

        /**
         * Get a layout
         */
        public static XmlResourceParser getLayout(int localId) {
            return XmlResourceParser.class.cast(_getValue(localId, "getLayout"));
        }
    }
    
    /**
     * Theme packages manager
     */
    public static class Manager {
        
        /**
         * Theme information
         */
        protected static class Info {
            /**
             * Compatibility types
             */
            public static final String COMPATIBILITY_POWERAMP = "PowerAmp";
            public static final String COMPATIBILITY_PLAYERPRO = "PlayerPro";
            public static final String COMPATIBILITY_STANDALONE = "Standalone";
            
            /**
             * Local context
             */
            protected Context _localContext = null;
            
            /**
             * Theme context
             */
            protected Context _remoteContext = null;
            
            /**
             * Theme package name
             */
            protected String _remotePackageName = null;
            
            /**
             * Theme resources
             */
            protected android.content.res.Resources _remoteResources = null;
            
            /**
             * Author name
             */
            protected String _themeAuthor = null;
            
            /**
             * Theme name
             */
            protected String _themeName = null;
            
            /**
             * Theme compatibility
             */
            protected String _themeCompatibility = null;
            
            /**
             * Theme icon
             */
            protected Drawable _themeIcon = null;
            
            /**
             * Theme is animated
             */
            protected boolean _themeAnimated = false;
            
            /**
             * Constructor
             */
            public Info (String packageName) {
            	// Initializer
            	this._init(packageName);
                
            	// Get the name
            	this._getThemeName();
            	
            	// Get the author
            	this._getThemeAuthor();
            	
            	// Get the theme compatibility
            	this._getThemeCompatibility();
            	
            	// Get the icon
                this._getThemeIcon();
                
                // Get wether the theme is animated
                this._getThemeAnimated();
            }
            
            /**
             * Get the theme name
             */
            protected void _getThemeName() {
            	// Get the icon remote ID
            	int remoteId = this._getRemoteId(R.string.appName);
            	
            	// Valid remote ID
            	if (0 != remoteId && null != this._remoteResources) {
                    try {
                        this._themeName = this._remoteResources.getString(remoteId);
                    } catch (Exception exc) {}
            	}
            }
            
            /**
             * Get the theme author
             */
            protected void _getThemeAuthor() {
            	// Get the icon remote ID
            	int remoteId = this._getRemoteId(R.string.authorName);
            	
            	// Valid remote ID
            	if (0 != remoteId && null != this._remoteResources) {
                    try {
                        this._themeAuthor = this._remoteResources.getString(remoteId);
                    } catch (Exception exc) {}
            	}
            }
            
            /**
             * Get the theme compatibility
             */
            protected void _getThemeCompatibility() {
            	do {
                    // PowerAmp theme
                    if (this._remotePackageName.matches("^com\\.poweramp\\.theme.*")) {
                        this._themeCompatibility = Theme.Manager.Info.COMPATIBILITY_POWERAMP;
                        break;
                    }

                    // PlayerPro theme
                    if (this._remotePackageName.matches("^com\\.tbig\\.playerpro\\.skins.*")) {
                        this._themeCompatibility = Theme.Manager.Info.COMPATIBILITY_PLAYERPRO;
                        break;
                    }

                    // Standalone app
                    this._themeCompatibility = Theme.Manager.Info.COMPATIBILITY_STANDALONE;
            	} while (false);
            }
            
            /**
             * Get the theme icon
             */
            protected void _getThemeIcon() {
            	// Get the icon remote ID
            	int remoteId = this._getRemoteId(R.drawable.icon);
            	
            	// Valid remote ID
            	if (0 != remoteId && null != this._remoteResources) {
                    try {
                        this._themeIcon = this._remoteResources.getDrawable(remoteId);
                    } catch (Exception exc) {}
            	}
            }
            
            /**
             * Get wether the theme is animated
             */
            protected void _getThemeAnimated() {
            	// Get the first frame ID
            	int remoteId = this._getRemoteId(R.drawable.fp_frame_animation_0);
            	
            	// Valid remote ID
            	if (0 != remoteId && null != this._remoteResources) {
                    try {
                    	// Try to get the drawable
                    	this._remoteResources.getDrawable(remoteId);
                        
                    	// Animations are supported
                    	this._themeAnimated = true;
                    } catch (Exception exc) {}
            	}
            }
            
            /**
             * Initialize the info panel
             */
            protected void _init(String packageName) {
            	// Store the package name
            	this._remotePackageName = packageName;
            	
            	// Store the local context
            	this._localContext = ActivityCommon.getContext();
            	
            	// Set the theme context
                try {
                    this._remoteContext = this._localContext.createPackageContext(
                        packageName,
                        Context.CONTEXT_INCLUDE_CODE + Context.CONTEXT_IGNORE_SECURITY
                    );
                } catch (Exception e) {
                    Log.i(Constants.LOG_TAG, "Error (theme context): " + String.valueOf(e));
                }

                // Set the theme resources
                try {
                    this._remoteResources = _localContext.getPackageManager().getResourcesForApplication(packageName);
                } catch (Exception e) {
                    Log.i(Constants.LOG_TAG, "Error (theme resources): " + String.valueOf(e));
                }
            }
            
            /**
             * Get a remote ID
             */
            protected int _getRemoteId(int localId) {
                // Prepare the result
                int result = 0;

                // Get the remote ID
                if (null != this._remoteResources) {
                    try {
                        result = this._remoteResources.getIdentifier(
                            _localContext.getResources().getResourceEntryName(localId),
                            _localContext.getResources().getResourceTypeName(localId),
                            _remotePackageName
                        );
                    } catch (Exception e) {
                        Log.i(Constants.LOG_TAG, "Error (theme _getRemoteId): " + String.valueOf(e));
                    }
                }

                // All done
                return result;
            }
            
            /**
             * Theme author
             */
            public String themeAuthor() {
                return _themeAuthor;
            }
            
            /**
             * Theme name
             */
            public String themeName() {
                return _themeName;
            }
            
            /**
             * Theme compatibility
             */
            public String themeCompatibility() {
                return _themeCompatibility;
            }
            
            /**
             * Theme icon
             */
            public Drawable themeIcon() {
                return _themeIcon;
            }
            
            /**
             * Theme is animated
             */
            public boolean isAnimated() {
            	return _themeAnimated;
            }
        }
        
        /**
         * Constants
         */
        public static final String THEME_META_KEY = "com.fp.theme";
        public static final String THEME_INTENT_KEY = "packageName";
        
        /**
         * Check if the package is installed and compatible
         */
        public static boolean isThemeInstalled(String themePackageName) {
            // Package not set
            if (null == themePackageName || themePackageName.length() == 0) {
                return false;
            }

            // Try to create the context
            try {
                ActivityCommon.getContext().createPackageContext(themePackageName, 2);
            } catch (Exception exception) {
                return false;
            }

            // Get the theme meta
            try {
                ApplicationInfo appInfo = ActivityCommon.getContext().getPackageManager().getApplicationInfo(themePackageName, PackageManager.GET_META_DATA);

                // Could not get metadata
                if (appInfo == null || appInfo.metaData == null) {
                    return false;
                }

                // Get the meta key
                Object metaKey = appInfo.metaData.get(THEME_META_KEY);

                // Expected meta key
                if (null == metaKey) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            // Valid theme
            return true;
        }

        /**
         * Get a theme's information
         */
        public static Theme.Manager.Info getThemeInfo(String packageName) {
            // Not a valid theme
            if (!isThemeInstalled(packageName)) {
                return null;
            }
            
            // Get the information
            return new Theme.Manager.Info(packageName);
        }
        
        /**
         * Get the current theme's information
         */
        public static Theme.Manager.Info getThemeInfo() {
            // Get the current theme
            String packageName = getCurrentTheme();
            
            // Get the available information
            return getThemeInfo(packageName);
        }

        /**
         * Get the last updated theme
         */
        public static String getLastTheme() {
            // Get the themes list
            List<String> themes = getThemesList();

            // Prepare the result
            String result = null;

            // Prepare the timestamp
            long lastUpdatedMax = 0;

            // Valid themes
            if (themes.size() > 0) {
                // Go through the themes
                for (String packageName : themes) {
                    try {
                        // Get the last updated time
                        long lastUpdated = ActivityCommon.getContext().getPackageManager().getPackageInfo(packageName, 0).lastUpdateTime;

                        // Found a more recent app
                        if (lastUpdated > lastUpdatedMax) {
                            // Store the flag
                            lastUpdatedMax = lastUpdated;

                            // Update the result
                            result = packageName;
                        }
                    } catch (PackageManager.NameNotFoundException e) {}
                }
            }

            // All done
            return result;
        }
        
        /**
         * Get the themes list
         */
        public static List<String> getThemesList() {
            // Prepare the package manager
            PackageManager packageManager = ActivityCommon.getContext().getPackageManager();

            // Get a list of installed apps
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            // Prepare the result
            List<String> result = new ArrayList<String>();

            // Go through the packages
            for (ApplicationInfo packageInfo : packages) {
                do {
                    try {
                        // Could not get metadata
                        if (packageInfo == null || packageInfo.metaData == null) {
                            break;
                        }

                        // Get the meta key
                        Object metaKey = packageInfo.metaData.get(THEME_META_KEY);

                        // Expected meta key
                        if (null == metaKey) {
                            break;
                        }
                    } catch (Exception e) {
                        break;
                    }

                    // Add to the result
                    result.add(packageInfo.packageName);
                } while (false);
            }

            // All done
            return result;
        }

        /**
         * Get the current theme's package name
         */
        public static String getCurrentTheme() {
            // All done
            return PreferenceUtils.getString(Constants.Keys.SETTINGS_THEME_PACKAGE_NAME, Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME);
        }

        /**
         * Set the current theme's package name
         */
        public static void setCurrentTheme(String themePackageName) {
            // Reset mode
            if (null == themePackageName) {
                themePackageName = Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME;
            }

            // Force a redraw
            Theme.Resources.invalidate();
            
            // Clear the covers cache
            ElementSmallCover.clearCache();
            
            // Needs a reset or a valid package name
            if (themePackageName.length() == 0 || isThemeInstalled(themePackageName)) {
                // All done
                Editor editor = PreferenceUtils.edit();

                // Set the package name
                editor.putString(Constants.Keys.SETTINGS_THEME_PACKAGE_NAME, themePackageName);

                // Commit
                editor.commit();
            }
            
            // Prepare the context
            Context context = ActivityCommon.getContext();
            if (null != context) {
                // Update the widgets
                FpServiceRendering.get(context).updateWidgets();
                
                // Update the notification
                FpServiceRendering.get(context).updateNotification();
                
                // Prepare the Alarm Manager
                AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                
                // Set the pending intent for the main activity in 400ms
                alarmManager.set(
                    AlarmManager.RTC, 
                    System.currentTimeMillis() + 400, 
                    PendingIntent.getActivity(
                        context, 
                        123456, 
                        new Intent(context, ActivityNowplaying.class), 
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                );

                // Finish the current main activity
                Activity activity = ActivityCommon.getActivity();
                if (null != activity) {
                    activity.finish();
                }
            }
        }
    }
}

/*EOF*/