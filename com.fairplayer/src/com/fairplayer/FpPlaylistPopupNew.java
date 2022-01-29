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

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * Simple dialog to prompt to user to enter a playlist name. Has an EditText to
 * enter the name and two buttons, create and cancel. Create changes to
 * overwrite if a name that already exists is selected.
 */
public class FpPlaylistPopupNew extends Dialog implements TextWatcher, View.OnClickListener {

    /**
     * The create/overwrite button.
     */
    private Button mPositiveButton;
    /**
     * The text entry view.
     */
    private EditText mText;
    /**
     * Whether the dialog has been accepted. The dialog is accepted if create
     * was clicked.
     */
    private boolean mAccepted;
    /**
     * The text to display initially. When the EditText contains this text, the
     * positive button will be disabled.
     */
    private final String mInitialText;
    /**
     * The resource containing the string describing the default positive
     * action (e.g. "Create" or "Rename").
     */
    private final int mActionRes;
    /**
     * A playlist task that is simply stored in the dialog.
     */
    private final FpPlaylistTask mPlaylistTask;

    /**
     * Create a FpPlaylistPopupNew.
     *
     * @param context A Context to use.
     * @param initialText The text to show initially. The positive button is
     * disabled when the EditText contains this text.
     * @param actionText A string resource describing the default positive
     * action (e.g. "Create").
     */
    public FpPlaylistPopupNew(Context context, String initialText, int actionText, FpPlaylistTask playlistTask) {
        super(context);
        mInitialText = initialText;
        mActionRes = actionText;
        mPlaylistTask = playlistTask;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setContentView(R.layout.fp_dialog_playlist);

        setTitle(R.string.fp_playlist_new_name);

        mPositiveButton = (Button) findViewById(R.id.fp_create);
        mPositiveButton.setOnClickListener(this);
        mPositiveButton.setText(mActionRes);
        View negativeButton = findViewById(R.id.fp_cancel);
        negativeButton.setOnClickListener(this);

        mText = (EditText) findViewById(R.id.fp_playlist_name);
        mText.addTextChangedListener(this);
        mText.setText(mInitialText);
        mText.requestFocus();
    }

    /**
     * Returns the playlist name currently entered in the dialog.
     */
    public String getText() {
        return mText.getText().toString();
    }

    /**
     * Returns the stored playlist task.
     */
    public FpPlaylistTask getPlaylistTask() {
        return mPlaylistTask;
    }

    public void afterTextChanged(Editable s) {
        // do nothing
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    public void onTextChanged(CharSequence text, int start, int before, int count) {
        String string = text.toString();
        if (string.equals(mInitialText)) {
            mPositiveButton.setEnabled(false);
        } else {
            mPositiveButton.setEnabled(true);
            // Update the action button based on whether there is an
            // existing playlist with the given name.
            ContentResolver resolver = getContext().getContentResolver();
            int res = FpPlaylist.getPlaylist(resolver, string) == -1 ? mActionRes : R.string.fp_playlist_replace;
            mPositiveButton.setText(res);
        }
    }

    /**
     * Returns whether the dialog has been accepted. The dialog is accepted
     * when the create/overwrite button is clicked.
     */
    public boolean isAccepted() {
        return mAccepted;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fp_create:
                mAccepted = true;
            // fall through
            case R.id.fp_cancel:
                dismiss();
                break;
        }
    }
}