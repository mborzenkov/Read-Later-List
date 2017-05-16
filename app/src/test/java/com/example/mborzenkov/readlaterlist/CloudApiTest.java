package com.example.mborzenkov.readlaterlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev.DefaultResponse;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev.AllItemsResponse;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev.NewItemResponse;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev.SingleItemResponse;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;


/** Тестирует подключение к Cloud API. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости"
public class CloudApiTest {

    private static final int TEST_USER_ID = 100593;

    private static final ReadLaterItem.Builder item1Builder = new ReadLaterItem.Builder("Заголовок 1")
            .description("Descrip 232");
    private static final ReadLaterItem.Builder item2Builder = new ReadLaterItem.Builder("Заголовок 2");

    @Test
    public void testApiYufimtsev() throws IOException {

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CloudApiYufimtsev.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build();

        CloudApiYufimtsev cloudApi = retrofit.create(CloudApiYufimtsev.class);

        {
            Response<AllItemsResponse> responseAllItems =
                    cloudApi.getAllItems(TEST_USER_ID).execute();
            assertEquals(responseAllItems.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            AllItemsResponse responseAllItemsData = responseAllItems.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
        }

        int newItemId;
        {
            Response<CloudApiYufimtsev.NewItemResponse> newItemResponse =
                    cloudApi.createItem(TEST_USER_ID, item1Builder.build()).execute();
            assertEquals(newItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            NewItemResponse responseAllItemsData = newItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
            assertTrue(responseAllItemsData.data > 0);
            newItemId = responseAllItemsData.data;
        }

        {
            Response<CloudApiYufimtsev.SingleItemResponse> singleItemResponse =
                    cloudApi.getItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
            assertTrue(responseAllItemsData.data.equals(item1Builder.remoteId(newItemId).build()));
        }

        {
            Response<CloudApiYufimtsev.DefaultResponse> updateItemResponse =
                    cloudApi.updateItem(TEST_USER_ID, newItemId, item2Builder.build()).execute();
            assertEquals(updateItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            DefaultResponse responseAllItemsData = updateItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
        }

        {
            Response<CloudApiYufimtsev.SingleItemResponse> singleItemResponse =
                    cloudApi.getItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
            assertTrue(responseAllItemsData.data.equals(item2Builder.remoteId(newItemId).build()));
        }

        {
            Response<CloudApiYufimtsev.DefaultResponse> singleItemResponse =
                    cloudApi.deleteItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            DefaultResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
        }

        {
            Response<CloudApiYufimtsev.SingleItemResponse> singleItemResponse =
                    cloudApi.getItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), CloudApiYufimtsev.RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_ERROR);
            assertTrue(responseAllItemsData.error != null);
            assertTrue(responseAllItemsData.data == null);
        }

    }

}
