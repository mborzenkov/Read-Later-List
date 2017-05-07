package com.example.mborzenkov.readlaterlist.data;

import android.content.Context;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.activity.MainListActivity;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Этот класс представляет собой фильтр для MainList. */
public class MainListFilter {

    /** Типы сортировок. */
    public enum SortType {
        LABEL(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL),
        DATE_CREATED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED),
        DATE_MODIFIED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED),
        DATE_VIEWED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW);

        private final String columnName;
        SortType(String columnName) {
            this.columnName = columnName;
        }

        private String getColumnName() {
            return columnName;
        }
    }

    /** Варианты порядков сортировок. */
    public enum SortOrder {
        ASC("ASC"),
        DESC("DESC");

        private final String orderByQuery;
        SortOrder(String orderByQuery) {
            this.orderByQuery = orderByQuery;
        }

        private String getOrderByQuery() {
            return orderByQuery;
        }
    }

    /** Варианты фильтров. */
    public enum Selection {
        ALL(0, null),
        DATE_CREATED(1, ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED),
        DATE_MODIFIED(2, ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED),
        DATE_VIEWED(3, ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW);

        private final String columnName;
        private final int position;
        Selection(int position, String columnName) {
            this.position = position;
            this.columnName = columnName;
        }

        private String getColumnName() {
            return columnName;
        }

        public int getPosition() {
            return position;
        }
    }

    private SortType sortBy;
    private SortOrder sortOrder;
    private Selection selection;
    private Long dateFrom;
    private Long dateTo;
    private Set<Integer> colorFilter;

    /** Создает новый объект с данными по умолчанию. */
    public MainListFilter() {
        // default
        sortBy = SortType.LABEL;
        sortOrder = SortOrder.ASC;
        selection = Selection.ALL;
        dateFrom = null;
        dateTo = null;
        colorFilter = new HashSet<>();
    }

    /** Создает объект из строки.
     * Подходящая строка возвращается объектом через toString().
     *
     * @param filterString Строка подходящего формата
     * @return Объект
     */
    public static MainListFilter fromString(String filterString) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<MainListFilter> jsonAdapter = moshi.adapter(MainListFilter.class);
        MainListFilter filter;
        try {
            filter = jsonAdapter.fromJson(filterString);
        } catch (IOException e) {
            Log.e("Parser error", "Ошибка разбора fromString из: " + filterString);
            filter = new MainListFilter();
        }
        return filter;
    }

    @Override
    public String toString() {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<MainListFilter> jsonAdapter = moshi.adapter(MainListFilter.class);
        return jsonAdapter.toJson(this);
    }

    /** Создает строку ORDER BY на основании объекта.
     *
     * @return Строка формата field ASC
     */
    public String getSqlSortOrder() {
        return String.format(Locale.US, "%s %s", sortBy.getColumnName(), sortOrder.getOrderByQuery());
    }

    /** Создает строку отбора WHERE на основании объекта.
     * Переменные заменены на ?.
     * В фильтр по цветам попадают только те цвета, которые присутствуют в избранных (хотя сохранены могут и другие).
     *
     * @param context Контекст
     * @return Строка специального формата
     */
    public String getSqlSelection(Context context) {
        StringBuilder sqlSelectionString = new StringBuilder();
        if (selection != Selection.ALL) {
            if (dateFrom != null) {
                sqlSelectionString.append(selection.getColumnName()).append(">=?");
            }
            if (dateTo != null) {
                if (dateFrom != null) {
                    sqlSelectionString.append(" AND ");
                }
                sqlSelectionString.append(selection.getColumnName()).append("<=?");
            }
        }
        if (!colorFilter.isEmpty()) {
            int[] favColors = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(context, null);
            Set<Integer> realColors = new HashSet<>();
            for (int color : favColors) {
                if (colorFilter.contains(color)) {
                    realColors.add(color);
                }
            }
            if (!realColors.isEmpty()) {
                if (selection != Selection.ALL && (dateFrom != null | dateTo != null)) {
                    sqlSelectionString.append(" AND ");
                }
                sqlSelectionString.append(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR).append(" IN (");
                for (Integer color : realColors) {
                    sqlSelectionString.append("?,");
                }
                sqlSelectionString.delete(sqlSelectionString.length() - 1, sqlSelectionString.length()).append(')');
            }
        }
        return sqlSelectionString.toString();
    }

    /** Создает selectionArgs на основании объекта.
     * selectionArgs ровно столько, сколько "?" в getSqlSelection и порядок у них соответствующий.
     *
     * @param context Контекст
     * @return Набор аргументов
     */
    public String[] getSqlSelectionArgs(Context context) {
        List<String> selectionArgs = new ArrayList<>();
        if (selection != Selection.ALL) {
            if (dateFrom != null) {
                selectionArgs.add(String.valueOf(dateFrom));
            }
            if (dateTo != null) {
                selectionArgs.add(String.valueOf(dateTo));
            }
        }
        if (!colorFilter.isEmpty()) {
            int[] favColors = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(context, null);
            Set<Integer> realColors = new HashSet<>();
            for (int color : favColors) {
                if (colorFilter.contains(color)) {
                    realColors.add(color);
                }
            }
            for (Integer color : realColors) {
                selectionArgs.add(String.valueOf(color));
            }
        }
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    /** Возвращает форматированную дату "от". */
    public String getDateFrom() {
        if (dateFrom != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(MainListActivity.FORMAT_DATE, Locale.US);
            return sdf.format(dateFrom);
        } else {
            return "";
        }
    }

    /** Возвращает форматированную дату "до". */
    public String getDateTo() {
        if (dateTo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(MainListActivity.FORMAT_DATE, Locale.US);
            return sdf.format(dateTo);
        } else {
            return "";
        }
    }

    public void setDateFrom(long dateMs) {
        dateFrom = dateMs;
    }

    public void setDateTo(long dateMs) {
        dateTo = dateMs;
    }

    public SortType getSortType() {
        return sortBy;
    }

    public void setSortType(SortType sortType) {
        sortBy = sortType;
        sortOrder = SortOrder.ASC;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void nextSortOrder() {
        sortOrder = sortOrder == SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    }

    public Selection getSelection() {
        return selection;
    }

    public void setSelection(Selection newSelection) {
        selection = newSelection;
    }

    public Set<Integer> getColorFilter() {
        return colorFilter;
    }

    public void addColorFilter(int color) {
        colorFilter.add(color);
    }

    public void removeColorFilter(int color) {
        colorFilter.remove(color);
    }
}
