package com.example.mborzenkov.readlaterlist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterContentProviderTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    public ReadLaterContentProviderTest() {
        super(ReadLaterContentProvider.class, ReadLaterContentProvider.class.getName());
    }

    private static final int USER_ID = 1005931;
    private static final int REMOTE_ID_DEFAULT = 123;
    private static final int REMOTE_ID_SECOND  = 345;
    private static final ReadLaterItem defaultItem = new ReadLaterItem.Builder("label")
            .description("description").color(Color.RED).imageUrl("http://i.imgur.com/TyCSG9A.png")
            .remoteId(REMOTE_ID_DEFAULT).build();
    private static final ReadLaterItem secondItem = new ReadLaterItem.Builder("label2")
            .description("description2").color(Color.BLUE).remoteId(REMOTE_ID_SECOND).build();

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();
    }

    @Test
    public void testGetType() {
        ContentProvider provider = getProvider();
        assertEquals(null, provider.getType(ReadLaterEntry.CONTENT_URI));
    }

    @Test
    public void testInsert() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);
        assertEquals(1L, ContentUris.parseId(itemUri));

        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testBulkInsert() {
        final ContentProvider provider = getProvider();
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        ContentValues[] contentValues = new ContentValues[] { getValidContentValues(defaultItem),
                getValidContentValues(secondItem) };
        final int inserted = provider.bulkInsert(ReadLaterEntry.CONTENT_URI, contentValues);
        assertEquals(2, inserted);

        Cursor cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(REMOTE_ID_DEFAULT), null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();

        cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(REMOTE_ID_SECOND), null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(secondItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testQueryAll() {
        final ContentProvider provider = getProvider();
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        Cursor cursor = provider.query(ReadLaterEntry.CONTENT_URI, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testQueryOneItem() {
        final ContentProvider provider = getProvider();
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        // После insert
        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(secondItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        final int itemid = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // с buildUriForOneItem
        cursor = provider.query(ReadLaterEntry.buildUriForOneItem(itemid), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testQueryByRemoteId() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(secondItem));
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(REMOTE_ID_DEFAULT), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();

        cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(REMOTE_ID_DEFAULT), null,
                ReadLaterEntry.COLUMN_USER_ID + "=?", new String[] { String.valueOf(USER_ID) }, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testDelete() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();

        provider.delete(itemUri, null, null);
        cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertFalse(cursor.moveToNext());
        cursor.close();

        itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);
        provider.delete(ReadLaterEntry.CONTENT_URI, null, null);
        cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testUpdate() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);
        int updated = provider.update(itemUri, getValidContentValues(secondItem), null, null);
        assertEquals(1, updated);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(secondItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testUpdateFts() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        final Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);

        ContentValues contentValues;
        // UPDATE, потому что только label
        final ReadLaterItem changedLabel = new ReadLaterItem.Builder(defaultItem).label("newLabel").build();
        contentValues = getValidContentValues(changedLabel);
        contentValues.remove(ReadLaterEntry.COLUMN_DESCRIPTION);
        int updated = provider.update(itemUri, contentValues, null, null);
        assertEquals(1, updated);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(changedLabel, dbAdapter.itemFromCursor(cursor));
        cursor.close();

        // UPDATE, потому что только description
        final ReadLaterItem changedDesc = new ReadLaterItem.Builder(changedLabel).description("newDesc").build();
        contentValues = getValidContentValues(changedDesc);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        updated = provider.update(itemUri, contentValues, null, null);
        assertEquals(1, updated);
        cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(changedDesc, dbAdapter.itemFromCursor(cursor));
        cursor.close();

        // UPDATE без FTS
        final ReadLaterItem noFts = new ReadLaterItem.Builder(changedDesc).color(Color.BLACK).build();
        contentValues = getValidContentValues(noFts);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        contentValues.remove(ReadLaterEntry.COLUMN_DESCRIPTION);
        updated = provider.update(itemUri, contentValues, null, null);
        assertEquals(1, updated);
        cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(noFts, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testUpdateOrder() {
        final ContentProvider provider = getProvider();
        final int firstPos = 1;
        final int secndPos = 2;
        final int thirdPos = 3;
        final int totalItems = 3;

        // Создаем заметки
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(secondItem));
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(secondItem));

        // Запоминаем id по текущим позициям
        final int item1Id = getItemIdAtPosition(provider, firstPos);
        final int item2Id = getItemIdAtPosition(provider, secndPos);
        final int item3Id = getItemIdAtPosition(provider, thirdPos);

        int updated;
        // Меняем элемент 1 на позицию SECND_POS, при этом элемент на позиции THIRD_POS не должен измениться
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(item1Id, secndPos), null, null, null);
        assertEquals(totalItems - 1, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item3Id);

        // Меняем элемент 3 на позицию SECND_POS, при этом элемент на позиции FIRST_POS не должен измениться
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(item3Id, secndPos), null, null, null);
        assertEquals(totalItems - 1, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции FIRST_POS на позицию THIRD_POS, при этом у остальных элементов станет позиция -1
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(item2Id, thirdPos), null, null, null);
        assertEquals(totalItems, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item2Id);

        // Меняем элемент на позиции THIRD_POS на позицию FIRST_POS, при этом у остальных элементов станет позиция +1
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(item2Id, firstPos), null, null, null);
        assertEquals(totalItems, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции SECND_POS на SECND_POS
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(item3Id, secndPos), null, null, null);
        assertEquals(0, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);
    }

    @Test
    public void testUpdateUnavailableItemId() {
        final ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        int updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(2, 1), null, null, null);
        assertEquals(updated, 0);
    }


    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testInsertUriException() {
        ContentProvider provider = getProvider();
        provider.insert(Uri.EMPTY, getValidContentValues(defaultItem));
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testInserNullValuesException() {
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testInserWrongValuesException() {
        ContentProvider provider = getProvider();
        ContentValues contentValues = getValidContentValues(defaultItem);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        provider.insert(ReadLaterEntry.CONTENT_URI, contentValues);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testBulkInsertUriException() {
        ContentProvider provider = getProvider();
        provider.bulkInsert(Uri.EMPTY, new ContentValues[] { getValidContentValues(defaultItem) });
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testBulkInserNullValuesException() {
        ContentProvider provider = getProvider();
        //noinspection ConstantConditions
        provider.bulkInsert(ReadLaterEntry.CONTENT_URI, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testBulkInserWrongValuesException() {
        ContentProvider provider = getProvider();
        ContentValues contentValues = getValidContentValues(defaultItem);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        provider.bulkInsert(ReadLaterEntry.CONTENT_URI, new ContentValues[] { contentValues });
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateUriException() {
        ContentProvider provider = getProvider();
        provider.update(Uri.EMPTY, getValidContentValues(defaultItem), null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNullValuesException() {
        ContentProvider provider = getProvider();
        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        assertTrue(itemUri != null);
        provider.update(itemUri, null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWrongValuesException() {
        ContentProvider provider = getProvider();
        ContentValues contentValues = getValidContentValues(defaultItem);
        Uri itemUri = provider.insert(ReadLaterEntry.CONTENT_URI, contentValues);
        assertTrue(itemUri != null);
        contentValues.put(ReadLaterEntry.COLUMN_LABEL, (String) null);
        provider.update(itemUri, contentValues, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateOrderNegative() {
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        final int itemId = getItemIdAtPosition(provider, 1);
        provider.update(ReadLaterEntry.buildUriForUpdateOrder(itemId, -1), null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateOrderMax() {
        final int bigOrder = 100000;
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        final int itemId = getItemIdAtPosition(provider, 1);
        provider.update(ReadLaterEntry.buildUriForUpdateOrder(itemId, bigOrder), null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testQueryUriException() {
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        Cursor cursor = provider.query(Uri.EMPTY, null, null, null, null);
        assertTrue(cursor != null);
        cursor.close();
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteUriException() {
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.CONTENT_URI, getValidContentValues(defaultItem));
        provider.delete(Uri.EMPTY, null, null);
    }

    /** Возвращает ContentValues, пригодные для вставки в базу данных без изменений.
     *
     * @param item объект, на основании которого нужно подготовить ContentValues.
     *
     * @return ContentValues для вставки в базу данных, заполненные данными из объекта и с указанным user id = USER_ID
     */
    private ContentValues getValidContentValues(ReadLaterItem item) {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(item);
        contentValues.put(ReadLaterEntry.COLUMN_USER_ID, USER_ID);
        return contentValues;
    }

    /** Возвращает _id элемента на указанной позиции.
     * Проверяет, что cursor != null и cursor.getCount() == 0.
     *
     * @param provider провайдер для доступа к базе данных
     * @param position позиция элемента
     *
     * @return _id элемента на позиции COLUMN_ORDER = position
     *
     * @throws AssertionError если по запросу указанной позиции cursor == null или cursor.getCount() != 1
     */
    private int getItemIdAtPosition(@NonNull ContentProvider provider, @IntRange(from = 0) int position) {
        Cursor cursor = provider.query(ReadLaterEntry.CONTENT_URI, null, ReadLaterEntry.COLUMN_ORDER + "=?",
                new String[] { String.valueOf(position) }, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();
        return itemId;
    }

}
