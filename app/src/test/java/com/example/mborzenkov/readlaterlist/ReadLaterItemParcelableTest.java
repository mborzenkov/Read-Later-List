package com.example.mborzenkov.readlaterlist;

import android.graphics.Color;
import android.os.Parcel;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ReadLaterItemParcelableTest {

    private final String normalLabel = "Заголовок";
    private final String normalDescription = "Описание";
    private final int normalColor = Color.RED;

    @Test
    public void testParcelable() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor);
        ReadLaterItemParcelable itemParcelable = new ReadLaterItemParcelable(item);
        Parcel parcel = Parcel.obtain();
        itemParcelable.writeToParcel(parcel, itemParcelable.describeContents());
        parcel.setDataPosition(0);

        ReadLaterItem itemFromParcel = ReadLaterItemParcelable.CREATOR.createFromParcel(parcel).getItem();
        assertEquals(item, itemFromParcel);
    }

}
