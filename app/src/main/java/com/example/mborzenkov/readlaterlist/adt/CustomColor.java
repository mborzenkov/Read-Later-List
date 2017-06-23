package com.example.mborzenkov.readlaterlist.adt;

import android.graphics.Color;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;

/** Неизменяемый класс, представляющий собой цвет. */
public class CustomColor {

    /** Перечисление, описывающее цветовую модель HSV.
     * Значения - координаты цветовой модели.
      */
    public enum Hsv {
        /** Цветовой тон, значения от 0f до 360f. */
        HUE(0f, 360f, 0),
        /** Насыщенность, значения от 0f до 1f. */
        SAT(0f, 1f, 1),
        /** Яркость, значения от 0f до 1f. */
        VAL(0f, 1f, 2);

        /** Левая граница координаты. */
        private final float leftBorder;
        /** Правая граница координаты. */
        private final float rightBorder;
        /** Соответствующий индекс в массиве float[HSV_SIZE]. */
        private final int index;

        Hsv(float leftBorder, float rightBorder, int index) {
            this.leftBorder = leftBorder;
            this.rightBorder = rightBorder;
            this.index = index;
        }

        /** Возвращает левую границу координаты цветовой модели. */
        public float from() {
            return leftBorder;
        }

        /** Возвращает правую границу координаты цветовой модели. */
        public float to() {
            return rightBorder;
        }

        /** Возвращает соответствующий координате индекс в массиве float[HSV_SIZE]. */
        public int index() {
            return index;
        }
    }

    /** Размерность HSV. */
    private static final int HSV_SIZE = 3;
    /** Точность float, при которой значение приравнивается к 0. */
    private static final float FLOAT_PRECISION = 0.001f;
    /** Формат цвета. */
    private static final String FORMAT_COLOR = "#%s";
    /** Размерность HEX. */
    private static final int HEX = 16;
    /** CustomColor с прозрачным цветом. */
    private static final CustomColor TRANSPARENT_COLOR = new CustomColor(Color.TRANSPARENT);

    /** Возвращает CustomColor с прозрачным цветом.
     * CustomColor.getTransparent().isTransparent == true
     *
     * @return CustomColor с прозрачным цветом
     */
    public static CustomColor getTransparent() {
        return TRANSPARENT_COLOR;
    }

    /** Возвращает новый CustomColor с измененным значением HUE, SAT или VAL на delta.
     * Если color.getHsv(type) + delta < type.from || > type.to, то применяется граничное значение
     * Если 0 <= |delta| < 0.001f, то возвращается color
     * Объекты CustomColor, полученные с помощью этого метода, могут быть не равны между собой:
     *      someColor.equals(CustomColor.colorWithModifiedHue(
     *          CustomColor.colorWithModifiedHue(someColor, Hsv.HUE, -1f), 1f))
     *      не определено, может быть true или false из-за конвертаций sRGB <-> HSV
     *
     * @param color начальный цвет
     * @param hsvAttr координата цветовой модели Hsv
     * @param delta изменение hue
     *
     * @return новый CustomColor с измененным HUE
     *
     * @throws NullPointerException если color == null || type == null
     */
    public static CustomColor colorWithModifiedHsv(@NonNull CustomColor color, @NonNull Hsv hsvAttr, float delta) {
        if (Math.abs(delta) < FLOAT_PRECISION) {
            return color;
        }
        float[] hsv = Arrays.copyOf(color.colorHsv, HSV_SIZE);
        if (delta < 0f) {
            hsv[hsvAttr.index] = Math.max(hsv[hsvAttr.index] + delta, hsvAttr.from());
        } else {
            hsv[hsvAttr.index] = Math.min(hsv[hsvAttr.index] + delta, hsvAttr.to());
        }
        return new CustomColor(Color.HSVToColor(hsv));
    }


    /** Значение цвета в sRGB. */
    private final int color;
    /** Значение цвета в HSV. */
    private final float[] colorHsv = new float[HSV_SIZE];

    // Инвариант:
    //      color - цвет в sRGB, где каждые 8 бит последовательно представляют: Alpha, Red, Green, Blue
    //      colorHsv - цвет в HSV в виде массива float размерностью HSV_SIZE, см. индексы в Hsv
    //
    // Абстрактная функция:
    //      представляет значение цвета
    //
    // Безопасность представления:
    //      у класса отсутствуют мутаторы
    //      colorHsv изменяемый объект, не возвращается методами класса
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    /** Создает новый CustomColor с указанным цветом.
     * getColorRgb всегда будет возвращать тот цвет в sRGB, с которым CustomColor был создан:
     *      (new CustomColor(someColorRgb)).getColorRgb() == someColorRgb
     * Также два объекта CustomColor, созданных с одним и тем же цветом в sRGB, всегда будут равны:
     *      (new CustomColor(anyColorRgb)).equals(new CustomColor(anyColorRgb)) == true
     *
     * @param colorRgb цвет в sRGB
     */
    public CustomColor(int colorRgb) {
        color = colorRgb;
        Color.colorToHSV(color, colorHsv);
    }

    /** Возвращает связанный с CustomColor цвет в sRGB.
     * Всегда возвращает тот цвет в sRGB, с которым CustomColor был создан:
     *      (new CustomColor(someColorRgb)).getColorRgb() == someColorRgb
     *
     * @return цвет CustomColor в sRGB
     */
    public int getColorRgb() {
        return color;
    }

    /** Возвращает значение координаты цветовой модели, соответствующее CustomColor.
     *
     * @return значение координаты цветовой модели в диапазоне от hsvAttr.from() до hsvAttr.to()
     */
    public float getHsvAttr(Hsv hsvAttr) {
        return colorHsv[hsvAttr.index];
    }

    /** Проверяет, является ли связанный с CustomColor цвет прозрачным.
     *
     * @return true только если SomeCustomColor.equals(CustomColor.getTransparent()), иначе false
     */
    public boolean isTransparent() {
        return color == Color.TRANSPARENT;
    }

    /** Два объекта CustomColor равны, если связанные с ним значения sRGB (с которыми они были созданы), равны
     *      (new CustomColor(anyColorRgb)).equals(new CustomColor(anyColorRgb)) == true
     *  При этом значения HSV в редких случаях могут отличаться.
     *
     * @param obj объект для сравнения
     *
     * @return true, если оба объекта CustomColor были созданы с одним значением цвета
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CustomColor)) {
            return false;
        }
        CustomColor thatObject = (CustomColor) obj;
        return color == thatObject.color;
    }

    @Override
    public int hashCode() {
        return color;
    }

    /** Возвращает строковое представление связанного с CustomColor цвета в HEX.
     *
     * @return например, "#FFFFFF"
     */
    @Override
    public String toString() {
        return String.format(FORMAT_COLOR, Integer.toString(color, HEX));
    }

}
