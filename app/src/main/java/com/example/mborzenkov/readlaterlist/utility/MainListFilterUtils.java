package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.MainListFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainListFilterUtils {

    public static final String FILTER_KEY = "com.example.mborzenkov.mainlist.filter";

    public static final int INDEX_DATE_ALL = 0;
    public static final int INDEX_SAVED_DEFAULT = 0;

    private static MainListFilter sCurrentFilter = null;

    private static Map<String, MainListFilter> sCustomFilters = null;
    private static String sCurrentFilterName = null;
    private static int sIndexSavedAdd = 1;
    private static int sIndexSavedDelete = 2;

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
            sIndexSavedAdd = sCustomFilters.size();
            sIndexSavedDelete = sIndexSavedAdd + 1;
        }
        List<String> result = new ArrayList<>();
        result.addAll(sCustomFilters.keySet());
        result.add(context.getString(R.string.mainlist_drawer_filters_save));
        result.add(context.getString(R.string.mainlist_drawer_filters_remove));
        return result;
    }

    public static List<String> getsDateFiltersList(Context context) {
        List<String> dateFilters = new ArrayList<>();
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_all));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_creation));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_modified));
        dateFilters.add(context.getString(R.string.mainlist_drawer_date_viewed));
        return dateFilters;
    }

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

    public static MainListFilter getCurrentFilter() {
        if (sCurrentFilter == null) {
            sCurrentFilter = new MainListFilter();
        }
        return sCurrentFilter;
    }

    public static void clickOnSavedFilter(Context context, int position) {
        if (position == INDEX_SAVED_DEFAULT) {
            sCurrentFilter = new MainListFilter();
            sCurrentFilterName = null;
        } else {
            Iterator iterator = sCustomFilters.keySet().iterator();
            for (int i = 0; i < position; i++) {
                iterator.next();
            }
            sCurrentFilter = sCustomFilters.get(iterator.toString());
            sCurrentFilterName = iterator.toString();
        }
    }

    public static void saveFilter(Context context, String name) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, sCurrentFilter.toString());
        editor.apply();
        sCustomFilters = null; // будет reset
        sCurrentFilterName = name;
        getSavedFiltersList(context);
    }

    public static void removeCurrentFilter(Context context) {
        if (sCurrentFilterName != null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILTER_KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(sCurrentFilterName);
            editor.apply();
            sCustomFilters = null; // будет reset
            sCurrentFilterName = null;
        }
    }

    public static int getIndexSavedAdd() {
        return sIndexSavedAdd;
    }

    public static int getIndexSavedDelete() {
        return sIndexSavedDelete;
    }

    public static int getIndexSavedCurrent() {
        if (sCurrentFilterName != null) {
            Iterator iterator = sCustomFilters.keySet().iterator();
            int position = 0;
            while (iterator.hasNext()) {
                if (iterator.toString().equals(sCurrentFilterName)) {
                    return position;
                }
                position++;
            }
        }
        return INDEX_SAVED_DEFAULT;
    }
}
