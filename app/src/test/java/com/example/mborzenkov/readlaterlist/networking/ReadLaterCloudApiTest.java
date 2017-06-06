package com.example.mborzenkov.readlaterlist.networking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Тестирует UserInfoUtils. */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterCloudApiTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    private static final int DEFAULT_USER = 1005930;
    private static final int SECOND_USER  = 1005940;
    private static final ReadLaterItem defaultItem = new ReadLaterItem.Builder("label")
            .description("description").color(Color.RED).imageUrl("http://i.imgur.com/TyCSG9A.png").build();
    private static final ReadLaterItem secondItem = new ReadLaterItem.Builder("label2")
            .description("description2").color(Color.BLUE).build();

    private final MockWebServer mServer = new MockWebServer();
    private ReadLaterCloudApi mCloudApi;


    @Before
    @SuppressWarnings("CheckStyle")
    public void onStart() throws IOException {
        mServer.start();
        HttpUrl serverUrl = mServer.url("");
        mServer.setDispatcher(new CloudApiMockDispatcher(serverUrl.host() + ":" + serverUrl.port()));
        CloudApiComponent component = DaggerCloudApiComponent.builder()
                .cloudApiModule(new CloudApiModule(serverUrl)).build();
        mCloudApi = new ReadLaterCloudApi(component);
        component.inject(mCloudApi);
        // ShadowLog.stream = System.out; // Раскомментируйте строчку для вывода в лог всех обращений
    }

    @After
    public void onStop() throws IOException {
        mServer.shutdown();
    }

    @Test
    public void testInsertAndQuery() {
        List<ReadLaterItem> allItems;

        // Проверим, что заметок пока действительно нет ни у DEFAULT_USER, ни у SECOND_USER
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertTrue(allItems.isEmpty());
        allItems = mCloudApi.getAllItemsOnServer(SECOND_USER);
        assertTrue(allItems != null);
        assertTrue(allItems.isEmpty());

        Integer newItemId;
        // Добавим заметку DEFAULT_USER
        newItemId = mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem);
        assertTrue(newItemId != null);

        // У DEFAULT_USER теперь должна быть одна заметка и она равна defaultItem по содержанию, а также id == newItemId
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertEquals(1, allItems.size());

        ReadLaterItem itemFromServer;
        itemFromServer = allItems.get(0);
        assertTrue(defaultItem.equalsByContent(itemFromServer));
        assertEquals((int) newItemId, itemFromServer.getRemoteId());

        // У SECOND_USER не должно быть заметок
        allItems = mCloudApi.getAllItemsOnServer(SECOND_USER);
        assertTrue(allItems != null);
        assertTrue(allItems.isEmpty());

        // Добавим еще одну заметку DEFAULT_USER
        newItemId = mCloudApi.insertItemOnServer(DEFAULT_USER, secondItem);
        assertTrue(newItemId != null);

        // У DEFAULT_USER теперь должно быть две заметки, равные по содержанию соответственно defaultItem и secondItem
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertEquals(2, allItems.size());

        for (ReadLaterItem item : allItems) {
            if (item.getRemoteId() == newItemId) {
                assertTrue(secondItem.equalsByContent(item));
            } else {
                assertTrue(defaultItem.equalsByContent(item));
            }
        }

        // У SECOND_USER не должно быть заметок
        allItems = mCloudApi.getAllItemsOnServer(SECOND_USER);
        assertTrue(allItems != null);
        assertTrue(allItems.isEmpty());

        // Добавим одну для SECOND_USER
        newItemId = mCloudApi.insertItemOnServer(SECOND_USER, secondItem);
        assertTrue(newItemId != null);

        // У SECOND_USER теперь должна быть одна заметка и она равна secondItem по содержанию, а также id == newItemId
        allItems = mCloudApi.getAllItemsOnServer(SECOND_USER);
        assertTrue(allItems != null);
        assertEquals(1, allItems.size());

        itemFromServer = allItems.get(0);
        assertTrue(secondItem.equalsByContent(itemFromServer));
        assertEquals((int) newItemId, itemFromServer.getRemoteId());

        // У DEFAULT_USER по прежнему 2 заметки
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertEquals(2, allItems.size());
    }

    @Test
    public void testUpdate() {
        Integer newItemId;
        // Добавим заметку DEFAULT_USER
        newItemId = mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem);
        assertTrue(newItemId != null);

        // Изменим заметку
        mCloudApi.updateItemOnServer(DEFAULT_USER, newItemId, secondItem);

        List<ReadLaterItem> allItems;
        // У DEFAULT_USER теперь должна быть одна заметка и она равна secondItem по содержанию, а также id == newItemId
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertEquals(1, allItems.size());

        ReadLaterItem itemFromServer;
        itemFromServer = allItems.get(0);
        assertTrue(secondItem.equalsByContent(itemFromServer));
        assertEquals((int) newItemId, itemFromServer.getRemoteId());
    }

    @Test
    public void testDelete() {
        Integer newItemId;
        // Добавим заметку DEFAULT_USER
        newItemId = mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem);
        assertTrue(newItemId != null);

        // Удалим заметку
        mCloudApi.deleteItemOnServer(DEFAULT_USER, newItemId);

        List<ReadLaterItem> allItems;
        // У DEFAULT_USER больше нет заметок
        allItems = mCloudApi.getAllItemsOnServer(DEFAULT_USER);
        assertTrue(allItems != null);
        assertEquals(0, allItems.size());
    }

    @Test
    public void testNoResponse() throws IOException {
        // Сервер упал
        mServer.shutdown();

        // Проверим все методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

    @Test
    public void testEmptyResponse() {
        // Сервер вовзвращает нулы
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyDispatcher());

        // Проверим все методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

    @Test
    public void testMalformedResponse() {
        // Сервер вовзвращает JSON неправильного формата
        mServer.setDispatcher(new CloudApiMockDispatcher.MalformedDispatcher());

        // Проверим все методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

    @Test
    public void testErrors() {
        // Сервер вовзвращает JSON неправильного формата
        mServer.setDispatcher(new CloudApiMockDispatcher.ErrorDispatcher());

        // Проверим все методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

    @Test
    public void testEmptyErrorResponse() {
        // Сервер вовзвращает JSON с ошибкой, но без error
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyErrorDispatcher());

        // Проверим все методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

    @Test
    public void testEmptyDataResponse() {
        // Сервер вовзвращает JSON с OK, но без data
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyDataDispatcher());

        // Проверим методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
    }

    @Test
    public void testEmptyBodyResponse() {
        // Сервер вовзвращает JSON с OK, но без data
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyBodyDispatcher());

        // Проверим методы должны выполняться без ошибок и возвращать null или false
        assertEquals(null, mCloudApi.getAllItemsOnServer(DEFAULT_USER));
        assertEquals(null, mCloudApi.insertItemOnServer(DEFAULT_USER, defaultItem));
        assertFalse(mCloudApi.updateItemOnServer(DEFAULT_USER, 0, secondItem));
        assertFalse(mCloudApi.deleteItemOnServer(DEFAULT_USER, 0));
    }

}
