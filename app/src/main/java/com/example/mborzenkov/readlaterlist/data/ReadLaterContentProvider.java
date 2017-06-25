package com.example.mborzenkov.readlaterlist.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.UriSegments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Контент провайдер для работы с базой данных. */
public class ReadLaterContentProvider extends ContentProvider {

    /** Код запроса всех данных пользователя. */
    private static final int CODE_ITEMS = 100;
    /** Код запроса отдельного элемента по uid. */
    private static final int CODE_ITEMS_ID = 101;
    /** Код запроса отдельного элемента по remoteId. */
    private static final int CODE_ITEMS_REMOTEID = 102;
    /** Код запроса обновления порядка у элемента. */
    private static final int CODE_ITEMS_ORDER = 103;

    /** Матчер для сравнения запрашиваемых uri. */
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    /** DbHelper для работы с базой. */
    private ReadLaterDbHelper mReadLaterDbHelper;

    /** Создает новый UriMatcher.
     *
     * @return Матчер для сравнения запрашиваемых uri
     */
    private static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ReadLaterContract.CONTENT_AUTHORITY;
        // Uri для доступа ко всем данным пользователя
        matcher.addURI(authority, UriSegments.ITEMS + "/"
                + UriSegments.USER + "/#", CODE_ITEMS);
        // Uri для доступа к отдельному элементу по uid
        matcher.addURI(authority, UriSegments.ITEMS + "/"
                + UriSegments.USER + "/#/"
                + UriSegments.ITEM_UID + "/#", CODE_ITEMS_ID);
        // Uri для доступа к отдельному элементу по remoteId
        matcher.addURI(authority, UriSegments.ITEMS + "/"
                + UriSegments.USER + "/#/"
                + UriSegments.ITEM_REMID + "/#", CODE_ITEMS_REMOTEID);
        // Uri для обновления порядка у элемента
        matcher.addURI(authority, UriSegments.ITEMS + "/"
                + UriSegments.USER + "/#/"
                + UriSegments.ITEM_UID + "/#/"
                + UriSegments.ORDER + "/#", CODE_ITEMS_ORDER);
        return matcher;

    }

    @Override
    public boolean onCreate() {
        // Создание DbHelper
        mReadLaterDbHelper = new ReadLaterDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        // Не нужен в этой версии
        return null;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws UnsupportedOperationException если запрос CODE_ITEMS_ID и selection не пустой,
     *      т.к. uri подразумевает запрос одной заметки по уникальному идентификатору, selection это ошибка
     */
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        final SQLiteDatabase db = mReadLaterDbHelper.getReadableDatabase();
        final Map<UriSegments, Integer> uriMatch = analyzeUri(uri);
        final Cursor returnCursor;

        switch (uriMatch.get(UriSegments.ITEMS)) {
            case CODE_ITEMS:
                returnCursor = db.query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        appendSelectionWithValues(selection,
                                ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER))),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case CODE_ITEMS_ID:
                if ((selection != null) && (!selection.trim().isEmpty())) {
                    throw new UnsupportedOperationException(
                            "Error @ ReadLaterContentProvider.query: for uri " + uri + " selection == " + selection);
                }
                returnCursor = db.query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        appendSelectionWithValues(selection,
                                ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                ReadLaterEntry._ID, String.valueOf(uriMatch.get(UriSegments.ITEM_UID))),
                        null,
                        null,
                        null,
                        null);
                break;
            case CODE_ITEMS_REMOTEID:
                returnCursor = db.query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        appendSelectionWithValues(selection,
                                ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                ReadLaterEntry.COLUMN_REMOTE_ID, String.valueOf(uriMatch.get(UriSegments.ITEM_REMID))),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.query: unknown uri " + uri);
        }

        return returnCursor;

    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws UnsupportedOperationException если запрос CODE_ITEMS_ID и selection не пустой,
     *      т.к. CODE_ITEMS_ID подразумевает запрос одной заметки по уникальному идентификатору, selection это ошибка
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        final SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        final Map<UriSegments, Integer> uriMatch = analyzeUri(uri);
        final int itemsDeleted;

        switch (uriMatch.get(UriSegments.ITEMS)) {
            case CODE_ITEMS:
                db.beginTransaction();
                try {
                    itemsDeleted = db.delete(
                            ReadLaterEntry.TABLE_NAME,
                            appendSelectionWithValues(selection,
                                    ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER))),
                            selectionArgs);
                    db.delete(
                            ReadLaterEntry.TABLE_NAME_FTS,
                            appendSelectionWithValues(selection,
                                    ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER))),
                            selectionArgs);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case CODE_ITEMS_ID:
                if ((selection != null) && (!selection.trim().isEmpty())) {
                    throw new UnsupportedOperationException(
                            "Error @ ReadLaterContentProvider.delete: for uri " + uri + " selection == " + selection);
                }
                db.beginTransaction();
                try {
                    itemsDeleted = db.delete(
                            ReadLaterEntry.TABLE_NAME,
                            appendSelectionWithValues(selection,
                                    ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                    ReadLaterEntry._ID, String.valueOf(uriMatch.get(UriSegments.ITEM_UID))),
                            null);
                    db.delete(
                            ReadLaterEntry.TABLE_NAME_FTS,
                            appendSelectionWithValues(selection,
                                    ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                    "docid", String.valueOf(uriMatch.get(UriSegments.ITEM_UID))),
                            null);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.delete: unknown uri " + uri);
        }

        return itemsDeleted;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws IllegalArgumentException если values == null или не содержит всех необходимых данных
     * @throws IllegalArgumentException если values содержит ReadLaterEntry.COLUMN_USER_ID
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (values == null) {
            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.insert: ContentValues == null");
        } else if (values.containsKey(ReadLaterEntry.COLUMN_USER_ID)) {
            throw new IllegalArgumentException(
                    "Error @ ReadLaterContentProvider.insert: ContentValues should not contain USER_ID - " + values);
        }

        final SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        final Map<UriSegments, Integer> uriMatch = analyzeUri(uri);
        final Uri returnUri;

        switch (uriMatch.get(UriSegments.ITEMS)) {
            case CODE_ITEMS:

                final int userId = uriMatch.get(UriSegments.USER);
                values.put(ReadLaterEntry.COLUMN_ORDER, getMaxOrder(uri) + 1);
                values.put(ReadLaterEntry.COLUMN_USER_ID, userId);

                db.beginTransaction();
                try {
                    // INSERT INTO TABLE_ITEMS
                    long id = db.insert(ReadLaterEntry.TABLE_NAME, null, values);
                    if (id > 0) {
                        returnUri = ReadLaterEntry.buildUriForOneItem(userId, (int) id);
                    } else {
                        throw new IllegalArgumentException("Error @ ReadLaterContentProvider.insert: when inserting "
                                + values);
                    }

                    // INSERT INTO TABLE_FTS
                    ContentValues ftsValues = new ContentValues();
                    ftsValues.put("docid", id);
                    ftsValues.put(ReadLaterEntry.COLUMN_USER_ID, userId);
                    ftsValues.put(ReadLaterEntry.COLUMN_LABEL, values.getAsString(ReadLaterEntry.COLUMN_LABEL));
                    ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                            values.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                    db.insert(ReadLaterEntry.TABLE_NAME_FTS, null, ftsValues);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.insert: unknown uri " + uri);
        }

        return returnUri;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws IllegalArgumentException если values == null
     * @throws IllegalArgumentException если любой из values содержит ReadLaterEntry.COLUMN_USER_ID
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @Nullable ContentValues[] values) {
        if (values == null) {
            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.bulkInsert: ContentValues == null");
        }

        final SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        final Map<UriSegments, Integer> uriMatch = analyzeUri(uri);

        switch (uriMatch.get(UriSegments.ITEMS)) {
            case CODE_ITEMS:

                final int userId = uriMatch.get(UriSegments.USER);
                int maxOrder = getMaxOrder(uri);
                int inserted = 0;

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {

                        if (value.containsKey(ReadLaterEntry.COLUMN_USER_ID)) {
                            throw new IllegalArgumentException(
                                    "Error @ ReadLaterContentProvider.bulkInsert: value should not contain USER_ID - "
                                            + value);
                        }

                        maxOrder++;

                        // INSERT INTO TABLE_ITEMS
                        value.put(ReadLaterEntry.COLUMN_ORDER, maxOrder);
                        value.put(ReadLaterEntry.COLUMN_USER_ID, userId);
                        long id = db.insert(ReadLaterEntry.TABLE_NAME, null, value);
                        if (id < 0) {
                            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.bulkInsert: inserting "
                                    + value);
                        }

                        // INSERT INTO TABLE_FTS
                        ContentValues ftsValues = new ContentValues();
                        ftsValues.put("docid", id);
                        ftsValues.put(ReadLaterEntry.COLUMN_USER_ID, userId);
                        ftsValues.put(ReadLaterEntry.COLUMN_LABEL, value.getAsString(ReadLaterEntry.COLUMN_LABEL));
                        ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                                value.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                        db.insert(ReadLaterEntry.TABLE_NAME_FTS, null, ftsValues);
                        inserted++;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return inserted;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.bulkInsert: unknown uri "
                        + uri);
        }

    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws UnsupportedOperationException если uri == CODE_ITEMS_ID или CODE_ITEMS_ORDER и selection не пустой,
     *      т.к. uri подразумевает запрос одной заметки по уникальному идентификатору, selection это ошибка
     * @throws UnsupportedOperationException если uri == CODE_ITEMS_ORDER и позиция < -1
     * @throws IllegalArgumentException если values содержит ReadLaterEntry.COLUMN_USER_ID
     * @throws IllegalArgumentException если uri != CODE_ITEMS_ORDER и values == null
     * @throws IllegalArgumentException если uri == CODE_ITEMS_ORDER и новая позиция > максимальной
     * @throws SQLiteConstraintException если не удалось выполнить update
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        if ((values != null) && (values.containsKey(ReadLaterEntry.COLUMN_USER_ID))) {
            throw new IllegalArgumentException(
                    "Error @ ReadLaterContentProvider.update: ContentValues should not contain USER_ID - " + values);
        }

        final SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        final Map<UriSegments, Integer> uriMatch = analyzeUri(uri);
        final int itemsUpdated;

        switch (uriMatch.get(UriSegments.ITEMS)) {
            case CODE_ITEMS_ID:
                if (values == null) {
                    throw new IllegalArgumentException(
                            "Error @ ReadLaterContentProvider.update: update 1 item and ContentValues == null");
                } else if ((selection != null) && (!selection.trim().isEmpty())) {
                    throw new UnsupportedOperationException(
                            "Error @ ReadLaterContentProvider.update: for uri " + uri + " selection == " + selection);
                }

                // Решаем, нужно ли будет обновлять fts и создаем ftsValues
                boolean updateFts = false;
                ContentValues ftsValues = new ContentValues();
                if (values.containsKey(ReadLaterEntry.COLUMN_LABEL)) {
                    ftsValues.put(ReadLaterEntry.COLUMN_LABEL,
                            values.getAsString(ReadLaterEntry.COLUMN_LABEL));
                    updateFts = true;
                }
                if (values.containsKey(ReadLaterEntry.COLUMN_DESCRIPTION)) {
                    ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                            values.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                    updateFts = true;
                }

                db.beginTransaction();
                try {
                    itemsUpdated = db.update(
                            ReadLaterEntry.TABLE_NAME,
                            values,
                            appendSelectionWithValues(selection,
                                    ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                    ReadLaterEntry._ID, String.valueOf(uriMatch.get(UriSegments.ITEM_UID))),
                            null);
                    if (updateFts) {
                        db.update(
                                ReadLaterEntry.TABLE_NAME_FTS,
                                ftsValues,
                                appendSelectionWithValues(selection,
                                        ReadLaterEntry.COLUMN_USER_ID, String.valueOf(uriMatch.get(UriSegments.USER)),
                                        "docid=?", String.valueOf(uriMatch.get(UriSegments.ITEM_UID))),
                                null);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case CODE_ITEMS_ORDER:
                if ((selection != null) && (!selection.trim().isEmpty())) {
                    throw new UnsupportedOperationException(
                            "Error @ ReadLaterContentProvider.update: for uri " + uri + " selection == " + selection);
                }

                final int userId = uriMatch.get(UriSegments.USER);
                final int maxOrder = getMaxOrder(ReadLaterEntry.buildUriForUserItems(userId));
                final int newPosition = uriMatch.get(UriSegments.ORDER);
                final int itemId = uriMatch.get(UriSegments.ITEM_UID);

                if (newPosition > maxOrder) {
                    throw new IllegalArgumentException("Error @ ReadLaterContentProvider.update: position > MAX :: "
                            + newPosition + ">" + maxOrder);
                }

                db.beginTransaction();
                try {
                    // Позиция элемента itemId
                    int oldPosition = getMaxOrder(ReadLaterEntry.buildUriForOneItem(userId, itemId));
                    if ((oldPosition > 0) && (oldPosition != newPosition)) {
                        // Обновляем все соседние элементы
                        db.execSQL(getRawQueryForUpdateOrder(oldPosition, newPosition));
                        ContentValues updateOrderVal = new ContentValues();
                        updateOrderVal.put(ReadLaterEntry.COLUMN_ORDER, newPosition);

                        // Обновляем текущий элемент
                        db.update(
                                ReadLaterEntry.TABLE_NAME,
                                updateOrderVal,
                                appendSelectionWithValues(selection,
                                        ReadLaterEntry.COLUMN_USER_ID, String.valueOf(userId),
                                        ReadLaterEntry._ID, String.valueOf(itemId)),
                                null);
                        itemsUpdated = Math.abs(oldPosition - newPosition) + 1;
                    } else {
                        itemsUpdated = 0;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.update: unknown uri " + uri);
        }

        return itemsUpdated;
    }

    /** Возвращает значения сегментов из uri.
     *
     * @param uri uri для анализа
     *
     * @return соответствие сегментов и значений:
     *      CODE_ITEMS          -> ITEMS::CODE_READLATER_ITEMS, USER::id
     *      CODE_ITEMS_ID       -> ITEMS::CODE_ITEMS_ID,        USER::id, ITEM_UID::uid
     *      CODE_ITEMS_REMOTEID -> ITEMS::CODE_ITEMS_REMOTEID,  USER::id, ITEM_REMID::rid
     *      CODE_ITEMS_ORDER    -> ITEMS::CODE_ITEMS_OREDER,    USER::id, ITEM_UID::uid, ORDER::neword
     *      ???                 -> ITEMS::-1 - если uri не соответствует указанным выше
     *
     * @throws NullPointerException если uri == null
     */
    private @NonNull Map<UriSegments, Integer> analyzeUri(@NonNull Uri uri) {

        Map<UriSegments, Integer> result = new HashMap<>();

        final int match = sUriMatcher.match(uri);
        result.put(UriSegments.ITEMS, match);

        final List<String> pathSegments = uri.getPathSegments();
        switch (match) {
            case CODE_ITEMS:
                result.put(UriSegments.USER, Integer.valueOf(pathSegments.get(UriSegments.USER.getSegment())));
                break;
            case CODE_ITEMS_ID:
                result.put(UriSegments.USER, Integer.valueOf(pathSegments.get(UriSegments.USER.getSegment())));
                result.put(UriSegments.ITEM_UID, Integer.valueOf(pathSegments.get(UriSegments.ITEM_UID.getSegment())));
                break;
            case CODE_ITEMS_REMOTEID:
                result.put(UriSegments.USER, Integer.valueOf(pathSegments.get(UriSegments.USER.getSegment())));
                result.put(UriSegments.ITEM_REMID,
                        Integer.valueOf(pathSegments.get(UriSegments.ITEM_REMID.getSegment())));
                break;
            case CODE_ITEMS_ORDER:
                result.put(UriSegments.USER, Integer.valueOf(pathSegments.get(UriSegments.USER.getSegment())));
                result.put(UriSegments.ITEM_UID, Integer.valueOf(pathSegments.get(UriSegments.ITEM_UID.getSegment())));
                result.put(UriSegments.ORDER, Integer.valueOf(pathSegments.get(UriSegments.ORDER.getSegment())));
                break;
            default:
                break;
        }
        return result;

    }

    /** Добавляет в начало к selection запрос пары "values[i] = values[i+1]".
     * Этот метод НЕ безопасен к SQL Injection.
     * Все values вставляются as-is,поэтому следует дополнительно следить за этим.
     *
     * @param selection строка отбора без ключевого слова WHERE
     * @param values значения, которые нужно добавить, должно быть четное количество
     *               каждый четный индекс - ключ
     *               каждый нечетный индекс - значение
     *
     * @return Если selection == null || selection.trim().isEmpty(), возвращает "values[i] = values[i+1] AND ...";
     *         Иначе заменяет на "values[i] = values[i+1] AND ... AND (selection)"
     *
     * @throws IllegalArgumentException если values.length == 0 || values.length % 2 != 0
     * @throws NullPointerException если values == null
     */
    private @NonNull String appendSelectionWithValues(@Nullable String selection,
                                                      @NonNull String... values) {
        if ((values.length == 0) || (values.length % 2 != 0)) {
            throw new IllegalArgumentException(
                    "Error @ ReadLaterContentProvider.appendSelectionWithValues :: values.length == " + values.length);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i += 2) {
            if (i != 0) {
                builder.append(" AND ");
            }
            builder.append(values[i]).append(" = ").append(values[i + 1]);

        }
        if ((selection != null) && (!selection.trim().isEmpty())) {
            builder.append(" AND (").append(selection).append(')');
        }
        return builder.toString();
    }

    /** Возвращает максимальное значение порядка для указанной uri.
     *
     * @param uri uri для доступа к данным: CODE_ITEMS, CODE_ITEMS_ID или CODE_ITEMS_REMOTEID
     *
     * @return максимальное значение порядка для указанной uri или -1, если порядки не были найдены
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws NullPointerException если uri == null
     */
    private @IntRange(from = -1) int getMaxOrder(@NonNull Uri uri) {
        Cursor maxOrderCursor = query(
                uri,
                new String[] {"MAX (" + ReadLaterEntry.COLUMN_ORDER + ")"},
                null,
                null,
                null);
        int maxOrder = -1;
        if (maxOrderCursor != null) {
            maxOrderCursor.moveToFirst();
            maxOrder = maxOrderCursor.getInt(0);
            maxOrderCursor.close();
        }
        return maxOrder;
    }

    /** Возвращает запрос на изменение порядка элемента.
     * Пример:
     *      getRawQueryForUpdateOrder(4, 1)
     *      UPDATE items SET item_order = item_order + 1 WHERE item_order >= 1 AND item_order < 4;
     * Если oldPosition == newPosition, возвращает пустую строку
     *
     * @param oldPosition старая позиция элемента
     * @param newPosition новая позиция элемента
     */
    private @NonNull String getRawQueryForUpdateOrder(int oldPosition, int newPosition) {

        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ").append(ReadLaterEntry.TABLE_NAME)
                .append(" SET ").append(ReadLaterEntry.COLUMN_ORDER).append('=').append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append("-1");
        } else {
            builder.append("+1");
        }
        builder.append(" WHERE ").append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append('>').append(oldPosition);
        } else {
            builder.append(">=").append(newPosition);
        }
        builder.append(" AND ").append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append("<=").append(newPosition);
        } else {
            builder.append('<').append(oldPosition);
        }
        return builder.toString();

    }

}
