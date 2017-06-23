package com.example.mborzenkov.readlaterlist.adt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.CustomColor.Hsv;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


/** Тестирует ReadLaterItem. */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class CustomColorTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    private static final int HEX = 16;
    private static final int HSV_SIZE = 3;
    private static final float FLOAT_PRECISION = 0.000001f;

    @Test
    public void testGetTransparent() {
        assertTrue(CustomColor.getTransparent().isTransparent());
    }

    @Test
    public void testColorsRgb() {
        CustomColor someColor = new CustomColor(Color.RED);
        assertEquals(Color.RED, someColor.getColorRgb());

        CustomColor zeroColor = new CustomColor(0);
        assertEquals(0, zeroColor.getColorRgb());

        CustomColor negativeColor = new CustomColor(-1);
        assertEquals(-1, negativeColor.getColorRgb());
    }

    @Test
    public void testColorsHsv() {
        CustomColor someColor = new CustomColor(Color.BLUE);
        assertEquals(Color.BLUE, someColor.getColorRgb());
        float[] hsvColorBlue = new float[HSV_SIZE];
        Color.colorToHSV(Color.BLUE, hsvColorBlue);
        assertTrue(Math.abs(hsvColorBlue[Hsv.HUE.index()] - someColor.getHsvAttr(Hsv.HUE)) < FLOAT_PRECISION);
        assertTrue(Math.abs(hsvColorBlue[Hsv.SAT.index()] - someColor.getHsvAttr(Hsv.SAT)) < FLOAT_PRECISION);
        assertTrue(Math.abs(hsvColorBlue[Hsv.VAL.index()] - someColor.getHsvAttr(Hsv.VAL)) < FLOAT_PRECISION);
    }

    @Test
    public void testEquals() {
        CustomColor colorBlue1 = new CustomColor(Color.BLUE);
        CustomColor colorBlue2 = new CustomColor(Color.BLUE);
        CustomColor colorRed = new CustomColor(Color.RED);
        assertEquals(colorBlue1, colorBlue2);
        assertEquals(colorBlue1.hashCode(), colorBlue2.hashCode());
        assertFalse(colorBlue1.equals(colorRed));
        assertFalse(colorBlue2.equals(colorRed));
    }

    @Test
    public void testToString() {
        CustomColor someColor = new CustomColor(Color.BLUE);
        assertEquals(String.format("#%s", Integer.toString(Color.BLUE, HEX)), someColor.toString());
    }

}