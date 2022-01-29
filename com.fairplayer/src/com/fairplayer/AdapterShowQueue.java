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
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.widget.TextView;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

public class AdapterShowQueue
        extends ArrayAdapter<FpTrack> {

    private int mResource;
    private int mHighlightRow;
    private Context mContext;

    public AdapterShowQueue(Context context, int resource) {
        super(context, resource);
        mResource = resource;
        mContext = context;
        mHighlightRow = -1;
    }

    /**
     * Tells the adapter to highlight a specific row id
     * Set this to -1 to disable the feature
     */
    public void highlightRow(int pos) {
        mHighlightRow = pos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ElementRowDraggable row;

        if (convertView != null) {
            row = (ElementRowDraggable) convertView;
        } else {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = (ElementRowDraggable) inflater.inflate(mResource, parent, false);
            row.setupLayout(ElementRowDraggable.LAYOUT_COVERVIEW);
        }

        // Prepare the colors
        int[] colors = {0, Theme.Resources.getColor(R.color.fp_color_row_divider), 0};
    
        // Get the divider
        View divider = row.findViewById(R.id.fp_divider);
    
        // Found
        if (null != divider) {
            // Set the divider
            divider.setBackground(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        }
        
        FpTrack song = getItem(position);

        if (song != null) { // unlikely to fail but seems to happen in the wild.
            SpannableStringBuilder sb = new SpannableStringBuilder(song.title);
            sb.append('\n');
            sb.append(song.album + ", " + song.artist);
            sb.setSpan(new ForegroundColorSpan(Theme.Resources.getColor(R.color.fp_color_row_subtitle)), song.title.length() + 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            row.getTextView().setText(sb);
            row.getTextView().setTextColor(Theme.Resources.getColor(R.color.fp_color_row_title));
            row.getCoverView().setCover(FpUtilsMedia.TYPE_ALBUM, song.albumId);
        }

        row.highlightRow(position == mHighlightRow);

        return row;
    }

}
/*EOF*/