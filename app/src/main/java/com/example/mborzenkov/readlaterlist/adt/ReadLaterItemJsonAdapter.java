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
    private static final String FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
    /** Формат цвета в JSON. */
    private static final String FORMAT_COLOR = "#%S";
    /** Формат toString объекта ReadLaterItemJson. */
    private static final String FORMAT_JSON_TOSTRING = "{%n"
            + "  \"id\": \"%s\",%n"
            + "  \"title\": \"%s\",%n"
            + "  \"description\": \"%s\",%n"
            + "  \"color\": \"%s\",%n"
            + "  \"created\": \"%s\",%n"
            + "  \"edited\": \"%s\",%n"
            + "  \"viewed\": \"%s\",%n"
            + "  \"imageUrl\": \"%s\"%n"
            + "}";
    /** Размерность HEX. */
    private static final int HEX = 16;

    /** Тэг ошибки разбора json. */
    private static final String FROM_JSON_ERROR_TAG = "FROM_JSON";

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
        json.id             = String.valueOf(item.getRemoteId());
        json.title          = item.getLabel();
        json.description    = item.getDescription();
        json.color          = String.format(FORMAT_COLOR, Integer.toHexString(item.getColor()));
        json.created        = dateFormatter.format(item.getDateCreated());
        json.edited         = dateFormatter.format(item.getDateModified());
        json.viewed         = dateFormatter.format(item.getDateViewed());
        json.imageUrl       = item.getImageUrl();
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
            ReadLaterItem.Builder resultBuilder = new ReadLaterItem.Builder(json.title)
                    .color((int) Long.parseLong(json.color.substring(1), HEX))
                    .dateCreated(dateFormatter.parse(json.created).getTime())
                    .dateModified(dateFormatter.parse(json.edited).getTime())
                    .dateViewed(dateFormatter.parse(json.viewed).getTime());
            if (json.id != null) {
                resultBuilder.remoteId(Integer.parseInt(json.id));
            }
            if (json.description != null) {
                resultBuilder.description(json.description);
            }
            if (json.imageUrl != null) {
                resultBuilder.imageUrl(json.imageUrl);
            }
            result = resultBuilder.build();
        } catch (ParseException e) {
            Log.e(FROM_JSON_ERROR_TAG, String.format("%s %s%n%s %s",
                    "dateFormatter.parse error:",
                    e.toString(),
                    "ReadLaterItemJson: ",
                    json.toString()));
        } catch (NumberFormatException e) {
            Log.e(FROM_JSON_ERROR_TAG, String.format("%s %s%n%s %s",
                    "color -> Integer error: ",
                    e.toString(),
                    "ReadLaterItemJson: ",
                    json.toString()));
        } catch (NullPointerException e) { // NPE тут может быть
            Log.e(FROM_JSON_ERROR_TAG, String.format("%s %s%n%s %s",
                    "NULL in JSON error: ",
                    e.toString(),
                    "ReadLaterItemJson: ",
                    json.toString()));
        } catch (IllegalArgumentException e) {
            Log.e(FROM_JSON_ERROR_TAG, String.format("%s %s%n%s %s",
                    "JSON parse error: ",
                    e.toString(),
                    "ReadLaterItemJson: ",
                    json.toString()));
        }
        return result;
    }

    private static class ReadLaterItemJson {
        private String id;
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
            return String.format(Locale.US, FORMAT_JSON_TOSTRING,
                    id, title, description, color, created, edited, viewed, imageUrl);
        }
    }

}
