package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.BuildConfig;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Неизменяемый АТД, представляющий элемент списка ReadLater.
 * Обладает заголовком (label), описанием (description), цветом (color), датой создания (dateCreated), датой изменения
 * (dateModified) и датой просмотра (dateViewed).
 */
public class ReadLaterItem {

    /** Формат дат. */
    private static final String FORMAT_DATE = "yyyy-MM-dd'T'hh:mm:ss.SSSZ";
    /** Формат цвета. */
    private static final String FORMAT_COLOR = "#%s";

    /** Заголовок. */
    private final String label;
    /** Описание. */
    private final String description;
    /** Цвет. */
    private final int color;
    /** Дата создания. */
    private final long dateCreated;
    /** Дата изменения. */
    private final long dateModified;
    /** Дата просмотра. */
    private final long dateViewed;


    // Инвариант:
    //      label - непустая строка без переносов, заголовок элемента
    //      description - строка, описание элемента
    //      color - цвет в sRGB, где каждые 8 бит последовательно представляют: Alpha, Red, Green, Blue
    //      created - дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
    //      edited - дата изменения в формате timestamp
    //      viewed - дата просмотра в формате timestamp
    //
    // Абстрактная функция:
    //      представляет элемент списка ReadLater, обладающий заголовком, описанием, цветом, датой создания, изменения
    //          и просмотра
    //
    // Безопасность представления:
    //      все поля - неизменяемые объекты и объявлены final
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    private void checkRep() {
        if (BuildConfig.DEBUG) {
            if (label.trim().isEmpty()) {
                throw new AssertionError();
            }
        }
    }

    /** Создает новый элемент для помещения в список ReadLater.
     *
     * @param label Заголовок элемента, непустой и однострочный
     *              (содержит буквы, цифры или символы, не содержит переносов строки)
     * @param description Описание элемента
     * @param color Цвет в sRGB
     * @param dateCreated Дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
     * @param dateModified Дата изменения в формате timestamp
     * @param dateViewed Дата просмотра в формате timestamp
     *
     * @throws IllegalArgumentException если переданы неподходящие параметры
     */
    public ReadLaterItem(@NonNull String label,
                         @NonNull String description,
                         int color,
                         long dateCreated,
                         long dateModified,
                         long dateViewed) {

        if (label.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок ReadLaterItem не может быть пустым");
        } else if (label.contains("\n")) {
            throw new IllegalArgumentException("Заголовок должен быть однострочным");
        }
        this.label = label;
        this.description = description;
        this.color = color;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.dateViewed = dateViewed;
        checkRep();

    }

    /** Возвращает заголовок элемента.
     *
     * @return Заголовок элемента
     */
    public String getLabel() {
        return label;
    }

    /** Возвращает описание элемента.
     *
     * @return Описание элемента
     */
    public String getDescription() {
        return description;
    }

    /** Возвращает цвет элемента.
     *
     * @return Цвет элемента в sRGB
     */
    public int getColor() {
        return color;
    }

    /** Возвращает дату создания в формате timestamp.
     *
     * @return Разница в миллесекундах между датой создания и 1 января 1970 00:00:00 GMT
     */
    public long getDateCreated() {
        return dateCreated;
    }

    /** Возвращает дату изменения в формате timestamp.
     *
     * @return Разница в миллесекундах между датой изменения и 1 января 1970 00:00:00 GMT
     */
    public long getDateModified() {
        return dateModified;
    }

    /** Возвращает дату просмотра в формате timestamp.
     *
     * @return Разница в миллесекундах между датой просмотра и 1 января 1970 00:00:00 GMT
     */
    public long getDateViewed() {
        return dateViewed;
    }

    /** Два объекта ReadLaterItem равны, если у них одинаковые заголовок, описание, цвет и даты создания, изменения
     * и просмотра.
     *
     * @param obj Объект для сравнения
     * @return True, если равны; False в противном случае
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReadLaterItem)) {
            return false;
        }
        ReadLaterItem thatObject = (ReadLaterItem) obj;
        return ((color == thatObject.color)
                && label.equals(thatObject.label)
                && description.equals(thatObject.description)
                && dateCreated == thatObject.dateCreated
                && dateModified == thatObject.dateModified
                && dateViewed == thatObject.dateViewed);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + label.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + color;
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateModified ^ (dateModified >>> 32));
        return 31 * result + (int) (dateViewed ^ (dateViewed >>> 32));
    }

    /** Возвращает строковое представление ReadLaterItem.
     *
     * @return Строковое представление состоит из:
     *              label\ndescription\n(#color)\nC: dateCreated\nM: dateModified\nV: dateViewed, где
     *              color - цвет в HEX, даты формата yyyy-MM-dd'T'hh:mm:ss.SSSZ
     *              Например:
     *              Примерный заголовок!
     *              Длинное описание, возможно
     *              многострочное
     *              (#FFFFFF)
     *              C: 2017-05-08T15:28:01.232+0400
     *              M: 2017-05-08T15:28:01.232+0400
     *              V: 2017-05-08T15:28:01.232+0400
     */
    @Override
    public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        return String.format("%s\n%s\n(%s)\nC: %s\nM: %s\nV: %s",
                label,
                description,
                String.format(FORMAT_COLOR, Integer.toString(color, 16)),
                dateFormatter.format(dateCreated),
                dateFormatter.format(dateModified),
                dateFormatter.format(dateViewed));
    }

}
