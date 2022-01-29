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

import java.util.Arrays;
import java.io.File;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.content.SharedPreferences;

public class ActivityBrowser extends Activity {

    private ListView mListView;
    private TextView mPathDisplay;
    private Button mSaveButton;
    private AdapterBrowser mListAdapter;
    private String mCurrentPath;
    private SharedPreferences.Editor mPrefEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.StyleActionBar);
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings_scanner_root_title);
        setContentView(R.layout.fp_activity_browser);
        mCurrentPath = (String) FpUtilsFilesystem.getFilesystemBrowseStart(this).getAbsolutePath();
        mPrefEditor = PreferenceUtils.edit();
        mListAdapter = new AdapterBrowser((ActivityBrowser) this, 0);
        mPathDisplay = (TextView) findViewById(R.id.fp_path_display);
        mListView = (ListView) findViewById(R.id.fp_list);
        mSaveButton = (Button) findViewById(R.id.fp_save_button);

        mListView.setAdapter(mListAdapter);

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPrefEditor.putString(Constants.Keys.SETTINGS_ROOT, mCurrentPath);
                mPrefEditor.commit();
                finish();
            }
        });
    }

    /*
     ** Called when we are displayed (again)
     ** This will always refresh the whole song list
     */
    @Override
    public void onResume() {
        super.onResume();
        refreshDirectoryList();
        
        // Track pageview
    	Tracker.trackPageview(this);
    }
    
    /*
     ** Create a bare-bones actionbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } 
        return super.onOptionsItemSelected(item);
    }

    /*
     ** Enters selected directory at 'pos'
     */
    public void onDirectoryClicked(int pos) {
        String dirent = mListAdapter.getItem(pos);

        if (pos == 0) {
            mCurrentPath = (new File(mCurrentPath)).getParent();
        } else {
            mCurrentPath += "/" + dirent;
        }

        /*
         * let java fixup any strange paths
         */
        mCurrentPath = (new File(mCurrentPath == null ? "/" : mCurrentPath)).getAbsolutePath();

        refreshDirectoryList();
    }

    /*
     ** display mCurrentPath in the dialog
     */
    private void refreshDirectoryList() {
        File path = new File(mCurrentPath);
        File[] dirs = path.listFiles();

        mListAdapter.clear();
        mListAdapter.add("../");

        if (dirs != null) {
            Arrays.sort(dirs);
            for (File fentry : dirs) {
                if (fentry.isDirectory()) {
                    mListAdapter.add(fentry.getName());
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.fp_browser_no_display) + " " + mCurrentPath, Toast.LENGTH_SHORT).show();
        }
        
        mPathDisplay.setText(mCurrentPath);
        mListView.setSelectionFromTop(0, 0);
    }

}
/*EOF*/