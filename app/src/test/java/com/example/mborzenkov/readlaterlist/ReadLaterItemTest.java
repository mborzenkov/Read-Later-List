package com.example.mborzenkov.readlaterlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

/** Тестирует ReadLaterItem. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости"
public class ReadLaterItemTest {

    /*
     * Стратегия тестирования
     *
     * label.length: 1, >1
     * description: "", "  ", непустая
     * description.length: 1, >1
     * description.contains: "\n
     * color: <0, 0, >0, TRANSPARENT
     * dateCreated: <0, 0, >0>сейчас, >сейчас
     * dateModified: <0, 0, >0>сейчас, >сейчас
     * dateViewed: <0, 0, >0>сейчас, >сейчас
     * imageUrl: "", null, !картинка, картинка, недоступная ссылка, короткая ссылка, ftp
     *
     */

    private static final String normalLabel = "Заголовок";
    private static final String singleCharLabel = "Я";
    private static final String normalDescription = "Описание";
    private static final String emptyDescription = "";
    private static final String onlySpaceDescription = " ";
    private static final String multilineDescription = "Описание\nНа несколько строк\nВот так вот";
    private static final int normalColor = Color.RED;
    private static final int zeroColor = 0;
    private static final int negativeColor = -1;
    private static final long longTimeAgo = -100000;
    private static final long zeroTime = 0;
    private static final long currentTime = System.currentTimeMillis();
    private static final long futureTime = currentTime + 10000000;
    private static final String emptyUrl = "";
    private static final String nullUrl = null;
    private static final String notImageUrl = "https://www.google.ru/";
    private static final String normalImageUrl = "http://i.imgur.com/TyCSG9A.png";
    private static final String unreachableUrl = "http://a.ru";
    private static final String ftpUrl = "ftp://somebody.once.told.me.ru";

    /* Покрывает label.length: >1
     *           description: непустая
     *           description.length > 1
     *           color: >0
     *           dateCreated: >0>сейчас
     *           dateModified: >0>сейчас
     *           dateViewed: >0>сейчас
     *           imageUrl: картинка
     */
    @Test
    public void normalItemTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
        assertEquals(currentTime, item.getDateCreated());
        assertEquals(currentTime, item.getDateModified());
        assertEquals(currentTime, item.getDateViewed());
    }

    /* Покрывает description: "", " "
     *           description.length: 1
     *           color: >0
     */
    @Test
    public void emptyDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, emptyDescription, normalColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(emptyDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());

        item = new ReadLaterItem(normalLabel, onlySpaceDescription, normalColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(onlySpaceDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }

    /* Покрывает description.contains: "\n */
    @Test
    public void multilineDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, multilineDescription, normalColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(multilineDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }


    /* Покрывает color: <0, 0 */
    @Test
    public void colorTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, Color.RED,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());

        item = new ReadLaterItem(normalLabel, normalDescription, zeroColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(zeroColor, item.getColor());

        item = new ReadLaterItem(normalLabel, normalDescription, negativeColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(negativeColor, item.getColor());
    }

    /* Покрывает label.length: 1 */
    @Test
    public void singleCharacterLabelTest() {
        ReadLaterItem item = new ReadLaterItem(singleCharLabel, normalDescription, normalColor,
                currentTime, currentTime, currentTime, normalImageUrl);
        assertEquals(singleCharLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }

    @Test
    public void testEquals() {
        ReadLaterItem item1 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, futureTime, normalImageUrl);
        ReadLaterItem item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, futureTime, normalImageUrl);
        assertTrue(item1.equals(item2));
        assertTrue(item1.hashCode() == item2.hashCode());

        item2 = new ReadLaterItem(normalLabel + "b", normalDescription, normalColor,
                zeroTime, currentTime, futureTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription + "a", normalColor,
                zeroTime, currentTime, futureTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor + 1,
                zeroTime, currentTime, futureTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                currentTime, currentTime, futureTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, zeroTime, futureTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, zeroTime, normalImageUrl);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, futureTime, ftpUrl);
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testToString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ", Locale.US);
        String assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s%nimage: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, 16),
                dateFormatter.format(zeroTime),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime),
                normalImageUrl);

        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, currentTime, normalImageUrl);
        assertEquals(assertedString, item.toString());

        // без url
        assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, 16),
                dateFormatter.format(zeroTime),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime));
        item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, currentTime, nullUrl);
        assertEquals(assertedString, item.toString());
        item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, currentTime, currentTime, emptyUrl);
        assertEquals(assertedString, item.toString());
    }

    /* Покрывает dateCreated: <0
     *           dateModified: <0
     *           dateViewed: <0
     */
    @Test
    public void testNegativeDates() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                longTimeAgo, longTimeAgo, longTimeAgo, normalImageUrl);
        assertEquals(longTimeAgo, item.getDateCreated());
        assertEquals(longTimeAgo, item.getDateModified());
        assertEquals(longTimeAgo, item.getDateViewed());
    }

    /* Покрывает dateCreated: 0
     *           dateModified: 0
     *           dateViewed: 0
     */
    @Test
    public void testZeroDates() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                zeroTime, zeroTime, zeroTime, normalImageUrl);
        assertEquals(zeroTime, item.getDateCreated());
        assertEquals(zeroTime, item.getDateModified());
        assertEquals(zeroTime, item.getDateViewed());
    }

    /* Покрывает dateCreated: >сейчас
     *           dateModified: >сейчас
     *           dateViewed: >сейчас
     */
    @Test
    public void testFutureDates() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, normalImageUrl);
        assertEquals(futureTime, item.getDateCreated());
        assertEquals(futureTime, item.getDateModified());
        assertEquals(futureTime, item.getDateViewed());
    }

    /* Покрывает imageUrl: "", null */
    @Test
    public void testImageUrlEmpty() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, emptyUrl);
        assertEquals(emptyUrl, item.getImageUrl());

        item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, nullUrl);
        assertEquals("", item.getImageUrl());
    }

    /* Покрывает imageUrl: !картинка, недоступная ссылка, ftp */
    @Test
    public void testImageUrlNotPicture() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, notImageUrl);
        assertEquals(notImageUrl, item.getImageUrl());

        item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, unreachableUrl);
        assertEquals(unreachableUrl, item.getImageUrl());

        item = new ReadLaterItem(normalLabel, normalDescription, normalColor,
                futureTime, futureTime, futureTime, ftpUrl);
        assertEquals(ftpUrl, item.getImageUrl());
    }

}