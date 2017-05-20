package com.example.mborzenkov.readlaterlist.activity.main;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** Запускает {@link AsyncTask} для выполнения длительного действия.
 * Показывает значок загрузки и устанавливает isActive = true, что должно блокировать все другие действия.
 * Показывает notification с прогрессом выполнения.
 * По окончанию разблокирует интерфейс и обновляет список.
 */
public class MainActivityLongTask extends AsyncTask<Runnable, Integer, Void>  {

    /** Признак, есть ли запущенные длительные процессы. */
    private static boolean isActive = false;
    /** Запущенный процесс. */
    private static @Nullable
    MainActivityLongTask runningProcess = null;


    /** Проверяет, выполняется ли сейчас длительное действие.
     *
     * @return true или false, результат
     */
    public static synchronized boolean isActive() {
        return isActive;
    }

    /** Блокирует интерфейс, не вызывая при этом AsyncTask.
     * По завершению работ интерфейс обязательно нужно разблокировать с помощью stopAnotherLongTask().
     *
     * @return true - если все прошло успешно и можно начинать работу, иначе false
     */
    public static synchronized boolean startAnotherLongTask() {
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
    public static synchronized boolean stopAnotherLongTask() {
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
     * @param activity ссылка на MainActivity, где нужно отображать процесс
     * @return true, если выполнение началось или false, если было отклонено
     */
    public static synchronized boolean startLongBackgroundTask(@NonNull Runnable task,
                                                        @NonNull MainActivity activity) {

        // Может выполняться только одно действие
        if (isActive) {
            return false;
        }
        isActive = true;
        runningProcess = new MainActivityLongTask();
        runningProcess.setActivity(activity);
        runningProcess.execute(task, null, null);
        return true;

    }

    /** Меняет Activity у запущенного процесса.
     *
     * @param activity новая Activity или null, если нужно отписаться от AsyncTask.
     */
    static synchronized void swapActivity(@Nullable MainActivity activity) {
        if (runningProcess != null) {
            runningProcess.setActivity(activity);
        }
    }


    /** Activity, в которой нужно отображать выполнение. */
    private @Nullable
    MainActivity mActivity = null;

    /** Создает новый экземпляр класса. */
    private MainActivityLongTask() { }

    /** Устанавливает новую Activity у экземпляра.
     *
     * @param activity новая Activity, может быть null, тогда выполнение не будет отображаться
     */
    private void setActivity(@Nullable MainActivity activity) {
        mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        // Лок от изменений mActivity на null
        synchronized (MainActivityLongTask.class) {
            if (mActivity != null) {
                // mActivity.showLoading(); TODO: [v.0.7.0-MainListFragment] Show loading
            }
        }
    }

    @Override
    protected Void doInBackground(@NonNull Runnable... backgroundTask) {
        backgroundTask[0].run();
        return null;
    }

    @Override
    protected void onPostExecute(Void onFinishTask) {
        synchronized (MainActivityLongTask.class) {
            isActive = false;
            if (mActivity != null) {
               //  mActivity.reloadData(); TODO: [v.0.7.0-MainListFragment] Show loading
            }
        }
        // покажет данные по окончанию
    }

}
