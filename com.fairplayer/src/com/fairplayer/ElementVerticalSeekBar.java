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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class ElementVerticalSeekBar extends SeekBar {

    private boolean mIsMovingThumb = false;
    private static final float THUMB_SLOP = 25;

    protected Drawable thumbDrawable;
    
    public ElementVerticalSeekBar(Context context) {
        super(context);
    }

    public ElementVerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ElementVerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // Set the progress
        this.setProgressDrawable(Theme.Resources.getDrawable(R.drawable.fp_vertical_seekbar));
        
        // Set the thumb 
        this.thumbDrawable = Theme.Resources.getDrawable(R.drawable.fp_vertical_seekbar_thumb);
        this.setThumb(thumbDrawable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    private boolean isWithinThumb(MotionEvent event) {
        final float progress = getProgress();
        final float density = this.getResources().getDisplayMetrics().density;
        final float height = getHeight();
        final float y = event.getY();
        final float max = getMax();
        return progress >= max - (int) (max * (y + THUMB_SLOP * density) / height) && progress <= max - (int) (max * (y - THUMB_SLOP * density) / height);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            // Set the thumb 
            thumbDrawable.setState(new int[]{});
            
            // Stop here
            return false;
        }

        boolean handled = false; 

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isWithinThumb(event)) {
                    // Stop propagation
                    getParent().requestDisallowInterceptTouchEvent(true);
                    
                    // Set the thumb 
                    thumbDrawable.setState(new int[]{android.R.attr.state_focused, android.R.attr.state_pressed});
                    
                    // Started moving
                    mIsMovingThumb = true;
                    
                    // Event handled
                    handled = true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mIsMovingThumb) {
                    final int max = getMax();
                    setProgress( max - (int) (max* event.getY() / getHeight()));
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                    handled = true;
                }
                break;
                
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            	// Stop propagation
            	getParent().requestDisallowInterceptTouchEvent(false);
                
            	// Set the thumb 
                thumbDrawable.setState(new int[]{});
            	
                // Stopped moving
                mIsMovingThumb = false;
                
                // Event handled
                handled = true;
                break;
        }
        
        // All done
        return handled;
    }
}
/*EOF*/