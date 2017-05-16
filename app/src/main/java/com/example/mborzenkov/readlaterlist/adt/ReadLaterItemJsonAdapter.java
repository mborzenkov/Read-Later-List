package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Адаптер для превращения объекта ReadLaterItem в JSON. */
public class ReadLaterItemJsonAdapter {

    /** Формат дат в JSON. */
    private static final String FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /** Формат цвета в JSON. */
    private static final String FORMAT_COLOR = "#%S";

    /** Конструктор по умолчанию. */
    public ReadLaterItemJsonAdapter() { }

    /** Превращает полученный объект ReadLaterItem в объект ReadLaterItemJson.
     *
     * @param item объект ReadLaterItem, который нужно преобразовать
     * @return объект ReadLaterItemJson
     */
    @Keep // Используется moshi с помощью аннотации @ToJson
    @SuppressWarnings("unused")
    @ToJson
    public ReadLaterItemJson toJson(@NonNull ReadLaterItem item) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        ReadLaterItemJson json = new ReadLaterItemJson();
        json.title = item.getLabel();
        json.description = item.getDescription();
        json.color = String.format(FORMAT_COLOR, Integer.toHexString(item.getColor()));
        json.created = dateFormatter.format(item.getDateCreated());
        json.edited = dateFormatter.format(item.getDateModified());
        json.viewed = dateFormatter.format(item.getDateViewed());
        json.imageUrl = item.getImageUrl();
        return json;
    }

    /** Превращает полученный объект ReadLaterItemJson в объект ReadLaterItem.
     * Может вернуть null, если возникла ошибка при преобразовании даты из строки.
     * При разборе файла из множества объектов стандартным jsonAdapter.fromJson(String), в результирующую коллекцию
     *      могут быть добавлены null.
     *
     * @param json объект ReadLaterItemJson, который нужно преобразовать
     * @return объект ReadLaterItem
     */
    @Keep // Используется moshi с помощью аннотации @FromJson
    @SuppressWarnings("unused")
    @FromJson
    public @Nullable ReadLaterItem fromJson(@NonNull ReadLaterItemJson json) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        ReadLaterItem result = null;
        try {
            result = new ReadLaterItem(
                    json.title,
                    json.description,
                    (int) Long.parseLong(json.color.substring(1), 16),
                    dateFormatter.parse(json.created).getTime(),
                    dateFormatter.parse(json.edited).getTime(),
                    dateFormatter.parse(json.viewed).getTime(),
                    json.imageUrl);
        } catch (ParseException e) {
            Log.e("Parse error", String.format("%s %s%n%s %s",
                    "Ошибка разбора дат из ReadLaterItemJson:",
                    e.toString(),
                    "Объект ReadLaterItemJson:",
                    json.toString()));
        } catch (NumberFormatException e) {
            Log.e("Parse error", String.format("%s %s%n%s %s",
                    "Ошибка конвертации color в int из ReadLaterItemJson: ",
                    e.toString(),
                    "Объект ReadLaterItemJson:",
                    json.toString()));
        }
        return result;
    }

    private static class ReadLaterItemJson {
        private String title;
        private String description;
        private String color;
        private String created;
        private String edited;
        private String viewed;
        private String imageUrl;
        // private String extra; // Зарезервировано API

        private ReadLaterItemJson() { }

        @Override
        public String toString() {
            return String.format("title:%s, description:%s, color:%s, created:%s, edited:%s, viewed:%s, imageUrl: %s",
                    title, description, color, created, edited, viewed, imageUrl);
        }
    }

}
