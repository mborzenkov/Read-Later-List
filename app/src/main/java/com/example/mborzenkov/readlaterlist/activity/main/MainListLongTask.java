package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.EditItemActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

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
        runningProcess.setActivity(activity);
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
    private @Nullable MainListActivity mActitivy = null;

    /** Создает новый экземпляр класса. */
    private MainListLongTask() { }

    /** Устанавливает новую Activity у экземпляра.
     *
     * @param activity новая Activity
     */
    private void setActivity(@Nullable MainListActivity activity) {
        mActitivy = activity;
    }

    @Override
    protected void onPreExecute() {
        // Лок от изменений mActivity на null
        synchronized (MainListLongTask.class) {
            if (mActitivy != null) {
                mActitivy.showLoading();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
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
            if (mActitivy != null) {
                mActitivy.reloadData();
            }
        }
        // покажет данные по окончанию
    }

}
