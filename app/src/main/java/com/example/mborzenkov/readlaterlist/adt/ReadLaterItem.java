package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Неизменяемый АТД, представляющий элемент списка ReadLater.
 * Обладает заголовком (label), описанием (description), цветом (color), датой создания (dateCreated), датой изменения
 * (dateModified) и датой просмотра (dateViewed).
 */
public class ReadLaterItem {

    /** Формат дат. */
    private static final String FORMAT_DATE = "yyyy-MM-dd'T'hh:mm:ssZZZZZ";
    /** Формат цвета. */
    private static final String FORMAT_COLOR = "#%s";
    /** Цвет по умолчанию. */
    private static final int DEFAULT_COLOR = 16761095;
    /** Размерность HEX. */
    private static final int HEX = 16;
    /** Количество миллисекунд в секундах. */
    private static final int MILLIS = 1000;

    // -- Builder
    /** Создает новый объект ReadLaterItem.
     *  Примеры использования:
     *      Создает новый объект с заголовком, описанием, цветом и остальными полями по умолчанию -
     *      new ReadLaterItem.Builder("Заголовок однострочный непустой").description("descr").color(Color.RED).build();
     *      Создает новый объект на основании имеющегося элемента с измененными датами.
     *      new ReadLaterItem.Builder(item).allDates(System.currentTimeMillis()).build();
     */
    public static class Builder {

        // Обязательные параметры
        private @NonNull String label;

        // Необязательные параметры
        private @NonNull String description = "";
        private int color = DEFAULT_COLOR;
        private long dateCreated;
        private long dateModified;
        private long dateViewed;
        private @Nullable URI imageUrl = null;
        private @IntRange(from = 0) int remoteId = 0;

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

        /** Начинает создание элемента.
         * Заполняет все параметры значениями из объекта ReadLaterItem.
         *
         * @param item объект, на основании которого нужно создать Builder
         *
         * @throws NullPointerException если item == null
         */
        public Builder(@NonNull ReadLaterItem item) {
            label           = item.label;
            description     = item.description;
            color           = item.color;
            dateCreated     = item.dateCreated;
            dateModified    = item.dateModified;
            dateViewed      = item.dateViewed;
            remoteId        = item.remoteId;
            imageUrl        = item.imageUrl;
        }

        /** Изменяет заголовок у элемента.
         *
         * @param label Заголовок элемента, непустой и однострочный
         *              (содержит буквы, цифры или символы, не содержит переносов строки)
         *
         * @throws IllegalArgumentException если label пустой или многострочный
         * @throws NullPointerException если label == null
         */
        public Builder label(@NonNull String label) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("label == \"\"");
            } else if (label.contains("\n")) {
                throw new IllegalArgumentException("label.contains(\"\\n\")");
            }
            this.label = label;
            return this;
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
         * @param dateCreated Дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT),
         *                          будет округлена в меньшую сторону до ближайшей секунды
         */
        public Builder dateCreated(long dateCreated) {
            this.dateCreated = dateCreated;
            return this;
        }

        /** Устанавливает дату изменения у элемента.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param dateModified Дата изменения в формате timestamp,
         *                          будет округлена в меньшую сторону до ближайшей секунды
         */
        public Builder dateModified(long dateModified) {
            this.dateModified = dateModified;
            return this;
        }

        /** Устанавливает дату просмотра у элемента.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param dateViewed Дата просмотра в формате timestamp,
         *                          будет округлена в меньшую сторону до ближайшей секунды
         */
        public Builder dateViewed(long dateViewed) {
            this.dateViewed = dateViewed;
            return this;
        }

        /** Устанавливает все даты у элемента (создания, редактирования, просмотра) сразу.
         *  Значение по умолчанию: timestamp на момент вызова конструктора ReadLaterItem.Builder().
         *
         * @param date Дата в формате timestamp, будут округлены в меньшую сторону до ближайшей секунды
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
         *
         * @throws IllegalArgumentException если imageUrl не пустая строка и не является Url
         * @throws NullPointerException если imageUrl == null
         */
        public Builder imageUrl(@NonNull String imageUrl) {
            if (!imageUrl.trim().isEmpty()) {
                URL url;
                try {
                    url = new URL(imageUrl);
                    this.imageUrl = url.toURI();
                } catch (URISyntaxException | MalformedURLException e) {
                    throw new IllegalArgumentException("imageUrl is not url: " + imageUrl, e);
                }
            } else {
                this.imageUrl = null;
            }
            return this;
        }

        /** Устанавливает идентификатор у элемента.
         *  Значение по умолчанию: 0.
         *
         * @param remoteId Идентификатор элемента, число >= 0
         *                 0 означает, что remoteId не задан
         *
         * @throws IllegalArgumentException если remoteId < 0
         */
        public Builder remoteId(@IntRange(from = 0) int remoteId) {
            if (remoteId < 0) {
                throw new IllegalArgumentException("remoteId < 0");
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
    /** URI картинки. */
    private final @Nullable URI imageUrl;
    /** Дата создания. */
    private final long dateCreated;
    /** Дата изменения. */
    private final long dateModified;
    /** Дата просмотра. */
    private final long dateViewed;
    /** Внешний идентификатор элемента. */
    private final int remoteId;

    // Инвариант:
    //      label - непустая строка без переносов, заголовок элемента
    //      description - строка, описание элемента
    //      color - цвет в sRGB, где каждые 8 бит последовательно представляют: Alpha, Red, Green, Blue
    //      created - дата создания в формате timestamp (миллисекунд с 1 Января 1970 00:00:00 GMT),
    //                      округленная в меньшую сторону до ближайшей секунды
    //      edited - дата изменения в формате timestamp, округленная в меньшую сторону до ближайшей секунды
    //      viewed - дата просмотра в формате timestamp, округленная в меньшую сторону до ближайшей секунды
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

    private ReadLaterItem(Builder builder) {
        label           = builder.label;
        description     = builder.description;
        color           = builder.color;
        dateCreated     = MILLIS * (builder.dateCreated / MILLIS);
        dateModified    = MILLIS * (builder.dateModified / MILLIS);
        dateViewed      = MILLIS * (builder.dateViewed / MILLIS);
        imageUrl        = builder.imageUrl;
        remoteId        = builder.remoteId;
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
    public int getRemoteId() {
        return remoteId;
    }

    /** Сравнивает содержательную часть двух объектов ReadLaterItem.
     * Объекты равны содержательно, если у них равен цвет, заголовок, описание и ссылка.
     *
     * @param item объект для сравнения
     * @return true, если объекты равны содержательно, иначе false
     */
    public boolean equalsByContent(@Nullable ReadLaterItem item) {
        return (item != null)
                && (color == item.color)
                && label.equals(item.label)
                && description.equals(item.description)
                && (((imageUrl != null) && imageUrl.equals(item.imageUrl)) // Не null и equals
                    || (imageUrl == item.imageUrl));                       // null и равны
    }

    /** Два объекта ReadLaterItem равны, если у них одинаковые заголовок, описание, цвет и даты создания, изменения,
     * просмотра, ссылка на картинку и внешний идентификатор.
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
                && remoteId == thatObject.remoteId
                && label.equals(thatObject.label)
                && description.equals(thatObject.description);
        if (imageUrl != null) {
            equality = equality && imageUrl.equals(thatObject.imageUrl);
        } else {
            equality = equality && (thatObject.imageUrl == null);
        }
        return equality;
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
        return 31 * result + remoteId;
    }

    /** Возвращает строковое представление ReadLaterItem.
     *
     * @return Строковое представление состоит из всех полей объекта, где:
     *                  color - цвет в HEX,
     *                  даты формата yyyy-MM-dd'T'hh:mm:ss.SSSZZZZZ,
     *                  \nimage: нет, если imageUrl == null
     *                  \nremoteId: нет, если remoteId == 0
     *
     *              Например:
     *              Примерный заголовок!
     *              Длинное описание, возможно
     *              многострочное
     *              (#FFFFFF)
     *              C: 2017-05-08T15:28:01+0400
     *              M: 2017-05-08T15:28:01+0400
     *              V: 2017-05-08T15:28:01+0400
     *              image: https://s-media-cache-ak0.pinimg.com/736x/92/9d/3d/929d3d9f76f406b5ac6020323d2d32dc.jpg
     *              remoteId: 1010
     */
    @Override
    public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        StringBuilder result = new StringBuilder(String.format("%s%n%s%n(%s)%nC: %s%nM: %s%nV: %s",
                label,
                description,
                String.format(FORMAT_COLOR, Integer.toString(color, HEX)),
                dateFormatter.format(dateCreated),
                dateFormatter.format(dateModified),
                dateFormatter.format(dateViewed)));
        if (imageUrl != null) {
            result.append(String.format("%nimage: %s", imageUrl.toString()));
        }
        if (remoteId != 0) {
            result.append(String.format("%nremoteId: %s", String.valueOf(remoteId)));
        }
        return result.toString();
    }

}
