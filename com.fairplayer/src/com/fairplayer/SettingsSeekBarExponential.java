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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A preference that provides a dialog with a slider for idle time.
 *
 * The slider produces a value in seconds from 60 (1 minute) to 10800
 * (3 hours). The values range on an approximately exponential scale.
 */
public class SettingsSeekBarExponential extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private static final int VALUE_DEFAULT = 3600;
    private static final int VALUE_MIN = 60;
    private static final int VALUE_MAX = 10800;

    /**
     * The current idle timeout displayed on the slider. Will not be persisted
     * until the dialog is closed.
     */
    private int mValue;
    /**
     * The view in which the value is displayed.
     */
    private TextView mValueText;

    public SettingsSeekBarExponential(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return formatTime(getPersistedInt(VALUE_DEFAULT));
    }

    @SuppressLint("InflateParams") 
    @Override
    protected View onCreateDialogView() {
        // Prepare the inflater
    	LayoutInflater inflater = LayoutInflater.from(getContext());
    	
    	// Set the custom layout
        View view = inflater.inflate(R.layout.fp_settings_seekbar, null);

        mValue = getPersistedInt(VALUE_DEFAULT);

        mValueText = (TextView) view.findViewById(R.id.fp_value);

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.fp_seek_bar);
        seekBar.setMax(1000);
        seekBar.setProgress((int) (Math.pow((float) (mValue - VALUE_MIN) / (VALUE_MAX - VALUE_MIN), 0.25f) * 1000));
        seekBar.setOnSeekBarChangeListener(this);

        updateText();

        // All done
        return view;
    }

    /**
     * Format seconds into a human-readable time description.
     *
     * @param value The time, in seconds.
     *
     * @return A human-readable string, such as "1 hour, 21 minutes"
     */
    private String formatTime(int value) {
        Resources res = getContext().getResources();
        StringBuilder text = new StringBuilder();
        if (value >= 3600) {
            int hours = value / 3600;
            text.append(res.getQuantityString(R.plurals.fp_plurals_hour, hours, hours));
            text.append(", ");
            int minutes = value / 60 - hours * 60;
            text.append(res.getQuantityString(R.plurals.fp_plurals_minute, minutes, minutes));
        } else {
            int minutes = value / 60;
            text.append(res.getQuantityString(R.plurals.fp_plurals_minute, minutes, minutes));
            text.append(", ");
            int seconds = value - minutes * 60;
            text.append(res.getQuantityString(R.plurals.fp_plurals_second, seconds, seconds));
        }
        return text.toString();
    }

    /**
     * Update the text view with the current value.
     */
    private void updateText() {
        mValueText.setText(formatTime(mValue));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && shouldPersist()) {
            persistInt(mValue);
            notifyChanged();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            // Get the value ^4
            float value = (float) Math.pow(seekBar.getProgress() / 1000.0f, 4f);
            
            // Scale back to integer
            mValue = (int) (value * (VALUE_MAX - VALUE_MIN)) + VALUE_MIN;
            
            // All done
            updateText();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}

/*EOF*/