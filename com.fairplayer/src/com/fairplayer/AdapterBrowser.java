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
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.graphics.drawable.Drawable;

public class AdapterBrowser
        extends ArrayAdapter<String>
        implements View.OnClickListener {

    private final ActivityBrowser mActivity;
    private final Drawable mFolderIcon;
    private final LayoutInflater mInflater;

    public AdapterBrowser(ActivityBrowser activity, int resource) {
        super(activity, resource);
        mActivity = activity;
        mFolderIcon = activity.getResources().getDrawable(R.drawable.fp_library_folder);
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view;
        FpElementRowHolder holder;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.fp_row_expandable, null);
            holder = new FpElementRowHolder();
            holder.text = (TextView) view.findViewById(R.id.fp_text);
            holder.cover = (ElementSmallCover) view.findViewById(R.id.fp_cover);
            holder.arrow = (ImageView) view.findViewById(R.id.fp_more);
            
            holder.cover.setImageDrawable(mFolderIcon);
            holder.text.setOnClickListener(this);
            holder.cover.setVisibility(View.VISIBLE);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (FpElementRowHolder) view.getTag();
        }

        String label = getItem(pos);
        holder.id = pos;
        holder.text.setText(label);
        return view;
    }

    @Override
    public void onClick(View view) {
        FpElementRowHolder holder = (FpElementRowHolder) ((View) view.getParent()).getTag();
        mActivity.onDirectoryClicked((int) holder.id);
    }

}