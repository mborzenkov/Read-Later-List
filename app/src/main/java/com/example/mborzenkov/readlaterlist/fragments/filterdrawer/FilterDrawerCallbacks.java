package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.adt.MainListFilter;

import java.util.List;

/** Интерфейс для оповещений о событиях в Drawer. */
public interface FilterDrawerCallbacks {

    /** Вызывается при нажатии на одну из кнопок действий.
     * Нажатие на кнопки только вызывает этот колбек, не показывает окон и не закрывает Drawer.
     *
     * @param action действие
     */
    void onActionToggled(FilterDrawerFragment.DrawerActions action);

    /** Вызывается, когда выбран один из сохраненных фильтров.
     *
     * @param position позиция выбранного сохраненного фильтра
     */
    void onSavedFilterClick(int position);

    /**  Вызывается, когда выбран фильтр по дате.
     *
     * @param position позиция выбранного фильтра по дате
     */
    void onDateFilterClick(int position);

    /** Вызывается при выборе новой даты "от" в фильтре. */
    void onDateFromSet(long date);

    /** Вызывается при выборе новой даты "до" в фильтре. */
    void onDateToSet(long date);

    /** Вызывается, когда выбран любимый цвет.
     *
     * @param color цвет
     */
    void onFavoriteColorClick(int color);

    /** Вызывается, когда выбран новый пользователь. */
    void onChangeUserClick();

    /** Вызывается при нажатии на кнопку сортировки.
     *
     * @param type тип сортировки, связанный с кнопкой
     */
    void onSortButtonClick(MainListFilter.SortType type);

    /** Возвращает текущего пользователя. */
    @NonNull String getCurrentUser();

    /** Возвращает текущий установленный фильтр. */
    @NonNull MainListFilter getCurrentFilter();

    /** Возвращает список названий сохраненных фильтров. */
    List<String> getSavedFiltersList();

    /** Возвращает список названий фильтров по датам. */
    List<String> getDateFiltersList();

    /** Возвращает список любимых цветов. */
    int[] getFavoriteColors();

}