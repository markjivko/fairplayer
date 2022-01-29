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

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Set;

public class PreferenceUtils {

    /**
     * Preferences
     */
    private static SharedPreferences mPreferences = null;

    /**
     * Get the preferences
     */
    public static SharedPreferences getPreferences() {
        try {
            // Prepare the preferences
            return getPreferences(ActivityCommon.getContext());
        } catch(IllegalStateException exc) {}

        // Something went wrong
        return mPreferences;
    }

    /**
     * Get the preferences for the specified context
     */
    public static SharedPreferences getPreferences(Context context) {
        // Needs initialization
        if(null == mPreferences && null != context) {
            // Prepare the preferences
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        // All done
        return mPreferences;
    }

    /**
     * Preferences editor
     */
    public static SharedPreferences.Editor edit() {
        return getPreferences().edit();
    }

    /**
     * Get a boolean
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    /**
     * Get a string
     */
    public static String getString(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    /**
     * Get an integer
     */
    public static int getInt(String key, int defaultValue) {
        return getPreferences().getInt(key, defaultValue);
    }

    /**
     * Get a long
     */
    public static long getLong(String key, long defaultValue) {
        return getPreferences().getLong(key, defaultValue);
    }

    /**
     * Get a string set
     */
    public static Set<String> getStringSet(String key, Set<String> defaultValue) {
        return getPreferences().getStringSet(key, defaultValue);
    }

    /**
     * Get a float array
     */
    public static float[] getFloatArray(String key) {
        float[] array = null;
        String s = getPreferences().getString(key, null);
        if(s != null) {
            try {
                JSONArray json = new JSONArray(s);
                array = new float[json.length()];
                for(int i = 0; i < array.length; i++) {
                    array[i] = (float) json.getDouble(i);
                }
            } catch(JSONException e) {
                Log.e(Constants.LOG_TAG, "getFloatArray: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return array;
    }

    public static void putFloatArray(SharedPreferences.Editor editor, String key, float[] array) {
        try {
            JSONArray json = new JSONArray();
            for(float f : array) {
                json.put(f);
            }
            editor.putString(key, json.toString());
            editor.commit();
        } catch(JSONException e) {
            Log.e(Constants.LOG_TAG, "putFloatArray: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }
}

/*EOF*/