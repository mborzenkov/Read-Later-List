package com.example.mborzenkov.readlaterlist.data;

import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.activity.MainListActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainListFilter {

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

    public enum Selection {
        ALL(null),
        DATE_CREATED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED),
        DATE_MODIFIED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED),
        DATE_VIEWED(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW);

        private final String columnName;
        Selection(String columnName) {
            this.columnName = columnName;
        }

        private String getColumnName() {
            return columnName;
        }
    };

    private SortType sortBy;
    private SortOrder sortOrder;
    private Selection selection;
    private Date dateFrom;
    private Date dateTo;
    private Set<Integer> colorFilter;

    public MainListFilter() {
        // default
        sortBy = SortType.LABEL;
        sortOrder = SortOrder.ASC;
        selection = Selection.ALL;
        dateFrom = null;
        dateTo = null;
        colorFilter = new HashSet<>();
    }

    public static MainListFilter fromString(String filterString) {
        return new MainListFilter();
    }

    public String getSqlSortOrder() {
        return String.format(Locale.US, "%s %s", sortBy.getColumnName(), sortOrder.getOrderByQuery());
    }

    public String getSqlSelection() {
        StringBuilder sqlSelectionString = new StringBuilder();
        if (selection != Selection.ALL) {
            if (dateFrom != null) {
                sqlSelectionString.append(selection.getColumnName()).append(">?");
            }
            if (dateTo != null) {
                if (dateFrom != null) {
                    sqlSelectionString.append(" AND ");
                }
                sqlSelectionString.append(selection.getColumnName()).append("<?");
            }
        }
        if (!colorFilter.isEmpty()) {
            if (dateFrom != null | dateTo != null) {
                sqlSelectionString.append(" AND ");
            }
            sqlSelectionString.append(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR).append(" IN (");
            for (Integer color : colorFilter) {
                sqlSelectionString.append("?,");
            }
            sqlSelectionString.delete(sqlSelectionString.length() - 1, sqlSelectionString.length()).append(")");
        }
        return sqlSelectionString.toString();
    }

    public String[] getSqlSelectionArgs() {
        List<String> selectionArgs = new ArrayList<>();
        if (selection != Selection.ALL) {
            if (dateFrom != null) {
                selectionArgs.add(String.valueOf(dateFrom.getTime()));
            }
            if (dateTo != null) {
                selectionArgs.add(String.valueOf(dateTo.getTime()));
            }
        }
        if (!colorFilter.isEmpty()) {
            for (Integer color : colorFilter) {
                selectionArgs.add(String.valueOf(color));
            }
        }
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    public String getDateFrom() {
        if (dateFrom != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(MainListActivity.FORMAT_DATE, Locale.US);
            return sdf.format(dateFrom.getTime());
        } else {
            return "";
        }
    }

    public String getDateTo() {
        if (dateTo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(MainListActivity.FORMAT_DATE, Locale.US);
            return sdf.format(dateTo.getTime());
        } else {
            return "";
        }
    }

    public void setDateFrom(long dateMs) {
        dateFrom = new Date(dateMs);
    }

    public void setDateTo(long dateMs) {
        dateTo = new Date(dateMs);
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
