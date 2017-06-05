package com.example.mborzenkov.readlaterlist;

import static org.junit.Assert.assertEquals;

import android.graphics.Color;
import android.os.Parcel;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости"
public class ReadLaterItemParcelableTest {

    private static final String normalLabel = "Заголовок";
    private static final String normalDescription = "Описание";
    private static final int normalColor = Color.RED;
    private static final long currentTime = System.currentTimeMillis();
    private static final String normalImageUrl = "http://i.imgur.com/TyCSG9A.png";

    @Test
    public void testParcelable() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .allDates(currentTime)
                .imageUrl(normalImageUrl)
                .build();
        ReadLaterItemParcelable itemParcelable = new ReadLaterItemParcelable(item);
        Parcel parcel = Parcel.obtain();
        itemParcelable.writeToParcel(parcel, itemParcelable.describeContents());
        parcel.setDataPosition(0);

        ReadLaterItem itemFromParcel = ReadLaterItemParcelable.CREATOR.createFromParcel(parcel).getItem();
        assertEquals(item, itemFromParcel);
    }

    @Test
    public void testParcelableEmptyUrl() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .allDates(currentTime)
                .imageUrl("")
                .build();
        ReadLaterItemParcelable itemParcelable = new ReadLaterItemParcelable(item);
        Parcel parcel = Parcel.obtain();
        itemParcelable.writeToParcel(parcel, itemParcelable.describeContents());
        parcel.setDataPosition(0);

        ReadLaterItem itemFromParcel = ReadLaterItemParcelable.CREATOR.createFromParcel(parcel).getItem();
        assertEquals(item, itemFromParcel);
    }

}
