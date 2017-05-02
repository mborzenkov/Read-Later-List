package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.MainListFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainListFilterUtils {

    public static final String FILTER_KEY = "com.example.mborzenkov.mainlist.filter";

    public static final int INDEX_SAVED_DEFAULT = 0;

    public static final int INDEX_DATE_ALL = 0;

    private static Map<String, MainListFilter> sSavedFilters = null;
    private static List<String> sDateFilters = null;
    private static MainListFilter sCurrentFilter = null;
    private static int sIndexSavedAdd = 1;
    private static int sIndexSavedDelete = 2;

    private MainListFilterUtils() {
        throw new UnsupportedOperationException("Класс MainListFilterUtils - static util, не может иметь экземпляров");
    }

    public static Map<String, MainListFilter> getSavedFiltersContents(Context context) {
        if (sSavedFilters == null) {
            sSavedFilters = new LinkedHashMap<>();
            sSavedFilters.put(context.getString(R.string.mainlist_drawer_filters_default), new MainListFilter());
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
            Map<String, ?> userFilters = sharedPreferences.getAll();
            for (String key : userFilters.keySet()) {
                sSavedFilters.put(key, MainListFilter.fromString((String) userFilters.get(key)));
            }
            sSavedFilters.put(context.getString(R.string.mainlist_drawer_filters_save), null);
            sSavedFilters.put(context.getString(R.string.mainlist_drawer_filters_remove), null);
            sIndexSavedDelete = sSavedFilters.size() - 1;
            sIndexSavedAdd = sIndexSavedDelete - 1;
        }
        return sSavedFilters;
    }

    public static List<String> getsDateFilters(Context context) {
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
        return sCurrentFilter;
    }

    public static void clickOnSavedFilter(int position) {

    }

    public static void saveFilter(String name) {

    }

    public static void removeSavedFilter(int position) {

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

}
