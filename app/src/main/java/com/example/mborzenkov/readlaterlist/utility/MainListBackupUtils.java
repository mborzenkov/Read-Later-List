package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
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
    /** Отсечка для показывания оповещений. */
    private static final int NOTIFICATION_FROM_FILES_COUNT = 1;
    /** Максимальное количество процентов в нотификейшене. */
    private static final int NOTIFICATION_PERCENTAGE_MAX = 100;
    /** Число процентов в нотификейшене для чтения файлов. */
    private static final int NOTIFICATION_PERCENTAGE_LOAD_FILES = 20;
    /** Число процентов в нотификейшене для разбора и записи данных. */
    private static final int NOTIFICATION_PERCENTAGE_SAVE_DATA = 80;

    /** Тэг для ошибки закрытия writer или reader. */
    private static final String CLOSE_EXCEPTION = "Close exception";
    /** Тэг для ошибки чтения. */
    private static final String READ_EXCEPTION = "Read exception";
    /** Тэг для ошибки записи. */
    private static final String WRITE_EXCEPTION = "Write exception";
    /** Тэг для ошибки доступа к External Storage. */
    private static final String EXTERNAL_STORAGE_EXCEPTION = "Ex storage exception";
    /** Тэг для ошибки создания папок. */
    private static final String FOLDERS_CREATE_ERROR = "Folders creation fail";
    /** Тэг для ошибки разбора. */
    private static final String PARSE_EXCEPTION = "Parse exception";

    private static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return dir.toString().contains(FOLDER_NAME) && name.matches(FILE_NAME_REGEX);
        }
    };

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
        final String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {

            // Получаем путь к папке
            File backupFolder = getBackupFolder();

            // Создаем папки, если их еще нет
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                Log.e(FOLDERS_CREATE_ERROR, String.format(FORMAT_ERROR, "Не удалось создать папку",
                        backupFolder.toString()));
                return; // Не удалось создать папки
            }

            // Генерируем строку JSON
            final List<String> jsonStrings = generateJsonStrings(context);

            final int jsonSize = jsonStrings.size();
            final boolean showNotification;

            // Оцениваем длительность операций и если файлов > 1, показываем процесс в панели уведомлений.
            if (jsonSize > NOTIFICATION_FROM_FILES_COUNT) {

                showNotification = true;

                // Показываем нотификейшн
                LongTaskNotifications.setupNotification(context,
                        context.getString(R.string.notification_backup_save_title));
                LongTaskNotifications.showNotificationWithProgress(0, false);

            } else {
                showNotification = false;
            }

            // Записываем, предварительно очистив все бэкапы
            removeAllBackups();
            for (int i = 0; i < jsonSize; i++) {
                String json = jsonStrings.get(i);
                writeStringToFile(backupFolder, String.format(FILE_NAME_FORMAT, i), json);
                if (showNotification) {
                    LongTaskNotifications.showNotificationWithProgress(
                            (NOTIFICATION_PERCENTAGE_MAX / jsonSize) * (i + 1), false);
                }
            }

            if (showNotification) {
                // Скрываем нотификешн
                LongTaskNotifications.cancelNotification();
            }

        } else {
            Log.e(EXTERNAL_STORAGE_EXCEPTION, String.format(FORMAT_ERROR,
                    "Ошибка доступа к хранилищу на запись, статус: ", externalStorageState));
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

            List<ReadLaterItem> savedData = new ReadLaterItemDbAdapter().allItemsFromCursor(allData);
            if (!savedData.isEmpty()) {
                result.add(jsonAdapter.toJson(savedData));
            }

            allData.close();

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
            Log.e("File remove fail", String.format(FORMAT_ERROR, "Не удалось перезаписать файл",
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
    }

    /** Возвращает путь к папке с бэкапами. */
    private static File getBackupFolder() {
        final String root = Environment.getExternalStorageDirectory().toString();
        return new File(root + FOLDER_NAME);
    }

    /** Удаляет все файлы бэкапов из папки. */
    private static void removeAllBackups() {
        File[] backupFiles = getBackupFolder().listFiles(FILENAME_FILTER);
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

    /** Восстанавливает всю базу данных из файла в формате Json.
     *
     * @param context Контекст
     */
    public static void restoreEverythingFromJsonFile(Context context) {

        // Проверяем доступность хранилища
        String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState)) {

            // Получаем путь к папке
            File backupFolder = getBackupFolder();
            if (!backupFolder.exists()) {
                Log.e("Folder not exist", String.format(FORMAT_ERROR, "Не удалось найти папку с бэкапами",
                        backupFolder.toString()));
                return; // Нет папки? :(
            }

            // Получаем список файлов
            File[] backupFiles = backupFolder.listFiles(FILENAME_FILTER);

            if (backupFiles == null) {
                return; // Нет файлов
            }

            // Оцениваем длительность операций и если файлов > 1, показываем процесс в панели уведомлений.
            final boolean showNotification;
            if (backupFiles.length > NOTIFICATION_FROM_FILES_COUNT) {

                showNotification = true;

                // Показываем нотификейшн
                LongTaskNotifications.setupNotification(context,
                        context.getString(R.string.notification_backup_restore_title));
                LongTaskNotifications.showNotificationWithProgress(0, false);

            } else {
                showNotification = false;
            }

            // Читаем файлы
            List<String> backupData = readBackupFiles(backupFiles, showNotification);

            // Записываем данные в базу
            loadJsonData(context, backupData, showNotification);

            if (showNotification) {
                LongTaskNotifications.cancelNotification();
            }


        } else {
            Log.e(EXTERNAL_STORAGE_EXCEPTION, String.format("%s %s", "Ошибка доступа к хранилищу на чтение, статус: ",
                    externalStorageState));
        }

    }

    /** Читает содержимое всех файлов в список строк.
     * Каждый элемент соответствует отдельному файлу.
     *
     * @param backupFiles список файлов
     * @return список содержимого файлов
     */
    private static List<String> readBackupFiles(File[] backupFiles, boolean showNotification) {

        final List<String> result = new ArrayList<>();

        // Читаем файлы
        for (int i = 0, totalFiles = backupFiles.length; i < totalFiles; i++) {

            File file = backupFiles[i];

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
                Log.e(READ_EXCEPTION, String.format(FORMAT_ERROR, "Файл не найден: ", e.toString()));
            } catch (IOException e) {
                Log.e(READ_EXCEPTION, String.format(FORMAT_ERROR, "Ошибка чтения JSON: ", e.toString()));
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        Log.e(CLOSE_EXCEPTION, String.format(FORMAT_ERROR, "Не удалось закрыть inStream: ",
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
            }

            if (showNotification) {
                // Все чтение файлов как 20%
                LongTaskNotifications.showNotificationWithProgress(
                        ((NOTIFICATION_PERCENTAGE_LOAD_FILES / totalFiles) * (i + 1)), false);
            }
        }

        return result;

    }

    /** Читает данные из строк в JSON и записывает их в базу.
     *
     * @param context контекст
     * @param jsonStrings строки в формате JSON
     */
    private static void loadJsonData(Context context, List<String> jsonStrings, boolean showNotification) {

        if (!jsonStrings.isEmpty()) {

            ReadLaterDbUtils.deleteAll(context);

            // Создаем адаптер
            Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
            JsonAdapter<List<ReadLaterItem>> jsonAdapter =
                    moshi.adapter(Types.newParameterizedType(List.class, ReadLaterItem.class));

            for (int i = 0, totalStrings = jsonStrings.size(); i < totalStrings; i++) {

                String json = jsonStrings.get(i);

                List<ReadLaterItem> restoredData = new ArrayList<>();
                try {
                    restoredData = jsonAdapter.fromJson(json);
                } catch (IOException e) {
                    Log.e(PARSE_EXCEPTION, String.format(FORMAT_ERROR, "Ошибка разбора файла: ", e.toString()));
                }

                // restoredData может содержать null при ошибках разбора, нужно их исключить
                if (restoredData.contains(null)) {
                    //noinspection SuspiciousMethodCalls
                    restoredData.removeAll(Collections.singleton(null));
                }

                if (restoredData.size() > 0) {
                    ReadLaterDbUtils.bulkInsertItems(context, restoredData);
                }

                if (showNotification) {
                    // Все чтение строк как 80% начиная с 20%
                    LongTaskNotifications.showNotificationWithProgress(NOTIFICATION_PERCENTAGE_LOAD_FILES
                            + ((NOTIFICATION_PERCENTAGE_SAVE_DATA / totalStrings) * (i + 1)), false);
                }
            }
        }
    }

}
