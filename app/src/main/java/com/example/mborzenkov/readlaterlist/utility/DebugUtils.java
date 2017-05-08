package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.MainListActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DebugUtils {

    /** Количество плейсхолдеров, которое создается. */
    private static final int PLACEHOLDERS_COUNT = 100;
    /** Количество строк в description для автоматического создания. */
    private static final int DESCRIPTION_LINES = 3;

    private DebugUtils() {
        throw new UnsupportedOperationException("Класс DebugUtils - static util, не может иметь экземпляров");
    }

    /** Показывает предупреждение и выполняет добавление данных при подтверждении.
     *
     * @param context Контекст
     * @param activity Активити для обновления данных
     */
    public static void showAlertAndAddPlaceholders(final Context context, final MainListActivity activity) {
        ActivityUtils.showAlertDialog(
            context,
            context.getString(R.string.mainlist_menu_add_placeholders_question_title),
            context.getString(R.string.mainlist_menu_add_placeholders_question_text),
            () -> {
                addPlaceholdersToDatabase(context);
                activity.getSupportLoaderManager().restartLoader(MainListActivity.ITEM_LOADER_ID, null,
                        activity);
            },
            null);
    }

    /** Показывает предупреждение и выполняет удаление данных при подтверждении.
     *
     * @param context Контекст
     * @param activity Активити для обновления данных
     */
    public static void showAlertAndDeleteItems(final Context context, final MainListActivity activity) {
        ActivityUtils.showAlertDialog(
            context,
            context.getString(R.string.mainlist_menu_delete_all_question_title),
            context.getString(R.string.mainlist_menu_delete_all_question_text),
            () -> {
                ReadLaterDbUtils.deleteAll(context);
                activity.getSupportLoaderManager().restartLoader(MainListActivity.ITEM_LOADER_ID, null,
                        activity);
            },
            null);
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет PLACEHOLDERS_COUNT штук записей с заранее определенными
     *  Label, случайными description и случайными цветами
     *
     * @param context Контекст
     */
    private static void addPlaceholdersToDatabase(Context context) {

        long currentTime = System.currentTimeMillis();
        String[] text = context.getString(R.string.debug_large_text).split("\n");
        int textRows = text.length;
        String label = context.getString(R.string.mainlist_menu_add_placeholders_label);
        Random randomizer = new Random();
        List<ReadLaterItem> listItems = new ArrayList<>();
        for (int i = 0; i < PLACEHOLDERS_COUNT; i++) {
            // Description создается из случайных строк large_text
            StringBuilder description = new StringBuilder();
            for (int j = 0; j < DESCRIPTION_LINES; j++) {
                description.append(text[randomizer.nextInt(textRows)]).append('\n');
            }
            // Конвертация int в HSV и обратно нужна, чтобы ColorPickerActivity красиво работал
            // (не каждый int без потерь конвертируется в HSV)
            // Это допущение используется только в этом тестовом методе, во всех остальных местах используются цвета,
            // конвертируемые в обе стороны без потерь
            float[] colorHsv = new float[3];
            Color.colorToHSV(randomizer.nextInt(), colorHsv);
            listItems.add(new ReadLaterItem(
                    label + " " + i,
                    description.toString().trim(),
                    Color.HSVToColor(colorHsv),
                    currentTime,
                    currentTime,
                    currentTime));
        }
        ReadLaterDbUtils.bulkInsertItems(context, listItems);

    }

}
