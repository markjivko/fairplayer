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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * SeekBar preference
 */
public class SettingsAdsPercent extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    /**
     * The current value.
     */
    protected int mValue;
    
    /**
     * The minimum value to use
     */
    protected int mMinValue;
    
    /**
     * The initially configured value (updated on dialog close)
     */
    protected int mInitialValue;
    
    /**
     * Steps to take for the value
     */
    protected int mSteps = 1;
    
    /**
     * The text to use in the summary
     */
    protected String mSummaryText;
    
    /**
     * TextView to display current percent
     */
    protected TextView mValueText;

    /**
     * TextView to display current info
     */
    protected TextView mValueInfo;
    
    /**
     * Loyalty program
     */
    protected FpLoyalty mLoyalty;

    /**
     * Resources
     */
    protected Resources mResources;
    
    /**
     * Context
     */
    protected Context mContext;
    
    public SettingsAdsPercent(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // Store the context
        mContext = context;
        
        // Get the loyalty class
        mLoyalty = new FpLoyalty(context);
        
        // Store the minimum probability
        mMinValue = 100 - mLoyalty.getAdsChance();
    }

    @Override
    public CharSequence getSummary() {
        return getSummary(mValue);
    }
    
    @Override
    public CharSequence getTitle() {
    	if (null == mLoyalty) {
    		mLoyalty = new FpLoyalty(getContext());
    	}
    	return getContext().getString(R.string.settings_ads_percent_title) + " - " + getContext().getString(R.string.settings_ads_level) + " " + String.valueOf(mLoyalty.getCurrentLevel());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 100);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        mInitialValue = mValue = restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue;
    }

    /**
     * Create the summary for the given value.
     *
     * @param value The force threshold.
     *
     * @return A string representation of the threshold.
     */
    protected String getSummary(int value) {
        // Prepare the result
        String result = String.valueOf(value) + "%";
        
        // All done
        return result;
    }

    @Override
    protected View onCreateDialogView() {
        // Prepare the inflater
    	LayoutInflater inflater = LayoutInflater.from(getContext());
    	
    	// Set the custom layout
        View view = inflater.inflate(R.layout.fp_settings_ads_percent, null);

        // Get the info
        mValueInfo = (TextView) view.findViewById(R.id.fp_info);
        
        // Prepare the text
        String infoText = mContext.getString(R.string.settings_ads_percent_info_current, mLoyalty.getAdsChance());
        
        // Get the time remaining until the next level
        String infoTextNext = mLoyalty.getNextString();
        if (null != infoTextNext) {
            infoText += " " + mContext.getString(R.string.settings_ads_percent_info_next, infoTextNext);
        }
        mValueInfo.setText(infoText);
        
        // Get the text
        mValueText = (TextView) view.findViewById(R.id.fp_value);
        mValueText.setText(getSummary(mValue));

        // Get the seek bar
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.fp_seek_bar);
        
        // Initialize the seek bar
        seekBar.setMax(mMinValue);
        seekBar.setProgress(100 - mValue);
        seekBar.setOnSeekBarChangeListener(this);

        // All done
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mInitialValue = mValue;
            persistInt(mValue);
        } else {
            // User aborted: Set remembered start value
            setValue(mInitialValue);
        }
        notifyChanged();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            seekBar.setProgress(progress);
            int value = (100 - progress);
            setValue(value);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void setValue(int value) {
        mValue = value;
        mValueText.setText(getSummary(value));
    }
}

/*EOF*/