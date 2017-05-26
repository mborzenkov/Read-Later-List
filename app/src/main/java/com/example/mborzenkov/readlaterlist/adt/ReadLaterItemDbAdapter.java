package com.example.mborzenkov.readlaterlist.adt;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;

import java.util.ArrayList;
import java.util.List;

/** Адаптер для соединения объекта ReadLaterItem и базы данных. */
public class ReadLaterItemDbAdapter {

    /** Конструктор по умолчанию. */
    public ReadLaterItemDbAdapter() { }

    /** Преобразует текущую позицию cursor в объект ReadLaterItem.
     *
     * @param cursor курсор, как в ReadLaterItemCursorProjection
     *
     * @return объект ReadLaterItem, соответствующий текущей позиции курсора или null, если курсор закрыт или пустой
     *
     * @throws IllegalArgumentException если cursor не соответствует требованиям
     * @see ReadLaterItemCursorProjection
     */
    public @Nullable ReadLaterItem itemFromCursor(@NonNull Cursor cursor) {
        ReadLaterItemCursorProjection projection = new ReadLaterItemCursorProjection(cursor);
        return itemFromCursor(cursor, projection);
    }

    /** Выполняет преобразование курсора в объект ReadLaterItem с указанными индексами колонок в projection.
     * Метод необходим для ускорения обхода курсора, так как индексы колонок получаются только один раз.
     *
     * @param cursor курсор, как в ReadLaterItemCursorProjection
     * @param projection объект, содержащий индексы колонок
     *
     * @return объект ReadLaterItem, соответствующий текущей позиции курсора или null, если курсор закрыт или пустой
     *
     * @throws IllegalArgumentException если cursor не соответствует требованиям
     * @see ReadLaterItemCursorProjection
     */
    private @Nullable ReadLaterItem itemFromCursor(@NonNull Cursor cursor,
                                                   @NonNull ReadLaterItemCursorProjection projection) {

        if (cursor.isClosed() || cursor.getCount() == 0) {
            return null;
        }
        return new ReadLaterItem.Builder(cursor.getString(projection.indexLabel))
                .description(cursor.getString(projection.indexDescription))
                .color(cursor.getInt(projection.indexColor))
                .dateCreated(cursor.getLong(projection.indexCreated))
                .dateModified(cursor.getLong(projection.indexModified))
                .dateViewed(cursor.getLong(projection.indexViewed))
                .imageUrl(cursor.getString(projection.indexImageUrl))
                .remoteId(cursor.getInt(projection.indexRemoteId))
                .build();

    }

    /** Преобразует Cursor в список ReadLaterItem.
     * Во время выполнения этого метода изменяется позиция cursor, по завершению она возвращается на первоначальную.
     * Нельзя изменять позицию cursor параллельно с этим методом, иначе работа метода может быть нарушена.
     *
     * @param cursor курсор, как в ReadLaterItemCursorProjection
     * @return список всех объектов ReadLaterItem, преобразованных из cursor
     * @throws IllegalArgumentException если cursor не соответствует требованиям
     * @see ReadLaterItemCursorProjection
     */
    public List<ReadLaterItem> allItemsFromCursor(@NonNull Cursor cursor) {
        final int currentPosition = cursor.getPosition();
        List<ReadLaterItem> result = new ArrayList<>();
        ReadLaterItemCursorProjection projection = new ReadLaterItemCursorProjection(cursor);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            result.add(itemFromCursor(cursor, projection));
        }
        cursor.moveToPosition(currentPosition);
        return result;
    }

    /** Возвращает ContentValues на основании ReadLaterItem.
     *
     * @param item ReadLaterItem, на основании которого нужно подготовить ContentValues
     * @return ContentValues
     */
    public ContentValues contentValuesFromItem(@NonNull ReadLaterItem item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, item.getColor());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED, item.getDateCreated());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED, item.getDateModified());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW, item.getDateViewed());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_IMAGE_URL, item.getImageUrl());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_REMOTE_ID, item.getRemoteId());
        return contentValues;
    }

    /** Объект, содержащий соответствие индексов колонок в курсоре полям объекта ReadLaterItem. */
    private class ReadLaterItemCursorProjection {
        private final int indexLabel;
        private final int indexDescription;
        private final int indexColor;
        private final int indexCreated;
        private final int indexModified;
        private final int indexViewed;
        private final int indexImageUrl;
        private final int indexRemoteId;

        /** Получает индексы колонок из курсора, соответствующие полям объекта ReadLaterItem.
         *
         * @param cur курсор, содержащий колонки из ReadLaterEnrty: COLUMN_LABEL, COLUMN_DESCRIPTION, COLUMN_COLOR,
         *               COLUMN_DATE_CREATED, COLUMN_DATE_LAST_MODIFIED, COLUMN_LAST_VIEW
         * @throws IllegalArgumentException если курсор не содержит какой либо колонки
         * @see com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry
         */
        private ReadLaterItemCursorProjection(@NonNull Cursor cur) {
            this.indexLabel = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL);
            this.indexDescription = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION);
            this.indexColor = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR);
            this.indexCreated = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED);
            this.indexModified = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED);
            this.indexViewed = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW);
            this.indexImageUrl = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_IMAGE_URL);
            this.indexRemoteId = cur.getColumnIndexOrThrow(ReadLaterContract.ReadLaterEntry.COLUMN_REMOTE_ID);
        }
    }

}
