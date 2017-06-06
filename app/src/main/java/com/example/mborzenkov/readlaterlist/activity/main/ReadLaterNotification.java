package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.example.mborzenkov.readlaterlist.R;

/** Класс для отправки уведомлений. */
class ReadLaterNotification {

    /** Идентификатор первого уведомления. */
    private static final int NOTIFICATION_ID_FROM = 99;
    /** Длина индикатора уведомлений. */
    private static final int NOTIFICATION_MAX_PROGRESS = 100;

    /** Текущий максимальный идентификатор. */
    private static int sCurrentMaxId = NOTIFICATION_ID_FROM;

    /** Возвращает новый уникальный идентификатор (на 1 больше предыдущего выданного).
     * При этом увеличивает счетчик на 1.
     */
    static synchronized int nextId() {
        sCurrentMaxId++;
        return sCurrentMaxId;
    }


    /** ReadLaterNotification Manager для рассылки уведомлений. */
    private final @NonNull NotificationManager mNotificationManager;
    /** ReadLaterNotification Builder. */
    private final @NonNull NotificationCompat.Builder mNotificationBuilder;
    /** Уникальный идентификатор этого уведомления. */
    private final @IntRange(from = NOTIFICATION_ID_FROM) int mNotificationId;

    /** Подготавливает новое оповещение с заголовком title, но не запускает его.
     * Устанавливает для оповещения новый уникальный идентификатор.
     *
     * @param context контекст
     * @param title заголовок оповещения
     * @param id уникальный идентификатор для уведомления
     */
    ReadLaterNotification(@NonNull Context context, @NonNull String title, @IntRange(from = NOTIFICATION_ID_FROM) int id) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(context);
        mNotificationBuilder.setOngoing(true)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setProgress(0, 0, true);
        mNotificationId = id;
    }

    /** Показывает оповещение с указанным прогрессом.
     *
     * @param progress прогресс от 0 до 100
     * @param infinite признак бесконечного прогресса, если true - будет показан бесконечный прогресс
     */
    void showWithProgress(int progress, boolean infinite) {
        if (infinite) {
            mNotificationBuilder.setProgress(0, 0, true);
        } else {
            mNotificationBuilder.setProgress(NOTIFICATION_MAX_PROGRESS, progress, false);
        }
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }

    /** Отменяет запущенное оповещение. */
    void cancel() {
        mNotificationManager.cancel(mNotificationId);
    }

}
