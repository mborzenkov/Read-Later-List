package com.example.mborzenkov.readlaterlist.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

/** Класс для доступа к базе данных. */
class ReadLaterDbHelper extends SQLiteOpenHelper {

    /** Имя базы данных. */
    private static final String DATABASE_NAME = "readlaterlist.db";
    /** Версия базы данных. */
    private static final int DATABASE_VERSION = 10; // Текущая: 10

    public ReadLaterDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("PRAGMA auto_vacuum = FULL;");
        // Создание таблицы
        final String sqlCreateReadLaterTable =
            "CREATE TABLE " + ReadLaterEntry.TABLE_NAME + " ("
                    + ReadLaterEntry._ID                       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ReadLaterEntry.COLUMN_USER_ID            + " INTEGER NOT NULL, "
                    + ReadLaterEntry.COLUMN_REMOTE_ID          + " INTEGER, "
                    + ReadLaterEntry.COLUMN_LABEL              + " TEXT NOT NULL, "
                    + ReadLaterEntry.COLUMN_DESCRIPTION        + " TEXT NOT NULL, "
                    + ReadLaterEntry.COLUMN_COLOR              + " INTEGER NOT NULL, "
                    + ReadLaterEntry.COLUMN_DATE_CREATED       + " INTEGER NOT NULL, "
                    + ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED + " INTEGER NOT NULL, "
                    + ReadLaterEntry.COLUMN_DATE_LAST_VIEW     + " INTEGER NOT NULL, "
                    + ReadLaterEntry.COLUMN_IMAGE_URL          + " TEXT, "
                    + ReadLaterEntry.COLUMN_ORDER              + " INTEGER NOT NULL);";
        sqLiteDatabase.execSQL(sqlCreateReadLaterTable);

        final String sqlCreateFtsTable =
                "CREATE VIRTUAL TABLE " + ReadLaterEntry.TABLE_NAME_FTS + " USING fts4 ("
                        + "content='" + ReadLaterEntry.TABLE_NAME   + "', "
                        + ReadLaterEntry.COLUMN_LABEL               + ", "
                        + ReadLaterEntry.COLUMN_DESCRIPTION         + ");";
        sqLiteDatabase.execSQL(sqlCreateFtsTable);
    }

    void resetDb(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + ReadLaterEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ReadLaterEntry.TABLE_NAME_FTS);
        onCreate(db);
    }

    // Создание таблицы для полнотекстового поиска
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Пока что мы просто дропаем всю таблицу и создаем новую
        resetDb(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: onDowngrade не должно быть в Release
        // Тут он нужен для тестирования, чтобы не увеличивать бесконечно версию БД
        onUpgrade(db, 0, DATABASE_VERSION);
    }
}
