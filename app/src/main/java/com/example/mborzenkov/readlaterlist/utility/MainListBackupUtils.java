package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

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

/** Сервисный static util класс для работы с бэкапами базы данных в формате Json. */
public class MainListBackupUtils {

    /** Имя файла. */
    private static final String FILE_NAME = "itemlist.ili";
    /** Формат ошибки. */
    private static final String FORMAT_ERROR = "%s %s";

    private MainListBackupUtils() {
        throw new UnsupportedOperationException("Класс MainListBackupUtils - static util, не может иметь экземпляров");
    }

    /** Сохраняет всю базу данных в файл в формате Json.
     *
     * @param context Контекст
     */
    public static void saveEverythingAsJsonFile(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<List<ReadLaterDbJson>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterDbJson.class));
        Cursor allData = context.getContentResolver()
                .query(ReadLaterContract.ReadLaterEntry.CONTENT_URI, null, null, null, null);
        if (allData != null) {
            List<ReadLaterDbJson> savedData = new ArrayList<>();
            for (int i = 0; i < allData.getCount(); i++) {
                allData.moveToPosition(i);
                savedData.add(ReadLaterDbJson.fromCursor(allData));
            }
            try {
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
                outputStreamWriter.write(jsonAdapter.toJson(savedData));
                outputStreamWriter.close();
            } catch (IOException e) {
                Log.e("Exception", String.format(FORMAT_ERROR, "Ошибка записи: ", e.toString()));
            }
        }
    }

    /** Восстанавливает всю базу данных из файла в формате Json.
     *
     * @param context Контекст
     */
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
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                jsonString = stringBuilder.toString().trim();
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("Read exception", String.format(FORMAT_ERROR, "Файл не найден: ", e.toString()));
        } catch (IOException e) {
            Log.e("Read exception", String.format(FORMAT_ERROR, "Ошибка чтения: ", e.toString()));
        }

        if (!jsonString.isEmpty()) {
            List<ReadLaterDbJson> restoredData = new ArrayList<>();
            try {
                restoredData = jsonAdapter.fromJson(jsonString);
            } catch (IOException e) {
                Log.e("Parse exception", String.format(FORMAT_ERROR, "Ошибка разбора файла: ", e.toString()));
            }

            if (restoredData.size() > 0) {
                ReadLaterDbUtils.bulkInsertJson(context, restoredData);
            }
        }
    }

}
