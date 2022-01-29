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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

@SuppressLint({ "Recycle", "NewApi" }) 
public class ElementKnob extends RelativeLayout { 

    // Maximum number of degrees
    protected final int maxDegrees = 270;

    // W:H ratio for the knob
    protected final int ratioWidth = 1;
    protected final int ratioHeight = 1;
    
    // ImageView helpers
    protected GestureDetector mGestureDetector;
    protected ImageView mKnob;
    protected int mKnobWidth = 0, mKnobHeight = 0;
    protected ElementKnob.Listener mListener;

    // Degrees helpers
    protected float degreesOffset = 0f;
    protected float degreesStart = 0f;
    protected float degreesCurrent = 0f;
    protected float degreesClockwise = 0f;

    /**
     * Knob listener
     */
    interface Listener {
        /**
         * Called with each step of the knob scroll
         */
        public void onRotate(int percentage);
        
        /**
         * Called when the user sets a final value to the knob scroll
         */
        public void onChange(int percentage);
    }

    /**
     * Set the knob listener
     */
    public void setListener(ElementKnob.Listener listener) {
        mListener = listener;
    }

    /**
     * Set the angle
     */
    protected void _setRotorPosAngle(float deg) {
        if (null != mKnob) {
            degreesCurrent = deg;
            degreesOffset = deg;
            if (mKnobWidth != 0) {
            	mKnob.setPivotX(mKnobWidth/2);
            }
            if (mKnobHeight != 0) {
            	mKnob.setPivotY(mKnobHeight/2);
            }
            mKnob.setRotation(deg);
        }
    }

    /**
     * Set the percentage
     */
    public void setPercentage(int percentage) {
        _setRotorPosAngle(Math.round(maxDegrees * percentage / 100));
    }

    /**
     * Get the percentage
     */
    public int getPercentage() {
        return Math.round(degreesCurrent / maxDegrees * 100);
    }

    /**
     * Constructor
     */
    public ElementKnob(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override 
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
    	// Compute the ratio
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
        int calculatedHeight = originalWidth * ratioWidth / ratioHeight;

        // Get the final width and height
        if (calculatedHeight > originalHeight) {
        	mKnobWidth = originalHeight * ratioWidth / ratioHeight; 
        	mKnobHeight = originalHeight;
        } else {
        	mKnobWidth = originalWidth;
            mKnobHeight = calculatedHeight;
        }

        // Call the parent
        super.onMeasure(
    		MeasureSpec.makeMeasureSpec(mKnobWidth, MeasureSpec.EXACTLY), 
    		MeasureSpec.makeMeasureSpec(mKnobHeight, MeasureSpec.EXACTLY)
		);
    }
    
    /**
     * Default constructor
     */
    @SuppressLint("ClickableViewAccessibility") 
    public ElementKnob(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Prepare the background
        ImageView bkgLayout = new ImageView(context);

        // Set the drawable
        bkgLayout.setBackground(Theme.Resources.getDrawable(R.drawable.fp_knob_bkg));

        // Prepare the background layout parameters
        RelativeLayout.LayoutParams bkgLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        bkgLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Add the background
        addView(bkgLayout, bkgLayoutParams);

        // Prepare the background
        mKnob = new ImageView(context);

        // Set the drawable
        mKnob.setBackground(Theme.Resources.getDrawable(R.drawable.fp_knob_over_normal));

        // Prepare the background layout parameters
        RelativeLayout.LayoutParams overLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        overLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Set the scale type
        mKnob.setScaleType(ScaleType.FIT_CENTER);

        // Add the knob
        addView(mKnob, overLayoutParams);

        // Enable gesture detector
        mGestureDetector = new GestureDetector(context, new OnGestureListener() {
            /**
             * Scroll event
             */
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Get the current position
                float newDegreesPointer = _cartesianToPolar(e2);

                // Get the actual degrees
                float newDegreesCurrent = degreesOffset + newDegreesPointer - degreesStart;

                // Step-by-step navigation
                degreesStart = newDegreesPointer;

                // Store the direction
                degreesClockwise = (newDegreesCurrent - degreesCurrent);

                // Store the current degrees
                if (Math.abs(newDegreesCurrent - degreesCurrent) < maxDegrees / 2) {
                    degreesCurrent = newDegreesCurrent;
                }

                // Fit inside [0-Max]
                if (degreesCurrent > maxDegrees) {
                    degreesCurrent = maxDegrees;
                }
                if (degreesCurrent < 0) {
                    degreesCurrent = 0;
                }

                // Rotate the knob
                _setRotorPosAngle(degreesCurrent);

                // Call the listener
                if (mListener != null) {
                    mListener.onRotate(getPercentage());
                }

                // All done
                return true;
            }

            public void onShowPress(MotionEvent e) {}

            public void onLongPress(MotionEvent e) {}

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }

            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            public boolean onDown(MotionEvent event) {
                // Store the start degrees
                degreesStart = _cartesianToPolar(event);
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility") 
    @Override 
    public boolean onTouchEvent(MotionEvent event) {
        // Change the knob state
        mKnob.setBackground(Theme.Resources.getDrawable(event.getAction() == MotionEvent.ACTION_UP ? R.drawable.fp_knob_over_normal : R.drawable.fp_knob_over_pressed));

        // Final value
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mListener != null) {
                mListener.onChange(getPercentage());
            }
        }
        
        // Listen for the scroll
        if (mGestureDetector.onTouchEvent(event))  {
            return true;
        }

        // All done
        return super.onTouchEvent(event);
    }

    /**
     * Convert the current x,y point into an angle (degrees)
     */
    protected float _cartesianToPolar(MotionEvent event) {
        float x = (float) mKnobWidth / 2 - event.getX();
        float y = (float) mKnobHeight / 2 - event.getY();
        return 180f - (float) Math.toDegrees(Math.atan2(x, y));
    }
}

/*EOF*/