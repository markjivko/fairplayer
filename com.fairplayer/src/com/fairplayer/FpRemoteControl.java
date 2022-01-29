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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

public class FpRemoteControl {

    /**
     * Returns a FpRemoteControl.Client
     */
    public FpRemoteControl.Client getClient(Context context) {
        return new FpRemoteControl.Client(context);
    }

    /**
     * Interface definition of our FpRemoteControl API
     */
    public class Client {

    	/**
         * Context of this instance
         */
        protected Context mContext;
        
        /**
         * Used with updateRemote method.
         */
        protected RemoteControlClient mRemote;
        
        /**
         * Creates a new instance
         *
         * @param context The context to use
         */
        public Client(Context context) {
            mContext = context;
        }

        /**
         * Perform initialization required for RemoteControlClient.
         *
         * @param am The AudioManager service.
         */
        public void initializeRemote() {
            // Make sure there is only one registered remote
            unregisterRemote();

            // Receive 'background' play button events
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            ComponentName receiver = new ComponentName(mContext.getPackageName(), FpReceiverMediaButtonEvent.class.getName());
            audioManager.registerMediaButtonEventReceiver(receiver);

            // Preapare the intent
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(new ComponentName(mContext.getPackageName(), FpReceiverMediaButtonEvent.class.getName()));
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(mContext, 0, mediaButtonIntent, 0);
            RemoteControlClient remote = new RemoteControlClient(mediaPendingIntent);

            // Things we can do (eg: buttons to display on lock screen)
            int flags = RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                    | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                    | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE;
            remote.setTransportControlFlags(flags);

            audioManager.registerRemoteControlClient(remote);
            mRemote = remote;
        }

        /**
         * Unregisters a remote control client
         */
        public void unregisterRemote() {
            if (mRemote != null) {
                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                ComponentName receiver = new ComponentName(mContext.getPackageName(), FpReceiverMediaButtonEvent.class.getName());
                audioManager.unregisterMediaButtonEventReceiver(receiver);
                audioManager.unregisterRemoteControlClient(mRemote);
                mRemote = null;
            }
        }

        /**
         * Update the remote with new metadata.
         * #registerRemote(Context, AudioManager) must have been called
         * first.
         *
         * @param song The song containing the new metadata.
         * @param state FpServiceRendering state, used to determine playback state.
         * @param keepPaused whether or not to keep the remote updated in paused
         * mode
         */
        public void updateRemote(FpTrack song, int state, boolean keepPaused) {
            RemoteControlClient remote = mRemote;
            if (remote == null) {
                return;
            }

            boolean isPlaying = ((state & FpServiceRendering.FLAG_PLAYING) != 0);

            remote.setPlaybackState(isPlaying ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
            RemoteControlClient.MetadataEditor editor = remote.editMetadata(true);
        	try {
        		if (song != null && song.artist != null && song.album != null) {
	                String artist_album = song.artist + " - " + song.album;
	                artist_album = (song.artist.length() == 0 ? song.album : artist_album); 
	                artist_album = (song.album.length() == 0 ? song.artist : artist_album);
	
	                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, artist_album);
	                editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.title);
	                Bitmap bitmap = song.getCover(mContext);
	                if (bitmap != null && (isPlaying || keepPaused)) {
	                    // Create a copy of the cover art, since RemoteControlClient likes
	                    // to recycle what we give it.
	                    bitmap = bitmap.copy(Bitmap.Config.RGB_565, false);
	                } else {
	                    // Some lockscreen implementations fail to clear the cover artwork
	                    // if we send a null bitmap. We are creating a 16x16 transparent
	                    // bitmap to work around this limitation.
	                    bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
	                }
	                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
        		}
        	} catch (Exception exc) {
        		// Something went wrong
        	}
        	
        	// Apply the changes
            editor.apply();
        }
    }
}

/*EOF*/