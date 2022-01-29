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
import android.content.Intent;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.MediaPlayer.*;

import java.io.IOException;
import java.util.ArrayList;

public class FpMediaPlayer {

    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    // Context
    protected Context mContext = null;
    
    // Media player
    protected Media mCurrentMedia = null;
    protected MediaPlayer mMediaPlayer = null;
    protected LibVLC mLibVLC = null;
    protected String mDataSource = null;
    protected FpMediaPlayer.Listener mMediaPlayerListener = null;
    
    // Sound alteration
    protected float mDuckingFactor = Float.NaN;
    protected boolean mIsDucking = false;

    // Media information
    protected int mediaAudioStreamType = AudioManager.STREAM_MUSIC;
    protected int mediaAudioSessionId = 0;
    
    /**
     * Constructs a new FpMediaPlayer class
     */
    public FpMediaPlayer (FpMediaPlayer.Listener mediaPlayerListener) {
        // Prepare the options
    	ArrayList<String> options = new ArrayList<String>();
        options.add("--http-reconnect");
        options.add("--network-caching=2000");
        
        // Set the VLC library
        mLibVLC = new LibVLC(options);
        
        // Prepare the media player
        mMediaPlayer = new MediaPlayer(mLibVLC);
        
        // Store the context
        mContext = ActivityCommon.getContext();
        
        // Store the playback service
        mMediaPlayerListener = mediaPlayerListener;
        
    	// Prepare the media player event listener
    	EventListener eventListener = new EventListener() {
            @Override
            public void onEvent(Event event) {
            	switch (event.type) {
                    // End reached
                    case Event.EndReached:
                        mMediaPlayerListener.onCompletion(mMediaPlayer);
                        break;

                    // Encountered an error
                    case Event.EncounteredError:
                        mMediaPlayerListener.onError(mMediaPlayer, (int) event.getTimeChanged(), (int) event.getPositionChanged());
                        break;
            	}
            }
    	};
    	
    	// Set the event on the media player
    	mMediaPlayer.setEventListener(eventListener);
    }
    
    /**
     * Media player event listeners
     */
    public interface Listener {
    	/**
         * On completion listener
         */
        void onCompletion(MediaPlayer mp);

        /**
         * On error listener
         */
        boolean onError(MediaPlayer mp, int what, int extra);
    }
    
    /**
     * Resets the media player to an unconfigured state
     */
    public void reset() {
        mDataSource = null;
        mMediaPlayer.stop();
    }

    /**
     * Releases the media player and frees any claimed AudioEffect
     */
    public void release() {
        mDataSource = null;
        if (null != mCurrentMedia) {
            mCurrentMedia.release();
        }
    }

    /**
     * Sets the data source to use
     */
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mCurrentMedia = new Media(mLibVLC, path);
        
    	if (null != mMediaPlayer) {
            mMediaPlayer.setMedia(mCurrentMedia);
        }
        
        mDataSource = path;
    }

    /**
     * Returns the configured data source, may be null
     */
    public String getDataSource() {
        return mDataSource;
    }

    /**
     * Creates a new AudioEffect for our AudioSession
     */
    public void openAudioFx() {
    	// Prepare the intent
        Intent intentAudioEffect = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        
        // Set the session
        intentAudioEffect.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaAudioSessionId);
        
        // Set the package name
        intentAudioEffect.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName());
        
        // Send the broadcast
        mContext.sendBroadcast(intentAudioEffect);
    }

    /**
     * Releases a previously claimed audio session id
     */
    public void closeAudioFx() {
    	// Prepare the intent
        Intent intentAudioEffect = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        
        // Set the session
        intentAudioEffect.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaAudioSessionId);
        
        // Set the package name
        intentAudioEffect.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName());
        
        // Send the broadcast
        mContext.sendBroadcast(intentAudioEffect);
    }

    /**
     * Sets whether we are ducking or not. Ducking is when we temporarily
     * decrease the volume for
     * a transient sound to play from another application, such as a
     * notification's beep.
     *
     * @param isDucking true if we are ducking, false if we are not
     */
    public void setIsDucking(boolean isDucking) {
        mIsDucking = isDucking;
        updateVolume();
    }

    /**
     * Sets the desired scaling while ducking.
     *
     * @param duckingFactor the factor to adjust the volume by ducking.
     * Must be between 0 and 1 (inclusive) or Float#NaN to disable ducking
     */
    public void setDuckingFactor(float duckingFactor) {
        mDuckingFactor = duckingFactor;
        updateVolume();
    }

    /**
     * Sets the volume, using ducking if appropriate
     */
    public void updateVolume() {
        float volume = 1.0f;
        if (mIsDucking && !Float.isNaN(mDuckingFactor)) {
            volume *= mDuckingFactor;
        }
        this.setVolume(volume, volume);
    }

    /**
     * Get the media player in use
     */
    public MediaPlayer getPlayer() {
    	return mMediaPlayer;
    }

    /**
     * Start the media player
     */
    public void start() throws IllegalStateException {
        mMediaPlayer.play();
    }

    /**
     * Stop the media player
     */
    public void stop() throws IllegalStateException {
        mMediaPlayer.stop();
    }

    /**
     * Pause the media player
     */
    public void pause() throws IllegalStateException {
        mMediaPlayer.pause();
    }

    /**
     * Returns true if any media is playing
     */
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    /**
     * Navigate to a millisecond
     */
    public void seekTo(int msec) throws IllegalStateException {
    	mMediaPlayer.setTime((long) msec);
    }

    /**
     * Get the current position (in ms), -1 if there is no media
     */
    public int getCurrentPosition() {
        return (int) mMediaPlayer.getTime();
    }

    /**
     * Get the duration (in ms), -1 if there is no media
     */
    public int getDuration() {
        return (int) mMediaPlayer.getLength();
    }

    /**
     * Set the audio stream type
     */
    public void setAudioStreamType(int streamtype) {
    	mediaAudioStreamType = streamtype;
    }
    
    /**
     * Get the audio stream type
     */
    public int getAudioStreamType() {
    	return mediaAudioStreamType;
    }

    /**
     * Set the volume
     */
    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(
            (int) ((leftVolume + rightVolume) * 100/2)
        );
    }
    
    /**
     * Get the volume as float 0.0f...1.0f
     */
    public float getVolume() {
        return (float) mMediaPlayer.getVolume() / 100;
    }

    /**
     * Set the audio session ID
     */
    public void setAudioSessionId(int sessionId)  throws IllegalArgumentException, IllegalStateException {
    	mediaAudioSessionId = sessionId;
    }

    /**
     * Get the audio session ID
     */
    public int getAudioSessionId() {
        return mediaAudioSessionId;
    }
}

/*EOF*/