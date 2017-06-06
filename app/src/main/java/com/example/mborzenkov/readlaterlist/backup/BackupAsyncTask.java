package com.example.mborzenkov.readlaterlist.backup;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.List;

/** {@link AsyncTask} для фоновой работы с резервными копиями.
 */
class BackupAsyncTask extends AsyncTask<Void, Void, Boolean> {

    /** Тэг для ошибки доступа к External Storage. */
    private static final String EXTERNAL_STORAGE_EXCEPTION = "Ex storage exception";
    /** Тэг для ошибки создания папок. */
    private static final String FOLDERS_CREATE_ERROR = "Folders creation fail";
    /** Имя файла. */
    private static final String FILE_NAME_FORMAT = "itemlist_%s.ili";
    /** Формат ошибки. */
    private static final String FORMAT_ERROR = "%s %s";

    /** Отсечка для оповещения onProgressUpdate, количество файлов от. */
    private static final int NOTIFY_FROM_FILES_COUNT = 1;
    /** Число процентов для оповещения о генерации строк. */
    private static final int NOTIFY_PERCENTAGE_READ_DB = 20;
    /** Число процентов для оповещения о сохранении файлов. */
    private static final int NOTIFY_PERCENTAGE_WRITE_FILES = 80;

    /** Максимальное количество объектов в одном файле. */
    private static final int FILE_MAX_SIZE = 10000;


    /** Режим, в котором запущен AsyncTask. */
    private final @NonNull BackupCallback.BackupMode mBackupMode;
    /** Callback для оповещений о ходе выполнения операции. */
    private @Nullable BackupCallback mBackupCallback;

    /** Создает новый AsyncTask.
     *
     * @param mode режим работы
     * @param callback интерфейс для оповещения о ходе выполнения операции
     */
    BackupAsyncTask(@NonNull BackupCallback.BackupMode mode, @Nullable BackupCallback callback) {
        mBackupMode = mode;
        mBackupCallback = callback;
    }

    /** Устанавливает колбек для этого таска.
     *
     * @param callback новый колбек, может быть null, если нужно отписаться
     */
    void setCallback(@Nullable BackupCallback callback) {
        mBackupCallback = callback;
    }

    @Override
    protected @NonNull Boolean doInBackground(Void... params) {

        if (isCancelled() || (mBackupCallback == null)) {
            return Boolean.FALSE;
        }

        // Запоминаем контекст приложения (для бд)
        final Context appContext = mBackupCallback.getApplicationContext();

        switch (mBackupMode) {
            case SAVE:
                return saveEverythingAsJsonFile(appContext);
            case RESTORE:
                return restoreEverythingFromJsonFile(appContext);
            default:
                return Boolean.FALSE;
        }

    }

    @Override
    protected void onPostExecute(@Nullable Boolean backupSuccess) {
        if (mBackupCallback != null) {
            if (backupSuccess == Boolean.TRUE) {
                mBackupCallback.onBackupSuccess(mBackupMode);
            } else {
                mBackupCallback.onBackupFailed(mBackupMode);
            }
        }
    }


    /** Сохраняет всю базу данных в файл в формате Json.
     * Удаляет все предудыщие бэкапы.
     *
     * @param context Контекст
     *
     * @return результат выполнения
     */
    private @NonNull Boolean saveEverythingAsJsonFile(Context context) {

        // Проверяем доступность хранилища
        final String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {

            // Получаем путь к папке
            File backupFolder = BackupUtils.getBackupFolder();

            // Создаем папки, если их еще нет
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                Log.e(FOLDERS_CREATE_ERROR, String.format(FORMAT_ERROR, "Не удалось создать папку",
                        backupFolder.toString()));
                return Boolean.FALSE; // Не удалось создать папки
            }

            // Генерируем строку JSON
            final List<String> jsonStrings = BackupUtils.generateJsonStrings(context, FILE_MAX_SIZE);
            final int jsonSize = jsonStrings.size();

            // Оцениваем длительность операций и если файлов > 1, оповещаем.
            if ((jsonSize > NOTIFY_FROM_FILES_COUNT) && (mBackupCallback != null)) {
                mBackupCallback.onBackupProgressUpdate(mBackupMode, NOTIFY_PERCENTAGE_READ_DB);
            }

            // Записываем, предварительно очистив все бэкапы
            BackupUtils.removeAllBackups(backupFolder);
            for (int i = 0; i < jsonSize; i++) {
                String json = jsonStrings.get(i);
                BackupUtils.writeStringToFile(backupFolder, String.format(FILE_NAME_FORMAT, i), json);
                if (mBackupCallback != null) {
                    mBackupCallback.onBackupProgressUpdate(mBackupMode,
                            // Процентов после чтения + ( всего процентов для записи * доля записанных от всех )
                            NOTIFY_PERCENTAGE_READ_DB + ((NOTIFY_PERCENTAGE_WRITE_FILES / jsonSize) * (i + 1)));
                }
            }

            return Boolean.TRUE;

        } else {
            Log.e(EXTERNAL_STORAGE_EXCEPTION, String.format(FORMAT_ERROR,
                    "Ошибка доступа к хранилищу на запись, статус: ", externalStorageState));
        }

        return Boolean.FALSE;

    }

    /** Восстанавливает всю базу данных из файла в формате Json.
     *
     * @param context Контекст
     *
     * @return результат выполнения
     */
    private @NonNull Boolean restoreEverythingFromJsonFile(Context context) {

        // Проверяем доступность хранилища
        String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState)) {

            // Получаем путь к папке
            File backupFolder = BackupUtils.getBackupFolder();
            if (!backupFolder.exists()) {
                Log.e("Folder not exist", String.format(FORMAT_ERROR, "Не удалось найти папку с бэкапами",
                        backupFolder.toString()));
                return Boolean.FALSE; // Нет папки? :(
            }

            // Получаем список файлов
            File[] backupFiles = BackupUtils.listBackupFiles(backupFolder);
            if ((backupFiles == null) || (backupFiles.length == 0)) {
                return Boolean.FALSE; // Нет файлов
            }

            for (int i = 0, size = backupFiles.length; i < size; i++) {

                File file = backupFiles[i];

                String dataFromFile = BackupUtils.readFileToString(file);

                if (dataFromFile == null) {
                    return Boolean.FALSE; // не удалось прочитать файл
                }

                if (!BackupUtils.saveDataFromJson(context, dataFromFile)) {
                    return Boolean.FALSE; // не удалось разобрать файл в объекты и записать в базу
                }

                if (mBackupCallback != null) {
                    mBackupCallback.onBackupProgressUpdate(mBackupMode,
                            // Всего процетов * доля обработанных файлов от всех
                            ((BackupCallback.PROGRESS_MAX / size) * (i + 1)));
                }

            }

            return Boolean.TRUE;

        } else {
            Log.e(EXTERNAL_STORAGE_EXCEPTION, String.format("%s %s", "Ошибка доступа к хранилищу на чтение, статус: ",
                    externalStorageState));
        }

        return Boolean.FALSE;

    }

}
