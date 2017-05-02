package com.example.mborzenkov.readlaterlist.data;

import android.support.annotation.Nullable;

import java.util.Date;
import java.util.Locale;

public class MainListFilter {

    // TODO: Поменять названия на R.string

    private enum Sorting {
        LABEL_ASC("Label", "Ascending",
                ReadLaterContract.ReadLaterEntry.COLUMN_LABEL + " ASC"),
        LABEL_DESC("Label", "Descending",
                ReadLaterContract.ReadLaterEntry.COLUMN_LABEL + " DESC"),
        DATE_CREATED_ASC("Creation date", "Ascending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED + " ASC"),
        DATE_CREATED_DESC("Creation date", "Descending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED + " DESC"),
        DATE_MODIFIED_ASC("Last modify", "Ascending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED + " ASC"),
        DATE_MODIFIED_DESC("Last modify", "Descending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED + " DESC"),
        DATE_VIEWED_ASC("Last view", "Ascending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW + " ASC"),
        DATE_VIEWED_DESC("Last view", "Descending",
                ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW + " DESC");

        private final String fieldLabel;
        private final String directionLabel;
        private final String sqlOrderBy;

        Sorting(String fieldLabel, String directionLabel, String sqlOrderBy) {
            this.fieldLabel = fieldLabel;
            this.directionLabel = directionLabel;
            this.sqlOrderBy = sqlOrderBy;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "&s &s", fieldLabel, directionLabel);
        }

        public String getSqlOrderBy() {
            return sqlOrderBy;
        }
    }

    private enum Selection {
        ALL("No filter"),
        DATE_CREATED("Creation date"),
        DATE_MODIFIED("Last modify"),
        DATE_VIEWED("Last view");

        private final String stringRep;

        Selection(String stringRep) {
            this.stringRep = stringRep;
        }

        @Override
        public String toString() {
            return stringRep;
        }
    };

    private Sorting sortOrder;
    private Selection selection;
    private @Nullable Date dateFrom;
    private @Nullable Date dateTo;
    private int[] colorFilter;

    public MainListFilter() {
        // default
        sortOrder = Sorting.LABEL_ASC;
        selection = Selection.ALL;
        dateFrom = null;
        dateTo = null;
        colorFilter = new int[0];
    }

    public static MainListFilter fromString(String filterString) {
        return new MainListFilter();
    }

    public String getSqlSortOrder() {
        return sortOrder.getSqlOrderBy();
    }

    public String getSqlSelection() {
        return "";
    }

    public String[] getSqlSelectionArgs() {
        return new String[0];
    }
}
