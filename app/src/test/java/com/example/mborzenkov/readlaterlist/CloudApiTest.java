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
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости"
public class CloudApiTest {

    private static final int TEST_USER_ID = 100593;
    private static final int RESPONSE_CODE_SUCCESS = 200;

    private static final long currentTime = System.currentTimeMillis();
    private static final String normalImageUrl = "http://i.imgur.com/aYioFT9.jpg";
    private static final ReadLaterItem item1 = new ReadLaterItem.Builder("Заголовок 1")
            .description("Descrip 232")
            .build();
    private static final ReadLaterItem item2 = new ReadLaterItem.Builder("Заголовок 2")
            .build();

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
            assertEquals(responseAllItems.code(), RESPONSE_CODE_SUCCESS);
            AllItemsResponse responseAllItemsData = responseAllItems.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
        }

        int newItemId;
        {
            Response<CloudApiYufimtsev.NewItemResponse> newItemResponse =
                    cloudApi.createItem(TEST_USER_ID, item1).execute();
            assertEquals(newItemResponse.code(), RESPONSE_CODE_SUCCESS);
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
            assertEquals(singleItemResponse.code(), RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
            assertTrue(responseAllItemsData.data.equals(item1));
        }

        {
            Response<CloudApiYufimtsev.DefaultResponse> updateItemResponse =
                    cloudApi.updateItem(TEST_USER_ID, newItemId, item2).execute();
            assertEquals(updateItemResponse.code(), RESPONSE_CODE_SUCCESS);
            DefaultResponse responseAllItemsData = updateItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
        }

        {
            Response<CloudApiYufimtsev.SingleItemResponse> singleItemResponse =
                    cloudApi.getItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
            assertTrue(responseAllItemsData.data != null);
            assertTrue(responseAllItemsData.data.equals(item2));
        }

        {
            Response<CloudApiYufimtsev.DefaultResponse> singleItemResponse =
                    cloudApi.deleteItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), RESPONSE_CODE_SUCCESS);
            DefaultResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_SUCCESS);
            assertEquals(responseAllItemsData.error, null);
        }

        {
            Response<CloudApiYufimtsev.SingleItemResponse> singleItemResponse =
                    cloudApi.getItem(TEST_USER_ID, newItemId).execute();
            assertEquals(singleItemResponse.code(), RESPONSE_CODE_SUCCESS);
            SingleItemResponse responseAllItemsData = singleItemResponse.body();
            assertTrue(responseAllItemsData != null);
            assertEquals(responseAllItemsData.status, CloudApiYufimtsev.STATUS_ERROR);
            assertTrue(responseAllItemsData.error != null);
            assertTrue(responseAllItemsData.data == null);
        }

    }

}
