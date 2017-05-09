package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.example.mborzenkov.readlaterlist.R;

// TODO: JDoc
class MainListNotifications {

    /** ID уведомления. */
    private static final int NOTIFICATION_ID = 100;
    /** Длина индикатора уведомлений. */
    private static final int NOTIFICATION_PROGRESS = 100;

    /** Notification Manager для рассылки уведомлений. */
    private static NotificationManager notificationManager = null;
    /** Notification Builder. */
    private static NotificationCompat.Builder notificationBuilder = null;

    static void setupNotification(Context context, String title) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(context);
        }
        notificationBuilder.setOngoing(true)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setProgress(NOTIFICATION_PROGRESS, 0, false);
    }

    static void showNotificationWithProgress(int progress) {
        notificationBuilder.setProgress(NOTIFICATION_PROGRESS, progress, false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    static void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private MainListNotifications() {
        throw new UnsupportedOperationException("Класс MainListNotifications - static, не может иметь экземпляров");
    }

}
