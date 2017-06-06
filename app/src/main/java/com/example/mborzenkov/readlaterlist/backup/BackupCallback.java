package com.example.mborzenkov.readlaterlist.backup;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

/** Колбек для оповещений о результатах резервного копирования / восстановления. */
public interface BackupCallback {

    /** Минимальное значение прогресса для onBackupProgressUpdate. */
    int PROGRESS_MIN = 0;
    /** Максимальное значение прогресса для onBackupProgressUpdate. */
    int PROGRESS_MAX = 100;

    /** Возможные варианты работы с резервным копированием. */
    enum BackupMode {
        /** Сохраняет всю базу данных в файлы резервных копий.
         * Формат файлов: JSON.
         * Удаляет все предудыщие бэкапы.
         */
        SAVE,
        /** Заменяет данные в базе данными из файлов с резервными копиями. */
        RESTORE
    }

    /** Возвращает контекст приложения.
     *
     * @return контекст приложения
     */
    @NonNull Context getApplicationContext();

    /** Вызывается в процессе выполнения операции.
     * Операция условно делится на части и для каждой части вызывается onBackupProgressUpdate.
     * Количество вызовов не определено, может быть 0, 1, ..., > 100.
     * Может не вызываться совсем, если длительность операции определена незначительной.
     *
     * @param mode режим, в котором была запущена операция
     * @param progress прогресс в процентах, от PROGRESS_MIN до PROGRESS_MAX
     */
    void onBackupProgressUpdate(@NonNull BackupMode mode,
                                @IntRange(from = PROGRESS_MIN, to = PROGRESS_MAX) int progress);

    /** Вызывается, если операция завершилась с ошибкой.
     * Ошибки будут записаны в Log.e.
     *
     * @param mode режим, в котором была запущена операция
     */
    void onBackupFailed(@NonNull BackupMode mode);

    /** Вызывается, если операция завершилась успешно.
     *
     * @param mode режим, в котором была запущена операция
     */
    void onBackupSuccess(@NonNull BackupMode mode);

}
