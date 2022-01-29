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

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class FpPermissions {

    /**
     * Default permissions to request
     */
    @SuppressLint("InlinedApi")
    protected static String[] mPermissions = new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.WAKE_LOCK,
    };

    /**
     * Request the permissions
     */
    public static void request(Activity activity) {
        // Need to request permissions
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Get the ungranted permissions
            String[] ungrantedPermissions = FpPermissions.getUngrantedPermissions(activity, mPermissions);

            // Request the permissions			
            if(ungrantedPermissions.length > 0) {
                ActivityCompat.requestPermissions(activity, ungrantedPermissions, 1);
            }
        }
    }

    /**
     * Get the ungranted permissions
     */
    protected static String[] getUngrantedPermissions(Activity activity, String[] permissions) {
        // Prepare the result
        List<String> result = new ArrayList<String>();

        // Go through the permissions
        for(int i = 0; i < permissions.length; i++) {
            // Permission not granted yet
            if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, permissions[i])) {
                result.add(permissions[i]);
            }
        }

        // All done
        return result.toArray(new String[0]);
    }
}

/*EOF*/