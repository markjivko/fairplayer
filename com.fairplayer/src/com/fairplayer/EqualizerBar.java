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

import com.fairplayer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class EqualizerBar extends LinearLayout {

    private static final int PRECISION = 10;

    private static final int RANGE = 20 * PRECISION;

    private ElementVerticalSeekBar mSeek;

    private TextView mValue;

    private OnEqualizerBarChangeListener listener;

    public interface OnEqualizerBarChangeListener {

        void onProgressChanged(float value);
    }

    public EqualizerBar(Context context, float band) {
        super(context);
        init(context, band);
    }

    public EqualizerBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, 0);
    }

    private void init(Context context, float band) {
        LayoutInflater.from(context).inflate(R.layout.fp_activity_equalizer_bar, this, true);

        mSeek = (ElementVerticalSeekBar) findViewById(R.id.fp_equalizer_seek);
        
        mSeek.setMax(2 * RANGE);
        mSeek.setProgress(RANGE);
        mSeek.setOnSeekBarChangeListener(mSeekListener);
        
        // Prepare the equalizer band
        TextView equalizerBand = (TextView) findViewById(R.id.fp_equalizer_band);
        equalizerBand.setTextColor(Theme.Resources.getColor(R.color.fp_color_equalizer_label));;
        equalizerBand.setText(band < 999.5f ? (int) (band + 0.5f) + " Hz" : (int) (band / 1000.0f + 0.5f) + " kHz");
        equalizerBand.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.fp_text_size_equalizer_label_bar));
        
        // Prepare the equalizer value
        mValue = (TextView) findViewById(R.id.fp_equalizer_value);
        mValue.setTextColor(Theme.Resources.getColor(R.color.fp_color_equalizer_label));
        mValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.fp_text_size_equalizer_label_bar));
    }

    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float value = (progress - RANGE) / (float) PRECISION;
            mValue.setText(value + " Db");
            if (listener != null) {
                listener.onProgressChanged(value);
            }
        }
    };

    public void setValue(float value) {
        mSeek.setProgress((int) (value * PRECISION + RANGE));
    }

    public void setListener(OnEqualizerBarChangeListener listener) {
        this.listener = listener;
    }
}
/*EOF*/