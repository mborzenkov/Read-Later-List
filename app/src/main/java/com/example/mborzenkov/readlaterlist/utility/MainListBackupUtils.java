package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Сервисный static util класс для работы с бэкапами базы данных в формате Json. */
public class MainListBackupUtils {

    /** Имя папки. */
    private static final String FOLDER_NAME = "/ReadLaterItem/Backups";
    /** Имя файла. */
    private static final String FILE_NAME = "itemlist.ili";
    /** Формат ошибки. */
    private static final String FORMAT_ERROR = "%s %s";
    /** Максимальное количество объектов в одном файле. */
    private static final int FILE_MAX_SIZE = 10000;

    
    private MainListBackupUtils() {
        throw new UnsupportedOperationException("Класс MainListBackupUtils - static util, не может иметь экземпляров");
    }

    /** Сохраняет всю базу данных в файл в формате Json.
     *
     * @param context Контекст
     */
    public static void saveEverythingAsJsonFile(Context context) {

        // Проверяем доступность хранилища
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

            // Получаем путь к папке
            final String root = Environment.getExternalStorageDirectory().toString();
            File backupFolder = new File(root + FOLDER_NAME);

            // Создаем папки, если их еще нет
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                return; // Не удалось создать папки
            }

            // Генерируем строку JSON
            final String jsonString = generateJson(context);
            Log.i("TO_JSON", "JSON: " + jsonString);

            // Записываем
            if (!jsonString.isEmpty()) {
                writeStringToFile(backupFolder, FILE_NAME, jsonString);
            }

        } else {
            Log.e("EX storage exception", String.format(FORMAT_ERROR, "Ошибка доступа к хранилищу на запись, статус: ",
                    externalStorageState));
        }
    }

    // TODO: JDoc
    private static String generateJson(Context context) {

        String result = "";

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));
        Cursor allData = context.getContentResolver()
                .query(ReadLaterContract.ReadLaterEntry.CONTENT_URI, null, null, null, null);
        if (allData != null) {
            ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
            List<ReadLaterItem> savedData = dbAdapter.allItemsFromCursor(allData);
            if (!savedData.isEmpty()) {
                result = jsonAdapter.toJson(savedData);
            }
        }

        return result;

    }

    // TODO: JDoc
    private static void writeStringToFile(File folder, String fileName, String content) {
        File backupFile = new File(folder, FILE_NAME);
        if (backupFile.exists() && !backupFile.delete()) {
            return; // Не удалось удалить существующий файл
        }

        FileOutputStream outStream = null;
        OutputStreamWriter outWriter = null;
        try {
            outStream = new FileOutputStream(backupFile);
            outWriter = new OutputStreamWriter(outStream);
            outWriter.write(content);
            outWriter.close();
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            Log.e("Write exception", String.format(FORMAT_ERROR, "Ошибка записи: ", e.toString()));
        } finally {
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e) {
                    Log.e("Close exception", String.format(FORMAT_ERROR, "Не удалось закрыть outWriter: ",
                            e.toString()));
                }
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    Log.e("Close exception", String.format(FORMAT_ERROR, "Не удалось закрыть outStream: ",
                            e.toString()));
                }
            }
        }
    }

    /** Восстанавливает всю базу данных из файла в формате Json.
     *
     * @param context Контекст
     */
    public static void restoreEverythingFromJsonFile(Context context) {
        // Проверяем доступность хранилища
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)
                || externalStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {

            Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
            JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                    moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));
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
            } catch (FileNotFoundException e) {
                Log.e("Read exception", String.format(FORMAT_ERROR, "Файл не найден: ", e.toString()));
            } catch (IOException e) {
                Log.e("Read exception", String.format(FORMAT_ERROR, "Ошибка чтения JSON: ", e.toString()));
            }

            Log.d("FROM_JSON", jsonString);

            if (!jsonString.isEmpty()) {
                List<ReadLaterItem> restoredData = new ArrayList<>();
                try {
                    restoredData = jsonAdapter.fromJson(jsonString);
                } catch (IOException e) {
                    Log.e("Parse exception", String.format(FORMAT_ERROR, "Ошибка разбора файла: ", e.toString()));
                }

                // restoredData может содержать null при ошибках разбора, нужно их исключить
                if (restoredData.contains(null)) {
                    //noinspection SuspiciousMethodCalls
                    restoredData.removeAll(Collections.singleton(null));
                }

                if (restoredData.size() > 0) {
                    ReadLaterDbUtils.deleteAll(context);
                    ReadLaterDbUtils.bulkInsertItems(context, restoredData);
                }
            }

        } else {
            Log.e("EX storage exception", String.format("%s %s", "Ошибка доступа к хранилищу на чтение, статус: ",
                    externalStorageState));
        }
    }

}
