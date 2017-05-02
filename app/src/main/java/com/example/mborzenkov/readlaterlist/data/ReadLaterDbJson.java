package com.example.mborzenkov.readlaterlist.data;

import android.database.Cursor;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReadLaterDbJson {

    private static final String FORMAT_DATE = "yyyy-MM-dd'T'hh:mm:ssXXX";

    private final String title;
    private final String description;
    private final String color;
    private final String created;
    private final String edited;
    private final String viewed;

    private ReadLaterDbJson(String title, String description, String color,
                                  String created, String edited, String viewed) {
        this.title = title;
        this.description = description;
        this.color = color;
        this.created = created;
        this.edited = edited;
        this.viewed = viewed;
    }

    public static ReadLaterDbJson fromCursor(Cursor cursor) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        return new ReadLaterDbJson(
                cursor.getString(cursor.getColumnIndex(ReadLaterEntry.COLUMN_LABEL)),
                cursor.getString(cursor.getColumnIndex(ReadLaterEntry.COLUMN_DESCRIPTION)),
                String.format("#%s", Integer.toString(cursor.getInt(cursor.getColumnIndex(ReadLaterEntry.COLUMN_COLOR)), 16)),
                dateFormatter.format(
                        new Date(cursor.getLong(cursor.getColumnIndex(ReadLaterEntry.COLUMN_DATE_CREATED)))),
                dateFormatter.format(
                        new Date(cursor.getLong(cursor.getColumnIndex(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED)))),
                dateFormatter.format(
                        new Date(cursor.getLong(cursor.getColumnIndex(ReadLaterEntry.COLUMN_DATE_LAST_VIEW)))));
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getColor() {
        return Integer.valueOf(color.substring(1), 16);
    }

    public long getDateCreated() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        try {
            return dateFormatter.parse(created).getTime();
        } catch (ParseException e) {
            Log.e("Parse error", "Ошибка разбора даты создания: " + e.toString());
        }
        return 0;
    }

    public long getDateModified() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        try {
            return dateFormatter.parse(edited).getTime();
        } catch (ParseException e) {
            Log.e("Parse error", "Ошибка разбора даты изменения: " + e.toString());
        }
        return 0;
    }

    public long getDateViewed() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        try {
            return dateFormatter.parse(viewed).getTime();
        } catch (ParseException e) {
            Log.e("Parse error", "Ошибка разбора даты просмотра: " + e.toString());
        }
        return 0;
    }
}