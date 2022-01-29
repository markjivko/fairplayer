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

import android.app.Activity;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class FpTutorial {

    protected static ShowListener sShowListener = null;

    public static abstract class ShowListener {
        public abstract void onItemEnd(int tutorialItemId);
    }
	
    /**
     * Check whether the tutorial is shown
     */
    public static boolean isShown(String tutorialType) {
        return PreferenceUtils.getBoolean(tutorialType, Constants.Defaults.SETTINGS_TUTORIAL_SHOWN);
    }

    /**
     * Set the On-Show Listener
     */
    public static void setOnShowListener(ShowListener sl) {
    	sShowListener = sl;
    }
    
    /**
     * Launch the tutorial
     */
    public static boolean show(Activity activity, int[][] items, final String tutorialType) {
    	// Store the activity
    	final Activity mActivity = activity;
    	
    	// Store the items
    	final int[][] mItems = items.clone();
    	
    	// Valid list
    	if (null != activity && null != items && items.length > 0) {
            // Tutorial not yet shown
            if (!PreferenceUtils.getBoolean(tutorialType, Constants.Defaults.SETTINGS_TUTORIAL_SHOWN)) {
                // Set the showcase view
            	new ShowcaseView
                    .Builder(mActivity)
                    .setTarget(new ViewTarget(mItems[0][0], mActivity))
                    .setContentTitle(mActivity.getString(mItems[0][1]))
                    .setContentText(mActivity.getString(mItems[0][2]))
                    .hideOnTouchOutside()
                    .setShowcaseEventListener(new OnShowcaseEventListener(){
                    	/**
                    	 * Item key
                    	 */
                    	protected int mItemsKey = 0;
                    	
                        @Override
                        public void onShowcaseViewHide(ShowcaseView showcaseView) {}

                        @Override
                        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                            // Catch the event
                            if (null != sShowListener) {
                                sShowListener.onItemEnd(mItems[mItemsKey][0]);
                            }
                        	
                            // Get the new items key
                            mItemsKey++;

                            // Valid key
                            do {
                                if (mItemsKey < mItems.length) {
                                    showcaseView.setShowcase(new ViewTarget(mItems[mItemsKey][0], mActivity), true);
                                    showcaseView.setContentTitle(mActivity.getString(mItems[mItemsKey][1]));
                                    showcaseView.setContentText(mActivity.getString(mItems[mItemsKey][2]));
                                    showcaseView.show();
                                    break;
                                }
                                
                                // Reset the key
                                mItemsKey = 0;

                                // Tutorial over
                                PreferenceUtils.edit().putBoolean(tutorialType, true).commit();

                                // Don't show ads for the next 10 seconds
                                Ads.touch();

                                // Tutorial over for now playing
                                if (tutorialType.equals(Constants.Keys.SETTINGS_TUTORIAL_NOWPLAYING)) {
                                    // Get the last theme
                                    final String lastTheme = Theme.Manager.getLastTheme();

                                    // Theme found
                                    if (null != lastTheme) {
                                        // Inform the user
                                        Toast.makeText(mActivity, mActivity.getString(R.string.settings_themes_applying_last), Toast.LENGTH_LONG).show();

                                        // Get the handler
                                        final Handler handler = new Handler();

                                        // Run after 1 second
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Set the current theme
                                                Theme.Manager.setCurrentTheme(lastTheme);
                                            }
                                        }, 500);
                                    }
                                }
                            } while (false);
                        }

                        @Override
                        public void onShowcaseViewShow(ShowcaseView showcaseView) {}

                        @Override
                        public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {}
                    })
                    .build();
                
                // Showing tutorial
                return true;
            }
    	}
    	
    	// Not showing any tutorial
    	return false;
    }
}

/*EOF*/