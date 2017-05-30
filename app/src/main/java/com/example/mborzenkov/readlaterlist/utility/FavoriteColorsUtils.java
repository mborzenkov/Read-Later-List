package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.mborzenkov.readlaterlist.R;

import java.util.Set;

/** Сервисный класс для работы с любимыми цветами. */
public class FavoriteColorsUtils {

    /** Константа для использования в качестве ключа при сохранении массива Favorites в SharedPreferences. */
    private static final String FAVORITES_KEY = "com.example.mborzenkov.colorpicker.favorites";
    /** Количество элементов favorites. */
    private static int sMaxFavorites = 0;

    private FavoriteColorsUtils() {
        throw new UnsupportedOperationException("Класс FavoriteColorsUtils - static util, не может иметь экземпляров");
    }

    public static int getMaxFavorites(Context context) {
        if (sMaxFavorites == 0) {
            sMaxFavorites = context.getResources().getInteger(R.integer.colorpicker_favorites);
        }
        return sMaxFavorites;
    }

    /** Получает любимые цвета из SharedPreferences.
     * Если какой либо из цветов не задан, он будет Color.TRANSPARENT.
     *
     * @param context Контекст
     * @param sharedPreferences Ссылка на SharedPreferences, если null - получается через контекст
     * @return Список любимых цветов, размерностью maxFavorites
     */
    public static int[] getFavoriteColorsFromSharedPreferences(Context context,
                                                               @Nullable SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(FAVORITES_KEY, Context.MODE_PRIVATE);
        }
        sMaxFavorites = getMaxFavorites(context);
        int[] result = new int[sMaxFavorites];

        for (int i = 0; i < result.length; i++) {
            int savedColor = sharedPreferences.getInt(String.valueOf(i), Color.TRANSPARENT);
            result[i] = savedColor;
        }
        return result;
    }

    /** Сохраняет любимый цвет в SharedPreferences.
     * Цвет будет сохранен с ключем, равным position.
     *
     * @param context Контекст, может быть null, если указан sharedPreferences
     * @param sharedPreferences Ссылка на SharedPreferences, если null - получается через контекста
     * @param newColor цвет для сохранения в формате sRGB
     * @param position ключ для сохранения (позиция любимого цвета)
     *
     * @throws IllegalArgumentException если context == null и sharedPreferences == null
     *              так как невозможно получить sharedPreferences
     */
    public static void saveFavoriteColor(@Nullable Context context,
                                         @Nullable SharedPreferences sharedPreferences,
                                         int newColor,
                                         int position) {

        if (sharedPreferences == null) {

            if (context == null) {
                throw new IllegalArgumentException(
                        "Error @ FavoriteColorsUtils.saveFavoriteColor: context and sharedPreferences both null");
            }

            sharedPreferences = context.getSharedPreferences(FAVORITES_KEY, Context.MODE_PRIVATE);

        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(String.valueOf(position), newColor);
        editor.apply();

    }

}
