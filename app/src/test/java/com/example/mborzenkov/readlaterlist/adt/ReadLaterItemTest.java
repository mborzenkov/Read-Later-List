package com.example.mborzenkov.readlaterlist.adt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

/** Тестирует ReadLaterItem. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ReadLaterItemTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    /** Размерность HEX. */
    private static final int HEX = 16;
    /** Количество миллисекунд в секундах. */
    private static final int MILLIS = 1000;

    /*
     * Стратегия тестирования
     *
     * Basic
     *      label.length: 1, >1
     *      description: "", "  ", непустая
     *      description.length: 1, >1
     *      description.contains: "\n
     *      color: <0, 0, >0, TRANSPARENT
     *      dateCreated: <0, 0, >0>сейчас, >сейчас
     *      dateModified: <0, 0, >0>сейчас, >сейчас
     *      dateViewed: <0, 0, >0>сейчас, >сейчас
     *      imageUrl: "", !картинка, картинка, недоступная ссылка, короткая ссылка, ftp
     *      remoteId: >0, 0
     *
     * Builder
     *      description, color, dateCreated, dateModified, dateViewed, imageUrl, remoteId: не заданы
     *      Builder.allDates
     *      из объекта ReadLaterItem
     *
     * Throws
     *      label: пустой, многострочный
     *      imageUrl: неправильно сформированная ссылка
     *      remoteId: <0
     *
     */

    private static final String normalLabel = "Заголовок";
    private static final String singleCharLabel = "Я";
    private static final String normalDescription = "Описание";
    private static final String onlySpaceDescription = " ";
    private static final String multilineDescription = "Описание\nНа несколько строк\nВот так вот";
    private static final int normalColor = Color.RED;
    private static final int zero = 0;
    private static final int negativeColor = -1;
    private static final long longTimeAgo = -100000;
    private static final long currentTime = MILLIS * (System.currentTimeMillis() / MILLIS);
    private static final long futureTime = currentTime + 10000000;
    private static final String notImageUrl = "https://www.google.ru/";
    private static final String normalImageUrl = "http://i.imgur.com/TyCSG9A.png";
    private static final String unreachableUrl = "http://a.ru";
    private static final String ftpUrl = "ftp://somebody.once.told.me.ru";
    private static final String malformedUrl = "htt://www.google.ru/";
    private static final int normalRemoteId = 1010;
    private static final int illegalRemoteId = -1;

    /* Покрывает label.length: >1
     *           description: непустая
     *           description.length > 1
     *           color: >0
     *           dateCreated: >0>сейчас
     *           dateModified: >0>сейчас
     *           dateViewed: >0>сейчас
     *           imageUrl: картинка
     *           remoteId: >0
     */
    @Test
    public void normalItemTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(currentTime)
                .dateModified(currentTime)
                .dateViewed(currentTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
        assertEquals(currentTime, item.getDateCreated());
        assertEquals(currentTime, item.getDateModified());
        assertEquals(currentTime, item.getDateViewed());
        assertEquals(normalImageUrl, item.getImageUrl());
        assertEquals(normalRemoteId, item.getRemoteId());
    }

    /* Покрывает description, color, dateCreated, dateModified, dateViewed, imageUrl, remoteId: не заданы
     */
    @Test
    public void shortBuilderTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertTrue(item.getDescription().isEmpty());
        assertEquals(item.getDateCreated(), item.getDateModified());
        assertEquals(item.getDateCreated(), item.getDateViewed());
        assertTrue(item.getImageUrl().isEmpty());
    }

    /* Покрывает Builder.allDates
     */
    @Test
    public void allDatesBuilderTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .allDates(currentTime)
                .build();
        assertEquals(currentTime, item.getDateCreated());
        assertEquals(currentTime, item.getDateModified());
        assertEquals(currentTime, item.getDateViewed());
    }

    /* Покрывает description: "", " "
     *           description.length: 1
     */
    @Test
    public void emptyDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description("")
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertTrue(item.getDescription().isEmpty());

        item = new ReadLaterItem.Builder(normalLabel)
                .description(onlySpaceDescription)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertTrue(item.getDescription().trim().isEmpty());
    }

    /* Покрывает description.contains: "\n */
    @Test
    public void multilineDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(multilineDescription)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertEquals(multilineDescription, item.getDescription());
    }

    /* Покрывает color: <0, 0 */
    @Test
    public void colorTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .color(normalColor)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalColor, item.getColor());

        item = new ReadLaterItem.Builder(normalLabel)
                .color(zero)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertEquals(zero, item.getColor());

        item = new ReadLaterItem.Builder(normalLabel)
                .color(negativeColor)
                .build();
        assertEquals(normalLabel, item.getLabel());
        assertEquals(negativeColor, item.getColor());
    }

    /* Покрывает label.length: 1 */
    @Test
    public void singleCharacterLabelTest() {
        ReadLaterItem item = new ReadLaterItem.Builder(singleCharLabel)
                .build();
        assertEquals(singleCharLabel, item.getLabel());
    }

    /* Покрывает equals
     *           remoteId: null
     */
    @Test
    public void testEquals() {
        ReadLaterItem item1 = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(futureTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();

        ReadLaterItem item2 = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(futureTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        item1 = new ReadLaterItem.Builder(normalLabel).build();
        item2 = new ReadLaterItem.Builder(normalLabel).build();
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        item1 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .build();
        item2 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .build();
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        item2 = new ReadLaterItem.Builder(normalLabel + "b")
                .allDates(futureTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription + "a")
                .allDates(futureTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .color(normalColor)
                .allDates(futureTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .dateCreated(currentTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .dateModified(currentTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .dateViewed(currentTime)
                .build();
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .imageUrl(ftpUrl)
                .build();
        assertFalse(item1.equals(item2));

        item1 = new ReadLaterItem.Builder(normalLabel).imageUrl(normalImageUrl).allDates(futureTime).build();
        item2 = new ReadLaterItem.Builder(normalLabel).imageUrl("").allDates(futureTime).build();
        assertFalse(item1.equals(item2));

        item1 = new ReadLaterItem.Builder(normalLabel).imageUrl(normalImageUrl).allDates(currentTime).build();
        item2 = new ReadLaterItem.Builder(normalLabel).imageUrl(normalImageUrl).allDates(futureTime).build();
        assertFalse(item1.equals(item2));

        item1 = new ReadLaterItem.Builder(normalLabel).allDates(futureTime).build();
        item2 = new ReadLaterItem.Builder(normalLabel).allDates(futureTime).dateModified(currentTime).build();
        assertFalse(item1.equals(item2));
        item2 = new ReadLaterItem.Builder(normalLabel).allDates(futureTime).dateCreated(currentTime).build();
        assertFalse(item1.equals(item2));
        item2 = new ReadLaterItem.Builder(normalLabel).allDates(futureTime).dateViewed(currentTime).build();
        assertFalse(item1.equals(item2));
        item2 = new ReadLaterItem.Builder(normalLabel).allDates(futureTime).remoteId(normalRemoteId).build();
        assertFalse(item1.equals(item2));

        item1 = new ReadLaterItem.Builder(normalLabel).remoteId(0).build();
        item2 = new ReadLaterItem.Builder(normalLabel).build();
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        Object obj = new Object();
        assertFalse(item1.equals(obj));
    }

    @Test
    public void testToString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.US);

        // +url -remoteId
        String assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s%nimage: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, HEX),
                dateFormatter.format(zero),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime),
                normalImageUrl);

        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(currentTime)
                .imageUrl(normalImageUrl)
                .build();
        assertEquals(assertedString, item.toString());

        // -url -remoteId
        assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, HEX),
                dateFormatter.format(zero),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime));
        item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(currentTime)
                .build();
        assertEquals(assertedString, item.toString());

        // +url +remoteId
        assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s%nimage: %s%nremoteId: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, HEX),
                dateFormatter.format(zero),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime),
                normalImageUrl,
                normalRemoteId);
        item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(currentTime)
                .imageUrl(normalImageUrl)
                .remoteId(normalRemoteId)
                .build();
        assertEquals(assertedString, item.toString());

        // -url +remoteId
        assertedString = String.format("%s%n%s%n(#%s)%nC: %s%nM: %s%nV: %s%nremoteId: %s",
                normalLabel,
                normalDescription,
                Integer.toString(normalColor, HEX),
                dateFormatter.format(zero),
                dateFormatter.format(currentTime),
                dateFormatter.format(currentTime),
                normalRemoteId);
        item = new ReadLaterItem.Builder(normalLabel)
                .description(normalDescription)
                .color(normalColor)
                .dateCreated(zero)
                .dateModified(currentTime)
                .dateViewed(currentTime)
                .remoteId(normalRemoteId)
                .build();
        assertEquals(assertedString, item.toString());
    }

    /* Покрывает dateCreated: <0
     *           dateModified: <0
     *           dateViewed: <0
     */
    @Test
    public void testNegativeDates() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .allDates(longTimeAgo)
                .build();
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
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .allDates(zero)
                .build();
        assertEquals(zero, item.getDateCreated());
        assertEquals(zero, item.getDateModified());
        assertEquals(zero, item.getDateViewed());
    }

    /* Покрывает dateCreated: >сейчас
     *           dateModified: >сейчас
     *           dateViewed: >сейчас
     */
    @Test
    public void testFutureDates() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .allDates(futureTime)
                .build();
        assertEquals(futureTime, item.getDateCreated());
        assertEquals(futureTime, item.getDateModified());
        assertEquals(futureTime, item.getDateViewed());
    }

    /* Покрывает imageUrl: ""  */
    @Test
    public void testImageUrlEmpty() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .imageUrl("")
                .build();
        assertEquals("", item.getImageUrl());
    }

    /* Покрывает imageUrl: !картинка, недоступная ссылка, ftp */
    @Test
    public void testImageUrlNotPicture() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .imageUrl(notImageUrl)
                .build();
        assertEquals(notImageUrl, item.getImageUrl());

        item = new ReadLaterItem.Builder(normalLabel)
                .imageUrl(unreachableUrl)
                .build();
        assertEquals(unreachableUrl, item.getImageUrl());

        item = new ReadLaterItem.Builder(normalLabel)
                .imageUrl(ftpUrl)
                .build();
        assertEquals(ftpUrl, item.getImageUrl());
    }

    /* Покрывает label: пустой */
    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testLabelEmpty() {
        ReadLaterItem item = new ReadLaterItem.Builder("").build();
        // throws exception
        assertEquals(null, item);
    }

    /* Покрывает label: многострочный */
    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testLabelMultiline() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel + "\n" + normalLabel).build();
        // throws exception
        assertEquals(null, item);
    }

    /* Покрывает imageUrl: неправильно сформированная ссылка */
    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testImageUrlMalformed() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .imageUrl(malformedUrl)
                .build();
        // throws exception
        assertEquals(null, item);
    }

    /* Покрывает remoteId: <0 */
    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testRemoteIdIllegal() {
        //noinspection Range
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel)
                .remoteId(illegalRemoteId)
                .build();
        // throws exception
        assertEquals(null, item);
    }

    /* Покрывает Builder: из объекта ReadLaterItem */
    @Test
    public void testBuilderBasedOnObject() {
        ReadLaterItem item = new ReadLaterItem.Builder(normalLabel).allDates(currentTime).build();
        ReadLaterItem itemFromItem = new ReadLaterItem.Builder(item).build();
        assertEquals(item, itemFromItem);
    }

    @Test
    public void testEqualsByContent() {
        ReadLaterItem item1 = new ReadLaterItem.Builder(normalLabel).color(normalColor).allDates(currentTime)
                .remoteId(normalRemoteId).description(normalDescription).imageUrl(normalImageUrl).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder(item1).allDates(currentTime + MILLIS)
                .remoteId(normalRemoteId + 1).build();
        assertTrue(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).label(normalLabel + "1").build();
        assertFalse(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).color(normalColor + 1).build();
        assertFalse(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).imageUrl("").build();
        assertFalse(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).description(normalDescription + "12").build();
        assertFalse(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).imageUrl("").build();
        assertFalse(item1.equalsByContent(item2));
        item2 = new ReadLaterItem.Builder(item1).imageUrl(ftpUrl).build();
        assertFalse(item1.equalsByContent(item2));
        assertFalse(item1.equalsByContent(null));
        item1 = new ReadLaterItem.Builder(item1).imageUrl("").build();
        item2 = new ReadLaterItem.Builder(item1).build();
        assertTrue(item1.equalsByContent(item2));
    }

}