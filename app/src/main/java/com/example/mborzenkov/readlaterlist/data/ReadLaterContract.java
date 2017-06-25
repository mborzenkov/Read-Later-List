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

    /** Представляет все возможные сегменты Uri. */
    enum UriSegments {
        /** Начальный сегмент, идентифицирующий все заметки. */
        ITEMS("items", 0),
        /** Сегмент с данными пользователя. */
        USER("user", 2),
        /** Сегмент с идентификатором заметки. */
        ITEM_UID("uid", 4),
        /** Сегмент с внутренним идентификатором заметки. */
        ITEM_REMID("rid", 4),
        /** Сегмент с новым значением порядка. */
        ORDER("reorder", 6);

        /** Строковое значение сегмента, применяется в Uri: .../ITEMS.toString()/USER.toString() == .../items/user */
        private final String value;
        /** Позиция, на которой находится значение, связанное с сегментом (см. getSegment). */
        private final int segment;

        UriSegments(String value, int segment) {
            this.value = value;
            this.segment = segment;
        }

        /** Возвращает позицию, на которой находится связанное с сегментом значение.
         *
         * @return Позиция, на которой находится связанное с сегментом значение, например:
         *          USER.getSegment() == 2 потому что uri (authority)/items/user/123, где user_id == 123
         *          ITEM_UID == 4 потому что uri (authority)/items/user/123/uid/500, где _id == 500
         */
        public int getSegment() {
            return segment;
        }

        /** Возвращает строковое представление сегмента, используемое в uri. */
        @Override
        public String toString() {
            return value;
        }
    }

    /** Описание таблиц. */
    public static final class ReadLaterEntry implements BaseColumns {

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
        public static final String COLUMN_ORDER = "man_order";

        /** Создает Uri для доступа ко всем заметкам пользователя.
         *
         * @param userId идентификатор пользователя
         *
         * @return Uri для доступа
         */
        public static Uri buildUriForUserItems(int userId) {
            return BASE_CONTENT_URI.buildUpon()
                    .appendPath(UriSegments.ITEMS.toString())
                    .appendPath(UriSegments.USER.toString())
                    .appendPath(String.valueOf(userId))
                    .build();
        }

        /** Создает Uri для доступа к одному элменту по id.
         *
         * @param userId идентификатор пользователя
         * @param itemId _id элемента
         *
         * @return Uri для доступа
         */
        public static Uri buildUriForOneItem(int userId, int itemId) {
            return BASE_CONTENT_URI.buildUpon()
                    .appendPath(UriSegments.ITEMS.toString())
                    .appendPath(UriSegments.USER.toString())
                    .appendPath(String.valueOf(userId))
                    .appendPath(UriSegments.ITEM_UID.toString())
                    .appendPath(String.valueOf(itemId))
                    .build();
        }

        /** Создает Uri для доступа к одному элменту по его remoteId.
         *
         * @param userId идентификатор пользователя
         * @param remoteId Внешний идентификатор элемента
         *
         * @return Uri для доступа
         */
        public static Uri buildUriForRemoteId(int userId, int remoteId) {
            return BASE_CONTENT_URI.buildUpon()
                    .appendPath(UriSegments.ITEMS.toString())
                    .appendPath(UriSegments.USER.toString())
                    .appendPath(String.valueOf(userId))
                    .appendPath(UriSegments.ITEM_REMID.toString())
                    .appendPath(String.valueOf(remoteId))
                    .build();
        }

        /** Создает Uri для обновления COLUMN_ORDER элемента по его id.
         * По этому Uri в update content provider обновляет позицию элемента и позиции всех промежуточных элементов
         *      между старой позицией элемента и новой
         *
         * @param userId идентификатор пользователя
         * @param itemId внутренний идентификатор элемента, _id
         * @param newPosition новая позиция (item_order)
         *
         * @return Uri для доступа
         */
        public static Uri buildUriForUpdateOrder(int userId, int itemId, int newPosition) {
            return BASE_CONTENT_URI.buildUpon()
                    .appendPath(UriSegments.ITEMS.toString())
                    .appendPath(UriSegments.USER.toString())
                    .appendPath(String.valueOf(userId))
                    .appendPath(UriSegments.ITEM_UID.toString())
                    .appendPath(String.valueOf(itemId))
                    .appendPath(UriSegments.ORDER.toString())
                    .appendPath(String.valueOf(newPosition))
                    .build();
        }

    }

}
