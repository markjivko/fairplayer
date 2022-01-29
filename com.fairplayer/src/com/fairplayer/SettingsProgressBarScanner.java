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

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * SeekBar preference
 */
public class SettingsProgressBarScanner extends DialogPreference implements FragmentScanner.ScanProgressCallbacks {

    FragmentScanner mScannerFragment;

    protected ProgressBar mProgressBar;
    protected TextView mProgressLabel;
    
    public SettingsProgressBarScanner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton(null, null);
    }
    
    @SuppressLint("InflateParams")
    @Override
    protected View onCreateDialogView() {
        // Prepare the inflater
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Set the custom layout
        View view = inflater.inflate(R.layout.fp_settings_progressbar_scanner, null);

        // Prepare the scanner fragment
        mScannerFragment = new FragmentScanner();

        // Set the callbacks
        mScannerFragment.setScanProgressCallbacks(this);

        // Get the elements
        mProgressBar = (ProgressBar) view.findViewById(R.id.fp_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressLabel = (TextView) view.findViewById(R.id.fp_progress_label);
        
        try {
            startScan();
        } catch (IOException e) {
        }

        // All done
        return view;
    }

    public void startScan() throws IOException {
        // Start the scan
        mScannerFragment.startScan(FpUtilsFilesystem.getFilesystemBrowseStart(ActivityCommon.getActivity()), true);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        mScannerFragment.setScanProgressCallbacks(null);
    }

    @Override
    public void updateProgressNum(int progressNum) {
        mProgressBar.setProgress(progressNum);
    }

    @Override
    public void updateProgressText(FragmentScanner.ScannerString progressText) {
        if (null != progressText) {
            mProgressLabel.setText(progressText.toString());
        }
        mProgressLabel.setSelected(true);
    }

    @Override
    public void updatePath(String path) {
        // Nothing to do
    }

    @Override
    public void updateStartButtonEnabled(boolean startButtonEnabled) {
        // Nothing to do
    }

    @Override
    public void signalFinished() {
        mProgressBar.setVisibility(View.GONE);
    }
}

/*EOF*/