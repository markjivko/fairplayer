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
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class ElementList extends ListView {

    /**
     * The calculated start position in pixel
     */
    private float mEdgeProtectStartPx = 0;
    /**
     * The calculated end position in pixel
     */
    private float mEdgeProtectEndPx = 0;

    public ElementList(Context context) {
        super(context);
    }

    public ElementList(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // Prepare the colors
        int[] colors = {0, Theme.Resources.getColor(R.color.fp_color_row_divider), 0};
    
        // Set the divider
        this.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        this.setDividerHeight(1);
    }

    public ElementList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Intercepted touch event from ListView
     * We will use this callback to send fake X-coord events if
     * the actual event happened in the protected area (eg: the hardcoded
     * fastscroll area)
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mEdgeProtectStartPx == 0) {
            mEdgeProtectStartPx = getWidth() - 50 * Resources.getSystem().getDisplayMetrics().density;
        }
        if (mEdgeProtectEndPx == 0) {
            mEdgeProtectEndPx = getWidth() - 15 * Resources.getSystem().getDisplayMetrics().density;
        }

        if (ev.getX() > mEdgeProtectStartPx && ev.getX() < mEdgeProtectEndPx) {
            // Cursor is in protected area: simulate an event with a faked x coordinate
            ev = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), ev.getAction(), mEdgeProtectStartPx, ev.getY(), ev.getMetaState());
        }
        return super.onInterceptTouchEvent(ev);
    }
}

/*EOF*/