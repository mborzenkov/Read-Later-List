package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Вспомогательный static util класс для работы с фильтрами. Соединяет Activity с MainListFilter. */
public class MainListFilterUtils {

    /** Ключ для хранения фильтров в SharedPreferences. */
    private static final String FILTER_KEY = "com.example.mborzenkov.mainlist.filter";

    /** Индекс варианта "Без фильтра по датам". */
    public static final int INDEX_DATE_ALL = 0;
    /** индекс варианта "Default" в сохраненных фильтрах. */
    public static final int INDEX_SAVED_DEFAULT = 0;

    /** Текущий фильтр. */
    private static MainListFilter sCurrentFilter = null;

    /** Сохраненные фильтры. */
    private static Map<String, MainListFilter> sCustomFilters = null;
    /** Имя текущего фильтра. Может быть null, что означает - по умолчанию. */
    private static @Nullable String sCurrentFilterName = null;
    /** Индекс варианта "+ Добавить" в сохраненных фильтрах. Изменяется. */
    private static int sIndexSavedAdd = 1;
    /** Индекс варианта "- Удалить" в сохраненных фильтрах. Изменяется. */
    private static int sIndexSavedDelete = 2;

    private MainListFilterUtils() {
        throw new UnsupportedOperationException("Класс MainListFilterUtils - static util, не может иметь экземпляров");
    }

    /** Обновляет список sCustomFilters, получая данные из Shared Preferences.
     *
     * @param context Контекст
     */
    private static void reloadCustomFilters(Context context) {
        sCustomFilters = new LinkedHashMap<>();
        sCustomFilters.put(context.getString(R.string.mainlist_drawer_filters_default), new MainListFilter());
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
        Map<String, ?> userFilters = sharedPreferences.getAll();
        for (Map.Entry<String, ?> key : userFilters.entrySet()) {
            sCustomFilters.put(key.getKey(), MainListFilter.fromString((String) key.getValue()));
        }
        sIndexSavedAdd = sCustomFilters.size();
        sIndexSavedDelete = sIndexSavedAdd + 1;
    }

    /** Возвращает список имен сохраненных фильтров (включая специальные: Default, + Добавить, - Удалить).
     *
     * @param context Контекст
     * @return Список имен сохраненных фильтров для использования в выпадающем списке
     */
    public static List<String> getSavedFiltersList(Context context) {
        if (sCustomFilters == null) {
            reloadCustomFilters(context);
        }
        List<String> result = new ArrayList<>();
        result.addAll(sCustomFilters.keySet());
        result.add(context.getString(R.string.mainlist_drawer_filters_save));
        result.add(context.getString(R.string.mainlist_drawer_filters_remove));
        return result;
    }

    /** Возвращает список имен отборов (4 шт. как в MainListFilter.Selection).
     *
     * @param context Контекст
     * @return Список имен отборов для использования в выпадающем списке
     */
    public static List<String> getsDateFiltersList(Context context) {
        List<String> dateFilters = new ArrayList<>();
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_all));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_creation));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_modified));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_viewed));
        return dateFilters;
    }

    /** Возвращает соответствующий позиции в списке объект MainListFilter.Selection.
     *
     * @param position Позиция в списке
     * @return Соответствущий объект MainListFilter.Selection
     */
    public static MainListFilter.Selection getDateFilterSelection(int position) {
        switch (position) {
            case 1:
                return MainListFilter.Selection.DATE_CREATED;
            case 2:
                return MainListFilter.Selection.DATE_MODIFIED;
            case 3:
                return MainListFilter.Selection.DATE_VIEWED;
            default:
                return MainListFilter.Selection.ALL;
        }
    }

    /** Возвращает текущий выбранный фильтр.
     *
     * @return Текущий выбранный фильтр.
     */
    public static MainListFilter getCurrentFilter() {
        if (sCurrentFilter == null) {
            sCurrentFilter = new MainListFilter();
        }
        return sCurrentFilter;
    }

    /** Обрабатывает выбор сохраненного фильтра.
     *
     * @param position Позиция
     */
    public static void clickOnSavedFilter(int position) {
        if (position == INDEX_SAVED_DEFAULT) {
            sCurrentFilter = new MainListFilter();
            sCurrentFilterName = null;
        } else {
            Iterator iterator = sCustomFilters.keySet().iterator();
            for (int i = 0; i < position; i++) {
                iterator.next();
            }
            sCurrentFilterName = iterator.next().toString();
            sCurrentFilter = sCustomFilters.get(sCurrentFilterName);
        }
    }

    /** Обработчик сохранения выбранного фильтра.
     *
     * @param context Контекст
     * @param name Имя для сохранения
     */
    public static void saveFilter(Context context, String name) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, sCurrentFilter.toString());
        editor.apply();
        reloadCustomFilters(context);
        sCurrentFilterName = name;
    }

    /** Обработчик удаления выбранного фильтра.
     *
     * @param context Контекст
     */
    public static void removeCurrentFilter(Context context) {
        if (sCurrentFilterName != null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(sCurrentFilterName);
            editor.apply();
            reloadCustomFilters(context);
            sCurrentFilterName = null;
        }
    }

    /** Возвращает индекс варианта "+ Добавить" в сохраненных фильтрах.
     *
     * @return Индекс
     */
    public static int getIndexSavedAdd() {
        return sIndexSavedAdd;
    }

    /** Возвращает индекс варианта "- Удалить" в сохраненных фильтрах.
     *
     * @return Индекс
     */
    public static int getIndexSavedDelete() {
        return sIndexSavedDelete;
    }

    /** Возвращает индекс текущего выбранного элемента в списке сохраненных фильтров.
     *
     * @return Индекс
     */
    public static int getIndexSavedCurrent() {
        if (sCurrentFilterName != null) {
            Iterator iterator = sCustomFilters.keySet().iterator();
            int position = 0;
            while (iterator.hasNext()) {
                if (iterator.next().toString().equals(sCurrentFilterName)) {
                    return position;
                }
                position++;
            }
        }
        return INDEX_SAVED_DEFAULT;
    }
}
