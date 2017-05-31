package com.example.mborzenkov.readlaterlist.adt;

import static org.junit.Assert.assertEquals;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Тестирует ReadLaterItemParcelable. */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterItemParcelableTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    /** Количество миллисекунд в секундах. */
    private static final int MILLIS = 1000;

    private static final String normalLabel = "Заголовок";
    private static final String normalDescription = "Описание";
    private static final int normalColor = Color.RED;
    private static final long currentTime = MILLIS * (System.currentTimeMillis() / MILLIS);
    private static final String normalImageUrl = "http://i.imgur.com/TyCSG9A.png";
    private static final int normalRemoteId = 12345;

    private static Parcel sParcel;

    /** Создает Parcel. */
    @Before
    public void initParcel() {
        sParcel = Parcel.obtain();
    }

    /** Уничтожает Parcel. */
    @After
    public void recycleParcel() {
        if (sParcel != null) {
            sParcel.recycle();
        }
    }

    @Test
    public void testParcelable() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .allDates(currentTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();
        ReadLaterItemParcelable itemParcelable = new ReadLaterItemParcelable(item);
        itemParcelable.writeToParcel(sParcel, itemParcelable.describeContents());
        sParcel.setDataPosition(0);

        ReadLaterItem itemFromParcel = ReadLaterItemParcelable.CREATOR.createFromParcel(sParcel).getItem();
        assertEquals(item, itemFromParcel);
    }

    @Test
    public void testParcelableEmptyUrl() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .allDates(currentTime)
                .imageUrl("")
                .remoteId(normalRemoteId)
                .build();
        ReadLaterItemParcelable itemParcelable = new ReadLaterItemParcelable(item);
        itemParcelable.writeToParcel(sParcel, itemParcelable.describeContents());
        sParcel.setDataPosition(0);

        ReadLaterItem itemFromParcel = ReadLaterItemParcelable.CREATOR.createFromParcel(sParcel).getItem();
        assertEquals(item, itemFromParcel);
    }

    @Test
    public void testParcelableArray() {
        ReadLaterItem item1 = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .allDates(currentTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();
        ReadLaterItem item2 = new ReadLaterItem.Builder(normalLabel + "2")
                .description(normalDescription + "2")
                .color(normalColor + 1)
                .allDates(currentTime + MILLIS)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId + 1)
                .build();


        ReadLaterItemParcelable[] itemParcelableArray = ReadLaterItemParcelable.CREATOR.newArray(2);
        itemParcelableArray[0] = new ReadLaterItemParcelable(item1);
        itemParcelableArray[1] = new ReadLaterItemParcelable(item2);
        sParcel.writeParcelableArray(itemParcelableArray, 0);
        sParcel.setDataPosition(0);

        Parcelable[] parcelableArray = sParcel.readParcelableArray(ReadLaterItem.class.getClassLoader());
        ReadLaterItemParcelable[] parcelableArrayFromParcel = Arrays.copyOf(
                parcelableArray, parcelableArray.length, ReadLaterItemParcelable[].class);
        assertEquals(parcelableArrayFromParcel[0].getItem(), item1);
        assertEquals(parcelableArrayFromParcel[1].getItem(), item2);
    }

}
