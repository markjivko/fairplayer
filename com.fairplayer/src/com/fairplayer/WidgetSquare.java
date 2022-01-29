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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

/**
 * 2x2 widget that shows cover art in the background and playback controls in
 * a semi-transparent widget on top of the cover.
 */
public class WidgetSquare extends WidgetCommon {
    
    @Override
    protected void _updateWidget(Context context, AppWidgetManager manager, FpTrack song, int state) {
        updateWidget(context, manager, song, state);
    }

    /**
     * Populate the widgets with the given ids with the given info.
     *
     * @param context A Context to use.
     * @param manager The AppWidgetManager that will be used to update the
     * widget.
     * @param song The current FpTrack in FpServiceRendering.
     * @param state The current FpServiceRendering state.
     */
    public static void updateWidget(Context context, AppWidgetManager manager, FpTrack song, int state) {
    	// Prepare the intent
        Intent intent;
        PendingIntent pendingIntent;
        
        // Get the remote views
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fp_widget_square);
        
        // Set the main action
        intent = new Intent(context, ActivityNowplaying.class);
        intent.setAction(Intent.ACTION_MAIN);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.fp_widget_holder, pendingIntent);
        
        do {
            // Default state
            views.setViewVisibility(R.id.fp_warning, View.VISIBLE);
            views.setViewVisibility(R.id.fp_title, View.INVISIBLE);
            views.setViewVisibility(R.id.fp_buttons, View.INVISIBLE);
            views.setViewVisibility(R.id.fp_cover, View.INVISIBLE);
            
            // Nothing here
            if (!FpServiceRendering.hasInstance() || null == song) {
                // Set the background
                views.setImageViewResource(R.id.fp_widget_background, R.drawable.fp_widget_preview_square);
            
                // Set the title
                views.setInt(R.id.fp_warning, "setText", R.string.fp_widget_tap_to_start);
                views.setViewVisibility(R.id.fp_warning, View.VISIBLE);
                
                // Stop here
                break;
            }
            
            // No media
            if ((state & FpServiceRendering.FLAG_NO_MEDIA) != 0) {
                // Set the background
                views.setImageViewResource(R.id.fp_widget_background, R.drawable.fp_widget_preview_square);
                
                // Set the title
                views.setInt(R.id.fp_warning, "setText", R.string.fp_widget_no_media);
                views.setViewVisibility(R.id.fp_warning, View.VISIBLE);
                
                // Stop here
                break;
            }
            
            // Show all
            views.setViewVisibility(R.id.fp_warning, View.GONE);
            views.setViewVisibility(R.id.fp_title, View.VISIBLE);
            views.setViewVisibility(R.id.fp_buttons, View.VISIBLE);
            views.setViewVisibility(R.id.fp_cover, View.VISIBLE);
            
            // Set the background
            views.setImageViewBitmap(R.id.fp_widget_background, FpServiceRendering.getThemedBitmap(R.drawable.fp_bg_widget_square));

            // Set the song title
            views.setTextViewText(R.id.fp_title, song.title);
            views.setTextColor(R.id.fp_title, Theme.Resources.getColor(R.color.fp_color_widget_title));

            // Get the cover
            Bitmap cover = song.getCover(context);
            if (null == cover) {
                cover = FpServiceRendering.getThemedBitmap(R.drawable.fp_albumart);
            }
            views.setImageViewBitmap(R.id.fp_cover, cover);

            // Play/Pause drawable
            views.setImageViewBitmap(R.id.fp_play_pause, FpServiceRendering.getThemedBitmap((state & FpServiceRendering.FLAG_PLAYING) != 0 ? R.drawable.fp_control_pause : R.drawable.fp_control_play));
            
            // Previous drawable
            views.setImageViewBitmap(R.id.fp_previous, FpServiceRendering.getThemedBitmap(R.drawable.fp_control_prev));
            
            // Next drawable
            views.setImageViewBitmap(R.id.fp_next, FpServiceRendering.getThemedBitmap(R.drawable.fp_control_next));

            // Prepare the service
            ComponentName service = new ComponentName(context, FpServiceRendering.class);

            // Play
            intent = new Intent(FpServiceRendering.ACTION_TOGGLE_PLAYBACK).setComponent(service);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.fp_play_pause, pendingIntent);

            // Next
            intent = new Intent(FpServiceRendering.ACTION_NEXT_SONG).setComponent(service);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.fp_next, pendingIntent);

            // Previous
            intent = new Intent(FpServiceRendering.ACTION_PREVIOUS_SONG).setComponent(service);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.fp_previous, pendingIntent);
            
        } while (false);
        
        // Update the widget
        manager.updateAppWidget(new ComponentName(context, WidgetSquare.class), views);
    }
}

/*EOF*/