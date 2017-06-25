package com.example.mborzenkov.readlaterlist.adt;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContentProvider;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterItemDbAdapterTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    public ReadLaterItemDbAdapterTest() {
        super(ReadLaterContentProvider.class, ReadLaterContentProvider.class.getName());
    }

    /** Размерность ContentValues при помещении ReadLaterItem полностью в ContentValues. */
    private static final int CONTENT_VALUES_FULL_SIZE = 8;

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
    public void testContentValuesFromItem() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(defaultItem);
        assertEquals(CONTENT_VALUES_FULL_SIZE, contentValues.size());
        assertEquals(defaultItem.getLabel(), contentValues.getAsString(ReadLaterEntry.COLUMN_LABEL));
        assertEquals(defaultItem.getDescription(), contentValues.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
        assertTrue(contentValues.getAsInteger(ReadLaterEntry.COLUMN_COLOR).equals(defaultItem.getColor()));
        assertTrue(contentValues.getAsLong(ReadLaterEntry.COLUMN_DATE_CREATED).equals(defaultItem.getDateCreated()));
        assertTrue(contentValues.getAsLong(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED)
                .equals(defaultItem.getDateCreated()));
        assertTrue(contentValues.getAsLong(ReadLaterEntry.COLUMN_DATE_LAST_VIEW).equals(defaultItem.getDateViewed()));
        assertEquals(defaultItem.getImageUrl(), contentValues.getAsString(ReadLaterEntry.COLUMN_IMAGE_URL));
        assertTrue(contentValues.getAsInteger(ReadLaterEntry.COLUMN_REMOTE_ID).equals(defaultItem.getRemoteId()));
    }

    @Test
    public void testItemFromCursor() {
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
    }

    @Test
    public void testItemFromCursorClosed() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(ReadLaterEntry.buildUriForUserItems(USER_ID),
                dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        cursor.close();
        assertEquals(null, dbAdapter.itemFromCursor(cursor));
    }

    @Test
    public void testItemFromCursorEmpty() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertFalse(cursor.moveToNext());
        assertEquals(null, dbAdapter.itemFromCursor(cursor));
    }

    @Test
    public void testItemFromCursorWrongPosition() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(null, dbAdapter.itemFromCursor(cursor));
        assertTrue(cursor.moveToNext());
        assertFalse(cursor.moveToNext());
        assertEquals(null, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testItemFromCursorWrongProjection() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Uri itemUri = provider.insert(
                ReadLaterEntry.buildUriForUserItems(USER_ID), dbAdapter.contentValuesFromItem(defaultItem));
        assertTrue(itemUri != null);
        Cursor cursor = provider.query(itemUri, new String[] { "_id" }, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(null, dbAdapter.itemFromCursor(cursor));
        cursor.close();
    }

    @Test
    public void testAllItemsFromCursor() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        ContentValues[] contentValues = new ContentValues[] { dbAdapter.contentValuesFromItem(defaultItem),
                dbAdapter.contentValuesFromItem(secondItem) };
        int inserted = provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
        assertEquals(2, inserted);
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(2, cursor.getCount());
        List<ReadLaterItem> items = dbAdapter.allItemsFromCursor(cursor);
        cursor.close();
        assertEquals(2, items.size());
        assertTrue(items.contains(defaultItem));
        assertTrue(items.contains(secondItem));
    }

    @Test
    public void testAllItemsFromCursorEmpty() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        List<ReadLaterItem> items = dbAdapter.allItemsFromCursor(cursor);
        cursor.close();
        assertEquals(0, items.size());
    }

    @Test
    public void testAllItemsFromCursorWrongPosition() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        ContentValues[] contentValues = new ContentValues[] { dbAdapter.contentValuesFromItem(defaultItem),
                dbAdapter.contentValuesFromItem(secondItem) };
        int inserted = provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
        assertEquals(2, inserted);
        Cursor cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(2, cursor.getCount());
        List<ReadLaterItem> items = dbAdapter.allItemsFromCursor(cursor);
        assertEquals(2, items.size());
        assertTrue(items.contains(defaultItem));
        assertTrue(items.contains(secondItem));

        assertTrue(cursor.moveToNext());
        assertTrue(cursor.moveToNext());
        assertFalse(cursor.moveToNext());
        items = dbAdapter.allItemsFromCursor(cursor);
        assertEquals(2, items.size());
        assertTrue(items.contains(defaultItem));
        assertTrue(items.contains(secondItem));
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllItemsFromCursorWrongProjection() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        ContentValues[] contentValues = new ContentValues[] { dbAdapter.contentValuesFromItem(defaultItem),
                dbAdapter.contentValuesFromItem(secondItem) };
        int inserted = provider.bulkInsert(ReadLaterEntry.buildUriForUserItems(USER_ID), contentValues);
        assertEquals(2, inserted);
        Cursor cursor = provider.query(
                ReadLaterEntry.buildUriForUserItems(USER_ID), new String[] { "_id" }, null, null, null);
        assertTrue(cursor != null);
        assertEquals(2, cursor.getCount());
        dbAdapter.allItemsFromCursor(cursor);
        cursor.close();
    }

}
