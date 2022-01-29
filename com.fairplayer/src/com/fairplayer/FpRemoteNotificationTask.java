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

import java.io.InputStream;
import java.util.Random;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

@SuppressLint("NewApi")
public class FpRemoteNotificationTask extends AsyncTask<String, Void, Bitmap> {

    // Logging tag
    protected static final String TAG = "ThemeTag";

    // The remote view
    RemoteViews remoteView;

    // Current context
    Context context;

    /**
     * Class constructor
     *
     * @param context
     */
    public FpRemoteNotificationTask(Context context) {
        // Store the context
        this.context = context;
        
        // Store the remote view
        this.remoteView = new RemoteViews(context.getPackageName(), R.layout.fp_parse_banner);
        
        // Log this
        Log.d(TAG, "Push notification started");
    }

    /**
     * Perform a background query
     */
    protected Bitmap doInBackground(String... urls) {
        // Prepare the result
        Bitmap bitmapResult = null;

        do {
            // Fallback intent icon
            String intentIcon = "fp_parse_banner_icon_url";
            
            // URL
            if (FpRemoteNotification.intentIcon.matches("^.*?\\:\\/\\/.*$")) {
                try {
                    // Get the input stream
                    InputStream inputStream = new java.net.URL(FpRemoteNotification.intentIcon).openStream();

                    // Store the result
                    bitmapResult = BitmapFactory.decodeStream(inputStream);

                    // Stop here
                    break;
                } catch (Exception e) {
                    // Falback to URL icon
                }
            } else {
            	intentIcon = "fp_parse_banner_icon_" + FpRemoteNotification.intentIcon;
            }

            // Get the drawable resource ID
            int drawableResourceId = this.context.getResources().getIdentifier(intentIcon, "drawable", this.context.getPackageName());

            // Use a local icon
            bitmapResult = BitmapFactory.decodeResource(this.context.getResources(), drawableResourceId);
        } while (false);

        // All done
        return bitmapResult;
    }

    /**
     * Set the layout
     */
    protected void onPostExecute(Bitmap result) {
        // Prepare the builder
        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FpRemoteNotification.intentStyle);
        
        // Custom notification
        if (FpRemoteNotification.intentStyle.equals(FpRemoteNotification.NOTIF_KEY_INTENT_STYLE_CUSTOM)) {
            // Set the icon
            remoteView.setImageViewBitmap(R.id.fp_parse_banner_icon, result);
            
            // Set the background
            int backgroundDrawableResourceId = this.context.getResources().getIdentifier("fp_parse_banner_bkg_" + FpRemoteNotification.intentTheme + "_" + FpRemoteNotification.intentColor, "drawable", this.context.getPackageName());
            remoteView.setInt(R.id.fp_parse_banner, "setBackgroundResource", backgroundDrawableResourceId);

            // Set the button
            int buttonDrawableResourceId = this.context.getResources().getIdentifier("fp_parse_banner_button_" + FpRemoteNotification.intentColor, "drawable", this.context.getPackageName());
            remoteView.setInt(R.id.fp_parse_banner_button, "setBackgroundResource", buttonDrawableResourceId);

            // Set the text
            remoteView.setTextViewText(R.id.fp_parse_banner_button, FpRemoteNotification.intentText);
            
            // Enable the remote view
            builder.setContent(remoteView);
        }
        
        // Set the intent
        Intent contentIntent = new Intent(context, ActivityNowplaying.class);
        
        // Not the index
        if (!FpRemoteNotification.intentActivity.equals(FpRemoteNotification.NOTIF_KEY_INTENT_ACTIVITY_INDEX)) {
            // URL
            if (FpRemoteNotification.intentActivity.matches("^.*?\\:\\/\\/.*$")) {
                // Visit a URL
                contentIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(FpRemoteNotification.intentActivity));
            } else if (FpRemoteNotification.intentActivity.equals(FpRemoteNotification.NOTIF_KEY_INTENT_ACTIVITY_UPDATE)) {
                // Update
                contentIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
            } else {
                // Some other Activity
                try {
                    // Try to dynamically set the intent
                    contentIntent = new Intent(context, Class.forName(context.getPackageName() + "." + FpRemoteNotification.getIntentActivityLocal()));
                } catch (Exception exc) {
                    Log.d(TAG, "Parse Intent: Reverting to Index.class intent");

                    // Revert to the default
                    contentIntent = new Intent(context, ActivityNowplaying.class);
                }
            }
        }
        
        // Add the flags
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // Add the extra parameter
        contentIntent.putExtra(context.getPackageName() + ".fromParse", true);
        
        // Set the notification builder
        builder
            .setSmallIcon(context.getApplicationInfo().icon, 0)
            .setWhen(0)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, (new Random()).nextInt(), contentIntent, 0))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setTicker(FpRemoteNotification.intentText)
            .setContentTitle(FpRemoteNotification.intentText)
            .setContentText(FpRemoteNotification.intentToast);

        // Build
        Notification notification = builder.build();

        // Prepare the notification manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Prepare the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
            new NotificationChannel(
                    FpRemoteNotification.intentStyle, 
                    "Default", 
                    NotificationManager.IMPORTANCE_HIGH
                )
            );
        }

        // Notify
        notificationManager.notify(0, notification);
        
        // Set the extra toast
        if (!FpRemoteNotification.intentToast.equals("")) {
            Toast.makeText(context, FpRemoteNotification.intentToast, Toast.LENGTH_LONG).show();
        }
    }
}

/*EOF*/