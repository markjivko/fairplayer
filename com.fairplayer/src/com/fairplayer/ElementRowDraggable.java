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
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.Checkable;

public class ElementRowDraggable extends LinearLayout implements Checkable {

    /**
     * True if the checkbox is checked
     */
    private boolean mChecked;
    /**
     * True if setupLayout has been called
     */
    private boolean mLayoutSet;

    private TextView mTextView;
    private CheckedTextView mCheckBox;
    private View mPmark;
    private ImageView mDragger;
    private ElementSmallCover mCoverView;

    /**
     * Layout types for use with setupLayout
     */
    public static final int LAYOUT_CHECKBOXES = 1;
    public static final int LAYOUT_COVERVIEW = 2;

    public ElementRowDraggable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        mCheckBox = (CheckedTextView) this.findViewById(R.id.fp_checkbox);
        mTextView = (TextView) this.findViewById(R.id.fp_text);
        mPmark = (View) this.findViewById(R.id.fp_pmark);
        mDragger = (ImageView) this.findViewById(R.id.fp_dragger);
        mCoverView = (ElementSmallCover) this.findViewById(R.id.fp_cover);
        
        // Set the dragger
        mDragger.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_row_handle));
        
        // Set the row color
        mTextView.setTextColor(Theme.Resources.getColor(R.color.fp_color_row_title));
        
        // Prepare the colors
        int[] colors = {0, Theme.Resources.getColor(R.color.fp_color_row_divider), 0};
    
        // Set the divider
        this.setDividerDrawable(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        
        // All done
        super.onFinishInflate();
    }

    /**
     * Sets up commonly used layouts - can only be called once per view
     *
     * @param type the layout type to use
     */
    public void setupLayout(int type) {
        if (!mLayoutSet) {
            switch (type) {
                case LAYOUT_CHECKBOXES:
                    mCheckBox.setVisibility(View.VISIBLE);
                    showDragger(true);
                    break;
                    
                case LAYOUT_COVERVIEW:
                    mCoverView.setVisibility(View.VISIBLE);
                    showDragger(true);
                    break;
                    
                default:
                    break; // do not care
            }
            mLayoutSet = true;
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        mCheckBox.setChecked(mChecked);
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    /**
     * Marks a row as highlighted
     *
     * @param state Enable or disable highlighting
     */
    public void highlightRow(boolean state) {
        mPmark.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Change visibility of dragger element
     *
     * @param state shows or hides the dragger
     */
    public void showDragger(boolean state) {
        mDragger.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Returns an instance of our textview
     */
    public TextView getTextView() {
        return mTextView;
    }

    /**
     * Returns an instance of our coverview
     */
    public ElementSmallCover getCoverView() {
        return mCoverView;
    }
}

/*EOF*/