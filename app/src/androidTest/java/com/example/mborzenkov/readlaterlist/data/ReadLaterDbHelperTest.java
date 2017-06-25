package com.example.mborzenkov.readlaterlist.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Тестирует ReadLaterDbHelper. */
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterDbHelperTest extends ProviderTestCase2<ReadLaterContentProvider> {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    /** Количество колонок в таблице items. */
    private static final int ITEMS_COLUMNS_COUNT = 11;
    /** Количество колонок в таблице fts. */
    private static final int FTS_COLUMNS_COUNT = 3;

    public ReadLaterDbHelperTest() {
        super(ReadLaterContentProvider.class, ReadLaterContentProvider.class.getName());
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();
    }


    @Test
    public void testUpgradeDatabase() {
        ReadLaterDbHelper helper = new ReadLaterDbHelper(getContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        helper.onUpgrade(db, 0, db.getVersion());
        testDatabase(db);
    }

    @Test
    public void testDowngradeDatabase() {
        ReadLaterDbHelper helper = new ReadLaterDbHelper(getContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        helper.onDowngrade(db, db.getVersion() + 1, db.getVersion());
        testDatabase(db);
    }

    @Test
    public void testResetDatabase() {
        ReadLaterDbHelper helper = new ReadLaterDbHelper(getContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        helper.resetDb(db);
        testDatabase(db);
    }

    /** Проверяет, что база данных соответствует контракту.
     *
     * @param db база данных для проверки, должна быть открыта на чтение
     */
    private void testDatabase(SQLiteDatabase db) {
        Cursor cursor;
        {
            cursor = db.rawQuery("SELECT * FROM " + ReadLaterEntry.TABLE_NAME, null);
            List<String> itemsColumns = Arrays.asList(cursor.getColumnNames());
            cursor.close();
            assertEquals(ITEMS_COLUMNS_COUNT, itemsColumns.size());
            assertTrue(itemsColumns.contains(ReadLaterEntry._ID));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_USER_ID));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_REMOTE_ID));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_LABEL));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_DESCRIPTION));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_COLOR));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_DATE_CREATED));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_DATE_LAST_VIEW));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_IMAGE_URL));
            assertTrue(itemsColumns.contains(ReadLaterEntry.COLUMN_ORDER));
        }

        {
            cursor = db.rawQuery("SELECT * FROM " + ReadLaterEntry.TABLE_NAME_FTS, null);
            List<String> ftsColumns = Arrays.asList(cursor.getColumnNames());
            cursor.close();
            assertEquals(FTS_COLUMNS_COUNT, ftsColumns.size());
            assertTrue(ftsColumns.contains(ReadLaterEntry.COLUMN_LABEL));
            assertTrue(ftsColumns.contains(ReadLaterEntry.COLUMN_DESCRIPTION));
        }
    }

}
