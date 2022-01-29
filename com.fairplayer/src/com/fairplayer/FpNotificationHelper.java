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

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class  FpNotificationHelper extends Service {

    private Context mContext;
    private ViewGroup mPopupLayout;
    private ViewGroup mParentView;
    boolean first = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intentArg, int flags, int startId) {
        mContext = getBaseContext();
        
        // Context not set
        if (null == ActivityCommon.getContext()) {
            ActivityCommon.setContext(mContext);
        }
        
        // Invalid intent
        if (null == intentArg) {
            return super.onStartCommand(intentArg, flags, startId);
        }
        
        // Loyalty activity
        if (intentArg.getBooleanExtra("loyalty", false)) {
            // Prepare the intent 
            Intent intent = new Intent(mContext, ActivitySettings.class);

            // Start the Ads Settings activity
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ActivitySettings.SettingsFragmentAds.class.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            
            // Get the loyalty program
            FpLoyalty loyalty = new FpLoyalty(mContext);
            
            // Inform the user
            Toast.makeText(mContext, mContext.getString(R.string.fp_notification_loyalty_message, loyalty.getCurrentLevel()), Toast.LENGTH_LONG).show();
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.fairplayer"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
        
        return super.onStartCommand(intentArg, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

/*EOF*/