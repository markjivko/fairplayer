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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

public class ControlbarBottom extends LinearLayout
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    /**
     * The application context
     */
    private final Context mContext;
    /**
     * The title of the currently playing song
     */
    private TextView mTitle;
    /**
     * The artist of the currently playing song
     */
    private TextView mArtist;
    /**
     * Cover image
     */
    private ImageView mCover;
    /**
     * A layout hosting the song information
     */
    private LinearLayout mControlsContent;
    /**
     * Standard android search view
     */
    private SearchView mSearchView;
    /**
     * The assembled PopupMenu
     */
    private PopupMenu mPopupMenu;
    /**
     * ControlsContent click consumer, may be null
     */
    private View.OnClickListener mParentClickConsumer;
    /**
     * Owner of our options menu and consumer of clicks
     */
    private Activity mParentMenuConsumer;

    public ControlbarBottom(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onFinishInflate() {
        
        this.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_bar));
        mTitle = (TextView) findViewById(R.id.fp_title);
        mArtist = (TextView) findViewById(R.id.fp_artist);
        mCover = (ImageView) findViewById(R.id.fp_cover);
        mSearchView = (SearchView) findViewById(R.id.fp_search_view);
        mControlsContent = (LinearLayout) findViewById(R.id.fp_content_controls);

        // Set the title color
        mTitle.setTextColor(Theme.Resources.getColor(R.color.fp_color_bar_title));
        
        // Set the subtitle color
        mArtist.setTextColor(Theme.Resources.getColor(R.color.fp_color_bar_subtitle));
        
        // Style the search view
        styleSearchView(mSearchView, Theme.Resources.getColor(R.color.fp_color_bar_title));

        // All done
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
            } else {
                if (view == mControlsContent && mParentClickConsumer != null) {
                    // dispatch this click to parent, claiming it came from
                    // the top view (= this)
                    mParentClickConsumer.onClick(this);
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mParentMenuConsumer.onOptionsItemSelected(item);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Forcefully hide (and clear) search as we are not going to restore the state
        showSearch(false);
        return super.onSaveInstanceState();
    }

    /**
     * Sets the ControlsContent to be clickable
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mParentClickConsumer = listener;
        mControlsContent.setOnClickListener(this);
    }

    /**
     * Configures a query text listener for the search view
     */
    public void setOnQueryTextListener(SearchView.OnQueryTextListener owner) {
        mSearchView.setOnQueryTextListener(owner);
    }

    /**
     * Boots the options menu
     *
     * @param owner the activity who will receive our callbacks
     */
    @SuppressLint("NewApi")
    public void enableOptionsMenu(Activity owner) {
        mParentMenuConsumer = owner;

        ImageButton menuButton = getImageButton(Theme.Resources.getDrawable(R.drawable.fp_panel_menu));
        mPopupMenu = new PopupMenu(mContext, menuButton);
        mPopupMenu.setOnMenuItemClickListener(this);

        // Let parent populate the menu
        mParentMenuConsumer.onCreateOptionsMenu(mPopupMenu.getMenu());

        // The menu is now ready, we an now add all invisible
        // items with an icon to the toolbar
        Menu menu = mPopupMenu.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.isVisible() == false && menuItem.getIcon() != null) {
            	// Prepare the drawable name
            	int buttonDrawable = 0;
            	switch (menuItem.getItemId()) {
                    case ActivityCommon.MENU_SEARCH:
                        buttonDrawable = R.drawable.fp_panel_search;
                        break;
            	}
            	
            	// Valid drawable found
            	if (0 != buttonDrawable) {
                    // Prepare the button
                    ImageButton button = getImageButton(Theme.Resources.getDrawable(buttonDrawable));

                    // Tag it
                    button.setTag(menuItem);

                    // Prepare the button
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.fp_bar_icon_size), (int) getResources().getDimension(R.dimen.fp_bar_icon_size));
                    
                    // Set the margins
                    buttonParams.setMargins(0, 0, (int) getResources().getDimension(R.dimen.fp_bar_icon_indent), 0);
                    
                    // Store the parameters
                    button.setLayoutParams(buttonParams);
                    
                    // Set the padding
                    button.setPadding(0, (int) getResources().getDimension(R.dimen.fp_bar_icon_padding_top), 0, 0);

                    // Set the on-click listener
                    button.setOnClickListener(this);

                    // Add to the view
                    mControlsContent.addView(button, -1);
            	}
            }
        }

        // Add menu button as last item
        menuButton.setTag(mPopupMenu);
        menuButton.setOnClickListener(this);
        menuButton.setLayoutParams(new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.fp_bar_height), LinearLayout.LayoutParams.WRAP_CONTENT));
        mControlsContent.addView(menuButton, -1);

        // Add a clickable and empty view
        // We will use this to add a margin to the menu button if required
        // Note that the view will ALWAYS be present, even if it is 0dp wide to keep
        // the menu button at position -2
        View spacer = new View(mContext);
        spacer.setOnClickListener(this);
        spacer.setTag(mPopupMenu);
        spacer.setLayoutParams(new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.fp_bar_icon_indent), LinearLayout.LayoutParams.MATCH_PARENT));
        mControlsContent.addView(spacer, -1);
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
     * Sets the search view to given state
     *
     * @param visible enables or disables the search box visibility
     *
     * @return boolean old state
     */
    public boolean showSearch(boolean visible) {
        boolean wasVisible = mSearchView.getVisibility() == View.VISIBLE;
        if (wasVisible != visible) {
            mSearchView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mControlsContent.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (visible) {
                mSearchView.setIconified(false); // requests focus AND shows the soft keyboard even if the view already was expanded
            } else {
                mSearchView.setQuery("", false);
            }
        }
        return wasVisible;
    }

    /**
     * Updates the cover image of this view
     *
     * @param cover the bitmap to display. Will use a placeholder image if cover
     * is null
     */
    public void setCover(Bitmap cover) {
        if (cover == null) {
            mCover.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_library_songs));
        } else {
            mCover.setImageBitmap(cover);
        }
    }

    /**
     * Updates the song metadata
     *
     * @param song the song info to display, may be null
     */
    public void setSong(FpTrack song) {
        if (song == null) {
            mTitle.setText(null);
            mArtist.setText(null);
            mCover.setImageBitmap(null);
        } else {
            Resources res = mContext.getResources();
            String title = song.title == null ? res.getString(R.string.fp_library_unknown) : song.title;
            String artist = song.artist == null ? res.getString(R.string.fp_library_unknown) : song.artist;
            mTitle.setText(title);
            mArtist.setText(artist);
        }
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

    /**
     * Changing the colors of a search view is a MAJOR pain using XML
     * This cheap trick just loop trough the view and changes the
     * color of all text- and image views to 'style'
     *
     * @param view the view to search
     * @param color the color to apply
     */
    private void styleSearchView(View view, int color) {
        if (view != null) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(color);
            } else {
                if (view instanceof ImageView) {
                    ((ImageView) view).setColorFilter(color);
                } else {
                    if (view instanceof ViewGroup) {
                        ViewGroup group = (ViewGroup) view;
                        for (int i = 0; i < group.getChildCount(); i++) {
                            styleSearchView(group.getChildAt(i), color);
                        }
                    }
                }
            }
        }
    }
}

/*EOF*/