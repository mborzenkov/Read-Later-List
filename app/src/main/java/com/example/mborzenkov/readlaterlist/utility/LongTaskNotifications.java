package com.example.mborzenkov.readlaterlist.utility;

import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.example.mborzenkov.readlaterlist.R;

/** Класс для отправки оповещений о длительных операциях. */
public class LongTaskNotifications {

    /** ID уведомления. */
    private static final int NOTIFICATION_ID = 100;
    /** Длина индикатора уведомлений. */
    private static final int NOTIFICATION_PROGRESS = 100;

    /** Notification Manager для рассылки уведомлений. */
    private static @Nullable NotificationManager notificationManager = null;
    /** Notification Builder. */
    private static NotificationCompat.Builder notificationBuilder = null;

    /** Подготавливает новое оповещение с заголовком title, но не запускает его.
     *
     * @param context контекст
     * @param title заголовок оповещения
     */
    public static void setupNotification(Context context, String title) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(context);
        }
        notificationBuilder.setOngoing(true)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setProgress(0, 0, true);
    }

    /** Показывает оповещение с указанным прогрессом.
     *
     * @param progress прогресс от 0 до 100
     * @param infinite признак бесконечного прогресса, если true - будет показан бесконечный прогресс
     */
    public static void showNotificationWithProgress(int progress, boolean infinite) {
        if (notificationManager != null) {
            if (infinite) {
                notificationBuilder.setProgress(0, 0, true);
            } else {
                notificationBuilder.setProgress(NOTIFICATION_PROGRESS, progress, false);
            }
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    /** Отменяет запущенное оповещение. */
    public static void cancelNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private LongTaskNotifications() {
        throw new UnsupportedOperationException("Класс LongTaskNotifications - static, не может иметь экземпляров");
    }

}
