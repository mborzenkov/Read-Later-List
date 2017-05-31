package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.NonNull;

/** Неизменяемый АТД, представляющий конфликт двух элементов ReadLaterItem.
 */
public class Conflict {

    /** Формат для вывода в toString. */
    @SuppressWarnings("FieldCanBeLocal")
    private static final String STRING_FORMAT = "Conflict of 2 items:%nLeft:%n%s%nRight:%n%s";

    /** Первый элемент конфликта. */
    private final ReadLaterItem leftItem;
    /** Второй элемент конфликта. */
    private final ReadLaterItem rightItem;

    // Инвариант:
    //      leftItem - первый элемент конфликта, не null
    //      rightItem - второй элемент конфликта, не null,
    //          leftItem.getRemoteId() == rightItem.getRemoteId()
    //          !leftItem.equalsByContent(rightItem)
    //
    // Абстрактная функция:
    //      представляет конфликт двух элементов ReadLaterItem с равными remoteId и не равными по содержанию
    //
    // Безопасность представления:
    //      все поля - неизменяемые объекты и объявлены final
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    /** Создает новый объект конфликта.
     * Конфликты используются при синхронизации с сервером.
     * Означают, что два элемента с одинаковым remoteId не равны по содержанию !(leftItem.equalsByContent(rightItem))
     *
     * @param leftItem первый элемент конфликта
     * @param rightItem второй элемент конфликта
     *
     * @throws NullPointerException если leftItem или rightItem == null
     * @throws IllegalArgumentException если leftItem.getRemoteId() != rightItem.getRemoteId()
     * @throws IllegalArgumentException если leftItem.equalsByContent(rightItem)
     */
    public Conflict(@NonNull ReadLaterItem leftItem, @NonNull ReadLaterItem rightItem) {
        if (leftItem.getRemoteId() != rightItem.getRemoteId()) {
            throw new IllegalArgumentException(
                    "Error @ new Conflict: leftItem.getRemoteId() != rightItem.getRemoteId()");
        } else if (leftItem.equalsByContent(rightItem)) {
            throw new IllegalArgumentException(
                    "Error @ new Conflict: leftItem.equalsByContent(rightItem) == true");
        }
        this.leftItem = leftItem;
        this.rightItem = rightItem;
    }

    /** Возвращает левый элемент конфликта.
     *
     * @return левый элемент конфликта
     */
    public @NonNull ReadLaterItem getLeft() {
        return leftItem;
    }

    /** Возвращает правый элемент конфликта.
     *
     * @return правый элемент конфликта
     */
    public @NonNull ReadLaterItem getRight() {
        return rightItem;
    }

    @Override
    public int hashCode() {
        int result = 17;
        return 31 * result + (rightItem.hashCode() + leftItem.hashCode());
    }

    /** Проверяет два объекта Conflict на равенство друг другу.
     * Два объекта Conflict равны, если они содержат одинаковые элементы, причем левый может быть = правому и наоборот.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Conflict)) {
            return false;
        }
        Conflict thatObject = (Conflict) obj;
        return (thatObject.leftItem.equals(leftItem) || thatObject.leftItem.equals(rightItem))
                && (thatObject.rightItem.equals(rightItem) || thatObject.rightItem.equals(leftItem));
    }

    @Override
    public String toString() {
        return String.format(STRING_FORMAT, leftItem.toString(), rightItem.toString());
    }

}
