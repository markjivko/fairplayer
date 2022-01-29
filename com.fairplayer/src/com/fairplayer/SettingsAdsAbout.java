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

import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * SeekBar preference
 */
public class SettingsAdsAbout extends DialogPreference {

    protected TextView mProgressLabel;
    
    public SettingsAdsAbout(Context context, AttributeSet attrs) {
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
    	// Store the context
    	Context mContext = getContext();
    	
        // Prepare the inflater
        LayoutInflater inflater = LayoutInflater.from(mContext);

        // Set the custom layout
        View view = inflater.inflate(R.layout.fp_settings_ads_about, null);
        
        // Prepare the parameters
        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        
        // Get the table
        TableLayout tableLayout = (TableLayout) view.findViewById(R.id.fp_settings_ads_about_table);

        // Get the loyalty class
        FpLoyalty loyalty = new FpLoyalty(mContext);
        
        // Get the items
        Map<String, String> tableItems = loyalty.getTable();
        int levelIdCurrent = loyalty.getCurrentLevel();
        int levelId = 0;
        for(Map.Entry<String, String> entry : tableItems.entrySet()) {
            // Prepare the row
            TableRow tableRow = new TableRow(mContext);
            tableRow.setLayoutParams(tableParams);

            // Level
            TextView textViewLevel = new TextView(mContext);
            textViewLevel.setLayoutParams(rowParams);
            textViewLevel.setText(mContext.getString(R.string.settings_ads_level) + " " + String.valueOf(levelId));
            if (levelId == levelIdCurrent) {
                textViewLevel.setTypeface(null, Typeface.BOLD);
            }
            tableRow.addView(textViewLevel);

            // Age
            TextView textViewAge = new TextView(mContext);
            textViewAge.setLayoutParams(rowParams);
            textViewAge.setText(entry.getKey());
            if (levelId == levelIdCurrent) {
                textViewAge.setTypeface(null, Typeface.BOLD);
            }
            tableRow.addView(textViewAge);

            // Percent
            TextView textViewPercent = new TextView(mContext);
            textViewPercent.setLayoutParams(rowParams);
            textViewPercent.setText(entry.getValue());
            if (levelId == levelIdCurrent) {
                textViewPercent.setTypeface(null, Typeface.BOLD);
            }
            tableRow.addView(textViewPercent);

            // Add the row
            tableLayout.addView(tableRow);

            // Next level
            levelId++;
        }
        
        // All done
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }
}

/*EOF*/