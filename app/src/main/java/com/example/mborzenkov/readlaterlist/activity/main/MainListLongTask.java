package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.R;

/** Запускает AsyncTask для выполнения длительного действия.
 * Показывает значок загрузки и устанавливает isActive = true, что должно блокировать все другие действия.
 * Показывает notification с прогрессом выполнения.
 * По окончанию разблокирует интерфейс и обновляет список.
 */
class MainListLongTask extends AsyncTask<Runnable, Integer, Void>  {

    /** Признак, есть ли запущенные длительные процессы. */
    private static boolean isActive = false;
    /** Запущенный процесс. */
    private static @Nullable MainListLongTask runningProcess = null;


    /** Проверяет, выполняется ли сейчас длительное действие.
     *
     * @return true или false, результат
     */
    static synchronized boolean isActive() {
        return isActive;
    }

    /** Блокирует интерфейс, не вызывая при этом AsyncTask.
     * По завершению работ интерфейс обязательно нужно разблокировать с помощью stopAnotherLongTask().
     *
     * @return true - если все прошло успешно и можно начинать работу, иначе false
     */
    static synchronized boolean startAnotherLongTask(Context context, String notificationtitle) {
        if (isActive) {
            return false;
        }
        isActive = true;
        MainListNotifications.setupNotification(context, notificationtitle);
        MainListNotifications.showNotificationWithProgress(0);
        return true;
    }

    /** Разблокирует интерфейс, не изменяя при этом AsyncTask.
     * Должен вызываться только если был ранее успешно вызван startAnotherLongTask().
     *
     * @return true - если все прошло успешно и можно начинать работу, иначе false
     */
    static synchronized boolean stopAnotherLongTask() {
        if (!isActive) {
            return false;
        }
        isActive = false;
        MainListNotifications.cancelNotification();
        return true;
    }

    /** Начинает выполнение длительного действия.
     * Действие не будет выполнено, если уже выполняется другое длительное действие (isActive == true).
     * Отклоняет другие длительные действия до оконачния выполнения.
     *
     * @param task действие, которое нужно выполнить
     * @param activity ссылка на MainListActivity, где нужно отображать процесс
     * @return true, если выполнение началось или false, если было отклонено
     */
    static synchronized boolean startLongBackgroundTask(Runnable task,
                                                        MainListActivity activity,
                                                        String notificationTitle) {

        // Может выполняться только одно действие
        if (isActive) {
            return false;
        }
        isActive = true;
        runningProcess = new MainListLongTask(activity, notificationTitle);
        runningProcess.execute(task, null, null);
        return true;

    }

    /** Меняет Activity у запущенного процесса.
     *
     * @param activity новая Activity
     */
    static synchronized void swapActivity(@Nullable MainListActivity activity) {
        if (runningProcess != null) {
            runningProcess.setActivity(activity);
        }
    }


    /** Activity, в которой нужно отображать выполнение. */
    private @Nullable MainListActivity mActivity = null;

    /** Создает новый экземпляр класса. */
    private MainListLongTask(@Nullable MainListActivity activity, String notificationTitle) {
        mActivity = activity;
        MainListNotifications.setupNotification(mActivity, notificationTitle);
    }

    /** Устанавливает новую Activity у экземпляра.
     *
     * @param activity новая Activity
     */
    private void setActivity(@Nullable MainListActivity activity) {
        mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        // Лок от изменений mActivity на null
        synchronized (MainListLongTask.class) {
            if (mActivity != null) {
                mActivity.showLoading();
                MainListNotifications.showNotificationWithProgress(0);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (MainListLongTask.class) {
            if (mActivity != null) {
                int progress = values[0];
                MainListNotifications.showNotificationWithProgress(progress);
            }
        }
    }

    @Override
    protected Void doInBackground(Runnable... backgroundTask) {
        backgroundTask[0].run();
        return null;
    }

    @Override
    protected void onPostExecute(Void onFinishTask) {
        synchronized (MainListLongTask.class) {
            isActive = false;
            if (mActivity != null) {
                mActivity.reloadData();
                MainListNotifications.cancelNotification();
            }
        }
        // покажет данные по окончанию
    }

}
