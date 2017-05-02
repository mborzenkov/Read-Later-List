package com.example.mborzenkov.readlaterlist.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.mborzenkov.readlaterlist.R;

public class FavoriteColorsUtils {

    /** Константа для использования в качестве ключа при сохранении массива Favorites. */
    private static final String FAVORITES_KEY = "com.example.mborzenkov.colorpicker.favorites";
    /** Количество элементов favorites. */
    private static int sMaxFavorites = 0;

    private FavoriteColorsUtils() {
        throw new UnsupportedOperationException("Класс FavoriteColorsUtils - static util, не может иметь экземпляров");
    }

    private static int getMaxFavorites(Context context) {
        return context.getResources().getInteger(R.integer.colorpicker_favorites);
    }

    public static void inflateFavLayout(Activity activity, LinearLayout layout, View.OnClickListener clickListener) {

        Context context = activity.getApplicationContext();
        LayoutInflater layoutInflater = activity.getLayoutInflater();

        if (sMaxFavorites == 0) {
            sMaxFavorites = getMaxFavorites(context);
        }

        for (int i = 0; i < sMaxFavorites; i++) {
            GradientDrawable circle = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.mainlist_circle);
            View favCircle = layoutInflater.inflate(R.layout.drawer_mainlist_favorites, layout, false);
            View circleButton = favCircle.findViewById(R.id.imageButton_favorite_color);
            circleButton.setOnClickListener(clickListener);
            circleButton.setBackground(circle);
            circleButton.setTag(i);
            layout.addView(favCircle);
        }

    }

    public static void updateFavLayoutFromSharedPreferences(Context context,
                                                         LinearLayout layout,
                                                         @Nullable SharedPreferences sharedPreferences) {

        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(FAVORITES_KEY, Context.MODE_PRIVATE);
        }
        if (sMaxFavorites == 0) {
            sMaxFavorites = getMaxFavorites(context);
        }

        for (int i = 0; i < sMaxFavorites; i++) {
            int savedColor = sharedPreferences.getInt(String.valueOf(i), Color.TRANSPARENT);
            View favCircle = layout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
            ((GradientDrawable) favCircle.getBackground()).setColor(savedColor);
        }
    }

}
