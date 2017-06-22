package com.example.mborzenkov.readlaterlist.utility;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.R;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/** Тестирует FavoriteColorsUtils. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class FavoriteColorsUtilsTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    private static final int MAX_FAVORITES = 3;
    private static final int[] FAVORITE_COLORS = new int[] { Color.RED, Color.GREEN, Color.BLUE };
    private static final int SAVED_COLOR = Color.BLACK;
    private static final int SAVED_COLOR_POSITION = 1;

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mSharedPrefsEditor;

    /** Подготавливает Mockito. */
    @SuppressLint("CommitPrefEdits") // это Mockito
    @Before
    public void prepareMock() {
        mContext = Mockito.mock(Context.class);
        mSharedPrefs = Mockito.mock(SharedPreferences.class);
        mSharedPrefsEditor = Mockito.mock(SharedPreferences.Editor.class);
        final Resources resources = Mockito.mock(Resources.class);

        // Res
        Mockito.when(mContext.getResources()).thenReturn(resources);
        Mockito.when(resources.getInteger(R.integer.colorpicker_favorites)).thenReturn(MAX_FAVORITES);

        // Shared Pref
        Mockito.when(mContext.getSharedPreferences(FavoriteColorsUtils.FAVORITES_KEY, Context.MODE_PRIVATE))
                .thenReturn(mSharedPrefs);
        Mockito.when(mSharedPrefs.getInt(anyString(), anyInt()))
                .thenReturn(FAVORITE_COLORS[0], FAVORITE_COLORS[1], FAVORITE_COLORS[2]);

        // Editing
        Mockito.when(mSharedPrefs.edit()).thenReturn(mSharedPrefsEditor);
        Mockito.when(mSharedPrefsEditor.putInt(anyString(), anyInt())).thenReturn(mSharedPrefsEditor);
    }

    @Test
    public void testGetFavorites() {
        int[] favoriteColors = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(mContext, null);
        assertTrue(Arrays.equals(FAVORITE_COLORS, favoriteColors));
    }

    @Test
    public void testGetFavoritesFromSharedPrefs() {
        int[] favoriteColors = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(mContext, mSharedPrefs);
        assertTrue(Arrays.equals(FAVORITE_COLORS, favoriteColors));
    }

    @Test
    public void testSaveFavColor() {
        InOrder inOrder = inOrder(mSharedPrefsEditor);

        FavoriteColorsUtils.saveFavoriteColor(mContext, null, SAVED_COLOR, SAVED_COLOR_POSITION);

        inOrder.verify(mSharedPrefsEditor).putInt(String.valueOf(SAVED_COLOR_POSITION), SAVED_COLOR);
        inOrder.verify(mSharedPrefsEditor).apply();
    }

    @Test
    public void testSaveFavColorToSharedPrefs() {
        InOrder inOrder = inOrder(mSharedPrefsEditor);

        FavoriteColorsUtils.saveFavoriteColor(null, mSharedPrefs, SAVED_COLOR, SAVED_COLOR_POSITION);

        inOrder.verify(mSharedPrefsEditor).putInt(String.valueOf(SAVED_COLOR_POSITION), SAVED_COLOR);
        inOrder.verify(mSharedPrefsEditor).apply();
    }

    @Test (expected = IllegalArgumentException.class)
    @SuppressWarnings("UnusedAssignment")
    public void testSaveFavIllegalArgument() {
        FavoriteColorsUtils.saveFavoriteColor(null, null, 0, 0);
    }

}
