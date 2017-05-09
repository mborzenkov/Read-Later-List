package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.MainListActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DebugUtils {

    /** Количество строк в description для автоматического создания. */
    private static final int DESCRIPTION_LINES = 3;

    private DebugUtils() {
        throw new UnsupportedOperationException("Класс DebugUtils - static util, не может иметь экземпляров");
    }

    /** Показывает окно ввода количества плейсхолдеров и выполняет добавление данных при подтверждении.
     * Если пользователь ввел
     *
     * @param context Контекст
     * @param activity Активити для обновления данных
     */
    public static void showAlertAndAddPlaceholders(final Context context, final MainListActivity activity) {
        EditText inputNumber = new EditText(context);
        inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(6)}); // Не более 999999
        ActivityUtils.showInputTextDialog(
            context,
            inputNumber,
            context.getString(R.string.mainlist_menu_add_placeholders_question_title),
            context.getString(R.string.mainlist_menu_add_placeholders_question_text),
            (String input) -> {
                try {
                    int number = Integer.parseInt(input);
                    // activity.showLoading();
                    new Thread(() -> addPlaceholdersToDatabase(context, number)).start();
                    // activity.showData() вызывается автоматически
                    // TODO: showLoading и showData только когда закончилось
                } catch (ClassCastException e) {
                    Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                }
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
                // activity.showLoading();
                new Thread(() -> ReadLaterDbUtils.deleteAll(context)).start();
                // activity.showData() вызывается автоматически
                // TODO: showLoading и showData если ничего ен удалено
            },
            null);
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет number штук записей с заранее определенными
     *  Label, случайными description и случайными цветами.
     *
     * @param context контекст
     * @param number число добавляемых плейсхолдеров
     */
    private static void addPlaceholdersToDatabase(Context context, int number) {

        long currentTime = System.currentTimeMillis();
        String[] text = context.getString(R.string.debug_large_text).split("\n");
        int textRows = text.length;
        String label = context.getString(R.string.mainlist_menu_add_placeholders_label);
        Random randomizer = new Random();
        // Вставляем number строк
        for (int inserted = 0; inserted < number; ) {
            List<ReadLaterItem> listItems = new ArrayList<>();
            // По 20 000
            for (int i = 0, step = Math.min(20000, number - inserted); i < step; i++, inserted++) {
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
                        label + " " + inserted,
                        description.toString().trim(),
                        Color.HSVToColor(colorHsv),
                        currentTime,
                        currentTime,
                        currentTime));
            }
            ReadLaterDbUtils.bulkInsertItems(context, listItems);
        }

    }

}
