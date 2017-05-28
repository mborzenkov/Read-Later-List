package com.example.mborzenkov.readlaterlist.adt;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Изменяемый тип данных, представляющий фильтр для списка ReadLaterItem. */
public class MainListFilter {

    /////////////////////////
    // Константы и перечисления

    /** Формат даты для вывода на формах Drawer. */
    static final String FORMAT_DATE = "dd/MM/yy";

    /** Типы сортировок. */
    public enum SortType {
        MANUAL(ReadLaterContract.ReadLaterEntry.COLUMN_ORDER),
        LABEL(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL),
        DATE_CREATED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED),
        DATE_MODIFIED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED),
        DATE_VIEWED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW);

        /** Имя соответствующей колонки в базе данных. */
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

        /** Ключевое слово в запросе, соответствующее направлению сортировки, ASC или DESC. */
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

        /** Имя соответствующей колонки в базе данных. */
        private final String columnName;
        /** Позиция фильтра для вывода в списках фильтров. */
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


    /////////////////////////
    // Поля объекта

    // Инвариант:
    //      sortBy      - текущая сортировка
    //      sortOrder   - направление сортировки
    //      selection   - текущий отбор
    //      dateFrom    - левая граница дат в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT),
    //                          может быть null (если не применяется)
    //      dateTo      - правая граница дат в формате timestamp, может быть null (если не применяется)
    //      colorFilter - текущий фильтр по цветам, содержит числовые значения цветов,
    //                          может быть пустым (если не применяется)
    //
    // Абстрактная функция:
    //      представляет фильтр списка элементов ReadLaterItem с возможностью изменения параметров фильтрации
    //          и получения SQL запросов для работы с базой данных
    //
    // Безопасность представления:
    //      все поля, кроме colorFilter - неизменяемые типы данных, поэтому сохраняются и возвращаются как есть;
    //      при запросе colorFilter, выполняется defensive copying и возвращается копия
    //
    // Потоковая безопасность:
    //      этот объект не безопасен для использования в нескольких потоках одновременно

    private @NonNull SortType sortBy;
    private @NonNull SortOrder sortOrder;
    private @NonNull Selection selection;
    private @Nullable Long dateFrom;
    private @Nullable Long dateTo;
    private final @NonNull Set<Integer> colorFilter;

    /** Создает новый объект с данными по умолчанию. */
    public MainListFilter() {
        // default
        sortBy = SortType.MANUAL;
        sortOrder = SortOrder.DESC;
        selection = Selection.ALL;
        dateFrom = null;
        dateTo = null;
        colorFilter = new HashSet<>();
    }

    /** Создает объект из строки.
     * Подходящая строка возвращается объектом через toString().
     *
     * @param filterString Строка подходящего формата
     *
     * @return Новый объект из строки такой, что
     *      MainListFilter.fromString(otherObject.toString()).equalsByContent(otherObject) == true
     *
     * @throws IllegalArgumentException в случае ошибки при разборе строки
     */
    public static MainListFilter fromString(String filterString) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<MainListFilter> jsonAdapter = moshi.adapter(MainListFilter.class);
        MainListFilter filter;
        try {
            filter = jsonAdapter.fromJson(filterString);
        } catch (IOException | JsonDataException e) {
            throw new IllegalArgumentException("Error @ MainListFilter.fromString when parsing: " + filterString);
        }
        return filter;
    }

    @Override
    public String toString() {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<MainListFilter> jsonAdapter = moshi.adapter(MainListFilter.class);
        return jsonAdapter.toJson(this);
    }

    /** Сравнивает два объекта по содержанию.
     * Объекты равны по содержанию, если у них равны sortBy, sortOrder, selection, dateTo и dateFrom,
     *      а также их colorFilter содержат одинаковые значения.
     *
     * @param otherFilter фильтр для сравнения
     *
     * @return true, если объекты равны по содержанию, иначе false
     */
    boolean equalsByContent(MainListFilter otherFilter) {
        boolean equality = sortBy ==  otherFilter.sortBy
                && sortOrder == otherFilter.sortOrder
                && selection == otherFilter.selection;
        if (dateFrom == null) {
            equality = equality && (otherFilter.dateFrom == null);
        } else {
            equality = equality && dateFrom.equals(otherFilter.dateFrom);
        }
        if (dateTo == null) {
            equality = equality && (otherFilter.dateTo == null);
        } else {
            equality = equality && dateTo.equals(otherFilter.dateTo);
        }
        equality = equality && (colorFilter.size() == otherFilter.colorFilter.size());
        return equality && (colorFilter.containsAll(otherFilter.colorFilter));
    }

    /////////////////////////
    // Методы get и set

    /** Возвращает текущую сортировку. */
    public @NonNull SortType getSortType() {
        return sortBy;
    }

    /** Возвращает направление сортировки. */
    public @NonNull SortOrder getSortOrder() {
        return sortOrder;
    }

    /** Возвращает текущий отбор. */
    public @NonNull Selection getSelection() {
        return selection;
    }

    /** Возвращает левую границу дат.
     *
     * @return левая граница дат ("от") - строка в формате dd/MM/yy или пустая строка
     */
    public @NonNull String getDateFrom() {
        if (dateFrom != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            return sdf.format(dateFrom);
        } else {
            return "";
        }
    }

    /** Возвращает правую границу дат.
     *
     * @return правая граница дат ("до") - строка в формате dd/MM/yy или пустая строка
     */
    public @NonNull String getDateTo() {
        if (dateTo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            return sdf.format(dateTo);
        } else {
            return "";
        }
    }

    /** Возвращает текущий фильтр по цветам.
     *
     * @return текущий фильтр по цветам, может быть пустым
     */
    public @NonNull Set<Integer> getColorFilter() {
        Set<Integer> defensiveCopying = new HashSet<>();
        defensiveCopying.addAll(colorFilter);
        return defensiveCopying;
    }

    /** Устанавливает текущую сортировку.
     * Вместе с сортировкой устанавливается направление сортировки по умолчанию для этого типа сортировки.
     *
     * @param sortType новая сортировка, не null
     *
     * @throws NullPointerException если sortType == null
     */
    public void setSortType(@NonNull SortType sortType) {
        sortBy = sortType;
        if (sortBy == SortType.MANUAL) {
            sortOrder = SortOrder.DESC;
        } else {
            sortOrder = SortOrder.ASC;
        }
    }

    /** Изменяет направление сортировки.
     * Устанавливается новое направление, не равное предыдущему.
     * Например, если был ASC, станет DESC и наоборот.
     * Не меняет направление сортировки если текущий тип сортировки не подразумевает изменения (например, MANUAL).
     */
    public void nextSortOrder() {
        if (sortBy != SortType.MANUAL) {
            sortOrder = sortOrder == SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
        }
    }

    /** Устанавливает текущий отбор.
     *
     * @param newSelection текущий отбор
     *
     * @throws NullPointerException если sortType == null
     */
    public void setSelection(@NonNull Selection newSelection) {
        selection = newSelection;
    }

    /** Устанавливает левую границу дат.
     *
     * @param dateMs левая граница дат ("от") в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
     */
    public void setDateFrom(long dateMs) {
        dateFrom = dateMs;
    }

    /** Устанавливает правую границу дат.
     *
     * @param dateMs права граница дат ("до") в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
     */
    public void setDateTo(long dateMs) {
        dateTo = dateMs;
    }

    /** Добавляет фильтр по цвету.
     *
     * @param color числовое значение цвета
     */
    public void addColorFilter(int color) {
        colorFilter.add(color);
    }

    /** Убирает фильтр по цвету.
     *
     * @param color числовое значение цвета
     */
    public void removeColorFilter(int color) {
        colorFilter.remove(color);
    }


    /////////////////////////
    // Вспомогательные методы для формирования запросов к БД

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
            // Фильтровать нужно только по тем цветам в фильтре, которые сохранены как Favorites
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
                for (int i = 0, size = realColors.size(); i < size; i++) {
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

}
