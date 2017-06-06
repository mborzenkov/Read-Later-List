package com.example.mborzenkov.readlaterlist.sync;

import android.database.Cursor;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.adt.Conflict;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContentProvider;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.CloudApiMockDispatcher;
import com.example.mborzenkov.readlaterlist.networking.CloudApiModule;
import com.example.mborzenkov.readlaterlist.networking.DaggerCloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.ReadLaterCloudApi;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Тестирует SyncAsyncTask. */
@RunWith(AndroidJUnit4.class)
public class SyncAsyncTaskTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int SECOND_DELAY = 1000;
    private static final int BASIC_TIMEOUT = 5000;
    private static final int USER_ID = 1005931;
    private static final long ALMOST_CURRENT_TIME = System.currentTimeMillis() - 5000;
    private static final ReadLaterItem defaultItem = new ReadLaterItem.Builder("label")
            .description("description").color(Color.RED).imageUrl("http://i.imgur.com/TyCSG9A.png")
            .allDates(ALMOST_CURRENT_TIME).build();
    private static final ReadLaterItem secondItem = new ReadLaterItem.Builder(defaultItem)
            .label("label2").description("description2").color(Color.BLUE).build();

    private final Object mSyncObject = new Object();

    private final @NonNull MockWebServer mServer = new MockWebServer();
    private ReadLaterCloudApi mCloudApi;

    public SyncAsyncTaskTest() {
        super(ReadLaterContentProvider.class, ReadLaterContract.CONTENT_AUTHORITY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();
        UserInfoUtils.changeCurrentUser(getContext(), USER_ID);
        getMockContentResolver().addProvider(ReadLaterContract.CONTENT_AUTHORITY, getProvider());
        mServer.start();
        HttpUrl serverUrl = mServer.url("");
        mServer.setDispatcher(new CloudApiMockDispatcher(serverUrl.host() + ":" + serverUrl.port()));
        CloudApiComponent component = DaggerCloudApiComponent.builder()
                .cloudApiModule(new CloudApiModule(serverUrl)).build();
        mCloudApi = new ReadLaterCloudApi(component);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        mServer.shutdown();
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncInsertServer() throws InterruptedException {
        final ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        // Добавляем 2 заметки
        final List<ReadLaterItem> itemsLocal = new ArrayList<>();
        itemsLocal.add(defaultItem);
        itemsLocal.add(secondItem);
        ReadLaterDbUtils.bulkInsertItems(getMockContext(), itemsLocal);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что на сервере действительно 2 заметки
        final List<ReadLaterItem> itemsServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsServer != null);
        assertEquals(2, itemsServer.size());
        for (ReadLaterItem item : itemsServer) {
            if (item.getLabel().equals(secondItem.getLabel())) {
                assertTrue(secondItem.equalsByContent(item));
            } else {
                assertTrue(defaultItem.equalsByContent(item));
            }
        }

        // Проверяем, что в БД заметки что надо
        Cursor cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        final List<ReadLaterItem> itemsLocalNew = dbAdapter.allItemsFromCursor(cursor);
        cursor.close();
        assertEquals(2, itemsLocalNew.size());
        for (ReadLaterItem item : itemsLocalNew) {
            if (item.getLabel().equals(secondItem.getLabel())) {
                assertTrue(secondItem.equalsByContent(item));
            } else {
                assertTrue(defaultItem.equalsByContent(item));
            }
        }

        // Пробуем еще раз синхронизироваться
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что на сервере ничего не поменялось
        final List<ReadLaterItem> itemsServerNew = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsServerNew != null);
        assertEquals(2, itemsServerNew.size());
        for (ReadLaterItem item : itemsServerNew) {
            if (item.getLabel().equals(secondItem.getLabel())) {
                assertTrue(secondItem.equalsByContent(item));
            } else {
                assertTrue(defaultItem.equalsByContent(item));
            }
        }

        // Провряем, что в базе ничего не поменялось
        Cursor cursorNew = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursorNew != null);
        final List<ReadLaterItem> itemsLocalNewNew = dbAdapter.allItemsFromCursor(cursorNew);
        cursorNew.close();
        assertEquals(2, itemsLocalNewNew.size());
        for (ReadLaterItem item : itemsLocalNewNew) {
            if (item.getLabel().equals(secondItem.getLabel())) {
                assertTrue(secondItem.equalsByContent(item));
            } else {
                assertTrue(defaultItem.equalsByContent(item));
            }
        }
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncUpdateServer() throws InterruptedException {
        // Добавляем на сервер и локально
        final Integer itemRemoteId = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(itemRemoteId != null);
        final ReadLaterItem itemWithId = new ReadLaterItem.Builder(defaultItem).remoteId(itemRemoteId).build();
        ReadLaterDbUtils.insertItem(getMockContext(), itemWithId);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Обновляем заметку локально, включая дату изменения
        ReadLaterDbUtils.updateItem(getMockContext(), new ReadLaterItem.Builder(secondItem).remoteId(itemRemoteId)
                .dateModified(System.currentTimeMillis() + SECOND_DELAY).build(), USER_ID, itemRemoteId);

        // Запускаем синхронизацию
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что локально не изменилась, а на сервере изменилась
        final ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, itemRemoteId);
        assertTrue(itemLocal != null);
        assertTrue(secondItem.equalsByContent(itemLocal));
        final List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsOnServer != null);
        assertEquals(1, itemsOnServer.size());
        assertTrue(itemLocal.equalsByContent(itemsOnServer.get(0)));
        assertTrue(itemRemoteId.equals(itemsOnServer.get(0).getRemoteId()));
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncDeleteServer() throws InterruptedException {
        // Добавляем на сервер и локально
        final Integer itemRemoteId = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(itemRemoteId != null);
        final ReadLaterItem itemWithId = new ReadLaterItem.Builder(defaultItem).remoteId(itemRemoteId).build();
        ReadLaterDbUtils.insertItem(getMockContext(), itemWithId);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        Cursor cursor;
        // Удаляем заметку локально
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertTrue(cursor.moveToNext());
        final int itemId = cursor.getInt(cursor.getColumnIndex(ReadLaterContract.ReadLaterEntry._ID));
        assertFalse(cursor.moveToNext());
        cursor.close();
        ReadLaterDbUtils.deleteItem(getMockContext(), itemId);

        // Запускаем синхронизацию
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что локально нет, на сервере тоже
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();
        List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsOnServer != null);
        assertTrue(itemsOnServer.isEmpty());
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncInsertLocal() throws InterruptedException {
        // Добавляем на сервер
        final Integer itemRemoteId = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(itemRemoteId != null);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что локально появилась заметка, а на сервере не изменилась
        final ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, itemRemoteId);
        assertTrue(itemLocal != null);
        assertTrue(defaultItem.equalsByContent(itemLocal));
        final List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsOnServer != null);
        assertEquals(1, itemsOnServer.size());
        assertTrue(itemLocal.equalsByContent(itemsOnServer.get(0)));
        assertTrue(itemRemoteId.equals(itemsOnServer.get(0).getRemoteId()));
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncUpdateLocal() throws InterruptedException {
        // Добавляем на сервер и локально
        final Integer itemRemoteId = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(itemRemoteId != null);
        final ReadLaterItem itemWithId = new ReadLaterItem.Builder(defaultItem).remoteId(itemRemoteId).build();
        ReadLaterDbUtils.insertItem(getMockContext(), itemWithId);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Обновляем заметку на сервере, включая дату изменения
        mCloudApi.updateItemOnServer(USER_ID, itemRemoteId, new ReadLaterItem.Builder(secondItem).remoteId(itemRemoteId)
                .dateModified(System.currentTimeMillis() + SECOND_DELAY).build());

        // Запускаем синхронизацию
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Проверяем, что локально и на сервере изменения синхронизировались
        final ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(getMockContext(), USER_ID, itemRemoteId);
        assertTrue(itemLocal != null);
        assertTrue(secondItem.equalsByContent(itemLocal));
        final List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsOnServer != null);
        assertEquals(1, itemsOnServer.size());
        assertTrue(itemLocal.equalsByContent(itemsOnServer.get(0)));
        assertTrue(itemRemoteId.equals(itemsOnServer.get(0).getRemoteId()));
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncDeleteLocal() throws InterruptedException {
        // Добавляем на сервер и локально
        final Integer itemRemoteId = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(itemRemoteId != null);
        final ReadLaterItem itemWithId = new ReadLaterItem.Builder(defaultItem).remoteId(itemRemoteId).build();
        ReadLaterDbUtils.insertItem(getMockContext(), itemWithId);

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        // Удаляем заметку на сервере
        assertTrue(mCloudApi.deleteItemOnServer(USER_ID, itemRemoteId));

        // Запускаем синхронизацию
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.SUCCESS, callbackTest.taskResult);

        Cursor cursor;
        // Проверяем, что локально нет, на сервере тоже
        cursor = ReadLaterDbUtils.queryAllItems(getMockContext(), USER_ID);
        assertTrue(cursor != null);
        assertEquals(0, cursor.getCount());
        cursor.close();
        List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(USER_ID);
        assertTrue(itemsOnServer != null);
        assertTrue(itemsOnServer.isEmpty());
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncWithConflicts() throws InterruptedException {
        // Добавляем заметку на сервер
        Integer id = mCloudApi.insertItemOnServer(USER_ID, defaultItem);
        assertTrue(id != null);
        // Добавляем заметку локально
        ReadLaterDbUtils.insertItem(getMockContext(), new ReadLaterItem.Builder(secondItem).remoteId(id).build());

        // Запускаем синхронизацию
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();

        // Ждем колбека
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.CONFLICT, callbackTest.taskResult);
        assertTrue(callbackTest.conflictList != null);
        assertEquals(1, callbackTest.conflictList.size());
        Conflict conflict = callbackTest.conflictList.get(0);
        assertTrue(conflict.getLeft().equalsByContent(defaultItem));
        assertTrue(conflict.getRight().equalsByContent(secondItem));
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testSyncApiError() throws InterruptedException, IOException {
        final SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        SyncAsyncTask asyncTask;

        // API сломалась
        mServer.setDispatcher(new CloudApiMockDispatcher.ErrorDispatcher());
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.FAIL, callbackTest.taskResult);

        // API возвращает ошибки
        mServer.setDispatcher(new CloudApiMockDispatcher.ErrorDispatcher());
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.FAIL, callbackTest.taskResult);

        // Сервер упал
        mServer.shutdown();
        asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();
        synchronized (mSyncObject) {
            mSyncObject.wait();
        }
        assertEquals(SyncCallbackTest.TaskResults.FAIL, callbackTest.taskResult);
    }

    @Test(timeout = BASIC_TIMEOUT)
    public void testNoNetwork() throws InterruptedException {
        SyncCallbackTest callbackTest = new SyncCallbackTest(mSyncObject, getMockContext());
        callbackTest.setNetworkAvailability(false);
        SyncAsyncTask asyncTask = new SyncAsyncTask(callbackTest, mCloudApi);
        asyncTask.execute();
        // В том же потоке onPreExecute
        assertEquals(SyncCallbackTest.TaskResults.FAIL, callbackTest.taskResult);
    }

}
