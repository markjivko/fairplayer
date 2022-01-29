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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

public class FpPlaylistPopup extends DialogFragment
        implements DialogInterface.OnClickListener {

    /**
     * A class implementing our callback interface
     */
    private Callback mCallback;
    /**
     * The intent to act on
     */
    private Intent mIntent;
    /**
     * Array of all found playlist names
     */
    private String[] mItemName;
    /**
     * Array of all found playlist values
     */
    private long[] mItemValue;
    /**
     * Magic value, indicating that a new
     * playlist shall be created
     */
    private final int VALUE_CREATE_PLAYLIST = -1;

    /**
     * Our callback interface
     */
    public interface Callback {

        void appendToPlaylistFromIntent(Intent intent);

        void createNewPlaylistFromIntent(Intent intent);
    }

    FpPlaylistPopup(Callback callback, Intent intent) {
        mCallback = callback;
        mIntent = intent;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Cursor cursor = FpPlaylist.queryPlaylists(getActivity().getContentResolver());
        if (cursor == null) {
            return null;
        }

        int count = cursor.getCount();
        mItemName = new String[1 + count];
        mItemValue = new long[1 + count];

        // Index 0 is always 'New Playlist...'
        mItemName[0] = getResources().getString(R.string.fp_playlist_new);
        mItemValue[0] = VALUE_CREATE_PLAYLIST;

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            mItemValue[1 + i] = cursor.getLong(0);
            mItemName[1 + i] = cursor.getString(1);
        }

        // All names are now known: we can show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.fp_menu_add_to_playlist)
                .setItems(mItemName, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mItemValue[which] == VALUE_CREATE_PLAYLIST) {
            mCallback.createNewPlaylistFromIntent(mIntent);
        } else {
            Intent copy = new Intent(mIntent);
            copy.putExtra(AdapterLibrary.DATA_PLAYLIST, mItemValue[which]);
            copy.putExtra(AdapterLibrary.DATA_PLAYLIST_NAME, mItemName[which]);
            mCallback.appendToPlaylistFromIntent(copy);
        }
    }
}