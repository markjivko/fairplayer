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
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class ElementImageButton extends ImageButton {

    public ElementImageButton(Context context) {
        this(context, null);
    }

    public ElementImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ElementImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageDrawable(Theme.Resources.getDrawable(resId));
    }
}

/*EOF*/