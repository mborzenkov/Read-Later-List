package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentProvider;
import android.database.Cursor;
import android.graphics.Color;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContentProvider;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Тестирует ReadLaterDbUrils. */
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterDbUtilsTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int USER_ID = 1005931;
    private static final int REMOTE_ID_DEFAULT = 123;
    private static final int REMOTE_ID_SECOND  = 345;
    private static final long ALMOST_CURRENT_TIME = System.currentTimeMillis() - 5000;
    private static final ReadLaterItem defaultItem = new ReadLaterItem.Builder("label")
            .description("description").color(Color.RED).imageUrl("http://i.imgur.com/TyCSG9A.png")
            .allDates(ALMOST_CURRENT_TIME).remoteId(REMOTE_ID_DEFAULT).build();
    private static final ReadLaterItem secondItem = new ReadLaterItem.Builder(defaultItem)
            .label("label2").description("description2").color(Color.BLUE).remoteId(REMOTE_ID_SECOND).build();

    public ReadLaterDbUtilsTest() {
        super(ReadLaterContentProvider.class, ReadLaterContract.CONTENT_AUTHORITY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();
        UserInfoUtils.changeCurrentUser(getContext(), USER_ID);
        getMockContentResolver().addProvider(ReadLaterContract.CONTENT_AUTHORITY, getProvider());
    }

    @Test
    public void testInsertItem() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Cursor cursor;

        // Проверяем, что еще нет данных для этого пользователя
        cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        // Проверяем, что данные в базе появились
        cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(defaultItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testBulkInsertItems() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        final ContentProvider provider = getProvider();

        Cursor cursor;

        // Проверяем, что еще нет данных для этого пользователя
        cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // Вставляем данные
        final List<ReadLaterItem> items = new ArrayList<>();
        items.add(defaultItem);
        items.add(secondItem);
        ReadLaterDbUtils.bulkInsertItems(getMockContext(), items);

        // Проверяем, что данные в базе появились
        cursor = provider.query(ReadLaterEntry.buildUriForUserItems(USER_ID), null, null, null, null);
        assertTrue(cursor != null);
        assertEquals(2, cursor.getCount());
        assertTrue(items.containsAll(dbAdapter.allItemsFromCursor(cursor)));
        cursor.close();
    }

    @Test
    public void testGetItemByRemoteId() {
        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        ReadLaterItem itemByRemoteId;
        // Пробуем ее получить по RemoteId
        itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_DEFAULT);
        assertTrue(itemByRemoteId != null);
        assertEquals(defaultItem, itemByRemoteId);

        // Пробуем получить заметку, которой нет
        itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_SECOND);
        assertEquals(null, itemByRemoteId);
    }

    @Test
    public void testQueryAllItems() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        // Вставляем данные
        final List<ReadLaterItem> items = new ArrayList<>();
        items.add(defaultItem);
        items.add(secondItem);
        ReadLaterDbUtils.bulkInsertItems(getMockContext(), items);

        // Пробуем получить их
        Cursor cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(2, cursor.getCount());
        assertTrue(items.containsAll(dbAdapter.allItemsFromCursor(cursor)));
        cursor.close();
    }

    @Test
    public void testUpdateItem() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        Cursor cursor;
        // Получаем _id
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        final int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // Обновляем
        ReadLaterDbUtils.updateItem(getMockContext(), secondItem, itemId);

        // Проверяем изменения
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(secondItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testUpdateItemByRemoteId() {
        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        // Обновляем
        ReadLaterDbUtils.updateItem(getMockContext(), secondItem, USER_ID, REMOTE_ID_DEFAULT);

        ReadLaterItem itemByRemoteId;
        // Проверяем изменения
        itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_SECOND);
        assertTrue(itemByRemoteId != null);
        assertEquals(secondItem, itemByRemoteId);
        assertEquals(null, ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_DEFAULT));

        // Пробуем обновить неправильный remote_id, должно пройти без ошибки (ничего не измениться)
        ReadLaterDbUtils.updateItem(getMockContext(), secondItem, USER_ID, REMOTE_ID_DEFAULT);
        assertEquals(null, ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_DEFAULT));
        itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_SECOND);
        assertTrue(itemByRemoteId != null);
        assertEquals(secondItem, itemByRemoteId);
    }

    @Test
    public void testUpdateItemViewDate() {
        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        Cursor cursor;
        // Получаем _id
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        final int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // Обновляем дату просмотра
        ReadLaterDbUtils.updateItemViewDate(getMockContext(), itemId);

        // Проверяем
        ReadLaterItem itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_DEFAULT);
        assertTrue(itemByRemoteId != null);
        assertTrue(defaultItem.equalsByContent(itemByRemoteId));
        assertFalse(defaultItem.equals(itemByRemoteId));
        assertTrue(defaultItem.getDateViewed() < itemByRemoteId.getDateViewed());
    }

    @Test
    public void testUpdateItemRemoteId() {
        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        Cursor cursor;
        // Получаем _id
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        final int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // Обновляем дату просмотра
        ReadLaterDbUtils.updateItemRemoteId(getMockContext(), itemId, REMOTE_ID_SECOND);

        // Проверяем
        ReadLaterItem itemByRemoteId = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, REMOTE_ID_SECOND);
        assertTrue(itemByRemoteId != null);
        assertTrue(defaultItem.equalsByContent(itemByRemoteId));
        assertFalse(defaultItem.equals(itemByRemoteId));
        assertEquals(REMOTE_ID_SECOND, itemByRemoteId.getRemoteId());
    }

    @Test
    public void testDeleteItem() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);

        Cursor cursor;
        // Получаем _id
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        final int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();

        // Вставляем еще одну запись
        ReadLaterDbUtils.insertItem(getMockContext(), secondItem);

        // Удаляем данные
        ReadLaterDbUtils.deleteItem(getMockContext(), itemId);

        // Проверяем
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertEquals(secondItem, dbAdapter.itemFromCursor(cursor));
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testDeleteAll() {
        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);
        ReadLaterDbUtils.insertItem(getMockContext(), secondItem);

        // Удаляем данные
        ReadLaterDbUtils.deleteAll(getMockContext());

        // Проверяем
        Cursor cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testQueryRange() {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();

        // Вставляем данные
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);
        ReadLaterDbUtils.insertItem(getMockContext(), secondItem);

        Cursor cursor;
        // Проверяем
        cursor = ReadLaterDbUtils.queryRange(getMockContext(), 0, 1);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        final ReadLaterItem itemFromCursor = dbAdapter.itemFromCursor(cursor);
        assertTrue(itemFromCursor != null);
        assertFalse(cursor.moveToNext());
        cursor.close();

        cursor = ReadLaterDbUtils.queryRange(getMockContext(), 1, 1);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        assertTrue(!itemFromCursor.equals(dbAdapter.itemFromCursor(cursor)));
        assertFalse(cursor.moveToNext());
        cursor.close();

        // Проверяем, что ничего не падает при неправильных запросах
        cursor = ReadLaterDbUtils.queryRange(getMockContext(), 0, 0);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = ReadLaterDbUtils.queryRange(getMockContext(), 1, 0);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = ReadLaterDbUtils.queryRange(getMockContext(), 2, 1);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testChangeItemOrder() {
        final ContentProvider provider = getProvider();
        final int firstPos = 1;
        final int secndPos = 2;
        final int thirdPos = 3;

        // Создаем заметки
        ReadLaterDbUtils.insertItem(getMockContext(), defaultItem);
        ReadLaterDbUtils.insertItem(getMockContext(), secondItem);
        ReadLaterDbUtils.insertItem(getMockContext(), secondItem);

        // Запоминаем id по текущим позициям
        final int item1Id = getItemIdAtPosition(provider, firstPos);
        final int item2Id = getItemIdAtPosition(provider, secndPos);
        final int item3Id = getItemIdAtPosition(provider, thirdPos);

        // Меняем элемент 1 на позицию SECND_POS, при этом элемент на позиции THIRD_POS не должен измениться
        ReadLaterDbUtils.changeItemOrder(getMockContext(), item1Id, secndPos);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item3Id);

        // Меняем элемент 3 на позицию SECND_POS, при этом элемент на позиции FIRST_POS не должен измениться
        ReadLaterDbUtils.changeItemOrder(getMockContext(), item3Id, secndPos);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции FIRST_POS на позицию THIRD_POS, при этом у остальных элементов станет позиция -1
        ReadLaterDbUtils.changeItemOrder(getMockContext(), item2Id, thirdPos);
        assertEquals(getItemIdAtPosition(provider, firstPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item1Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item2Id);

        // Меняем элемент на позиции THIRD_POS на позицию FIRST_POS, при этом у остальных элементов станет позиция +1
        ReadLaterDbUtils.changeItemOrder(getMockContext(), item2Id, firstPos);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);

        // Меняем элемент на позиции SECND_POS на SECND_POS
        ReadLaterDbUtils.changeItemOrder(getMockContext(), item3Id, secndPos);
        assertEquals(getItemIdAtPosition(provider, firstPos), item2Id);
        assertEquals(getItemIdAtPosition(provider, secndPos), item3Id);
        assertEquals(getItemIdAtPosition(provider, thirdPos), item1Id);
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
        Cursor cursor = provider.query(
                ReadLaterEntry.buildUriForUserItems(USER_ID),
                null,
                ReadLaterEntry.COLUMN_ORDER + "=?",
                new String[] { String.valueOf(position) },
                null);
        assertTrue(cursor != null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterEntry._ID));
        cursor.close();
        return itemId;
    }

}
