package com.example.mborzenkov.readlaterlist.data;

import android.net.Uri;
import android.provider.BaseColumns;

/** Описание базы данных, контракт и класс со всеми колонками.
 */
public final class ReadLaterContract {

    private ReadLaterContract() { }

    /** Имя идентификатор поставщика. */
    public static final String CONTENT_AUTHORITY = "com.example.mborzenkov.readlaterlist";
    /** Uri для поставщика. */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    /** Путь к списку элементов ReadLater. */
    public static final String PATH_ITEMS = "items";
    /** Путь к элементу по remoteId. */
    public static final String PATH_NOTE = "note";
    /** Путь к обновлению порядка. */
    public static final String PATH_ORDER = "setorder";

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
        /** Имя колонки с id пользователя. */
        public static final String COLUMN_USER_ID = "user_id";
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
        /** Имя колонки с ручным порядом в таблице порядокв. */
        public static final String COLUMN_ORDER = "item_order";

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

        /** Создает Uri для доступа к одному элменту по его remoteId.
         *
         * @param remoteId Внешний идентификатор элемента
         * @return Uri для доступа
         */
        public static Uri buildUriForRemoteId(int remoteId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(PATH_NOTE)
                    .appendPath(String.valueOf(remoteId))
                    .build();
        }

        /** Создает Uri для обновления COLUMN_ORDER элемента по его id.
         * По этому Uri в update content provider обновляет позицию элемента и позиции всех промежуточных элементов
         *      между старой позицией элемента и новой
         *
         * @param itemId внутренний идентификатор элемента, _id
         * @param newPosition новая позиция (item_order)
         * @return Uri для доступа
         */
        public static Uri buildUriForUpdateOrder(int itemId, int newPosition) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(itemId))
                    .appendPath(PATH_ORDER)
                    .appendPath(String.valueOf(newPosition))
                    .build();
        }

    }

}
