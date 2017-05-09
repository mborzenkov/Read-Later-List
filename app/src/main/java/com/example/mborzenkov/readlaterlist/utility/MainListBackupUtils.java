package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Сервисный static util класс для работы с бэкапами базы данных в формате Json. */
public class MainListBackupUtils {

    /** Имя папки. */
    private static final String FOLDER_NAME = "/ReadLaterItem/Backups";
    /** Имя файла. */
    private static final String FILE_NAME_FORMAT = "itemlist_%s.ili";
    /** Регулярное выражение, описывающее имена файлов. */
    private static final String FILE_NAME_REGEX = "itemlist_\\d{1,2}\\.ili";
    /** Формат ошибки. */
    private static final String FORMAT_ERROR = "%s %s";
    /** Максимальное количество объектов в одном файле. */
    private static final int FILE_MAX_SIZE = 10000;

    private static final FilenameFilter FILENAME_FILTER = (dir, name) ->
            dir.toString().contains(FOLDER_NAME) && name.matches(FILE_NAME_REGEX);

    private MainListBackupUtils() {
        throw new UnsupportedOperationException("Класс MainListBackupUtils - static util, не может иметь экземпляров");
    }

    /** Сохраняет всю базу данных в файл в формате Json.
     * Удаляет все предудыщие бэкапы.
     *
     * @param context Контекст
     */
    public static void saveEverythingAsJsonFile(Context context) {

        // Проверяем доступность хранилища
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

            // Получаем путь к папке
            File backupFolder = getBackupFolder();

            // Создаем папки, если их еще нет
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                Log.d("Folders creation fail", String.format(FORMAT_ERROR, "Не удалось создать папку",
                        backupFolder.toString()));
                return; // Не удалось создать папки
            }

            // Показываем нотификейшн
            LongTaskNotifications.setupNotification(context,
                    context.getString(R.string.notification_backup_save_title));
            LongTaskNotifications.showNotificationWithProgress(0, false);

            // Сколько данных в базе пока не известно, поэтому покаем бесконечный лоадер
            LongTaskNotifications.showNotificationWithProgress(0, true);

            // Генерируем строку JSON
            final List<String> jsonStrings = generateJsonStrings(context);

            // Теперь покажем сразу 20%, примерно столько работы сделано
            LongTaskNotifications.showNotificationWithProgress(20, false);

            // Записываем, предварительно очистив все бэкапы
            removeAllBackups();
            for (int i = 0, size = jsonStrings.size(); i < size; i++) {
                String json = jsonStrings.get(i);
                writeStringToFile(backupFolder, String.format(FILE_NAME_FORMAT, i), json);
                LongTaskNotifications.showNotificationWithProgress(20 + (80 / (size - i)), false);
            }

            // Скрываем нотификешн
            LongTaskNotifications.cancelNotification();

        } else {
            Log.e("EX storage exception", String.format(FORMAT_ERROR, "Ошибка доступа к хранилищу на запись, статус: ",
                    externalStorageState));
        }
    }

    /** Получает все данные из базы и возвращает их в формате строк JSON, готовых к записи в отдельные файлы.
     *
     * @param context контекст
     * @return список строк в формате JSON
     */
    private static List<String> generateJsonStrings(Context context) {

        final List<String> result = new ArrayList<>();

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));

        int currentPosition = 0;
        for (Cursor allData = ReadLaterDbUtils.queryRange(context, currentPosition, FILE_MAX_SIZE);
             allData != null && allData.getCount() > 0;
             currentPosition += FILE_MAX_SIZE,
                     allData = ReadLaterDbUtils.queryRange(context, currentPosition, FILE_MAX_SIZE)) {

            ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
            List<ReadLaterItem> savedData = dbAdapter.allItemsFromCursor(allData);
            if (!savedData.isEmpty()) {
                result.add(jsonAdapter.toJson(savedData));
            }

        }

        return result;

    }

    /** Записывает содержимое в файл.
     * Перезаписывает файл, если он уже существует.
     *
     * @param folder папка, должна быть уже создана
     * @param fileName имя файла
     * @param content содержимое в формате строки
     */
    private static void writeStringToFile(File folder, String fileName, String content) {
        File backupFile = new File(folder, fileName);
        if (backupFile.exists() && !backupFile.delete()) {
            Log.d("File remove fail", String.format(FORMAT_ERROR, "Не удалось перезаписать файл",
                    backupFile.toString()));
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

    /** Возвращает путь к папке с бэкапами. */
    private static File getBackupFolder() {
        final String root = Environment.getExternalStorageDirectory().toString();
        return new File(root + FOLDER_NAME);
    }

    /** Удаляет все файлы бэкапов из папки. */
    private static void removeAllBackups() {
        File[] backupFiles = getBackupFolder().listFiles(FILENAME_FILTER);
        for (File file : backupFiles) {
            if (file.exists()) {
                file.delete();
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

            // Получаем путь к папке
            File backupFolder = getBackupFolder();
            if (!backupFolder.exists()) {
                Log.d("Folder not exist", String.format(FORMAT_ERROR, "Не удалось найти папку с бэкапами",
                        backupFolder.toString()));
                return; // Нет папки? :(
            }

            // Показываем нотификешн
            LongTaskNotifications.
                    setupNotification(context, context.getString(R.string.notification_backup_restore_title));
            LongTaskNotifications.showNotificationWithProgress(0, false);

            // Получаем список файлов
            File[] backupFiles = backupFolder.listFiles(FILENAME_FILTER);

            // Записываем данные в базу
            loadJsonData(context, readBackupFiles(backupFiles));

            // Скрываем нотификешн
            LongTaskNotifications.cancelNotification();

        } else {
            Log.e("EX storage exception", String.format("%s %s", "Ошибка доступа к хранилищу на чтение, статус: ",
                    externalStorageState));
        }

    }

    /** Читает содержимое всех файлов в список строк.
     * Каждый элемент соответствует отдельному файлу.
     *
     * @param backupFiles список файлов
     * @return список содержимого файлов
     */
    private static List<String> readBackupFiles(File[] backupFiles) {

        final List<String> result = new ArrayList<>();
        final int totalFiles = backupFiles.length;
        int currentFile = 0;

        // Читаем файлы
        for (File file : backupFiles) {
            String jsonString = "";
            FileInputStream inStream = null;
            InputStreamReader inReader = null;
            try {
                inStream = new FileInputStream(file);
                inReader = new InputStreamReader(inStream);
                BufferedReader bufferedReader = new BufferedReader(inReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inReader.close();
                inStream.close();
                result.add(stringBuilder.toString().trim());
            } catch (FileNotFoundException e) {
                Log.e("Read exception", String.format(FORMAT_ERROR, "Файл не найден: ", e.toString()));
            } catch (IOException e) {
                Log.e("Read exception", String.format(FORMAT_ERROR, "Ошибка чтения JSON: ", e.toString()));
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        Log.e("Close exception", String.format(FORMAT_ERROR, "Не удалось закрыть inStream: ",
                                e.toString()));
                    }
                }
                if (inReader != null) {
                    try {
                        inReader.close();
                    } catch (IOException e) {
                        Log.e("Close exception", String.format(FORMAT_ERROR, "Не удалось закрыть inReader: ",
                                e.toString()));
                    }
                }
            }

            // Все чтение файлов как 50%
            LongTaskNotifications.showNotificationWithProgress(50 / (totalFiles - currentFile), false);
        }

        return result;

    }

    /** Читает данные из строк в JSON и записывает их в базу.
     *
     * @param context контекст
     * @param jsonStrings строки в формате JSON
     */
    private static void loadJsonData(Context context, List<String> jsonStrings) {

        if (!jsonStrings.isEmpty()) {

            ReadLaterDbUtils.deleteAll(context);

            // Создаем адаптер
            Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
            JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                    moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));

            final int totalStrings = jsonStrings.size();
            int currentString = 0;

            for (String json : jsonStrings) {

                List<ReadLaterItem> restoredData = new ArrayList<>();
                try {
                    restoredData = jsonAdapter.fromJson(json);
                } catch (IOException e) {
                    Log.e("Parse exception", String.format(FORMAT_ERROR, "Ошибка разбора файла: ", e.toString()));
                }

                // restoredData может содержать null при ошибках разбора, нужно их исключить
                if (restoredData.contains(null)) {
                    //noinspection SuspiciousMethodCalls
                    restoredData.removeAll(Collections.singleton(null));
                }

                if (restoredData.size() > 0) {
                    ReadLaterDbUtils.bulkInsertItems(context, restoredData);
                }

                // Все чтение строк как 50% начиная с 50%
                LongTaskNotifications.showNotificationWithProgress(50 + (50 / (totalStrings - currentString)), false);
            }
        }
    }

}
