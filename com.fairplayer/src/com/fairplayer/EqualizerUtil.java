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

import org.videolan.libvlc.MediaPlayer;

public class EqualizerUtil {

    /**
     * Current equalizer
     */
    protected static MediaPlayer.Equalizer sEqualizer = null;
    
    /**
     * Update the media player's equalizer
     */
    public static void updatePlayer(MediaPlayer mediaPlayer) {
    	// Player found
    	if (null != mediaPlayer) {
            // Set the equalizer
            mediaPlayer.setEqualizer(PreferenceUtils.getBoolean(Constants.Keys.SETTINGS_EQUALIZER_ENABLED, Constants.Defaults.SETTINGS_EQUALIZER_ENABLED) ? getEqualizer() : null);
    	}
    }
    
    /**
     * Set a new equalizer
     */
    public static void setEqualizer(MediaPlayer.Equalizer equalizer) {
    	sEqualizer = equalizer;
    }
    
    /**
     * Get the current equalizer
     */
    public static MediaPlayer.Equalizer getEqualizer() {
    	// Not set, try a refresh
    	if (null == sEqualizer) {
            sEqualizer = refreshEqualizer();
    	}
    	
    	// All done
    	return sEqualizer;
    }
    
    /**
     * Get the settings
     */
    public static MediaPlayer.Equalizer refreshEqualizer() {
        // Get the bands
        float[] bands = PreferenceUtils.getFloatArray(Constants.Keys.SETTINGS_EQUALIZER_VALUES);
        
        // Get the band count
        final int bandCount = MediaPlayer.Equalizer.getBandCount();

        // Settings are not set or invalid
        if (null == bands || bands.length != bandCount + 1) {
            // Get the default band count
            bands = new float[bandCount + 1];
            for (int i = 0; i <= bandCount; i++) {
            	bands[i] = 0;
            }
            
            // Store the default
            PreferenceUtils.putFloatArray(PreferenceUtils.edit(), Constants.Keys.SETTINGS_EQUALIZER_VALUES, bands);
        }
        
        // Prepare the equalizer
        final MediaPlayer.Equalizer equalizer = MediaPlayer.Equalizer.create();
        
        // Set the pre-amp
        equalizer.setPreAmp(bands[0]);
        
        // Set the amplification
        for (int i = 0; i < bandCount; ++i) {
            equalizer.setAmp(i, bands[i + 1]);
        }
        
        // All done
        return equalizer;
    }
}

/*EOF*/