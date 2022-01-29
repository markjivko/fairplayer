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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Button;

import java.io.File;

public class ActivityPicker extends ActivityCommon {

    protected FpTrack mSong;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Get the intent
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        // Get the URI
        Uri uri = intent.getData();
        if (uri == null) {
            finish();
            return;
        }

        // Get the song
        mSong = _getSongForUri(uri);
        if (mSong == null) {
            finish();
            return;
        }
        
        // No title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set the audiopicler
        setContentView(R.layout.fp_picker);

        // Set the file name
        TextView filePath = (TextView) findViewById(R.id.fp_file_name);
        filePath.setText(new File(mSong.path).getName());
        
        // Enqueue (if player is running)
        ((Button) findViewById(R.id.fp_button_enqueue)).setOnClickListener(this);
        ((Button) findViewById(R.id.fp_button_enqueue)).setEnabled(FpServiceRendering.hasInstance());
        
        // Play
        ((Button) findViewById(R.id.fp_button_play)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int mode;
        FpUtilsMedia.QueryTask query = null;

        switch (view.getId()) {
            case R.id.fp_button_play:
                mode = FpTrackTimeline.MODE_PLAY;
                break;
                
            case R.id.fp_button_enqueue:
                mode = FpTrackTimeline.MODE_ENQUEUE_AS_NEXT;
                break;
                
            default:
                finish();
                return;
        }

        // Play or enqueue
        if (mSong.id < 0) {
            query = FpUtilsMedia.buildFileQuery(mSong.path, FpTrack.FILLED_PROJECTION);
        } else {
            query = FpUtilsMedia.buildQuery(FpUtilsMedia.TYPE_SONG, mSong.id, FpTrack.FILLED_PROJECTION, null);
        }

        // Set the query mode
        query.mode = mode;

        // Get a new Playback service
        FpServiceRendering service = FpServiceRendering.get(this);
        
        // Add the song
        service.addSongs(query);
        
        // Bring to front
        if (FpTrackTimeline.MODE_PLAY == mode) {
            Intent intent = new Intent(this, ActivityNowplaying.class);
            this.startActivity(intent);
        }
        
        // Stop here
        finish();
    }

    /**
     * Attempts to resolve given uri to a song object
     *
     * @param uri The uri to resolve
     *
     * @return A song object, null on failure
     */
    protected FpTrack _getSongForUri(Uri uri) {
        FpTrack song = new FpTrack(-1);
        Cursor cursor = null;

        if (uri.getScheme().equals("content")) {
            cursor = FpUtilsMedia.queryResolver(getContentResolver(), uri, FpTrack.FILLED_PROJECTION, null, null, null);
        }
        if (uri.getScheme().equals("file")) {
            cursor = FpUtilsMedia.getCursorForFileQuery(uri.getPath());
        }

        if (cursor != null) {
            if (cursor.moveToNext()) {
                song.populate(cursor);
            }
            cursor.close();
        }
        return song.isFilled() ? song : null;
    }

    @Override
    public void goPlaces() {
        // TODO Auto-generated method stub

    }

}

/*EOF*/