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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class FpAnimation {

    /**
     * Animation frame
     */
    protected class AnimationFrame {

        protected int mResourceId;

        AnimationFrame(int resourceId) {
            mResourceId = resourceId;
        }

        public int getResourceId() {
            return mResourceId;
        }
    }

    protected ArrayList<AnimationFrame> mAnimationFrames = new ArrayList<AnimationFrame>();
    protected int mAnimationFramesIndex = -1;

    protected boolean mShouldRun = false;
    protected boolean mIsRunning = false;
    protected long mStartTime = 0L;

    protected SoftReference<ImageView> mSoftReferenceImageView;
    protected String mFramePrefix;
    protected Handler mHandler = new Handler();

    protected Bitmap mRecycleBitmap = null;

    // Listeners
    protected OnAnimationStoppedListener mOnAnimationStoppedListener;
    protected OnAnimationFrameChangedListener mOnAnimationFrameChangedListener;
    
    // Resources
    protected Resources mResources;
    protected FramesSequenceAnimation mRunnable;
    protected int mDuration;

    /**
     * FpAnimation
     */
    public FpAnimation(ImageView imageView, String framePrefix, int duration) {
    	// Set the image reference
        mSoftReferenceImageView = new SoftReference<ImageView>(imageView);
        
        // Set the frame prefix
        mFramePrefix = framePrefix;
        
        // Clear the frames
        mAnimationFrames.clear();
        
        // Store the frame duration
        mDuration = duration;
        
        // Is this a theme?
        mResources = Theme.Resources.get();
        
        // No, the default layout
        if (null == mResources) {
            mResources = imageView.getContext().getResources();
        }
        
        // Get the frames list
        List<Integer> frames = Theme.Resources.getAnimationFrames(framePrefix);
        
        // Go through the frames
        for (int i = 0; i < frames.size(); i++) {
            // Add the AnimationFrame
            mAnimationFrames.add(new AnimationFrame(frames.get(i)));
        }
    }

    /**
     * Get the number of frames
     */
    public int getNumberOfFrames() {
        return mAnimationFrames.size();
    }

    /**
     * add a frame of animation
     *
     * @param resId resource id of drawable
     * @param interval milliseconds
     */
    protected void addFrame(int resId) {
        mAnimationFrames.add(new AnimationFrame(resId));
    }

    /**
     * Listener of animation to detect stopped
     *
     */
    public interface OnAnimationStoppedListener {
        public void onAnimationStopped();
    }

    /**
     * Listener of animation to get index
     *
     */
    public interface OnAnimationFrameChangedListener {
        public void onAnimationFrameChanged(int index);
    }

    /**
     * set a listener for OnAnimationStoppedListener
     *
     * @param listener OnAnimationStoppedListener
     */
    public void setOnAnimationStoppedListener(OnAnimationStoppedListener listener) {
        mOnAnimationStoppedListener = listener;
    }

    /**
     * set a listener for OnAnimationFrameChangedListener
     *
     * @param listener OnAnimationFrameChangedListener
     */
    public void setOnAnimationFrameChangedListener(OnAnimationFrameChangedListener listener) {
        mOnAnimationFrameChangedListener = listener;
    }

    /**
     * Starts the animation
     */
    public synchronized void start() {
        mShouldRun = true;
        if (mIsRunning) {
            return;
        }
        
        // Prepare the runnable
        mRunnable = new FramesSequenceAnimation();
        
        // Start the animation
        mHandler.post(mRunnable);
    }
    
    public synchronized boolean isRunning() {
    	return mIsRunning;
    }

    /**
     * Stops the animation
     */
    public synchronized void stop() {
        mShouldRun = false;
    }

    protected AnimationFrame getNext() {
        mAnimationFramesIndex++;
        if (mAnimationFramesIndex >= mAnimationFrames.size()) {
            mAnimationFramesIndex = 0;
        }
        return mAnimationFrames.get(mAnimationFramesIndex);
    }
    
    protected class FramesSequenceAnimation implements Runnable {
    	
        @Override
        public void run() {
            // No frames defined
            if (mAnimationFrames.size() == 0) {
            	return;
            }
            
            // Set the start time
            mStartTime = System.currentTimeMillis();
            
            // Get the image view
            ImageView imageView = mSoftReferenceImageView.get();
            
            // Stop here
            if (!mShouldRun || imageView == null) {
                mIsRunning = false;
                
                if (mOnAnimationStoppedListener != null) {
                    mOnAnimationStoppedListener.onAnimationStopped();
                }
                
                // Hide the animation frame
                if (null != imageView) {
                    // Not the default theme
                    if (!Constants.Defaults.SETTINGS_THEME_PACKAGE_NAME.equals(Theme.Manager.getCurrentTheme())) {
                        imageView.setVisibility(View.INVISIBLE);
                    }
                }
                return;
            }
            
            // Show the animation frame
            if (null != imageView) {
            	if(View.VISIBLE != imageView.getVisibility()) {
                    imageView.setVisibility(View.VISIBLE);
            	}
            }
            
            try {
                // Get the next frame
                AnimationFrame frame = getNext();
                
                // Running flag
                mIsRunning = true;
            
                // Prepare the task
                GetImageDrawableTask task = new GetImageDrawableTask();

                // Execute it
                task.execute(frame.getResourceId());
            } catch (Exception exc) {
            	// No animation defined
            }
        }
    }

    @SuppressLint("NewApi") 
    protected class GetImageDrawableTask extends AsyncTask<Integer, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Integer... params) {
            // All done
            return mResources.getDrawable(params[0]);
        }

        @Override
        protected void onPostExecute(Drawable result) {
            super.onPostExecute(result);
            // Set the new drawable
            ImageView frame = mSoftReferenceImageView.get();
            
            // Valid result
            if (null != result && null != frame) {
                frame.setImageDrawable(result);
            }
            
            // Get the time it took to animate
            long frameDuration = System.currentTimeMillis() - mStartTime;
            if (frameDuration < 0) {
            	frameDuration = 0;
            }
            
            // Compute the delay
            long frameDelay = (long) mDuration - frameDuration;
            if (frameDelay < 1) {
            	frameDelay = 1;
            }
            
            // Next frame
            mHandler.postDelayed(mRunnable, frameDelay);
            
            // Animation frame changed
            if (mOnAnimationFrameChangedListener != null) {
                mOnAnimationFrameChangedListener.onAnimationFrameChanged(mAnimationFramesIndex);
            }
        }
    }
}

/*EOF*/