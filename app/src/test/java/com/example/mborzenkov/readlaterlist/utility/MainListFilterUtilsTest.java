package com.example.mborzenkov.readlaterlist.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


/** Тестирует MainListFilterUtils. */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class MainListFilterUtilsTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    private static final int NUMBER_OF_DATE_FILTERS = 4;
    private static final int NUMBER_OF_FILTERS_DEFAULT = 3; // default + add + remove
    private static final int POSITION_DELETE_DEFAULT = NUMBER_OF_FILTERS_DEFAULT - 1;
    private static final int POSITION_ADD_DEFAULT = POSITION_DELETE_DEFAULT - 1;
    private static final int POSITION_DEFAULT_FILTER = 0;
    private static final String NEW_FILTER_NAME = "somefilter_m";
    private static final MainListFilter DEFAULT_FILTER = new MainListFilter();

    @Test
    public void testGetCurrentFilter() {
        // Возвращает new MainListFilter(), если не был установлен другой ранее
        MainListFilter currentFilter = MainListFilterUtils.getCurrentFilter();
        assertTrue(checkDefault(currentFilter));

        // Повторный вызов должен вернуть тот же самый MainListFilter
        assertTrue(currentFilter == MainListFilterUtils.getCurrentFilter());
    }

    @Test
    public void testDateFilters() {
        List<String> dateFilters = MainListFilterUtils.getDateFiltersList(RuntimeEnvironment.application);
        assertEquals(NUMBER_OF_DATE_FILTERS, dateFilters.size());

        // Сверяем позиции MainListFilterUtils.getDateFilterSelection с MainListFilter.Selection
        assertEquals(MainListFilter.Selection.ALL,
                MainListFilterUtils.getDateFilterSelection(MainListFilter.Selection.ALL.getPosition()));
        assertEquals(MainListFilter.Selection.DATE_CREATED,
                MainListFilterUtils.getDateFilterSelection(MainListFilter.Selection.DATE_CREATED.getPosition()));
        assertEquals(MainListFilter.Selection.DATE_MODIFIED,
                MainListFilterUtils.getDateFilterSelection(MainListFilter.Selection.DATE_MODIFIED.getPosition()));
        assertEquals(MainListFilter.Selection.DATE_VIEWED,
                MainListFilterUtils.getDateFilterSelection(MainListFilter.Selection.DATE_VIEWED.getPosition()));
    }

    @Test
    public void testGetSavedFiltersList() {
        List<String> savedFilters = MainListFilterUtils.getSavedFiltersList(RuntimeEnvironment.application);
        assertEquals(NUMBER_OF_FILTERS_DEFAULT, savedFilters.size());
        assertEquals(POSITION_DELETE_DEFAULT, MainListFilterUtils.getIndexSavedDelete());
        assertEquals(POSITION_ADD_DEFAULT, MainListFilterUtils.getIndexSavedAdd());
        assertEquals(POSITION_DEFAULT_FILTER, MainListFilterUtils.getIndexSavedCurrent());
    }

    @Test
    public void testSaveRemoveFilter() {
        MainListFilter currentFilter = MainListFilterUtils.getCurrentFilter();
        currentFilter.setSelection(MainListFilter.Selection.DATE_MODIFIED);
        currentFilter.addColorFilter(Color.RED);
        currentFilter.setSortType(MainListFilter.SortType.DATE_VIEWED);
        MainListFilterUtils.saveFilter(RuntimeEnvironment.application, NEW_FILTER_NAME);

        List<String> savedFilters = MainListFilterUtils.getSavedFiltersList(RuntimeEnvironment.application);
        assertEquals(NUMBER_OF_FILTERS_DEFAULT + 1, savedFilters.size());
        assertEquals(POSITION_DELETE_DEFAULT + 1, MainListFilterUtils.getIndexSavedDelete());
        assertEquals(POSITION_ADD_DEFAULT + 1, MainListFilterUtils.getIndexSavedAdd());
        assertEquals(POSITION_DEFAULT_FILTER + 1, MainListFilterUtils.getIndexSavedCurrent());

        // Сохранен тот же фильтр
        assertTrue(currentFilter == MainListFilterUtils.getCurrentFilter());

        // Меняем на default, чекаем
        MainListFilterUtils.clickOnSavedFilter(POSITION_DEFAULT_FILTER);
        assertTrue(checkDefault(MainListFilterUtils.getCurrentFilter()));

        // Меняем на добавленный, чекаем
        MainListFilterUtils.clickOnSavedFilter(POSITION_DEFAULT_FILTER + 1);
        MainListFilter newCurrentFilter = MainListFilterUtils.getCurrentFilter();
        assertEquals(currentFilter.getColorFilter(), newCurrentFilter.getColorFilter());
        assertEquals(currentFilter.getSelection(), newCurrentFilter.getSelection());
        assertEquals(currentFilter.getSortType(), newCurrentFilter.getSortType());
        assertEquals(currentFilter.getSortOrder(), newCurrentFilter.getSortOrder());

        // Удаляем
        MainListFilterUtils.removeCurrentFilter(RuntimeEnvironment.application);

        savedFilters = MainListFilterUtils.getSavedFiltersList(RuntimeEnvironment.application);
        assertEquals(NUMBER_OF_FILTERS_DEFAULT, savedFilters.size());
        assertEquals(POSITION_DELETE_DEFAULT, MainListFilterUtils.getIndexSavedDelete());
        assertEquals(POSITION_ADD_DEFAULT, MainListFilterUtils.getIndexSavedAdd());
        assertEquals(POSITION_DEFAULT_FILTER, MainListFilterUtils.getIndexSavedCurrent());

        // Должен быть установлен Default
        assertTrue(checkDefault(MainListFilterUtils.getCurrentFilter()));
    }

    /** Проверяет, является ли otherFilter фильтром по умолчанию. */
    private boolean checkDefault(MainListFilter otherFilter) {
        return DEFAULT_FILTER.getSelection() == otherFilter.getSelection()
                && DEFAULT_FILTER.getSortType() == otherFilter.getSortType()
                && DEFAULT_FILTER.getSortOrder() == otherFilter.getSortOrder()
                && DEFAULT_FILTER.getColorFilter().equals(otherFilter.getColorFilter());
    }



}
