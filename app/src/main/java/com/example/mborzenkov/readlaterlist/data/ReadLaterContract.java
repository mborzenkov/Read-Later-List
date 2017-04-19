package com.example.mborzenkov.readlaterlist.data;

import android.net.Uri;
import android.provider.BaseColumns;

// TODO: Javadoc
public class ReadLaterContract {

    public static final String CONTENT_AUTHORITY = "com.example.mborzenkov.readlaterlist";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_ITEMS = "items";

    public static final class ReadLaterEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ITEMS)
                .build();

        public static final String TABLE_NAME = "items";
        public static final String COLUMN_LABEL = "label";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_COLOR = "color";

        public static Uri buildUriForOneItem(int id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(id))
                    .build();
        }
    }

}
