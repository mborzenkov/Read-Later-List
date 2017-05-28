package com.example.mborzenkov.readlaterlist.adt;

import android.graphics.Color;

import static org.junit.Assert.assertEquals;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Тестирует ReadLaterItemJsonAdapter. */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterItemJsonAdapterTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    /** Количество миллисекунд в секундах. */
    private static final int MILLIS = 1000;

    private static final String normalLabel = "Заголовок";
    private static final String normalDescription = "Описание";
    private static final int normalColor = Color.RED;
    private static final long currentTime = MILLIS * (System.currentTimeMillis() / MILLIS);
    private static final String normalImageUrl = "http://i.imgur.com/TyCSG9A.png";

    @Test
    public void testManual() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel).description(normalDescription).allDates(currentTime)
                .color(normalColor).imageUrl(normalImageUrl).build();
        ReadLaterItemJsonAdapter jsonAdapter = new ReadLaterItemJsonAdapter();
        assertEquals(jsonAdapter.fromJson(jsonAdapter.toJson(item)), item);
    }

    @Test
    public void testWithMoshi() throws IOException {
        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<ReadLaterItem> jsonAdapter =
                moshi.adapter(ReadLaterItem.class);

        ReadLaterItem basicItem = new ReadLaterItem.Builder(normalLabel).build();
        String json = jsonAdapter.toJson(basicItem);
        ReadLaterItem itemFromJson = jsonAdapter.fromJson(json);
        assertEquals(itemFromJson, basicItem);

        final String jsonNormal = "{\"color\":\"#FFC107\",\"created\":\"2017-05-27T21:43:27+0500\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"id\":\"0\",\"imageUrl\":\"\","
                + "\"title\":\"Заголовок\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";
        final String jsonNullId = "{\"color\":\"#FFC107\",\"created\":\"2017-05-27T21:43:27+0500\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"imageUrl\":\"\","
                + "\"title\":\"Заголовок\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";

        ReadLaterItem itemWithZeroId = jsonAdapter.fromJson(jsonNormal);
        ReadLaterItem itemWithNullId = jsonAdapter.fromJson(jsonNullId);
        assertEquals(itemWithNullId, itemWithZeroId);
    }

    @Test
    public void testExceptions() throws IOException {

        /* {"color":"#FFC107","created":"2017-05-27T21:43:27+0500","description":"","edited":"2017-05-27T21:43:27+0500",
         * "id":"0","imageUrl":"","title":"Заголовок","viewed":"2017-05-27T21:43:27+0500"}
         */

        final String jsonDateFail = "{\"color\":\"#FFC107\",\"created\":\"2017-05-27\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"id\":\"0\",\"imageUrl\":\"\","
                + "\"title\":\"Заголовок\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";
        final String jsonNumberFail = "{\"color\":\"#FFC107\",\"created\":\"2017-05-27T21:43:27+0500\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"id\":\"A8\",\"imageUrl\":\"\","
                + "\"title\":\"Заголовок\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";
        final String jsonNullFail = "{\"created\":\"2017-05-27T21:43:27+0500\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"id\":\"0\","
                + "\"title\":\"Заголовок\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";
        final String jsonEmptyLabelFail = "{\"color\":\"#FFC107\",\"created\":\"2017-05-27T21:43:27+0500\","
                + "\"description\":\"\",\"edited\":\"2017-05-27T21:43:27+0500\",\"id\":\"0\",\"imageUrl\":\"\","
                + "\"title\":\"\",\"viewed\":\"2017-05-27T21:43:27+0500\"}";

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<ReadLaterItem> jsonAdapter =
                moshi.adapter(ReadLaterItem.class);

        ReadLaterItem itemFromJson = jsonAdapter.fromJson(jsonDateFail);
        assertEquals(itemFromJson, null);

        itemFromJson = jsonAdapter.fromJson(jsonNumberFail);
        assertEquals(itemFromJson, null);

        itemFromJson = jsonAdapter.fromJson(jsonNullFail);
        assertEquals(itemFromJson, null);

        itemFromJson = jsonAdapter.fromJson(jsonEmptyLabelFail);
        assertEquals(itemFromJson, null);
    }



}
