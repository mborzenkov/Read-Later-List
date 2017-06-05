package com.example.mborzenkov.readlaterlist.adt;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/** Представляет вспомогательный класс для ReadLaterItem, который реализует интерфейс parcelable. */
public final class ReadLaterItemParcelable implements Parcelable {

    /** Константа для использования в Intent в качестве ключа при передаче объекта ReadLaterItem. */
    public static final String KEY_EXTRA = "com.example.mborzenkov.readlaterlist.readlateritem.extra";

    /** Объект ReadLaterItem, соответствующий экземпляру этого вспомогательного класса. */
    private final ReadLaterItem item;

    // Инвариант:
    //      item - объект ReadLaterItem, который нужно поместить в Parcel
    //
    // Безопасность представления:
    //      класс не имеет мутаторов и item объявлен final
    //      при сохранении item во внутреннее поле не выполняется safe-copying, так как ReadLaterItem -
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
            long dateCreated = source.readLong();
            long dateModified = source.readLong();
            long dateViewed = source.readLong();
            String imageUrl = source.readString();
            int remoteId = source.readInt();
            return new ReadLaterItemParcelable(
                    new ReadLaterItem.Builder(label)
                            .description(description)
                            .color(color)
                            .dateCreated(dateCreated)
                            .dateModified(dateModified)
                            .dateViewed(dateViewed)
                            .imageUrl(imageUrl)
                            .remoteId(remoteId)
                            .build());
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
        dest.writeLong(item.getDateCreated());
        dest.writeLong(item.getDateModified());
        dest.writeLong(item.getDateViewed());
        dest.writeString(item.getImageUrl());
        dest.writeInt(item.getRemoteId());
    }

}
