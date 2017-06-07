package com.example.mborzenkov.readlaterlist.backup;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Сервисный static util класс для работы с бэкапами базы данных в формате Json. */
class BackupUtils {

    /** Кодировка файлов с бэкапами. */
    private static final String ENCODING = "UTF8";
    /** Формат ошибки. */
    private static final String FORMAT_ERROR = "%s %s";

    /** Тэг для ошибки закрытия writer или reader. */
    private static final String CLOSE_EXCEPTION = "Close exception";
    /** Тэг для ошибки чтения. */
    private static final String READ_EXCEPTION = "Read exception";
    /** Тэг для ошибки записи. */
    private static final String WRITE_EXCEPTION = "Write exception";
    /** Тэг для ошибки разбора. */
    private static final String PARSE_EXCEPTION = "Parse exception";

    /** Имя папки. */
    private static final String FOLDER_NAME = "/ReadLaterItem/Backups";
    /** Регулярное выражение, описывающее имена файлов. */
    private static final String FILE_NAME_REGEX = "itemlist_\\d{1,2}\\.ili";
    /** Фильтр для поиска файлов бэкапов в папке. */
    private static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return dir.toString().contains(FOLDER_NAME) && name.matches(FILE_NAME_REGEX);
        }
    };

    private BackupUtils() {
        throw new UnsupportedOperationException("Класс BackupUtils - static util, не может иметь экземпляров");
    }

    /** Возвращает путь к папке с бэкапами. */
    static File getBackupFolder() {
        final String root = Environment.getExternalStorageDirectory().toString();
        return new File(root + FOLDER_NAME);
    }

    /** Получает список файлов бэкапов в папке. */
    static @Nullable File[] listBackupFiles(@NonNull File folder) {
        return folder.listFiles(FILENAME_FILTER);
    }

    /** Удаляет все файлы бэкапов из папки. */
    static void removeAllBackups(@NonNull File folder) {
        File[] backupFiles = listBackupFiles(folder);
        if (backupFiles == null) {
            return;
        }
        for (File file : backupFiles) {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /** Записывает содержимое в файл.
     * Перезаписывает файл, если он уже существует.
     *
     * @param folder папка, должна быть уже создана
     * @param fileName имя файла
     * @param content содержимое в формате строки
     *
     * @return успешность выполнения записи
     */
    static boolean writeStringToFile(File folder, String fileName, String content) {

        boolean result = false;

        File backupFile = new File(folder, fileName);
        if (backupFile.exists() && !backupFile.delete()) {
            Log.e("File remove fail", String.format(FORMAT_ERROR, "Не удалось перезаписать файл",
                    backupFile.toString()));
            return false; // Не удалось удалить существующий файл
        }

        FileOutputStream outStream = null;
        OutputStreamWriter outWriter = null;
        try {
            outStream = new FileOutputStream(backupFile);
            outWriter = new OutputStreamWriter(outStream, ENCODING);
            outWriter.write(content);
            outWriter.close();
            outStream.flush();
            outStream.close();
            result = true;
        } catch (IOException e) {
            Log.e(WRITE_EXCEPTION, String.format(FORMAT_ERROR, "Ошибка записи: ", e.toString()));
        } finally {
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e) {
                    Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть outWriter: ",
                            e.toString()));
                }
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть outStream: ",
                            e.toString()));
                }
            }
        }

        return result;

    }

    /** Читает содержимое файла в строку.
     *
     * @param file файл
     *
     * @return содержимое файла
     */
    static @Nullable String readFileToString(@NonNull File file) {

        String result = null;

        FileInputStream inStream = null;
        InputStreamReader inReader = null;
        BufferedReader bufReader = null;
        try {
            inStream = new FileInputStream(file);
            inReader = new InputStreamReader(inStream, ENCODING);
            bufReader = new BufferedReader(inReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();
            while ((receiveString = bufReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }
            inReader.close();
            inStream.close();
            result = stringBuilder.toString().trim();
        } catch (FileNotFoundException e) {
            Log.e(READ_EXCEPTION, String.format(FORMAT_ERROR, "Файл не найден: ", e.toString()));
        } catch (IOException e) {
            Log.e(READ_EXCEPTION, String.format(FORMAT_ERROR, "Ошибка чтения JSON: ", e.toString()));
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть bufReader: ",
                            e.toString()));
                }
            }
            if (inReader != null) {
                try {
                    inReader.close();
                } catch (IOException e) {
                    Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть inReader: ",
                            e.toString()));
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть inStream: ",
                            e.toString()));
                }
            }
        }

        return result;

    }

    /** Получает все данные из базы и возвращает их в формате строк JSON, готовых к записи в отдельные файлы.
     * В одной строке JSON будет не более splitCount объектов
     *
     * @param context контекст
     * @param splitCount требуемое количество объектов в одном файле
     *
     * @return список строк в формате JSON
     */
    static List<String> generateJsonStrings(Context context, int splitCount) {

        final List<String> result = new ArrayList<>();

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));

        int currentPosition = 0;
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        for (Cursor allData = ReadLaterDbUtils.queryRange(context, currentPosition, splitCount);
             allData != null && allData.getCount() > 0;
             currentPosition += splitCount,
                     allData = ReadLaterDbUtils.queryRange(context, currentPosition, splitCount)) {

            List<ReadLaterItem> savedData = dbAdapter.allItemsFromCursor(allData);
            if (!savedData.isEmpty()) {
                result.add(jsonAdapter.toJson(savedData));
            }

            allData.close();

        }

        return result;

    }

    /** Читает данные из строки в JSON и записывает данные в базу.
     *
     * @param context контекст
     * @param jsonString строка в формате JSON
     *
     * @return результат выполнения
     */
    static boolean saveDataFromJson(@NonNull Context context, @NonNull String jsonString) {

        if (!jsonString.isEmpty()) {

            ReadLaterDbUtils.deleteAll(context);

            // Создаем адаптер
            Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
            JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                    moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));

            List<ReadLaterItem> restoredData;
            try {
                restoredData = jsonAdapter.fromJson(jsonString);
            } catch (IOException e) {
                Log.e(PARSE_EXCEPTION, String.format(FORMAT_ERROR, "Ошибка разбора файла: ", e.toString()));
                return false;
            }

            // restoredData может содержать null при ошибках разбора, нужно их исключить
            if (restoredData.contains(null)) {
                //noinspection SuspiciousMethodCalls
                restoredData.removeAll(Collections.singleton(null));
            }

            if (restoredData.size() > 0) {
                ReadLaterDbUtils.bulkInsertItems(context, restoredData);
            }

        }

        return true;

    }

}
