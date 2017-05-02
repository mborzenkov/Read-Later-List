package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.MainListFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainListFilterUtils {

    public static final String FILTER_KEY = "com.example.mborzenkov.mainlist.filter";

    public static final int INDEX_DATE_ALL = 0;

    private static Map<String, MainListFilter> sCustomFilters = null;
    private static final int INDEX_SAVED_DEFAULT = 0;
    private static int sIndexSavedChosen = 0;
    private static int sIndexSavedAdd = 1;
    private static int sIndexSavedDelete = 2;

    private static List<String> sDateFilters = null;
    private static int sIndexDateChosen = 0;

    private static MainListFilter sCurrentFilter = null;


    private MainListFilterUtils() {
        throw new UnsupportedOperationException("Класс MainListFilterUtils - static util, не может иметь экземпляров");
    }

    public static List<String> getSavedFiltersList(Context context) {
        if (sCustomFilters == null) {
            sCustomFilters = new LinkedHashMap<>();
            sCustomFilters.put(context.getString(R.string.mainlist_drawer_filters_default), new MainListFilter());
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
            Map<String, ?> userFilters = sharedPreferences.getAll();
            for (String key : userFilters.keySet()) {
                sCustomFilters.put(key, MainListFilter.fromString((String) userFilters.get(key)));
            }
            sIndexSavedDelete = sCustomFilters.size() - 1;
            sIndexSavedAdd = sIndexSavedDelete - 1;
        }
        List<String> result = new ArrayList<>();
        result.addAll(sCustomFilters.keySet());
        result.add(context.getString(R.string.mainlist_drawer_filters_save));
        result.add(context.getString(R.string.mainlist_drawer_filters_remove));
        return result;
    }

    public static List<String> getsDateFiltersList(Context context) {
        if (sDateFilters == null) {
            sDateFilters = new ArrayList<>();
            sDateFilters.add(context.getString(R.string.mainlist_drawer_date_all));
            sDateFilters.add(context.getString(R.string.mainlist_drawer_date_creation));
            sDateFilters.add(context.getString(R.string.mainlist_drawer_date_modified));
            sDateFilters.add(context.getString(R.string.mainlist_drawer_date_viewed));
        }
        return sDateFilters;
    }

    public static MainListFilter getCurrentFilter() {
        if (sCurrentFilter == null) {
            sCurrentFilter = new MainListFilter();
        }
        return sCurrentFilter;
    }

    public static void clickOnSavedFilter(Context context, int position) {
        if (position == INDEX_SAVED_DEFAULT) {
            sCurrentFilter = new MainListFilter();
        } else {
            Iterator iterator = sCustomFilters.keySet().iterator();
            for (int i = 0; i < position; i++) {
                iterator.next();
            }
            sCurrentFilter = MainListFilter.fromString(sCustomFilters.get(iterator).toString());
        }
        sIndexSavedChosen = position;
    }

    public static void saveFilter(Context context, String name) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, sCurrentFilter.toString());
        editor.apply();
        sCustomFilters = null;
    }

    public static void removeSavedFilter(Context context, int position) {
        Iterator iterator = sCustomFilters.keySet().iterator();
        for (int i = 0; i < position; i++) {
            iterator.next();
        }
        String name = iterator.toString();

        SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(name);
        editor.apply();
        sCustomFilters = null;
    }

    public static void clickOnDateFilter(int position) {

    }

    public static void clickOnColorFilter(int position) {

    }

    public static void clickOnSortBy(int position) {

    }

    public static int getIndexSavedAdd() {
        return sIndexSavedAdd;
    }

    public static int getIndexSavedDelete() {
        return sIndexSavedDelete;
    }

    public static int getIndexSavedChosen() {
        return sIndexSavedChosen;
    }

    public static int getIndexDateChosen() {
        return sIndexDateChosen;
    }

}
