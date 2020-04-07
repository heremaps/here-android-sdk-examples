/*
 * Copyright (c) 2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.backgroundpositioningexample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * Helper class for creating and update Android notifications.
 */
public class NotificationUtils {

    /** Android notification ID. */
    private static final int NOTIFICATION_ID = 1;
    /** Android notification channel ID. */
    private static final String NOTIFICATION_CHANNEL_ID = "foreground_channel_1";
    /** Android context instance. */
    private final Context mContext;
    /** Pending intent instance, which will be triggered when notification is tapped. */
    private final PendingIntent mContentIntent;

    /**
     * Constructor.
     * @param context Android context instance.
     */
    public NotificationUtils(Context context, PendingIntent contentIntent) {
        mContext = context;
        mContentIntent = contentIntent;
    }

    /**
     * Get and return notification ID.
     * @return Notification ID.
     */
    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    /**
     * Sets up notification channel.
     */
    public void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    mContext.getText(R.string.notificationChannelName),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(mContext.getString(R.string.notificationChannelDescription));
            notificationChannel.setLightColor(Color.YELLOW);
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * Create new foreground service notification instance.
     * @param title Title resource ID.
     * @param message Message resource ID.
     */
    public Notification createNotification(int icon, int title, int message) {
        return createNotification(icon, mContext.getText(title), mContext.getText(message));
    }

    /**
     * Update foreground service notification.
     * @param icon Notification small icon ID.
     * @param title Title resource ID.
     * @param message Message resource ID.
     */
    public void updateNotification(int icon, int title, int message) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(icon, title, message));
        }
    }

    /**
     * Create new foreground service notification instance.
     * @param icon Notification small icon ID.
     * @param title Title character sequence.
     * @param message Message character sequence.
     * @return New Android foreground service notification instance.
     */
    public Notification createNotification(int icon, CharSequence title, CharSequence message) {
        return new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(mContentIntent)
                .setOnlyAlertOnce(true)
                .build();
    }
}
