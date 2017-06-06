package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

/** Интерфейс для оповещений о событиях в Drawer. */
public interface FilterDrawerCallbacks {

    /** Вызывается, когда выбран новый пользователь.
     * Если выбран тот же самый пользователь, не вызывается.
     */
    void onUserChanged();

    /** Вызывается при нажатии на одну из кнопок действий.
     * Нажатие на кнопки только вызывает этот колбек, не показывает окон и не закрывает Drawer.
     *
     * @param action действие
     */
    void onActionToggled(FilterDrawerFragment.DrawerActions action);

    /** Оповещает об изменениях фильтра. */
    void onFilterChanged();

}
