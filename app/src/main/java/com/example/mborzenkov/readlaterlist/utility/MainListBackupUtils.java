package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterDbJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainListBackupUtils {

    private static final String FILE_NAME = "itemlist.ili";

    private MainListBackupUtils() {
        throw new UnsupportedOperationException("Класс MainListBackupUtils - static util, не может иметь экземпляров");
    }

    public static void saveEverythingAsJsonFile(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<List<ReadLaterDbJson>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterDbJson.class));
        Cursor allData = context.getContentResolver()
                .query(ReadLaterContract.ReadLaterEntry.CONTENT_URI, null, null, null, null);
        List<ReadLaterDbJson> savedData = new ArrayList<>();
        for (int i = 0; i < allData.getCount(); i++) {
            allData.moveToPosition(i);
            savedData.add(ReadLaterDbJson.fromCursor(allData));
        }
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(jsonAdapter.toJson(savedData));
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "Ошибка записи: " + e.toString());
        }
    }

    public static void restoreEverythingFromJsonFile(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<List<ReadLaterDbJson>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterDbJson.class));
        String jsonString = "";
        try {
            InputStream inputStream = context.openFileInput(FILE_NAME);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                jsonString = stringBuilder.toString().trim();
            }
        } catch (FileNotFoundException e) {
            Log.e("Read exception", "Файл не найден: " + e.toString());
        } catch (IOException e) {
            Log.e("Read exception", "Ошибка чтения: " + e.toString());
        }

        if (!jsonString.isEmpty()) {
            List<ReadLaterDbJson> restoredData = new ArrayList<>();
            try {
                restoredData = jsonAdapter.fromJson(jsonString);
            } catch (IOException e) {
                Log.e("Parse exception", "Ошибка разбора файла: " + e.toString());
            }

            if (restoredData.size() > 0) {
                ReadLaterDbUtils.bulkInsertJson(context, restoredData);
            }
        }
    }

}
