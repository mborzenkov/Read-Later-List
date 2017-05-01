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
    private static final int DATABASE_VERSION = 1;

    public ReadLaterDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Создание таблицы
        final String sqlCreateWeatherTable =
            "CREATE TABLE " + ReadLaterEntry.TABLE_NAME + " ("
                    + ReadLaterEntry._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ReadLaterEntry.COLUMN_LABEL       + " STRING NOT NULL, "
                    + ReadLaterEntry.COLUMN_DESCRIPTION + " STRING NOT NULL, "
                    + ReadLaterEntry.COLUMN_COLOR       + " INTEGER NOT NULL);";

        sqLiteDatabase.execSQL(sqlCreateWeatherTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Пока что мы просто дропаем всю таблицу и создаем новую
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ReadLaterEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

}
