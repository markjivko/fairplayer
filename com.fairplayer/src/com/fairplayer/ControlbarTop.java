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
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

public class ControlbarTop extends RelativeLayout implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    /**
     * The application context
     */
    private final Context mContext;
    
    /**
     * The assembled PopupMenu
     */
    private PopupMenu mPopupMenu;
    
    /**
     * Controls
     */
    protected LinearLayout mControls;
    
    /**
     * ControlsContent click consumer, may be null
     */
    private View.OnClickListener mParentClickConsumer;
    
    /**
     * Owner of our options menu and consumer of clicks
     */
    private Activity mParentMenuConsumer;

    public ControlbarTop(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onFinishInflate() {
        // Prepare the controls
        mControls = (LinearLayout) findViewById(R.id.fp_content_controls);
        
        // Get views after inflation
        super.onFinishInflate();
    }

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        if (tag instanceof PopupMenu) {
            openMenu();
        } else {
            if (tag instanceof MenuItem) {
                mParentMenuConsumer.onOptionsItemSelected((MenuItem) tag);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mParentMenuConsumer.onOptionsItemSelected(item);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    /**
     * Sets the ControlsContent to be clickable
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mParentClickConsumer = listener;
    }
    
    /**
     * Boots the options menu
     *
     * @param owner the activity who will receive our callbacks
     */
    @SuppressLint("NewApi")
    public void enableOptionsMenu(Activity owner) {
        mParentMenuConsumer = owner;

        // Prepare the menu button
        ImageButton menuButton = getImageButton(Theme.Resources.getDrawable(R.drawable.fp_panel_menu));
        
        // Prepare the popup menu
        mPopupMenu = new PopupMenu(mContext, menuButton);
        mPopupMenu.setOnMenuItemClickListener(this);

        // Let parent populate the menu
        mParentMenuConsumer.onCreateOptionsMenu(mPopupMenu.getMenu());

        // Add menu button as last item
        menuButton.setTag(mPopupMenu);
        menuButton.setOnClickListener(this);
        menuButton.setLayoutParams(new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.fp_bar_height), RelativeLayout.LayoutParams.WRAP_CONTENT));
        mControls.addView(menuButton, -1);

        // Add a clickable and empty view
        View spacer = new View(mContext);
        spacer.setOnClickListener(this);
        
        spacer.setTag(mPopupMenu);
        spacer.setLayoutParams(new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.fp_playback_top_bar_icon_indent), RelativeLayout.LayoutParams.MATCH_PARENT));
        mControls.addView(spacer, -1);
    }

    /**
     * Opens the OptionsMenu of this view
     */
    public void openMenu() {
        if (mPopupMenu == null || mParentMenuConsumer == null) {
            return;
        }
        mParentMenuConsumer.onPrepareOptionsMenu(mPopupMenu.getMenu());
        mPopupMenu.show();
    }

    /**
     * Returns a new image button to be placed on the bar
     *
     * @param drawable The icon to use
     */
    private ImageButton getImageButton(Drawable drawable) {
        ImageButton button = new ImageButton(mContext);
        button.setBackground(drawable);
        return button;
    }
}

/*EOF*/