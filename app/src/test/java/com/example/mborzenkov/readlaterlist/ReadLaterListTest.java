package com.example.mborzenkov.readlaterlist;

import android.graphics.Color;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Тестирует ReadLaterItem
 */
public class ReadLaterListTest {

    /*
     * Стратегия тестирования
     *
     * label.length: 1, >1
     * description: "", "  ", непустая
     * description.length: 1, >1
     * description.contains: "\n
     * color: <0, 0, >0, TRANSPARENT
     *
     */

    private final String normalLabel = "Заголовок";
    private final String singleCharLabel = "Я";
    private final String normalDescription = "Описание";
    private final String emptyDescription = "";
    private final String onlySpaceDescription = " ";
    private final String multilineDescription = "Описание\nНа несколько строк\nВот так вот";
    private final int normalColor = Color.RED;
    private final int zeroColor = 0;
    private final int negativeColor = -1;

    /**
     * Покрывает label.length: >1
     *           description: непустая
     *           description.length > 1
     *           color: >0
     */
    @Test
    public void normalItemTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }

    /**
     * Покрывает description: "", " "
     *           description.length: 1
     *           color: >0
     */
    @Test
    public void emptyDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, emptyDescription, normalColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(emptyDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());

        item = new ReadLaterItem(normalLabel, onlySpaceDescription, normalColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(onlySpaceDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }

    /**
     * Покрывает description.contains: "\n
     */
    @Test
    public void multilineDescriptionTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, multilineDescription, normalColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(multilineDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }


    /**
     * Покрывает color: <0, 0
     */
    @Test
    public void colorTest() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, Color.RED);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());

        item = new ReadLaterItem(normalLabel, normalDescription, zeroColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(zeroColor, item.getColor());

        item = new ReadLaterItem(normalLabel, normalDescription, negativeColor);
        assertEquals(normalLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(negativeColor, item.getColor());
    }

    /**
     * Покрывает label.length: 1
     */
    @Test
    public void singleCharacterLabelTest() {
        ReadLaterItem item = new ReadLaterItem(singleCharLabel, normalDescription, normalColor);
        assertEquals(singleCharLabel, item.getLabel());
        assertEquals(normalDescription, item.getDescription());
        assertEquals(normalColor, item.getColor());
    }

    @Test
    public void testEquals() {
        ReadLaterItem item1 = new ReadLaterItem(normalLabel, normalDescription, normalColor);
        ReadLaterItem item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor);
        assertTrue(item1.equals(item2));
        assertTrue(item1.hashCode() == item2.hashCode());

        item2 = new ReadLaterItem(normalLabel + "b", normalDescription, normalColor);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription + "a", normalColor);
        assertFalse(item1.equals(item2));

        item2 = new ReadLaterItem(normalLabel, normalDescription, normalColor + 1);
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testToString() {
        ReadLaterItem item = new ReadLaterItem(normalLabel, normalDescription, normalColor);
        assertEquals(normalLabel + "\n" + normalDescription + "\n" + "(Цвет: " + String.format("#%06X", (0xFFFFFF & normalColor)) + ")", item.toString());
    }
}