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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import com.fairplayer.Ads;

public class FpNotification extends BroadcastReceiver {

    @SuppressWarnings({"deprecation"})
    @Override
    public void onReceive(Context context, Intent incomingIntent) {
        // Read the intent id
        int intentId = incomingIntent.getIntExtra("id", -1);
        
        // "Rate us" notification
        if (intentId == Constants.Notification.Default.ID) {
            // Check the internet
            Ads.checkForInternetLive();

            // Not online
            if(!Ads.isOnline) {
                // Delay notification
                try {
                    // New intent
                    Intent intent = new Intent(context, FpNotification.class);

                    // Store the request code
                    intent.putExtra("id", intentId);

                    // Prepare the intent sender
                    PendingIntent intentSender = PendingIntent.getBroadcast(context, intentId, intent, 0);

                    // Get the alarm manager
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    // Set the alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 120 * 1000, intentSender);

                    // Log the delay
                    Log.d(Constants.LOG_TAG, "Delaying notification #" + intentId);

                    // Stop here
                    return;
                } catch (Exception exc) {
                    // Nothing to do
                }
            }
        }
        
        // Get the loyalty program
        FpLoyalty loyalty = new FpLoyalty(context);
        
        // Loyalty notification
        if (intentId == Constants.Notification.Loyalty.ID) {
            // Get the next notification time
            int loyaltyNextNotifTime = loyalty.getNextCheck();

            // Last level notification will return -1
            if (loyaltyNextNotifTime > 0) {
                try {
                    // New intent
                    Intent intent = new Intent(context, FpNotification.class);

                    // Store the request code
                    intent.putExtra("id", intentId);

                    // Prepare the intent sender
                    PendingIntent intentSender = PendingIntent.getBroadcast(context, intentId, intent, 0);

                    // Get the alarm manager
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    // Set the alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + loyaltyNextNotifTime * 1000, intentSender);

                    // Log the delay
                    Log.d(Constants.LOG_TAG, "Delaying notification #" + intentId);
                } catch (Exception exc) {
                    // Nothing to do
                }
            }
        }

        // Log this notification start
        Log.d(Constants.LOG_TAG, "Started notification for intent #" + intentId);

        // Prepare the variables
        String notificationTitle = null;
        CharSequence notificationFrom = null;
        CharSequence notificationMessage = null;
        Intent notificationIntent = null;

        switch (intentId) {
            case Constants.Notification.Default.ID:
                // Prepare the title
                notificationTitle = (String) context.getString(R.string.fp_notification_rate_title);

                // Prepare the intent
                notificationIntent = new Intent(context, FpNotificationHelper.class);

                // Notification from
                notificationFrom = (CharSequence) context.getString(R.string.fp_notification_rate_title);

                // Notification message
                notificationMessage = (CharSequence) context.getString(R.string.fp_notification_rate_message);
                break;

            case Constants.Notification.Loyalty.ID:
            	// Store the context
            	if (null == ActivityCommon.getContext()) {
                    ActivityCommon.setContext(context);
            	}
            	
            	// Get the stored level
            	int storedLevel = PreferenceUtils.getPreferences(context).getInt(Constants.Keys.SETTINGS_ADS_CURRENT_LEVEL, Constants.Defaults.SETTINGS_ADS_CURRENT_LEVEL);
            	int currentLevel = loyalty.getCurrentLevel();
            	
            	// Level change
            	if (storedLevel != currentLevel) {
                    // Get the editor
                    SharedPreferences.Editor editor = PreferenceUtils.edit();

                    // Store the value
                    editor.putInt(Constants.Keys.SETTINGS_ADS_CURRENT_LEVEL, currentLevel);
                    editor.commit();

                    // Prepare the title
                    notificationTitle = (String) context.getString(R.string.fp_notification_loyalty_title);

                    // Prepare the intent
                    notificationIntent = new Intent(context, FpNotificationHelper.class);
                    notificationIntent.putExtra("loyalty", true);

                    // Notification from
                    notificationFrom = (String) context.getString(R.string.fp_notification_loyalty_title);

                    // Notification message
                    notificationMessage = (String) context.getString(R.string.fp_notification_loyalty_message, currentLevel);
            	}
            	break;
                
            case Constants.Notification.UpdateCheck.ID:
                // Prepare the title
                notificationTitle = (String) context.getString(R.string.fp_notification_vc_title);

                // Prepare the intent
                notificationIntent = new Intent(context, FpNotificationHelper.class);

                // Notification from
                notificationFrom = (CharSequence) context.getString(R.string.fp_notification_vc_title);

                // Notification message
                notificationMessage = (CharSequence) context.getString(R.string.fp_notification_vc_message);
                break;
                
            default:
            // Do nothing
        }

        // Notification prepared
        if (null != notificationTitle) {
            try {
                // Set the pendingintent
                PendingIntent pendingIntent = PendingIntent.getService(context, intentId, notificationIntent, 0);

                // Prepare the notification builder
                final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, createChannel(context, "core"))
                    .setSmallIcon(R.drawable.icon)
                    .setTicker(notificationTitle)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(notificationFrom)
                    .setContentText(notificationMessage)
                    .setContentIntent(pendingIntent);

                // Prepare the notification
                Notification notification = builder.getNotification();

                // Set the flags
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                // Get the notification manager
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                // Prepare the notification
                notificationManager.notify(intentId + 100, notification);
            } catch (Exception exc) {}
        }
    }

    /**
     * Set notification alarm
     */
    public static void setNotificationAlarm(Context context, int time, int requestCode) {
        // Log the preferences
        Log.d(Constants.LOG_TAG, "Setting alarm #" + requestCode + " for " + time + " seconds...");

        // New intent
        Intent intent = new Intent(context, FpNotification.class);

        // Store the request code
        intent.putExtra("id", requestCode);

        // Prepare the intent sender
        PendingIntent intentSender = PendingIntent.getBroadcast(context, requestCode, intent, 0);

        // Get the alarm manager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set the alarm
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time * 1000, intentSender);
    }

    @TargetApi(26)
    public static String createChannel(Context context, String channelName) {
        // Android 8.1 requires channels
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Prepare the channel
                NotificationChannel mChannel = new NotificationChannel(channelName, "com.fairplayer", NotificationManager.IMPORTANCE_LOW);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.BLUE);

                // Prepare the notification manager
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                // Valid notification manager
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(mChannel);
                    return channelName;
                }
            } catch (Exception exc) {
                // Nothing to do
            }
    	}
        
        // Something went wrong
        return "";
    } 
}

/*EOF*/