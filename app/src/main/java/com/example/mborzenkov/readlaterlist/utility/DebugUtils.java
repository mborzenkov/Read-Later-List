package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

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
    /** Минимальное разрешение картинки по вертикали. */
    private static final int IMAGE_HEIGHT = 300;

    private DebugUtils() {
        throw new UnsupportedOperationException("Класс DebugUtils - static util, не может иметь экземпляров");
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет number штук записей с заранее определенными
     *  Label, случайными description и случайными цветами.
     *
     * @param context контекст, не null
     * @param number число добавляемых плейсхолдеров
     *
     * @throws NullPointerException если context == null
     * @throws IllegalArgumentException если number < 0
     */
    public static void addPlaceholdersToDatabase(@NonNull Context context, @IntRange(from = 0) int number) {

        if (number < 0) {
            throw new IllegalArgumentException("Error @ DebugUtils.addPlaceholdersToDatabase: number < 0 == " + number);
        }

        final String[] text = context.getString(R.string.debug_large_text).split("\n");
        final int[] predefinedColors = context.getResources().getIntArray(R.array.full_gradient);
        final int textRows = text.length;
        final int numberOfColors = predefinedColors.length;
        final String label = context.getString(R.string.mainlist_menu_add_placeholders_label);
        final Random randomizer = new Random();
        final String imageUrl = "https://unsplash.it/1200/";

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

                listItems.add(new ReadLaterItem.Builder(label + " " + inserted)
                        .description(description.toString())
                        .color(predefinedColors[randomizer.nextInt(numberOfColors)])
                        .imageUrl(imageUrl + (IMAGE_HEIGHT + (i % IMAGE_HEIGHT)))
                        .build());
            }
            ReadLaterDbUtils.bulkInsertItems(context, listItems);
        }

    }

}
