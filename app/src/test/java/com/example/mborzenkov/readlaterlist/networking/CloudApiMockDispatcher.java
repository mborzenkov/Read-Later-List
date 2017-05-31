package com.example.mborzenkov.readlaterlist.networking;

import android.content.UriMatcher;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/** Класс {@link Dispatcher} для обработки запросов к fake-серверу.
 */
class CloudApiMockDispatcher extends Dispatcher {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    // Константы
    private static final int CODE_ALL_NOTES = 100;
    private static final int CODE_ONE_NOTE  = 101;
    private static final int URI_SEGMENT_USERID = 1;
    private static final int URI_SEGMENT_ITEMID = 3;
    private static final int RESPONSE_OK = 200;
    private static final int RESPONSE_NOT_FOUND = 404;
    private static final int RESPONSE_BAD_REQUEST = 400;
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_DELETE = "DELETE";

    /** Все сохраненные заметки, где key = userId, value = список заметок, где itemId = index. */
    private final SparseArray<List<ReadLaterItem>> mCurrentItems = new SparseArray<>();
    /** Матчер для Uri. */
    private final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    CloudApiMockDispatcher(@NonNull String serverAuthority) {
        mUriMatcher.addURI(serverAuthority, "user/#/notes", CODE_ALL_NOTES);
        mUriMatcher.addURI(serverAuthority, "user/#/note/#", CODE_ONE_NOTE);
    }


    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

        // Запоминаем метод, запрошенную Uri, userId и itemId
        final String method = request.getMethod();
        final Uri requestUri = Uri.parse(request.getRequestUrl().toString());
        final int userId;
        final int itemId;

        // Дефолтный ответ 404
        MockResponse response = new MockResponse().setResponseCode(RESPONSE_NOT_FOUND);

        switch (mUriMatcher.match(requestUri)) {
            case CODE_ALL_NOTES:
                // Запрос всех заметок
                userId = Integer.parseInt(requestUri.getPathSegments().get(URI_SEGMENT_USERID));

                if (METHOD_GET.equals(method)) {
                    // GET ALL NOTES -> status:ok, data:[заметки]
                    response = new MockResponse().setResponseCode(RESPONSE_OK)
                            .setBody("{\"status\":\"ok\",\"data\":" + getAllItemsJson(userId) + "}");
                } else if (METHOD_POST.equals(method)) {
                    // POST ALL NOTES -> status:ok, data:newId (это insert, возвращающий id новой заметки)
                    ReadLaterItem itemFromRequest;
                    try {
                        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
                        JsonAdapter<ReadLaterItem> jsonAdapter = moshi.adapter(ReadLaterItem.class);
                        itemFromRequest = jsonAdapter.fromJson(request.getBody());
                        response = new MockResponse().setResponseCode(RESPONSE_OK)
                                .setBody("{\"status\":\"ok\", \"data\":" + insertItem(userId, itemFromRequest) + "}");
                    } catch (Exception e) {
                        System.out.println("EXC INSERT: " + e.toString());
                        response = new MockResponse().setResponseCode(RESPONSE_OK)
                                .setBody("{\"status\":\"error\",\"error\":\"malformed_item\"}");
                    }
                }

                break;
            case CODE_ONE_NOTE:
                // Запрос конкретной заметки
                userId = Integer.parseInt(requestUri.getPathSegments().get(URI_SEGMENT_USERID));
                itemId = Integer.parseInt(requestUri.getPathSegments().get(URI_SEGMENT_ITEMID));

                // Еслси itemId не подходящий, error not_found сразу
                if (!isValidItemId(userId, itemId)) {
                    response = new MockResponse().setResponseCode(RESPONSE_OK)
                            .setBody("{\"status\":\"error\",\"error\":\"not_found\"}");
                    break;
                }

                switch (method) {
                    case METHOD_GET:
                        // GET NOTE -> status:ok, data:заметка
                        response = new MockResponse().setResponseCode(RESPONSE_OK)
                                .setBody("{\"status\":\"ok\",\"data\":" + getItemJson(userId, itemId) + "}");
                        break;
                    case METHOD_POST:
                        // POST NOTE -> status:ok (изменение заметки)
                        ReadLaterItem itemFromRequest;
                        try {
                            Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
                            JsonAdapter<ReadLaterItem> jsonAdapter = moshi.adapter(ReadLaterItem.class);
                            itemFromRequest = jsonAdapter.fromJson(request.getBody());
                            updateItem(userId, itemId, itemFromRequest);
                            response = new MockResponse().setResponseCode(RESPONSE_OK).setBody("{\"status\":\"ok\"}");
                        } catch (Exception e) {
                            // Что то не так с данными в Body request, значит ошибка
                            response = new MockResponse().setResponseCode(RESPONSE_OK)
                                    .setBody("{\"status\":\"error\",\"error\":\"malformed_item\"}");
                        }
                        break;
                    case METHOD_DELETE:
                        // DELETE NOTE -> status:ok
                        removeItem(userId, itemId);
                        response = new MockResponse().setResponseCode(RESPONSE_OK).setBody("{\"status\":\"ok\"}");
                        break;
                    default:
                        break;
                }

                break;
            default:
                break;
        }

        return response;
    }

    /** Проверяет, существует ли заметка с указанным id у указанного пользователя.
     *
     * @param userId идентификатор пользователя, > 0
     * @param itemId идентификатор заметки
     *
     * @return true, если у указанного пользователя существует заметка с указанным id
     */
    private boolean isValidItemId(@IntRange(from = 0) int userId, int itemId) {
        if (itemId < 0) {
            return false;
        }
        List<ReadLaterItem> items = mCurrentItems.get(userId, new ArrayList<ReadLaterItem>());
        return !items.isEmpty() && (itemId < items.size());
    }

    /** Возвращает список всех заметок в формате Json для указанного пользователя.
     *
     * @param userId идентификатор пользователя
     *
     * @return строка в формате Json со всеми заметками указанного пользователя
     */
    private @NonNull String getAllItemsJson(@IntRange(from = 0) int userId) {
        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));
        return jsonAdapter.toJson(mCurrentItems.get(userId, new ArrayList<ReadLaterItem>()));
    }

    /** Возвращает заметку в формате JSon с указанным id, принадлежащую указанному пользователю.
     *
     * @param userId идентификатор пользователя, > 0
     * @param itemId идентификатор заметки, >= 0 и < items.size() для указанного пользователя
     *
     * @return строка в формате Json с заметкой
     *
     * @throws IndexOutOfBoundsException если itemId < 0 или >= items.size() для указанного пользователя
     */
    private @NonNull String getItemJson(@IntRange(from = 0) int userId, @IntRange(from = 0) int itemId) {
        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<ReadLaterItem> jsonAdapter = moshi.adapter(ReadLaterItem.class);
        List<ReadLaterItem> items = mCurrentItems.get(userId, new ArrayList<ReadLaterItem>());
        return jsonAdapter.toJson(items.get(itemId));
    }

    /** Добавляет новую заметку для указанному пользователя.
     * Устанавливает у нее remoteId = return
     *
     * @param userId идентификатор пользователя, > 0
     * @param item заметка для добавления
     *
     * @return идентификатор заметки (items.size() - 1)
     *
     * @throws NullPointerException если item == null
     */
    private int insertItem(@IntRange(from = 0) int userId, @NonNull ReadLaterItem item) {
        List<ReadLaterItem> items = mCurrentItems.get(userId, new ArrayList<ReadLaterItem>());
        final int newItemId = items.size();
        items.add(new ReadLaterItem.Builder(item).remoteId(newItemId).build());
        mCurrentItems.put(userId, items);
        return newItemId;
    }

    /** Заменяет заметку с указанным id на новую, принадлежащую указанному пользователю.
     *
     * @param userId идентификатор пользователя, > 0
     * @param itemId идентификатор заметки, >= 0 и < items.size() для указанного пользователя
     * @param item новая заметка
     *
     * @throws IndexOutOfBoundsException если itemId < 0 или >= items.size() для указанного пользователя
     * @throws NullPointerException если item == null
     */
    private void updateItem(@IntRange(from = 0) int userId,
                            @IntRange(from = 0) int itemId,
                            @NonNull ReadLaterItem item) {

        List<ReadLaterItem> items = mCurrentItems.get(userId, new ArrayList<ReadLaterItem>());
        items.set(itemId, item);
    }

    /** Удаляет заметку с указанным id, принадлежащую указанному пользователю
     *
     * @param userId идентификатор пользователя, > 0
     * @param itemId идентификатор заметки, >= 0 и < items.size() для указанного пользователя
     *
     * @throws IndexOutOfBoundsException если itemId < 0 или >= items.size() для указанного пользователя
     */
    private void removeItem(@IntRange(from = 0) int userId, @IntRange(from = 0) int itemId) {
        List<ReadLaterItem> items = mCurrentItems.get(userId, new ArrayList<ReadLaterItem>());
        items.remove(itemId);
    }

    /** Dispatcher который всегда возвращает пустой MockResponse. */
    static class EmptyDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse();
        }
    }

    /** Dispatcher который всегда возвращает OK и ответ неправильного формата. */
    static class MalformedDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setResponseCode(RESPONSE_OK)
                    .setBody("{\"somebody\":\"oncetoldme\",\"theworld\":\"isgonnarollme\"}");
        }
    }

    /** Dispatcher который всегда возвращает OK и ошибку. */
    static class ErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setResponseCode(RESPONSE_OK)
                    .setBody("{\"status\":\"error\",\"error\":\"server_is_broken\"}");
        }
    }

    /** Dispatcher который всегда возвращает OK, status SUCCESS и DATA null. */
    static class EmptyDataDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setResponseCode(RESPONSE_OK)
                    .setBody("{\"status\":\"ok\"}");
        }
    }

    /** Dispatcher который всегда возвращает OK, status ERROR и ERROR null. */
    static class EmptyErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setResponseCode(RESPONSE_OK)
                    .setBody("{\"status\":\"error\"}");
        }
    }

    /** Dispatcher который всегда возвращает BAD REQUEST и пустую Body. */
    static class EmptyBodyDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setResponseCode(RESPONSE_BAD_REQUEST).setBody("");
        }
    }

}