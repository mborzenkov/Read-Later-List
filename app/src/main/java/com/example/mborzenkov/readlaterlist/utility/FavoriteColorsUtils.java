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

    private static int getMaxFavorites(Context context) {
        if (sMaxFavorites == 0) {
            sMaxFavorites = context.getResources().getInteger(R.integer.colorpicker_favorites);
        }
        return sMaxFavorites;
    }

    /** Добавляет Favorite кружки на layout.
     *
     * @param context контекст
     * @param inflater инфлейтер для инфлейтинга
     * @param layout Layout, в котором должны быть кружки
     */
    public static void inflateFavLayout(@NonNull Context context,
                                        @NonNull LayoutInflater inflater,
                                        @NonNull LinearLayout layout) {

        sMaxFavorites = getMaxFavorites(context);

        for (int i = 0; i < sMaxFavorites; i++) {
            StateListDrawable circle =
                    (StateListDrawable) ContextCompat.getDrawable(context, R.drawable.circle_default);
            View favCircle = inflater.inflate(R.layout.fragment_drawer_filter_favorites, layout, false);
            View circleButton = favCircle.findViewById(R.id.imageButton_favorite_color);
            circleButton.setBackground(circle);
            circleButton.setTag(i);

            // + Видимо activated состояние получается не сразу при инфлейтинге, по какой то причине цвет потом
            // не соответствует. Этот костыль позволяет добиться желаемого результата, но нужно поправить
            // на более элегантное решение.
            circleButton.setActivated(true);
            circleButton.setActivated(false);
            // -

            layout.addView(favCircle);
        }

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

    /** Обновляет layout с любимыми кругами на основании данных в Shared Preferences.
     *
     * @param context Контекст
     * @param layout Layout
     * @param sharedPreferences Ссылка на Shared Preferences, если null - получается через контекст
     * @param clickListener Ссылка на OnClickListener, который устанавливается для кругов
     * @param colorFilter Фильтр цвета, если указан, то круги будут помечены .active
     * @return Список любимых цветов, как getFavoriteColorsFromSharedPreferences(...)
     */
    public static int[] updateFavLayoutFromSharedPreferences(Context context,
                                                         LinearLayout layout,
                                                         @Nullable SharedPreferences sharedPreferences,
                                                         @Nullable View.OnClickListener clickListener,
                                                         @Nullable Set<Integer> colorFilter) {

        int[] result = getFavoriteColorsFromSharedPreferences(context, sharedPreferences);

        for (int i = 0; i < result.length; i++) {
            int savedColor = result[i];
            View favCircle = layout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
            if (savedColor != Color.TRANSPARENT) {
                Drawable[] children = ((DrawableContainer.DrawableContainerState) (
                        favCircle.getBackground()).getConstantState()).getChildren();
                ((GradientDrawable) children[0]).setColor(savedColor);
                ((GradientDrawable) children[1]).setColor(savedColor);
                ((GradientDrawable) children[2]).setColor(savedColor);
                favCircle.setOnClickListener(clickListener);
                favCircle.setClickable(true);
                if (colorFilter != null) {
                    favCircle.setActivated(colorFilter.contains(savedColor));
                }
            } else {
                favCircle.setOnClickListener(null);
                favCircle.setClickable(false);
                favCircle.setActivated(false);
            }
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
