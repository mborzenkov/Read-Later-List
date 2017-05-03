package com.example.mborzenkov.readlaterlist.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.mborzenkov.readlaterlist.R;

import java.util.Set;

public class FavoriteColorsUtils {

    /** Константа для использования в качестве ключа при сохранении массива Favorites. */
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

    public static void inflateFavLayout(Activity activity, LinearLayout layout) {

        Context context = activity.getApplicationContext();
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        sMaxFavorites = getMaxFavorites(context);

        for (int i = 0; i < sMaxFavorites; i++) {
            StateListDrawable circle = (StateListDrawable) ContextCompat.getDrawable(context, R.drawable.mainlist_drawer_circle);
            View favCircle = layoutInflater.inflate(R.layout.drawer_mainlist_favorites, layout, false);
            View circleButton = favCircle.findViewById(R.id.imageButton_favorite_color);
            circleButton.setBackground(circle);
            circleButton.setTag(i);
            layout.addView(favCircle);
        }

    }

    public static int[] updateFavLayoutFromSharedPreferences(Context context,
                                                         LinearLayout layout,
                                                         @Nullable SharedPreferences sharedPreferences,
                                                         @Nullable View.OnClickListener clickListener,
                                                         @Nullable Set<Integer> colorFilter) {

        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(FAVORITES_KEY, Context.MODE_PRIVATE);
        }
        sMaxFavorites = getMaxFavorites(context);
        int[] result = new int[sMaxFavorites];

        for (int i = 0; i < sMaxFavorites; i++) {
            int savedColor = sharedPreferences.getInt(String.valueOf(i), Color.TRANSPARENT);
            result[i] = savedColor;
            View favCircle = layout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
            if (savedColor != Color.TRANSPARENT) {
                Drawable[] children = ((DrawableContainer.DrawableContainerState) (
                        (StateListDrawable) favCircle.getBackground()).getConstantState()).getChildren();
                ((GradientDrawable) children[0]).setColor(savedColor);
                ((GradientDrawable) children[1]).setColor(savedColor);
                ((GradientDrawable) children[2]).setColor(savedColor);
                favCircle.setOnClickListener(clickListener);
                favCircle.setClickable(true);
                favCircle.setActivated(colorFilter.contains(savedColor));
            } else {
                favCircle.setOnClickListener(null);
                favCircle.setClickable(false);
                favCircle.setActivated(false);
            }
        }
        return result;
    }
}
