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

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * SeekBar preference
 */
public class SettingsSeekBar extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    public static final String VALUE_TYPE_SECONDS = "second";
    public static final String VALUE_TYPE_MINUTE = "minute";
    public static final String VALUE_TYPE_HOUR = "hour";
    
    /**
     * The current value.
     */
    protected int mValue;
    
    /**
     * The maximum value to use
     */
    protected int mMaxValue;
    
    /**
     * The initially configured value (updated on dialog close)
     */
    protected int mInitialValue;
    
    /**
     * Steps to take for the value
     */
    protected int mSteps;
    
    /**
     * The format to use for the summary
     */
    protected String mSummaryFormat;
    
    /**
     * The text to use in the summary
     */
    protected String mSummaryText;
    
    /**
     * CheckBox preferences key, may be null
     */
    protected String mCheckBoxKey;
    
    /**
     * Label of checkbox
     */
    protected String mCheckBoxText;
    
    /**
     * TextView to display current summary
     */
    protected TextView mValueText;
    
    /**
     * CheckBox to display, may be null
     */
    protected CheckBox mCheckBox;

    /**
     * Resources
     */
    protected Resources mResources;
    
    public SettingsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDefaults(context, attrs);
    }

    /**
     * Configures the view using the SettingsSeekBar XML attributes
     *
     * @param attrs An AttributeSet
     */
    protected void initDefaults(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsSeekBar);
        mResources = context.getResources();
        mMaxValue = attributes.getInt(R.styleable.SettingsSeekBar_ssbMax, 100);
        mSteps = attributes.getInt(R.styleable.SettingsSeekBar_ssbIncrement, 1);
        mSummaryFormat = attributes.getString(R.styleable.SettingsSeekBar_ssbInfoTemplate);
        mSummaryFormat = (mSummaryFormat == null ? "%s %.1f" : mSummaryFormat);
        mSummaryText = attributes.getString(R.styleable.SettingsSeekBar_ssbInfo);
        mSummaryText = (mSummaryText == null ? "" : mSummaryText);
        mCheckBoxKey = attributes.getString(R.styleable.SettingsSeekBar_ssbExtraKey);   // non-null if checkbox enabled
        mCheckBoxText = attributes.getString(R.styleable.SettingsSeekBar_ssbExtraInfo);
        attributes.recycle();
    }

    @Override
    public CharSequence getSummary() {
        return getSummary(mValue);
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
    private String getSummary(int value) {
        // Prepare the result
        String result = String.valueOf(value);
        
        // Prepare the format
        String format = mSummaryFormat;
        
        // Replace the placeholders
        format = format.replaceAll("#h", mResources.getQuantityString(R.plurals.fp_plurals_hour, value));
        format = format.replaceAll("#m", mResources.getQuantityString(R.plurals.fp_plurals_minute, value));
        format = format.replaceAll("#s", mResources.getQuantityString(R.plurals.fp_plurals_second, value));
        
        // Get whether an integer should be used
        boolean useInt = mSummaryFormat.matches(".*?#[hms].*?");
        
        try {
            // No summary provided
            if (mSummaryText == null || mSummaryText.length() == 0) {
                if (useInt) {
                    result = String.format(Locale.US, format, (int) value);
                } else {
                    result = String.format(Locale.US, format, (float) value);
                }
            } else {
                if (useInt) {
                    result = String.format(Locale.US, format, mSummaryText, (int) value);
                } else {
                    result = String.format(Locale.US, format, mSummaryText, (float) value);
                }
            }
        } catch (Exception exc) {
            // Bad format
        }
        
        // All done
        return result;
    }

    @Override
    protected View onCreateDialogView() {
        // Prepare the inflater
    	LayoutInflater inflater = LayoutInflater.from(getContext());
    	
    	// Set the custom layout
        View view = inflater.inflate(R.layout.fp_settings_seekbar, null);

        mValueText = (TextView) view.findViewById(R.id.fp_value);
        mValueText.setText(getSummary(mValue));

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.fp_seek_bar);
        seekBar.setMax(mMaxValue);
        seekBar.setProgress(mValue);
        seekBar.setOnSeekBarChangeListener(this);

        if (mCheckBoxKey != null) {
            mCheckBox = (CheckBox) view.findViewById(R.id.fp_check_box);
            mCheckBox.setText(mCheckBoxText);
            mCheckBox.setChecked(getCheckBoxPreference());
            mCheckBox.setVisibility(View.VISIBLE);
        }

        // All done
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mInitialValue = mValue;
            if (mCheckBox != null) {
                saveCheckBoxPreference(mCheckBox.isChecked());
            }
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
            progress = (progress / mSteps) * mSteps;
            seekBar.setProgress(progress);
            setValue(progress);
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

    private void saveCheckBoxPreference(boolean enabled) {
        SharedPreferences.Editor editor = PreferenceUtils.edit();
        editor.putBoolean(mCheckBoxKey, enabled);
        editor.apply();
    }

    private boolean getCheckBoxPreference() {
        SharedPreferences settings = PreferenceUtils.getPreferences();
        return settings.getBoolean(mCheckBoxKey, false);
    }
}

/*EOF*/