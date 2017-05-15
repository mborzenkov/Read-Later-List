package com.example.mborzenkov.readlaterlist.data;

import android.net.Uri;
import android.provider.BaseColumns;

/** Описание базы данных, контракт и класс со всеми колонками.
 */
public class ReadLaterContract {

    /** Имя идентификатор поставщика. */
    public static final String CONTENT_AUTHORITY = "com.example.mborzenkov.readlaterlist";
    /** Uri для поставщика. */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    /** Путь к списку элементов ReadLater. */
    public static final String PATH_ITEMS = "items";

    /** Описание таблиц. */
    public static final class ReadLaterEntry implements BaseColumns {

        /** Uri для доступа к таблице items. */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ITEMS)
                .build();

        /** Имя таблицы. */
        public static final String TABLE_NAME = "items";
        /** Имя таблицы FTS. */
        public static final String TABLE_NAME_FTS = "items_fts";
        // /** Имя колонки с id пользователя. */
        // public static final String COLUMN_USER_ID = "user_id";
        /** Имя колонки с внешним id заметки. */
        public static final String COLUMN_REMOTE_ID = "remote_id";
        /** Имя колонки с заголовком элемента. */
        public static final String COLUMN_LABEL = "label";
        /** Имя колонки с описанием элемента. */
        public static final String COLUMN_DESCRIPTION = "description";
        /** Имя колонки с цветом элемента. */
        public static final String COLUMN_COLOR = "color";
        /** Имя колонки с датой создания. */
        public static final String COLUMN_DATE_CREATED = "created";
        /** Имя колонки с датой последнего изменения. */
        public static final String COLUMN_DATE_LAST_MODIFIED = "last_modify";
        /** Имя колонки с датой последнего просмотра. */
        public static final String COLUMN_DATE_LAST_VIEW = "last_view";
        /** Имя колонки с url картинки. */
        public static final String COLUMN_IMAGE_URL = "image_url";

        /** Создает Uri для доступа к одному элменту по id.
         *
         * @param id _id элемента
         * @return Uri для доступа
         */
        public static Uri buildUriForOneItem(int id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(id))
                    .build();
        }
    }

}
