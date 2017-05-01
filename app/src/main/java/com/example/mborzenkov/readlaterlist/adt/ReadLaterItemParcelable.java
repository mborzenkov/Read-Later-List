package com.example.mborzenkov.readlaterlist.adt;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/** Представляет вспомогательный класс для ReadLaterItem, который реализует интерфейс parcelable. */
public final class ReadLaterItemParcelable implements Parcelable {

    // TODO: KEY_UID тут не место. Нужно изменить место хранения константы.

    /** Константа для использования в Intent в качестве ключа при передаче объекта ReadLaterItem. */
    public static final String KEY_EXTRA = "com.example.mborzenkov.readlaterlist.readlateritem.extra";
    /** Константа для использования в Intent в качестве ключа при передаче UID. */
    public static final String KEY_UID = "com.example.mborzenkov.readlaterlist.readlateritem.uid";

    /** Объект ReadLaterItem, соответствующий экземпляру этого вспомогательного класса. */
    private final ReadLaterItem item;

    // Инвариант:
    //      item - объект ReadLaterItem, который нужно поместить в Parcel
    //
    // Безопасность представления:
    //      класс не имеет мутаторов и item объявлен final
    //      при сохранении item во внутреннее поле не выполняется резервного копирования, так как ReadLaterItem -
    //              неизменяемый тип данных
    //
    // Потоковая безопасность:
    //      этот класс потокобезопасен, так как он неизменяемый (вместе со всеми полями)

    public ReadLaterItemParcelable(@NonNull ReadLaterItem item) {
        this.item = item;
    }

    /** Возвращает связанный объект ReadLaterItem. */
    public ReadLaterItem getItem() {
        return item;
    }

    public static final Parcelable.Creator<ReadLaterItemParcelable> CREATOR =
            new Parcelable.Creator<ReadLaterItemParcelable>() {
        @Override
        public ReadLaterItemParcelable createFromParcel(Parcel source) {
            String label = source.readString();
            String description = source.readString();
            int color = source.readInt();
            return new ReadLaterItemParcelable(new ReadLaterItem(label, description, color));
        }

        @Override
        public ReadLaterItemParcelable[] newArray(int size) {
            return new ReadLaterItemParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(item.getLabel());
        dest.writeString(item.getDescription());
        dest.writeInt(item.getColor());
    }

}
