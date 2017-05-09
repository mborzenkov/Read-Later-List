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

    /** ID уведомления. */
    private static final int NOTIFICATION_ID = 100;
    /** Длина индикатора уведомлений. */
    private static final int NOTIFICATION_PROGRESS = 100;


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
    static synchronized boolean startAnotherLongTask() {
        if (isActive) {
            return false;
        }
        isActive = true;
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
                                                                           @Nullable MainListActivity activity) {
        // Может выполняться только одно действие
        if (isActive) {
            return false;
        }
        isActive = true;
        runningProcess = new MainListLongTask();
        runningProcess.setupVisualFeedback(activity);
        runningProcess.execute(task, null, null);
        return true;
    }

    /** Меняет Activity у запущенного процесса.
     *
     * @param activity новая Activity
     */
    static synchronized void swapActivity(@Nullable MainListActivity activity) {
        if (runningProcess != null) {
            runningProcess.setupVisualFeedback(activity);
        }
    }


    /** Activity, в которой нужно отображать выполнение. */
    private @Nullable MainListActivity mActivity = null;
    /** Notification Manager для рассылки уведомлений. */
    private NotificationManager notificationManager = null;
    /** Notification Builder. */
    private NotificationCompat.Builder notificationBuilder = null;

    /** Создает новый экземпляр класса. */
    private MainListLongTask() { }

    /** Устанавливает новую Activity у экземпляра.
     *
     * @param activity новая Activity
     */
    private void setupVisualFeedback(@Nullable MainListActivity activity) {
        mActivity = activity;
        if (mActivity != null && notificationManager == null) {
            notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(mActivity);
            notificationBuilder.setOngoing(true)
                    .setContentTitle("Loading")
                    .setContentText("Long task")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setProgress(0, 0, true);
        }
    }

    @Override
    protected void onPreExecute() {
        // Лок от изменений mActivity на null
        synchronized (MainListLongTask.class) {
            if (mActivity != null) {
                mActivity.showLoading();
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (MainListLongTask.class) {
            if (mActivity != null) {
                int progress = values[0];

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
                notificationBuilder
                        .setProgress(0, 0, false);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        }
        // покажет данные по окончанию
    }

}
