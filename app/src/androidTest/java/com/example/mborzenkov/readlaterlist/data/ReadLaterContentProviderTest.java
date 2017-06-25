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

/** Тестирует ReadLaterContentProvider. */
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
        assertEquals(null, provider.getType(ReadLaterEntry.buildUriForUserItems(USER_ID)));
    }

    @Test
    public void testInsert() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
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

        ContentValues[] contentValues = new ContentValues[] { dbAdapter.contentValuesFromItem(defaultItem),
                dbAdapter.contentValuesFromItem(secondItem) };
        final int inserted = provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
        assertEquals(2, inserted);

        Cursor cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(USER_ID, REMOTE_ID_DEFAULT),
                null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();

        cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(USER_ID, REMOTE_ID_SECOND), null, null, null, null);
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

        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
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
        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(secondItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        final int itemid = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // с buildUriForOneItem
        cursor = provider.query(ReadLaterEntry.buildUriForOneItem(USER_ID, itemid), null, null, null, null);
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

        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(secondItem));
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForRemoteId(USER_ID, REMOTE_ID_DEFAULT),
                null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        cursor.close();

        cursor = provider.query(
                ReadLaterEntry.buildUriForRemoteId(USER_ID, REMOTE_ID_DEFAULT), null, null, null, null);
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

        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
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

        itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        provider.delete(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null);
        cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testUpdate() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        int updated = provider.update(itemUri, dbAdapter.contentValuesFromItem(secondItem), null, null);
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

        final Uri itemUri = provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID),
                dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);

        ContentValues contentValues;
        // UPDATE, потому что только label
        final ReadLaterItem changedLabel = new ReadLaterItem.Builder(defaultItem).label("newLabel").build();
        contentValues = dbAdapter.contentValuesFromItem(changedLabel);
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
        contentValues = dbAdapter.contentValuesFromItem(changedDesc);
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
        contentValues = dbAdapter.contentValuesFromItem(noFts);
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
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();
        final int firstPos = 1;
        final int secndPos = 2;
        final int thirdPos = 3;
        final int totalItems = 3;

        // Создаем заметки
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(secondItem));
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(secondItem));

        // Запоминаем id по текущим позициям
        final int item1Id = getItemIdAtPosition(provider, firstPos);
        final int item2Id = getItemIdAtPosition(provider, secndPos);
        final int item3Id = getItemIdAtPosition(provider, thirdPos);

        int updated;
        // Меняем элемент 1 на позицию SECND_POS, при этом элемент на позиции THIRD_POS не должен измениться
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, item1Id, secndPos), null, null, null);
        assertEquals(totalItems - 1, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item3Id);

        // Меняем элемент 3 на позицию SECND_POS, при этом элемент на позиции FIRST_POS не должен измениться
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, item3Id, secndPos), null, null, null);
        assertEquals(totalItems - 1, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции FIRST_POS на позицию THIRD_POS, при этом у остальных элементов станет позиция -1
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, item2Id, thirdPos), null, null, null);
        assertEquals(totalItems, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item2Id);

        // Меняем элемент на позиции THIRD_POS на позицию FIRST_POS, при этом у остальных элементов станет позиция +1
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, item2Id, firstPos), null, null, null);
        assertEquals(totalItems, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции SECND_POS на SECND_POS
        updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, item3Id, secndPos), null, null, null);
        assertEquals(0, updated);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);
    }

    @Test
    public void testUpdateUnavailableItemId() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        int updated = provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, 2, 1), null, null, null);
        assertEquals(updated, 0);
    }


    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testInsertUriException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.insert(Uri.EMPTY, dbAdapter.contentValuesFromItem(defaultItem));
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testInserNullValuesException() {
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testInserWrongValuesException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(defaultItem);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testBulkInsertUriException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.bulkInsert(Uri.EMPTY, new ContentValues[] { dbAdapter.contentValuesFromItem(defaultItem) });
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testBulkInserNullValuesException() {
        ContentProvider provider = getProvider();
        //noinspection ConstantConditions
        provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testBulkInserWrongValuesException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(defaultItem);
        contentValues.remove(ReadLaterEntry.COLUMN_LABEL);
        provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), new ContentValues[] { contentValues });
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateUriException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.update(Uri.EMPTY, dbAdapter.contentValuesFromItem(defaultItem), null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNullValuesException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        provider.update(itemUri, null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWrongValuesException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(defaultItem);
        Uri itemUri = provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
        assertTrue(itemUri != null);
        contentValues.put(ReadLaterEntry.COLUMN_LABEL, (String) null);
        provider.update(itemUri, contentValues, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateOrderNegative() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        final int itemId = getItemIdAtPosition(provider, 1);
        provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, itemId, -1), null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateOrderMax() {
        final int bigOrder = 100000;
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        final int itemId = getItemIdAtPosition(provider, 1);
        provider.update(ReadLaterEntry.buildUriForUpdateOrder(USER_ID, itemId, bigOrder), null, null, null);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testQueryUriException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        Cursor cursor = provider.query(Uri.EMPTY, null, null, null, null);
        assertTrue(cursor != null);
        cursor.close();
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteUriException() {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentProvider provider = getProvider();
        provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        provider.delete(Uri.EMPTY, null, null);
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
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID),
                null, ReadLaterEntry.COLUMN_ORDER + "=?",
                new String[] { String.valueOf(position) }, null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();
        return itemId;
    }

}
