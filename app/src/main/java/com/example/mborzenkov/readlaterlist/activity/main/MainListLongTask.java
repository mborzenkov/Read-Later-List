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
class MainListLongTask extends AsyncTask<Runnable, Integer, Void> {

    private static boolean isActive = false;

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
     * @return true, если действие запущено и false, если отклонено
     */
    static synchronized boolean startLongBackgroundTask(Runnable task, MainListActivity activity) {
        // Может выполняться только одно действие
        if (isActive) {
            return false;
        }
        isActive = true;
        new MainListLongTask(activity).execute(task, null, null);
        return true;
    }

    private MainListActivity mActitivy;

    // Создает новый экземпляр класса
    private MainListLongTask(MainListActivity activity) {
        mActitivy = activity;
    }

    @Override
    protected void onPreExecute() {
        mActitivy.showLoading();
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
        }
        mActitivy.reloadData();
        // покажет данные по окончанию
    }

}
