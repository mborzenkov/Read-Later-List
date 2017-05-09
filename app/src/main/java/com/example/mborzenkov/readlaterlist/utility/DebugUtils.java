package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainListActivity;
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

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет number штук записей с заранее определенными
     *  Label, случайными description и случайными цветами.
     *
     * @param context контекст
     * @param number число добавляемых плейсхолдеров
     */
    public static void addPlaceholdersToDatabase(Context context, int number) {

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
