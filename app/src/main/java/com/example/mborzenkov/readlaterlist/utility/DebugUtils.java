package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DebugUtils {

    /** Количество строк в description для автоматического создания. */
    private static final int DESCRIPTION_LINES = 3;
    /** Добавляется за раз. */
    private static final int BULK_INSERT_MAX = 5000;

    private DebugUtils() {
        throw new UnsupportedOperationException("Класс DebugUtils - static util, не может иметь экземпляров");
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет number штук записей с заранее определенными
     *  Label, случайными description и случайными цветами.
     *
     * @param context контекст
     * @param number число добавляемых плейсхолдеров
     */
    public static void addPlaceholdersToDatabase(Context context, int number) {

        // Оцениваем длительность операций и если вставок будет > 1, показываем процесс в панели уведомлений.
        final boolean showNotification;
        if (number > BULK_INSERT_MAX) {

            showNotification = true;
            LongTaskNotifications.setupNotification(context,
                    context.getString(R.string.notification_debug_fillplaceholders_title));
            LongTaskNotifications.showNotificationWithProgress(0, false);

        } else {
            showNotification = false;
        }

        final long currentTime = System.currentTimeMillis();
        final String[] text = context.getString(R.string.debug_large_text).split("\n");
        final int[] predefinedColors = context.getResources().getIntArray(R.array.full_gradient);
        final int textRows = text.length;
        final int numberOfColors = predefinedColors.length;
        final String label = context.getString(R.string.mainlist_menu_add_placeholders_label);
        final Random randomizer = new Random();

        // Вставляем number строк
        for (int inserted = 0; inserted < number; ) {
            List<ReadLaterItem> listItems = new ArrayList<>();
            // По BULK_INSERT_MAX
            for (int i = 0, step = Math.min(BULK_INSERT_MAX, number - inserted); i < step; i++, inserted++) {
                // Description создается из случайных строк large_text
                StringBuilder description = new StringBuilder();
                for (int j = 0; j < DESCRIPTION_LINES; j++) {
                    description.append(text[randomizer.nextInt(textRows)]).append('\n');
                }

                // Конвертация Color.colorToHSV и Color.HSVToColor в обоих случаях выдает погрешности.
                // Каждая новая конвертация может дать новый результат. Поэтому решено использовать предопределенные
                // цвета.

                listItems.add(new ReadLaterItem(
                        label + " " + inserted,
                        description.toString().trim(),
                        predefinedColors[randomizer.nextInt(numberOfColors)],
                        currentTime,
                        currentTime,
                        currentTime,
                        null));
            }
            ReadLaterDbUtils.bulkInsertItems(context, listItems);

            if (showNotification) {
                // Обновляем нотификешн
                LongTaskNotifications.showNotificationWithProgress((100 * inserted) / number, false);
            }
        }

        if (showNotification) {
            // Сворачиваемся
            LongTaskNotifications.cancelNotification();
        }
    }

}
