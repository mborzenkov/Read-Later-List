package com.example.mborzenkov.readlaterlist.adt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/** Тестирует MainListFilter. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class MainListFilterTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    @Test
    public void testMainListFilter() throws ParseException {
        MainListFilter defaultFilter = new MainListFilter();
        defaultFilter.setSelection(MainListFilter.Selection.DATE_VIEWED);
        defaultFilter.setSortType(MainListFilter.SortType.DATE_VIEWED);
        defaultFilter.nextSortOrder();
        defaultFilter.addColorFilter(Color.RED);
        defaultFilter.addColorFilter(Color.BLUE);
        assertEquals(defaultFilter.getSelection(), MainListFilter.Selection.DATE_VIEWED);
        assertEquals(defaultFilter.getSortType(), MainListFilter.SortType.DATE_VIEWED);
        assertEquals(defaultFilter.getSortOrder(), MainListFilter.SortOrder.DESC);
        assertEquals(defaultFilter.getDateFrom(), "");
        assertEquals(defaultFilter.getDateTo(), "");
        Set<Integer> colorFilter = defaultFilter.getColorFilter();
        assertEquals(colorFilter.size(), 2);
        assertTrue(colorFilter.contains(Color.RED) && colorFilter.contains(Color.BLUE));

        String dateFrom = "20/11/17";
        String dateTo = "22/11/17";
        SimpleDateFormat sdf = new SimpleDateFormat(MainListFilter.FORMAT_DATE, Locale.US);
        defaultFilter.setDateFrom(sdf.parse(dateFrom).getTime());
        assertEquals(defaultFilter.getDateFrom(), dateFrom);
        defaultFilter.setDateTo(sdf.parse(dateTo).getTime());
        assertEquals(defaultFilter.getDateTo(), dateTo);

        defaultFilter.setSortType(MainListFilter.SortType.MANUAL);
        assertEquals(defaultFilter.getSortType(), MainListFilter.SortType.MANUAL);
        assertEquals(defaultFilter.getSortOrder(), MainListFilter.SortOrder.DESC);
        defaultFilter.nextSortOrder();
        assertEquals(defaultFilter.getSortOrder(), MainListFilter.SortOrder.DESC);

        assertEquals(MainListFilter.Selection.ALL.getPosition(), 0);
    }

    @Test
    public void testToStringFromString() {
        MainListFilter defaultFilter = new MainListFilter();
        defaultFilter.setSelection(MainListFilter.Selection.DATE_VIEWED);
        defaultFilter.setDateFrom(0);
        defaultFilter.setDateTo(1);
        defaultFilter.setSortType(MainListFilter.SortType.DATE_VIEWED);
        defaultFilter.nextSortOrder();
        defaultFilter.addColorFilter(Color.RED);
        defaultFilter.addColorFilter(Color.BLUE);
        defaultFilter.addColorFilter(Color.BLACK);

        MainListFilter filterFromString = MainListFilter.fromString(defaultFilter.toString());
        assertTrue(defaultFilter.equalsByContent(filterFromString));
    }

    @Test
    public void testEqualsByContent() {
        MainListFilter filter1 = new MainListFilter();
        filter1.setSelection(MainListFilter.Selection.DATE_VIEWED);
        filter1.setDateFrom(0);
        filter1.setDateTo(1);
        filter1.setSortType(MainListFilter.SortType.DATE_CREATED);
        filter1.addColorFilter(Color.RED);
        filter1.addColorFilter(Color.BLUE);
        filter1.addColorFilter(Color.BLACK);
        MainListFilter filter2 = MainListFilter.fromString(filter1.toString());
        assertTrue(filter1.equalsByContent(filter2));

        filter2.setSortType(MainListFilter.SortType.DATE_VIEWED);
        assertFalse(filter1.equalsByContent(filter2));

        filter2.setSortType(MainListFilter.SortType.DATE_CREATED);
        assertTrue(filter1.equalsByContent(filter2));
        filter2.nextSortOrder();
        assertFalse(filter1.equalsByContent(filter2));
        filter2.nextSortOrder();
        assertTrue(filter1.equalsByContent(filter2));

        filter2.setSelection(MainListFilter.Selection.ALL);
        assertFalse(filter1.equalsByContent(filter2));
        filter2.setSelection(MainListFilter.Selection.DATE_VIEWED);
        assertTrue(filter1.equalsByContent(filter2));

        filter2.setDateFrom(1);
        assertFalse(filter1.equalsByContent(filter2));
        filter2.setDateFrom(0);
        assertTrue(filter1.equalsByContent(filter2));

        filter2.setDateTo(0);
        assertFalse(filter1.equalsByContent(filter2));
        filter2.setDateTo(1);
        assertTrue(filter1.equalsByContent(filter2));

        filter1 = new MainListFilter();
        filter2 = new MainListFilter();
        assertTrue(filter1.equalsByContent(filter2));
        filter2.setSelection(MainListFilter.Selection.DATE_VIEWED);
        assertFalse(filter1.equalsByContent(filter2));

        filter2 = new MainListFilter();
        assertTrue(filter1.equalsByContent(filter2));
        filter2.setDateFrom(0);
        assertFalse(filter1.equalsByContent(filter2));
        filter2 = new MainListFilter();
        filter2.setDateTo(1);
        assertFalse(filter1.equalsByContent(filter2));

        filter1 = new MainListFilter();
        filter2 = new MainListFilter();
        filter2.addColorFilter(1);
        assertFalse(filter1.equalsByContent(filter2));
        filter2.removeColorFilter(1);
        assertTrue(filter1.equalsByContent(filter2));
        filter1.addColorFilter(0);
        filter1.addColorFilter(1);
        filter1.addColorFilter(2);
        filter2.addColorFilter(2);
        filter2.addColorFilter(1);
        filter2.addColorFilter(0);
        assertTrue(filter1.equalsByContent(filter2));
        filter1.removeColorFilter(0);
        filter2.removeColorFilter(1);
        assertFalse(filter1.equalsByContent(filter2));
    }

    @Test
    public void testSqlSortOrder() {
        MainListFilter defaultFilter = new MainListFilter();
        defaultFilter.setSortType(MainListFilter.SortType.MANUAL);
        assertEquals(defaultFilter.getSqlSortOrder(), ReadLaterContract.ReadLaterEntry.COLUMN_ORDER + " DESC");
    }

    @Test
    public void testSqlSelection() {

        // Mockito
        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final Resources resources = Mockito.mock(Resources.class);
        final Context context = Mockito.mock(Context.class);
        final int maxFavorites = 3;
        Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);
        Mockito.when(context.getResources()).thenReturn(resources);
        Mockito.when(resources.getInteger(anyInt())).thenReturn(maxFavorites);
        Mockito.when(sharedPrefs.getInt(anyString(), anyInt())).thenReturn(Color.RED);

        // Test
        // Пустой
        MainListFilter defaultFilter = new MainListFilter();
        defaultFilter.setSortType(MainListFilter.SortType.MANUAL);
        assertEquals(defaultFilter.getSqlSelection(context), "");
        assertEquals(defaultFilter.getSqlSelectionArgs(context).length, 0);

        // Пустой, но с другим selection
        defaultFilter.setSelection(MainListFilter.Selection.DATE_CREATED);
        assertEquals(defaultFilter.getSqlSelection(context), "");
        assertEquals(defaultFilter.getSqlSelectionArgs(context).length, 0);

        String selection;
        String[] selectionArgs;

        // только Дата "от"
        String stringDateFrom = ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED + ">=?";
        defaultFilter.setDateFrom(1);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertTrue(selection.contains(stringDateFrom));
        assertEquals(selectionArgs.length, 1);
        assertTrue(Integer.valueOf(selectionArgs[0]).equals(1));

        // только дата "до"
        defaultFilter = new MainListFilter();
        String stringDateTo = ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED + "<=?";
        defaultFilter.setSelection(MainListFilter.Selection.DATE_CREATED);
        defaultFilter.setDateTo(2);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertTrue(selection.contains(stringDateTo));
        assertEquals(selectionArgs.length, 1);
        assertTrue(Integer.valueOf(selectionArgs[0]).equals(2));

        // Дата "от" и дата "до"
        defaultFilter.setDateFrom(1);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertTrue(selection.contains(stringDateFrom)
                && selection.contains(stringDateTo)
                && (selection.indexOf(stringDateFrom) < selection.indexOf(stringDateTo)));
        assertEquals(selectionArgs.length, 2);
        assertTrue(Integer.valueOf(selectionArgs[0]).equals(1));
        assertTrue(Integer.valueOf(selectionArgs[1]).equals(2));

        // Фильтры по цветам
        // Фильтр по цвету, которого нет в любимых
        defaultFilter = new MainListFilter();
        defaultFilter.addColorFilter(Color.BLACK);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertEquals(selection, "");
        assertEquals(selectionArgs.length, 0);

        // Фильтр по цветам, которые есть в любимых
        String stringColor = ReadLaterContract.ReadLaterEntry.COLUMN_COLOR + " IN (?)";
        defaultFilter.addColorFilter(Color.RED);
        defaultFilter.addColorFilter(Color.GREEN);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertEquals(stringColor, selection);
        assertEquals(selectionArgs.length, 1);
        assertTrue(Integer.valueOf(selectionArgs[0]).equals(Color.RED));

        defaultFilter.setSelection(MainListFilter.Selection.DATE_CREATED);
        defaultFilter.setDateFrom(0);
        defaultFilter.setDateTo(1);
        selection = defaultFilter.getSqlSelection(context);
        selectionArgs = defaultFilter.getSqlSelectionArgs(context);
        assertTrue(selection.contains(stringColor));
        assertEquals(selectionArgs.length, 3);
        assertTrue(Integer.valueOf(selectionArgs[2]).equals(Color.RED));

    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testFromStringException() {
        MainListFilter filterFromString = MainListFilter.fromString("233");
    }

}
