package com.example.mborzenkov.readlaterlist.fragments.edititem;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.fragments.BasicFragmentCallbacks;

/** Интерфейс взаимодействия фрагментов EditItemFragment и Activity.
 * Представляет собой действия, возможные с фрагментами, содержащими EditItemFragment, и колбеки для оповещения
 *      Activity об изменениях.
 */
public interface EditItemFragmentActions {

    /** Константа, обозначающая пустой UID. */
    int UID_EMPTY = -1;

    // Ключи для Bundle редактируемого объекта ReadLaterItem.
    String BUNDLE_ITEM_KEY = "item";
    String BUNDLE_ITEMID_KEY = "item_id";


    /////////////////////////
    // Колбеки Fragment -> Activity

    /** Интерфейс для оповещений о событиях во фрагменте. */
    interface EditItemCallbacks extends BasicFragmentCallbacks {

        /** Вызывается при нажатии пользователя на выбор цвета.
         * Получатель должен открыть выбиратель цвета и по окончанию выбора вызвать setColor().
         *
         * @param color цвет, который нужно установить по умолчанию
         * @param sharedElement shared element для использования при открытии фрагмента редактирования,
         *                      не null, у него обязательно установлен transition name
         */
        void onRequestColorPicker(int color, ImageView sharedElement);

        /** Вызывается при завершении редактирования объекта и необходимости сохранения изменений.
         * Если ничего не изменено, onCreateNewItem не вызывается.
         * Вызывается только для режима создания объекта.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         *
         * @param item новый объект
         */
        void onCreateNewItem(@NonNull ReadLaterItem item);

        /** Вызывается при завершении редактирования объекта и необходимости сохранения изменений.
         * Если изменений нет, onSaveItem не вызывается.
         * Вызывается только для режима редактирования объекта.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         *
         * @param item объект, который нужно сохранить
         * @param localId внутренний идентификатор объекта, всегда больше UID_EMPTY
         */
        void onSaveItem(@NonNull ReadLaterItem item, @IntRange(from = 0) int localId);

        /** Вызывается при необходимости удаления объекта.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         *
         * @param localId внутренний идентификатор объекта, всегда больше UID_EMPTY
         */
        void onDeleteItem(@IntRange(from = 0) int localId);

        /** Вызывается при выходе без изменений.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         * Получателю следует обновить last view date, если item != null.
         *
         * @param item редактируемый элемент или null, если закрыли режим добавления
         * @param localId id редактируемого элемента > UID_EMPTY или UID_EMPTY, если item == null
         */
        void onExitWithoutModifying(@Nullable ReadLaterItem item, @IntRange(from = UID_EMPTY) int localId);

    }


    /////////////////////////
    // Действия Activity -> Fragment

    /** Вызывается, когда нажата кнопка назад.
     * Управление полностью передается фрагменту. Фрагмент должен обработать нажатие самостоятельно.
     */
    void onBackPressed();

    /** Устанавливает цвет в фрагменте.
     *
     * @param newColor цвет, который нужно установить
     */
    void setColor(int newColor);

}
