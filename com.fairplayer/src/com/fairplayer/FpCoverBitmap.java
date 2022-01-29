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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

/**
 * Class containing utility functions to create Bitmap album art
 */
public final class FpCoverBitmap {

    /**
     * Create an image representing the given song
     */
    public static Bitmap createBitmap(Context context, Bitmap coverArt, FpTrack song, int width, int height) {
    	// Get the source dimensions
    	int sourceWidth = coverArt.getWidth();
        int sourceHeight = coverArt.getHeight();
        
        // Get the minimum scale
        float scale = Math.min((float) width / sourceWidth, (float) height / sourceHeight);
        
        // Adjust the image
        sourceWidth *= scale;
        sourceHeight *= scale;
        
        // Return the scaled bitmap
        return Bitmap.createScaledBitmap(coverArt, sourceWidth, sourceHeight, true);
    }

    /**
     * Generate the default cover bitmap
     */
    public static Bitmap generateDefaultCover(Context context, int width, int height) {
    	// Prepare the drawable size
        int size = Math.min(width, height);
        
        // Create the mutable bitmap
        Bitmap mutableBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        
        // Convert to canvas
        Canvas canvas = new Canvas(mutableBitmap);
        
        // Prepare the drawable
        Drawable drawable = Theme.Resources.getDrawable(R.drawable.fp_albumart);
        
        // Draw the default cover
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        // All done
        return mutableBitmap;
    }
}

/*EOF*/