package com.example.mborzenkov.readlaterlist.networking;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.List;

import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Класс описывает API для облачного сохранения списка ReadLaterList. */
interface CloudApiYufimtsev {

    /* Документация API
     *
     * Доступ осуществляется по адресу https://notesbackend-yufimtsev.rhcloud.com/
     *
     * В API присутствуют следующие методы:
     *      GET /info — возвращает все хедеры запроса
     *      GET /user/%user_id%/notes — возвращает все заметки пользователя %user_id%
     *      GET /user/%user_id%/note/%note_id% — возвращает соответствующую заметку
     *      POST /user/%user_id%/notes — создаёт новую заметку (в body запросе должен присутствовать JSON с создаваемой
     *          заметкой) и возвращает её ID
     *      POST /user/%user_id%/note/%note_id% — редактирует заметку (в body запросе должен присутствовать полный JSON
     *          с данными заметки)
     *      DELETE /user/%user_id%/note/%note_id% — удаляет заметку
     *
     * В успешном ответе всегда присутствует поле "status".
     *      Если он указан как "ok", то запрос выполнен удачно.
     *      Если запрос подразумевал возврат данных, они будут находиться в поле "data".
     */

    // -- Константы
    /** Адрес для подключения. */
    HttpUrl BASE_URL = new HttpUrl.Builder().scheme("https").host("notesbackend-yufimtsev.rhcloud.com").build();
    /** Параметр идентификатор пользователя в строке запроса. */
    String PARAM_USER_ID = "user_id";
    /** Параметр идентификатор заметки в строке запроса. */
    String PARAM_ITEM_ID = "item_id";
    /** Путь к списку всех заметок. */
    String PATH_NOTES_ALL = "user/{" + PARAM_USER_ID + "}/notes";
    /** Путь к конкретной заметке. */
    String PATH_NOTE = "user/{" + PARAM_USER_ID + "}/note/{" + PARAM_ITEM_ID + "}";

    // -- Статусы
    /** Статус успешного выполнения запроса. */
    String STATUS_SUCCESS = "ok";
    /** Статус ошибки при выполнении запроса. */
    String STATUS_ERROR = "error";

    /** Разрешенные статусы выполнения запроса. */
    @StringDef({STATUS_SUCCESS, STATUS_ERROR})
    @interface RequestStatus { }

    // -- Ответы сервера
    /** Ответ сервера 200 OK. */
    @SuppressWarnings("unused") // на будущее
    int RESPONSE_CODE_SUCCESS = 200;
    /** Стандартный ответ сервера. */
    @SuppressWarnings({"CanBeFinal", "unused"}) // Retrofit использует
    class DefaultResponse {
        /** Статус выполнения запроса, всегда присутствует и равен STATUS_SUCCESS или STATUS_ERROR. */
        public @RequestStatus String status;
        /** Если STATUS_ERROR, то содержит ошибку, иначе null. */
        public @Nullable String error;
    }

    /** Формат ответа сервера при запросе списка всех заметок. */
    @SuppressWarnings({"CanBeFinal", "unused"}) // Retrofit использует
    class AllItemsResponse extends DefaultResponse {
        /** Если STATUS_SUCCESS, то содержит список заметок пользователя, иначе null. */
        public @Nullable List<ReadLaterItem> data;
    }

    /** Формат ответа сервера при запросе создания новой заметки. */
    @SuppressWarnings({"CanBeFinal", "unused"}) // Retrofit использует
    class NewItemResponse extends DefaultResponse {
        /** Если STATUS_SUCCESS, то содержит присвоенный заметке id, иначе null. */
        public @Nullable Integer data;
    }

    /** Формат ответа сервера при запросе отдельной заметки. */
    @SuppressWarnings({"CanBeFinal", "unused"}) // Retrofit использует
    class SingleItemResponse extends DefaultResponse {
        /** Если STATUS_SUCCESS, то содержит запрошенную заметку, иначе null. */
        public @Nullable ReadLaterItem data;
    }

    // -- Доступные методы
    /** Возвращает все элементы ReadLaterItem, принадлежащие пользователю.
     *
     * @param userId идентификатор пользователя, положительное число
     * @return ответ в формате AllItemsResponse.
     *          Будет содержать ошибку, если указан неверный userId.
     */
    @GET(PATH_NOTES_ALL)
    Call<AllItemsResponse> getAllItems(@Path(PARAM_USER_ID) @IntRange(from = 0) int userId);

    /** Возвращает отдельную заметку, принадлежащую пользователю.
     *
     * @param userId идентификатор пользователя, положительное число
     * @param itemId идентификатор заметки, положительное число
     * @return ответ в формате SingleItemResponse.
     *          Будет содержать ошибку, если указан неверный userId или заметки с таким itemId у пользователя нет.
     */
    @SuppressWarnings("unused") // на будущее
    @GET(PATH_NOTE)
    Call<SingleItemResponse> getItem(@Path(PARAM_USER_ID) @IntRange(from = 0) int userId,
                          @Path(PARAM_ITEM_ID) @IntRange(from = 0) int itemId);

    /** Создает новую заметку для пользователя.
     *
     * @param userId идентификатор пользователя, положительное число
     * @param item заметка
     * @return ответ в формате NewItemResponse.
     *          Будет содержать ошибку, если указан неверный userId.
     */
    @POST(PATH_NOTES_ALL)
    Call<NewItemResponse> createItem(@Path(PARAM_USER_ID) @IntRange(from = 0) int userId,
                   @Body @NonNull ReadLaterItem item);

    /** Обновляет отдельную заметку, принадлежащую пользователю.
     *
     * @param userId идентификатор пользователя, положительное число
     * @param itemId идентификатор заметки, положительное число
     * @param item новая заметка
     * @return ответ в формате DefaultResponse.
     *          Будет содержать ошибку, если указан неверный userId или заметки с таким itemId у пользователя нет.
     */
    @POST(PATH_NOTE)
    Call<DefaultResponse> updateItem(@Path(PARAM_USER_ID) @IntRange(from = 0) int userId,
                       @Path(PARAM_ITEM_ID) @IntRange(from = 0) int itemId,
                       @Body @NonNull ReadLaterItem item);

    /** Удаляет отдельную заметку, принадлежащую пользователю.
     *
     * @param userId идентификатор пользователя, положительное число
     * @param itemId идентификатор заметки, положительное число
     * @return ответ в формате DefaultResponse.
     *          Будет содержать ошибку, если указан неверный userId или заметки с таким itemId у пользователя нет.
     */
    @DELETE(PATH_NOTE)
    Call<DefaultResponse> deleteItem(@Path(PARAM_USER_ID) @IntRange(from = 0) int userId,
                       @Path(PARAM_ITEM_ID) @IntRange(from = 0) int itemId);

}
