package com.example.mborzenkov.readlaterlist;

import android.support.annotation.NonNull;

/**
 * Неизменяемый АТД, представляющий элемент списка ReadLater
 * Обладает заголовком (label), описанием (description) и цветом (color).
 */
public class ReadLaterItem {

    /** Заголовок */
    private final String label;
    /** Описание */
    private final String description;
    /** Цвет */
    private final int color;

    // Инвариант:
    //      label - непустая строка без переносов, заголовок элемента
    //      description - строка, описание элемента
    //      color - цвет в sRGB, где каждые 8 бит последовательно представляют: Alpha, Red, Green, Blue
    //
    // Абстрактная функция:
    //      представляет элемент списка ReadLater, обладающий заголовком, описанием и цветом
    //
    // Безопасность представления:
    //      все поля - неизменяемые объекты и объявлены final
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    private void checkRep() {
        if (BuildConfig.DEBUG) {
            if (label.trim().isEmpty()) throw new AssertionError();
        }
    }

    /**
     * Создает новый элемент для помещения в список ReadLater
     * @param label Заголовок элемента, непустой и однострочный (содержит буквы, цифры или символы, не содержит переносов строки)
     * @param description Описание элемента
     * @param color Цвет в sRGB
     */
    public ReadLaterItem(@NonNull String label, @NonNull String description, int color) {
        if (label.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок ReadLaterItem не может быть пустым");
        } else if (label.contains("\n")) {
            throw new IllegalArgumentException("Заголовок должен быть однострочным");
        }
        this.label = label;
        this.description = description;
        this.color = color;
        checkRep();
    }

    /**
     * Возвращает заголовок элемента
     * @return Заголовок элемента
     */
    public String getLabel() {
        return label;
    }

    /**
     * Возвращает описание элемента
     * @return Описание элемента
     */
    public String getDescription() {
        return description;
    }

    /**
     * Возвращает цвет элемента
     * @return Цвет элемента в sRGB
     */
    public int getColor() {
        return color;
    }

    /**
     * Два объекта ReadLaterItem равны, если у них одинаковые заголовок, описание и цвет
     * @param obj Объект для сравнения
     * @return True, если равны; False в противном случае
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReadLaterItem)) {
            return false;
        }
        ReadLaterItem thatObject = (ReadLaterItem) obj;
        return ((color == thatObject.color) && label.equals(thatObject.label) && description.equals(thatObject.description));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + label.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + color;
        return result;
    }

    /**
     * Возвращает строковое представление ReadLaterItem
     * @return Строковое представление состоит из: label\n description\n (Цвет: color), где color - цвет в HEX
     * Например, "Примерный заголовок!\nДлинное описание, возможно многострочное\n(Цвет: #FFFFFF)
     */
    @Override
    public String toString() {
        return label + "\n" + description + "\n(Цвет: " + String.format("#%06X", (0xFFFFFF & color)) + ")";
    }

}
