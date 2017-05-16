package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.BuildConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
    /** Цвет по умолчанию. */
    private static final int DEFAULT_COLOR = 16761095;

    // -- Builder
    /** Создает новый объект ReadLaterItem.
     *  Пример использования:
     *      new ReadLaterItem.Builder("Заголовок однострочный непустой").description("descr").color(Color.RED).build();
     */
    public static class Builder {

        // Обязательные параметры
        private final String label;

        // Необязательные параметры
        private String description      = "";
        private int color               = DEFAULT_COLOR;
        private long dateCreated;
        private long dateModified;
        private long dateViewed;
        private @Nullable URL imageUrl  = null;
        private Integer remoteId        = null;

        /** Начинает создание элемента.
         *  Заполняет все необязательные параметры значениями по умолчанию.
         *
         * @param label Заголовок элемента, непустой и однострочный
         *              (содержит буквы, цифры или символы, не содержит переносов строки)
         *
         * @throws IllegalArgumentException если label пустой или многострочный
         * @throws NullPointerException если label == null
         */
        public Builder(@NonNull String label) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("label == \"\"");
            } else if (label.contains("\n")) {
                throw new IllegalArgumentException("label.contains(\"\\n\")");
            }
            this.label = label;
            long currentTime = System.currentTimeMillis();
            this.dateCreated = currentTime;
            this.dateModified = currentTime;
            this.dateViewed = currentTime;
        }

        /** Устанавливает описание у элемента.
         *  Значение по умолчанию: пустая строка.
         *
         * @param description Описание элемента, может быть пустое или многострочное, но не null
         * @throws NullPointerException если label == null
         */
        public Builder description(@NonNull String description) {
            this.description = description.trim();
            return this;
        }

        /** Устанавливает цвет у элемента.
         *  Значение по умолчанию: DEFAULT_COLOR.
         *
         * @param color Цвет в sRGB
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /** Устанавливает дату создания у элемента.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param dateCreated Дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
         */
        public Builder dateCreated(long dateCreated) {
            this.dateCreated = dateCreated;
            return this;
        }

        /** Устанавливает дату изменения у элемента.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param dateModified Дата изменения в формате timestamp
         */
        public Builder dateModified(long dateModified) {
            this.dateModified = dateModified;
            return this;
        }

        /** Устанавливает дату просмотра у элемента.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param dateViewed Дата просмотра в формате timestamp
         */
        public Builder dateViewed(long dateViewed) {
            this.dateViewed = dateViewed;
            return this;
        }

        /** Устанавливает все даты у элемента (создания, редактирования, просмотра) сразу.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param date Дата в формате timestamp
         */
        public Builder allDates(long date) {
            this.dateCreated = date;
            this.dateModified = date;
            this.dateViewed = date;
            return this;
        }

        /** Устанавливает ссылку на картинку у элемента.
         *  Значение по умолчанию: "".
         *
         * @param imageUrl Ссылка на картинку, должна быть корректно сформированным url, наличие картинки не проверяется
         *                 Может быть пустой строкой, тогда применяется значение по умолчанию.
         * @throws IllegalArgumentException если imageUrl не пустая строка и не является Url
         * @throws NullPointerException если imageUrl == null
         */
        public Builder imageUrl(@NonNull String imageUrl) {
            if (!imageUrl.trim().isEmpty()) {
                try {
                    this.imageUrl = new URL(imageUrl);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("imageUrl != URL: " + imageUrl);
                }
            } else {
                this.imageUrl = null;
            }
            return this;
        }

        /** Устанавливает идентификатор у элемента.
         *  Значение по умолчанию: null.
         *
         * @param remoteId Идентификатор элемента, число > 0 или null
         *                 null означает, что remoteId не задан
         * @throws IllegalArgumentException если remoteId <= 0
         */
        public Builder remoteId(@Nullable Integer remoteId) {
            if (remoteId != null && remoteId <= 0) {
                throw new IllegalArgumentException("remoteId <= 0");
            }
            this.remoteId = remoteId;
            return this;
        }

        /** Создает новый объект ReadLaterItem.
         *
         * @return Объект ReadLaterItem, созданный на основании Builder.
         */
        public ReadLaterItem build() {
            return new ReadLaterItem(this);
        }

    }


    // -- Объект ReadLaterItem
    /** Заголовок. */
    private final @NonNull String label;
    /** Описание. */
    private final @NonNull String description;
    /** Цвет. */
    private final int color;
    /** Дата создания. */
    private final long dateCreated;
    /** Дата изменения. */
    private final long dateModified;
    /** Дата просмотра. */
    private final long dateViewed;
    /** URL картинки. */
    private final @Nullable URL imageUrl;
    /** Внешний идентификатор элемента. */
    private final @Nullable Integer remoteId;

    // Инвариант:
    //      label - непустая строка без переносов, заголовок элемента
    //      description - строка, описание элемента
    //      color - цвет в sRGB, где каждые 8 бит последовательно представляют: Alpha, Red, Green, Blue
    //      created - дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT)
    //      edited - дата изменения в формате timestamp
    //      viewed - дата просмотра в формате timestamp
    //      imageUrl - ссылка, может быть null
    //      remoteId - внешний идентификатор: >0, если установлен или 0 в противном случае
    //
    // Абстрактная функция:
    //      представляет элемент списка ReadLater, обладающий заголовком, описанием, цветом, датой создания, изменения,
    //          просмотра и (опционально) ссылкой на картинку
    //
    // Безопасность представления:
    //      все поля - неизменяемые объекты и объявлены final
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    private void checkRep() {
        if (BuildConfig.DEBUG) {
            if (label.trim().isEmpty()) {
                throw new AssertionError("Заголовок ReadLaterItem оказался пустым.");
            }
            if (label.contains("\n")) {
                throw new AssertionError("Заголовок ReadLaterItem оказался многострочным.");
            }
            if (remoteId != null && remoteId < 0) {
                throw new AssertionError("Идентификатор ReadLaterItem оказался отрицательным.");
            }
        }
    }

    private ReadLaterItem(Builder builder) {
        label           = builder.label;
        description     = builder.description;
        color           = builder.color;
        dateCreated     = builder.dateCreated;
        dateModified    = builder.dateModified;
        dateViewed      = builder.dateViewed;
        imageUrl        = builder.imageUrl;
        remoteId        = builder.remoteId;
        checkRep();
    }

    /** Возвращает заголовок элемента.
     *
     * @return Заголовок элемента
     */
    public @NonNull String getLabel() {
        return label;
    }

    /** Возвращает описание элемента.
     *
     * @return Описание элемента
     */
    public @NonNull String getDescription() {
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

    /** Возвращает строку imageUrl.
     *
     * @return строка imageUrl, может быть пустой
     */
    public @NonNull String getImageUrl() {
        if (imageUrl != null) {
            return imageUrl.toString();
        } else {
            return "";
        }
    }

    /** Возвращает внешний идентификатор элемента.
     *
     * @return Внешний идентификатор > 0, если был установлен, иначе 0
     */
    public @Nullable Integer getRemoteId() {
        return remoteId;
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
        boolean equality = color == thatObject.color
                && dateCreated == thatObject.dateCreated
                && dateModified == thatObject.dateModified
                && dateViewed == thatObject.dateViewed
                && label.equals(thatObject.label)
                && description.equals(thatObject.description);
        if (imageUrl != null) {
            return equality && imageUrl.equals(thatObject.imageUrl);
        } else {
            return equality && (thatObject.imageUrl == null);
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + label.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + color;
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateModified ^ (dateModified >>> 32));
        result = 31 * result + (int) (dateViewed ^ (dateViewed >>> 32));
        if (imageUrl != null) {
            result = 31 * result + imageUrl.hashCode();
        }
        return result;
    }

    /** Возвращает строковое представление ReadLaterItem.
     *
     * @return Строковое представление состоит из:
     *              label\ndescription\n(#color)\nC: dateCreated\nM: dateModified\nV: dateViewed\nimage: , где
     *                  color - цвет в HEX,
     *                  даты формата yyyy-MM-dd'T'hh:mm:ss.SSSZ,
     *                  \nimage: нет, если imageUrl == null
     *
     *              Например:
     *              Примерный заголовок!
     *              Длинное описание, возможно
     *              многострочное
     *              (#FFFFFF)
     *              C: 2017-05-08T15:28:01.232+0400
     *              M: 2017-05-08T15:28:01.232+0400
     *              V: 2017-05-08T15:28:01.232+0400
     *              image: https://s-media-cache-ak0.pinimg.com/736x/92/9d/3d/929d3d9f76f406b5ac6020323d2d32dc.jpg
     */
    @Override
    public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        String result = String.format("%s%n%s%n(%s)%nC: %s%nM: %s%nV: %s",
                label,
                description,
                String.format(FORMAT_COLOR, Integer.toString(color, 16)),
                dateFormatter.format(dateCreated),
                dateFormatter.format(dateModified),
                dateFormatter.format(dateViewed));
        if (imageUrl != null) {
            result = String.format("%s%nimage: %s", result, imageUrl.toString());
        }
        return result;
    }

}
