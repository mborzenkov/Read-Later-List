package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentProvider;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContentProvider;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Тестирует DebugUtils. */
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class DebugUtilsTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int USER_ID = 1005930;

    public DebugUtilsTest() {
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
    public void testAddPlaceholders() {
        final ContentProvider provider = getProvider();
        final int firstInsert = 5;
        final int secondInsert = 1;
        final int thirdInsert = 0;

        {
            // Вызываем добавление плейсхолдеров
            DebugUtils.addPlaceholdersToDatabase(getMockContext(), firstInsert);

            // Проверяем, что данные в базе появились
            Cursor cursor = provider.query(ReadLaterContract.ReadLaterEntry.buildUriForUserItems(USER_ID),
                    null, null, null, null);
            assertTrue(cursor != null);
            assertEquals(firstInsert, cursor.getCount());
            cursor.close();
        }

        {
            ReadLaterDbUtils.deleteAll(getMockContext());

            // Вызываем добавление плейсхолдеров
            DebugUtils.addPlaceholdersToDatabase(getMockContext(), secondInsert);

            // Проверяем, что данные в базе появились
            Cursor cursor = provider.query(ReadLaterContract.ReadLaterEntry.buildUriForUserItems(USER_ID),
                    null, null, null, null);
            assertTrue(cursor != null);
            assertEquals(secondInsert, cursor.getCount());
            cursor.close();
        }

        {
            ReadLaterDbUtils.deleteAll(getMockContext());

            // Вызываем добавление плейсхолдеров
            DebugUtils.addPlaceholdersToDatabase(getMockContext(), thirdInsert);

            // Проверяем, что данные в базе появились
            Cursor cursor = provider.query(ReadLaterContract.ReadLaterEntry.buildUriForUserItems(USER_ID),
                    null, null, null, null);
            assertTrue(cursor != null);
            assertEquals(thirdInsert, cursor.getCount());
            cursor.close();
        }

    }

    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings({"UnusedAssignment", "Range"})
    public void testIllegalArgument() {
        DebugUtils.addPlaceholdersToDatabase(getMockContext(), -1);
    }

}
